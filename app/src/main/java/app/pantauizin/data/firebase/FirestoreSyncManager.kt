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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
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
                    val santriDao = database.santriDao()
                    for (doc in snapshot.documents) {
                        val fsSantri = doc.toObject(FirestoreSantri::class.java) ?: continue
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
                    val izinDao = database.izinDao()
                    val konfirmasiDao = database.konfirmasiDao()

                    for (doc in snapshot.documents) {
                        val fsIzin = doc.toObject(FirestoreIzin::class.java) ?: continue
                        val local = izinDao.get(fsIzin.id)

                        val entity = IzinEntity(
                            id = fsIzin.id,
                            santriId = fsIzin.santriId,
                            tanggalPulang = parseLocalDate(fsIzin.tanggalPulang) ?: LocalDate.now(),
                            alasanJenis = parseAlasan(fsIzin.alasanJenis),
                            alasanDetail = fsIzin.alasanDetail,
                            batasJenis = parseBatas(fsIzin.batasJenis),
                            batasAwal = parseLocalDate(fsIzin.batasAwal) ?: LocalDate.now(),
                            noWaWali = fsIzin.noWaWali,
                            tanggalKembali = parseLocalDate(fsIzin.tanggalKembali),
                            dibuatPada = if (fsIzin.dibuatPada > 0) Instant.ofEpochMilli(fsIzin.dibuatPada) else Instant.now()
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
                            // Cek jika gambar Base64 ada, simpan ke berkas lokal jika belum ada
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
