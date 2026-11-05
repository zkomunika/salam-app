package app.pantauizin.domain

import java.time.LocalDate

object DeadlineCalculator {
    /**
     * Menghitung batas awal kembali (spec 4.4).
     * HARI_1/2/3 = tanggalPulang + 1/2/3 hari. LEBIH_3_HARI memakai [tanggalPilihan],
     * yang harus setelah tanggalPulang + 3 hari.
     */
    fun batasAwal(
        tanggalPulang: LocalDate,
        jenis: BatasJenis,
        tanggalPilihan: LocalDate? = null
    ): LocalDate = when (jenis) {
        BatasJenis.HARI_1 -> tanggalPulang.plusDays(1)
        BatasJenis.HARI_2 -> tanggalPulang.plusDays(2)
        BatasJenis.HARI_3 -> tanggalPulang.plusDays(3)
        BatasJenis.LEBIH_3_HARI -> {
            require(tanggalPilihan != null) { "Tanggal kembali wajib diisi untuk batas lebih dari 3 hari" }
            require(tanggalPilihan.isAfter(tanggalPulang.plusDays(3))) {
                "Tanggal kembali harus setelah tanggal pulang + 3 hari"
            }
            tanggalPilihan
        }
    }
}

