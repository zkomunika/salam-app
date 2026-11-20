package app.pantauizin.data

import android.net.Uri
import app.pantauizin.data.firebase.FirestoreSyncManager
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.BatasJenis
import app.pantauizin.domain.DeadlineCalculator
import app.pantauizin.domain.PhoneNumberNormalizer
import app.pantauizin.domain.PhoneResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

data class IzinInput(
    val santriId: Long,
    val tanggalPulang: LocalDate,
    val alasanJenis: AlasanJenis,
    val alasanDetail: String,
    val batasJenis: BatasJenis,
    val tanggalBatasPilihan: LocalDate?,
    val noWaWali: String?,
    val pemberiIzin: String? = null
)

sealed interface SaveIzinResult {
    data class Ok(val id: Long) : SaveIzinResult
    data class SantriSudahPunyaIzinAktif(val izinId: Long) : SaveIzinResult
    data object BatasTidakValid : SaveIzinResult
    data object NomorWaTidakValid : SaveIzinResult
}

class IzinRepository(
    private val izinDao: IzinDao,
    private val konfirmasiDao: KonfirmasiDao,
    private val bukti: BuktiStorage,
    private val syncManager: FirestoreSyncManager? = null
) {
    fun observe(): Flow<List<IzinView>> =
        izinDao.observeAll().map { list -> list.map { it.toView() } }

    suspend fun get(id: Long): IzinView? = izinDao.get(id)?.toView()

    suspend fun tambah(input: IzinInput): SaveIzinResult {
        val noWa = when (val phone = PhoneNumberNormalizer.normalize(input.noWaWali)) {
            is PhoneResult.Invalid -> return SaveIzinResult.NomorWaTidakValid
            is PhoneResult.Empty -> null
            is PhoneResult.Valid -> phone.value
        }
        val batasAwal = try {
            DeadlineCalculator.batasAwal(input.tanggalPulang, input.batasJenis, input.tanggalBatasPilihan)
        } catch (_: IllegalArgumentException) {
            return SaveIzinResult.BatasTidakValid
        }

        val active = izinDao.findActiveBySantri(input.santriId)
        if (active != null) {
            return SaveIzinResult.SantriSudahPunyaIzinAktif(active.id)
        }

        val id = izinDao.insert(
            IzinEntity(
                santriId = input.santriId,
                tanggalPulang = input.tanggalPulang,
                alasanJenis = input.alasanJenis,
                alasanDetail = input.alasanDetail.trim(),
                batasJenis = input.batasJenis,
                batasAwal = batasAwal,
                noWaWali = noWa,
                tanggalKembali = null,
                dibuatPada = Instant.now(),
                pemberiIzin = input.pemberiIzin?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
        try {
            izinDao.get(id)?.let { syncManager?.uploadIzin(it) }
        } catch (_: Exception) {}
        return SaveIzinResult.Ok(id)
    }

    suspend fun ubah(id: Long, input: IzinInput): SaveIzinResult {
        val noWa = when (val phone = PhoneNumberNormalizer.normalize(input.noWaWali)) {
            is PhoneResult.Invalid -> return SaveIzinResult.NomorWaTidakValid
            is PhoneResult.Empty -> null
            is PhoneResult.Valid -> phone.value
        }
        val batasAwal = try {
            DeadlineCalculator.batasAwal(input.tanggalPulang, input.batasJenis, input.tanggalBatasPilihan)
        } catch (_: IllegalArgumentException) {
            return SaveIzinResult.BatasTidakValid
        }

        val active = izinDao.findActiveBySantri(input.santriId)
        if (active != null && active.id != id) {
            return SaveIzinResult.SantriSudahPunyaIzinAktif(active.id)
        }

        val current = izinDao.get(id)?.izin ?: return SaveIzinResult.BatasTidakValid
        izinDao.update(
            current.copy(
                santriId = input.santriId,
                tanggalPulang = input.tanggalPulang,
                alasanJenis = input.alasanJenis,
                alasanDetail = input.alasanDetail.trim(),
                batasJenis = input.batasJenis,
                batasAwal = batasAwal,
                noWaWali = noWa
            )
        )
        try {
            izinDao.get(id)?.let { syncManager?.uploadIzin(it) }
        } catch (_: Exception) {}
        return SaveIzinResult.Ok(id)
    }

    suspend fun hapus(id: Long) {
        val current = izinDao.get(id) ?: return
        for (k in current.konfirmasi) {
            k.buktiPath?.let { bukti.hapus(it) }
        }
        izinDao.delete(id)
        try {
            syncManager?.deleteIzin(id)
        } catch (_: Exception) {}
    }

    suspend fun tandaiKembali(id: Long, tanggal: LocalDate) {
        val current = izinDao.get(id)?.izin ?: return
        izinDao.update(current.copy(tanggalKembali = tanggal))
        try {
            izinDao.get(id)?.let { syncManager?.uploadIzin(it) }
        } catch (_: Exception) {}
    }

    suspend fun batalKembali(id: Long) {
        val current = izinDao.get(id)?.izin ?: return
        izinDao.update(current.copy(tanggalKembali = null))
        try {
            izinDao.get(id)?.let { syncManager?.uploadIzin(it) }
        } catch (_: Exception) {}
    }

    suspend fun catatHubungiWa(
        izinId: Long,
        oleh: String?,
        waktu: Instant = Instant.now()
    ) {
        val current = izinDao.get(izinId)?.izin ?: return
        izinDao.update(
            current.copy(
                terakhirWaPada = waktu,
                terakhirWaOleh = oleh?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
        try {
            izinDao.get(izinId)?.let { syncManager?.uploadIzin(it) }
        } catch (_: Exception) {}
    }

    suspend fun tambahKonfirmasi(
        izinId: Long,
        tanggalBaru: LocalDate?,
        catatan: String?,
        buktiUri: Uri?
    ) {
        val path = buktiUri?.let { bukti.salin(it) }
        konfirmasiDao.insert(
            KonfirmasiEntity(
                izinId = izinId,
                tanggalKembaliBaru = tanggalBaru,
                catatan = catatan?.trim()?.takeIf { it.isNotEmpty() },
                buktiPath = path,
                dibuatPada = Instant.now()
            )
        )
        try {
            izinDao.get(izinId)?.let { syncManager?.uploadIzin(it) }
        } catch (_: Exception) {}
    }

    suspend fun recalculateBatasInklusif(): Int {
        val list = izinDao.getAllEntities()
        var updatedCount = 0
        for (item in list) {
            if (item.batasJenis == BatasJenis.HARI_1 || item.batasJenis == BatasJenis.HARI_2 || item.batasJenis == BatasJenis.HARI_3) {
                val newBatas = DeadlineCalculator.batasAwal(item.tanggalPulang, item.batasJenis)
                if (newBatas != item.batasAwal) {
                    val updated = item.copy(batasAwal = newBatas)
                    izinDao.update(updated)
                    try {
                        izinDao.get(item.id)?.let { syncManager?.uploadIzin(it) }
                    } catch (_: Exception) {}
                    updatedCount++
                }
            }
        }
        return updatedCount
    }
}

