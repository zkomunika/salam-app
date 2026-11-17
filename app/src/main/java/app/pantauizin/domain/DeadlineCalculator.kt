package app.pantauizin.domain

import java.time.LocalDate

object DeadlineCalculator {
    /**
     * Menghitung batas awal kembali.
     * Batas harian terhitung inklusif sejak tanggal pulang (hari ke-1 = tanggal pulang):
     * HARI_1 = tanggalPulang (1 hari: hari pulang itu sendiri)
     * HARI_2 = tanggalPulang + 1 hari (2 hari: pulang s.d. esok)
     * HARI_3 = tanggalPulang + 2 hari (3 hari: misal pulang tgl 15, batas = tgl 17: 15, 16, 17)
     * LEBIH_3_HARI memakai [tanggalPilihan], yang harus setelah hari ke-3 (tanggalPulang + 2 hari).
     */
    fun batasAwal(
        tanggalPulang: LocalDate,
        jenis: BatasJenis,
        tanggalPilihan: LocalDate? = null
    ): LocalDate = when (jenis) {
        BatasJenis.HARI_1 -> tanggalPulang
        BatasJenis.HARI_2 -> tanggalPulang.plusDays(1)
        BatasJenis.HARI_3 -> tanggalPulang.plusDays(2)
        BatasJenis.LEBIH_3_HARI -> {
            require(tanggalPilihan != null) { "Tanggal kembali wajib diisi untuk batas lebih dari 3 hari" }
            require(tanggalPilihan.isAfter(tanggalPulang.plusDays(2))) {
                "Tanggal kembali harus lebih dari 3 hari dari tanggal pulang"
            }
            tanggalPilihan
        }
    }
}

