package app.pantauizin.domain

enum class IzinFilter {
    SEMUA,
    BERJALAN,
    TERLAMBAT,
    KEMBALI;

    fun matches(s: IzinStatus): Boolean = when (this) {
        SEMUA -> true
        BERJALAN -> s == IzinStatus.IZIN_BERJALAN
        TERLAMBAT -> s == IzinStatus.TERLAMBAT_BELUM_KONFIRMASI ||
            s == IzinStatus.TERLAMBAT_TERKONFIRMASI ||
            s == IzinStatus.TERLAMBAT_KONFIRMASI_HABIS ||
            s == IzinStatus.KEMBALI_TERLAMBAT
        KEMBALI -> s == IzinStatus.KEMBALI_TEPAT || s == IzinStatus.KEMBALI_TERLAMBAT
    }
}

