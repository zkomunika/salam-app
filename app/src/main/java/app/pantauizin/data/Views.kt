package app.pantauizin.data

import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.BatasJenis
import app.pantauizin.domain.IzinSnapshot
import app.pantauizin.domain.KonfirmasiFact
import app.pantauizin.domain.NotifJenis
import java.time.Instant
import java.time.LocalDate

data class KonfirmasiView(
    val id: Long,
    val tanggalKembaliBaru: LocalDate?,
    val catatan: String?,
    val buktiPath: String?,
    val dibuatPada: Instant
)

data class IzinView(
    val id: Long,
    val santriId: Long,
    val nama: String,
    val kamar: String,
    val tanggalPulang: LocalDate,
    val alasanJenis: AlasanJenis,
    val alasanDetail: String,
    val batasJenis: BatasJenis,
    val batasAwal: LocalDate,
    val noWaWali: String?,
    val tanggalKembali: LocalDate?,
    val konfirmasi: List<KonfirmasiView>,
    val pemberiIzin: String? = null
) {
    fun toSnapshot(): IzinSnapshot = IzinSnapshot(
        izinId = id,
        batasAwal = batasAwal,
        tanggalKembali = tanggalKembali,
        konfirmasi = konfirmasi.map { KonfirmasiFact(it.tanggalKembaliBaru) }
    )
}

data class NotifikasiView(
    val id: Long,
    val izinId: Long,
    val nama: String,
    val kamar: String,
    val jenis: NotifJenis,
    val pesan: String,
    val tanggalKey: LocalDate,
    val dibuatPada: Instant,
    val dibaca: Boolean
)

internal fun IzinWithKonfirmasi.toView(): IzinView = IzinView(
    id = izin.id,
    santriId = izin.santriId,
    nama = santri.nama,
    kamar = santri.kamar,
    tanggalPulang = izin.tanggalPulang,
    alasanJenis = izin.alasanJenis,
    alasanDetail = izin.alasanDetail,
    batasJenis = izin.batasJenis,
    batasAwal = izin.batasAwal,
    noWaWali = izin.noWaWali,
    tanggalKembali = izin.tanggalKembali,
    konfirmasi = konfirmasi
        .sortedWith(compareBy<KonfirmasiEntity> { it.dibuatPada }.thenBy { it.id })
        .map { k ->
            KonfirmasiView(
                id = k.id,
                tanggalKembaliBaru = k.tanggalKembaliBaru,
                catatan = k.catatan,
                buktiPath = k.buktiPath,
                dibuatPada = k.dibuatPada
            )
        },
    pemberiIzin = izin.pemberiIzin
)

internal fun NotifikasiWithSantri.toView(): NotifikasiView = NotifikasiView(
    id = notifikasi.id,
    izinId = notifikasi.izinId,
    nama = nama,
    kamar = kamar,
    jenis = notifikasi.jenis,
    pesan = notifikasi.pesan,
    tanggalKey = notifikasi.tanggalKey,
    dibuatPada = notifikasi.dibuatPada,
    dibaca = notifikasi.dibaca
)

