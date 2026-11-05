package app.pantauizin.domain

import java.time.LocalDate

enum class AlasanJenis { SAKIT, LAIN_LAIN }

enum class BatasJenis { HARI_1, HARI_2, HARI_3, LEBIH_3_HARI }

enum class IzinStatus {
    IZIN_BERJALAN,
    TERLAMBAT_BELUM_KONFIRMASI,
    TERLAMBAT_TERKONFIRMASI,
    TERLAMBAT_KONFIRMASI_HABIS,
    KEMBALI_TEPAT,
    KEMBALI_TERLAMBAT
}

enum class NotifJenis { LEWAT_BATAS, LEWAT_TANGGAL_BARU, PENGINGAT_HARIAN }

data class KonfirmasiFact(val tanggalKembaliBaru: LocalDate?)

data class IzinSnapshot(
    val izinId: Long,
    val batasAwal: LocalDate,
    val tanggalKembali: LocalDate?,
    val konfirmasi: List<KonfirmasiFact>
)

