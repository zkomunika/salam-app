package app.pantauizin.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class DeadlineCalculatorTest {
    @Test fun satuDuaTigaHari() {
        val p = LocalDate.of(2026, 11, 1)
        assertEquals(LocalDate.of(2026, 11, 1), DeadlineCalculator.batasAwal(p, BatasJenis.HARI_1))
        assertEquals(LocalDate.of(2026, 11, 2), DeadlineCalculator.batasAwal(p, BatasJenis.HARI_2))
        assertEquals(LocalDate.of(2026, 11, 3), DeadlineCalculator.batasAwal(p, BatasJenis.HARI_3))
    }

    @Test fun contohUserPulangTanggal15() {
        val p = LocalDate.of(2026, 11, 15)
        assertEquals(LocalDate.of(2026, 11, 15), DeadlineCalculator.batasAwal(p, BatasJenis.HARI_1))
        assertEquals(LocalDate.of(2026, 11, 16), DeadlineCalculator.batasAwal(p, BatasJenis.HARI_2))
        assertEquals(LocalDate.of(2026, 11, 17), DeadlineCalculator.batasAwal(p, BatasJenis.HARI_3))
    }

    @Test fun lintasBulanTahunDanKabisat() {
        assertEquals(LocalDate.of(2027, 1, 1), DeadlineCalculator.batasAwal(LocalDate.of(2026, 12, 30), BatasJenis.HARI_3))
        assertEquals(LocalDate.of(2028, 2, 28), DeadlineCalculator.batasAwal(LocalDate.of(2028, 2, 28), BatasJenis.HARI_1))
        assertEquals(LocalDate.of(2028, 2, 29), DeadlineCalculator.batasAwal(LocalDate.of(2028, 2, 28), BatasJenis.HARI_2))
    }

    @Test fun lebihTigaHariMemakaiTanggalPilihan() {
        val p = LocalDate.of(2026, 11, 1)
        assertEquals(
            LocalDate.of(2026, 11, 20),
            DeadlineCalculator.batasAwal(p, BatasJenis.LEBIH_3_HARI, LocalDate.of(2026, 11, 20))
        )
    }

    @Test fun lebihTigaHariTidakValid() {
        val p = LocalDate.of(2026, 11, 1)
        assertThrows(IllegalArgumentException::class.java) {
            DeadlineCalculator.batasAwal(p, BatasJenis.LEBIH_3_HARI, null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DeadlineCalculator.batasAwal(p, BatasJenis.LEBIH_3_HARI, LocalDate.of(2026, 11, 3)) // tepat p+2 (hari ke-3) tidak valid
        }
    }
}

