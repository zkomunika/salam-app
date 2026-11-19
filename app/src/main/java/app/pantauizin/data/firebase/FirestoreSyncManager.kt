package app.pantauizin.data.firebase

import android.content.Context
import android.util.Log
import app.pantauizin.data.AppDatabase
import app.pantauizin.data.BuktiStorage
import app.pantauizin.data.IzinEntity
import app.pantauizin.data.IzinWithKonfirmasi
import app.pantauizin.data.KonfirmasiEntity
import app.pantauizin.data.SantriEntity
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.BatasJenis
import app.pantauizin.domain.DeadlineCalculator
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate

class FirestoreSyncManager(
    private val context: Context,
    private val database: AppDatabase,
    private val buktiStorage: BuktiStorage,
    private val firestoreProvider: () -> FirebaseFirestore? = {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else null
        } catch (_: Exception) {
            null
        }
    }
) {
    private val firestore: FirebaseFirestore? get() = firestoreProvider()
    private var santriListener: ListenerRegistration? = null
    private var izinListener: ListenerRegistration? = null

    /**
     * Memulai pemantauan perubahan real-time dari Firestore ke database Room lokal.
     */
    fun startRealtimeSync(scope: CoroutineScope) {
        stopRealtimeSync()
        val db = firestore ?: return

        // 1. Sinkronisasi data Santri dari Firestore -> Room
        santriListener = db.collection("santri")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirestoreSync", "Gagal membaca santri dari cloud", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                scope.launch(Dispatchers.IO) {
                    val removedIds = snapshot.documentChanges
                        .filter { it.type == DocumentChange.Type.REMOVED }
                        .mapNotNull { it.document.id.toLongOrNull() }
                    applySantriSnapshot(
                        docs = snapshot.documents,
                        removedIds = removedIds,
                        reconcileMissing = !snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()
                    )
                }
            }

        // 2. Sinkronisasi data Izin & Konfirmasi dari Firestore -> Room
        izinListener = db.collection("izin")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirestoreSync", "Gagal membaca izin dari cloud", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                scope.launch(Dispatchers.IO) {
                    val removedIds = snapshot.documentChanges
                        .filter { it.type == DocumentChange.Type.REMOVED }
                        .mapNotNull { it.document.id.toLongOrNull() }
                    applyIzinSnapshot(
                        docs = snapshot.documents,
                        removedIds = removedIds,
                        reconcileMissing = !snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()
                    )
                }
            }
    }

    /**
     * Menarik data terbaru langsung dari server Firestore dan menyamakan database lokal (termasuk penghapusan).
     */
    suspend fun syncNow(): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase belum terhubung"))
        try {
            val santriSnap = db.collection("santri").get(Source.SERVER).await()
            applySantriSnapshot(
                docs = santriSnap.documents,
                removedIds = emptyList(),
                reconcileMissing = true
            )

            val izinSnap = db.collection("izin").get(Source.SERVER).await()
            applyIzinSnapshot(
                docs = izinSnap.documents,
                removedIds = emptyList(),
                reconcileMissing = true
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("FirestoreSync", "Gagal sinkronisasi manual", e)
            Result.failure(e)
        }
    }

    private suspend fun applySantriSnapshot(
        docs: List<DocumentSnapshot>,
        removedIds: List<Long>,
        reconcileMissing: Boolean
    ) {
        val santriDao = database.santriDao()
        val cloudIds = HashSet<Long>()

        for (doc in docs) {
            val fsSantri = doc.toObject(FirestoreSantri::class.java) ?: continue
            cloudIds.add(fsSantri.id)
            val local = santriDao.getById(fsSantri.id)
            val entity = SantriEntity(
                id = fsSantri.id,
                nama = fsSantri.nama,
                kamar = fsSantri.kamar,
                aktif = fsSantri.aktif
            )
            if (local == null) {
                try {
                    santriDao.insert(entity)
                } catch (_: Exception) {
                    // Ignore conflict if duplicate name
                }
            } else if (local != entity) {
                santriDao.update(entity)
            }
        }

        for (id in removedIds) {
            try {
                santriDao.delete(id)
            } catch (_: Exception) {
                // Ignore if still referenced by active izin
            }
        }

        if (reconcileMissing && docs.isNotEmpty()) {
            val localAll = santriDao.observeAll().first()
            for (local in localAll) {
                if (local.id !in cloudIds) {
                    try {
                        santriDao.delete(local.id)
                    } catch (_: Exception) {
                        // Ignore FK constraint
                    }
                }
            }
        }
    }

    private suspend fun applyIzinSnapshot(
        docs: List<DocumentSnapshot>,
        removedIds: List<Long>,
        reconcileMissing: Boolean
    ) {
        val izinDao = database.izinDao()
        val konfirmasiDao = database.konfirmasiDao()
        val cloudIds = HashSet<Long>()

        for (doc in docs) {
            val fsIzin = doc.toObject(FirestoreIzin::class.java) ?: continue
            cloudIds.add(fsIzin.id)
            val local = izinDao.get(fsIzin.id)

            val tglPulang = parseLocalDate(fsIzin.tanggalPulang) ?: LocalDate.now()
            val bJenis = parseBatas(fsIzin.batasJenis)
            val bAwal = if (bJenis == BatasJenis.HARI_1 || bJenis == BatasJenis.HARI_2 || bJenis == BatasJenis.HARI_3) {
                DeadlineCalculator.batasAwal(tglPulang, bJenis)
            } else {
                parseLocalDate(fsIzin.batasAwal) ?: LocalDate.now()
            }

            val entity = IzinEntity(
                id = fsIzin.id,
                santriId = fsIzin.santriId,
                tanggalPulang = tglPulang,
                alasanJenis = parseAlasan(fsIzin.alasanJenis),
                alasanDetail = fsIzin.alasanDetail,
                batasJenis = bJenis,
                batasAwal = bAwal,
                noWaWali = fsIzin.noWaWali,
                tanggalKembali = parseLocalDate(fsIzin.tanggalKembali),
                dibuatPada = if (fsIzin.dibuatPada > 0) Instant.ofEpochMilli(fsIzin.dibuatPada) else Instant.now(),
                pemberiIzin = fsIzin.pemberiIzin?.trim()?.takeIf { it.isNotEmpty() }
            )

            if (local == null) {
                try {
                    izinDao.insert(entity)
                } catch (_: Exception) {
                    izinDao.update(entity)
                }
            } else if (local.izin != entity) {
                izinDao.update(entity)
            }

            // Sync konfirmasi-konfirmasi
            for (fsKonf in fsIzin.konfirmasiList) {
                var localBuktiPath: String? = null
                if (!fsKonf.buktiBase64.isNullOrBlank()) {
                    localBuktiPath = ImageCompressor.saveBase64ToLocalFile(
                        context = context,
                        base64String = fsKonf.buktiBase64,
                        customFileName = "bukti_konf_${fsKonf.id}.jpg"
                    )
                }

                val konfEntity = KonfirmasiEntity(
                    id = fsKonf.id,
                    izinId = fsIzin.id,
                    tanggalKembaliBaru = parseLocalDate(fsKonf.tanggalKembaliBaru),
                    catatan = fsKonf.catatan,
                    buktiPath = localBuktiPath,
                    dibuatPada = if (fsKonf.dibuatPada > 0) Instant.ofEpochMilli(fsKonf.dibuatPada) else Instant.now()
                )

                try {
                    konfirmasiDao.insert(konfEntity)
                } catch (_: Exception) {
                    // Already exists
                }
            }
        }

        for (id in removedIds) {
            try {
                izinDao.delete(id)
            } catch (_: Exception) {}
        }

        if (reconcileMissing) {
            val localAll = izinDao.observeAll().first()
            for (local in localAll) {
                if (local.izin.id !in cloudIds) {
                    try {
                        izinDao.delete(local.izin.id)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun stopRealtimeSync() {
        santriListener?.remove()
        santriListener = null
        izinListener?.remove()
        izinListener = null
    }

    /**
     * Mengunggah data Santri lokal ke Firestore saat dibuat / diubah.
     */
    suspend fun uploadSantri(santri: SantriEntity) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        val dto = FirestoreSantri(
            id = santri.id,
            nama = santri.nama,
            kamar = santri.kamar,
            aktif = santri.aktif,
            updatedAt = System.currentTimeMillis()
        )
        db.collection("santri").document(santri.id.toString())
            .set(dto, SetOptions.merge())
            .await()
    }

    /**
     * Mengunggah data Izin beserta konfirmasi & bukti (Base64) ke Firestore.
     */
    suspend fun uploadIzin(izinWithKonfirmasi: IzinWithKonfirmasi) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        val izin = izinWithKonfirmasi.izin
        val konfirmasiDtos = izinWithKonfirmasi.konfirmasi.map { konf ->
            var buktiBase64: String? = null
            if (!konf.buktiPath.isNullOrBlank()) {
                val f = buktiStorage.file(konf.buktiPath)
                buktiBase64 = ImageCompressor.compressFileToBase64(f)
            }
            FirestoreKonfirmasi(
                id = konf.id,
                tanggalKembaliBaru = konf.tanggalKembaliBaru?.toString(),
                catatan = konf.catatan,
                buktiBase64 = buktiBase64,
                dibuatPada = konf.dibuatPada.toEpochMilli()
            )
        }

        val dto = FirestoreIzin(
            id = izin.id,
            santriId = izin.santriId,
            tanggalPulang = izin.tanggalPulang.toString(),
            alasanJenis = izin.alasanJenis.name,
            alasanDetail = izin.alasanDetail,
            batasJenis = izin.batasJenis.name,
            batasAwal = izin.batasAwal.toString(),
            noWaWali = izin.noWaWali,
            tanggalKembali = izin.tanggalKembali?.toString(),
            dibuatPada = izin.dibuatPada.toEpochMilli(),
            pemberiIzin = izin.pemberiIzin,
            konfirmasiList = konfirmasiDtos,
            updatedAt = System.currentTimeMillis()
        )

        db.collection("izin").document(izin.id.toString())
            .set(dto, SetOptions.merge())
            .await()
    }

    /**
     * Menghapus izin dari Firestore jika dihapus di lokal.
     */
    suspend fun deleteIzin(id: Long) = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            db.collection("izin").document(id.toString()).delete().await()
        } catch (_: Exception) {}
    }

    /**
     * Migrasi massal seluruh data santri & izin dari Room lokal ke Firestore (Dipanggil oleh Admin).
     */
    suspend fun migrateAllLocalToCloud(
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase belum terhubung"))
        try {
            val allSantri = database.santriDao().observeAll().first()
            val allIzin = database.izinDao().observeAll().first()

            val total = allSantri.size + allIzin.size
            var current = 0

            // Upload all santri
            for (santri in allSantri) {
                uploadSantri(santri)
                current++
                onProgress(current, total)
            }

            // Upload all izin
            for (izin in allIzin) {
                uploadIzin(izin)
                current++
                onProgress(current, total)
            }

            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseLocalDate(s: String?): LocalDate? =
        if (s.isNullOrBlank()) null else try { LocalDate.parse(s) } catch (_: Exception) { null }

    private fun parseAlasan(s: String): AlasanJenis =
        try { AlasanJenis.valueOf(s) } catch (_: Exception) { AlasanJenis.LAIN_LAIN }

    private fun parseBatas(s: String): BatasJenis =
        try { BatasJenis.valueOf(s) } catch (_: Exception) { BatasJenis.HARI_3 }
}
