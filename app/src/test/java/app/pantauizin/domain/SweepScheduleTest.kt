package app.pantauizin.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SweepScheduleTest {
    private val jam = LocalTime.of(8, 0)
    private val hari4 = LocalDate.of(2026, 11, 4)
    private val hari5 = LocalDate.of(2026, 11, 5)

    private fun now(h: Int, m: Int = 0) = LocalDateTime.of(2026, 11, 5, h, m)

    @Test fun sebelumJamSapuanKemarin() {
        assertEquals(LocalDateTime.of(2026, 11, 5, 8, 0), SweepSchedule.next(now(7), jam, hari4))
        assertFalse(SweepSchedule.isDue(now(7), jam, hari4))
    }

    @Test fun setelahJamSapuanBelumJalanDikejar() {
        assertEquals(LocalDateTime.of(2026, 11, 6, 8, 0), SweepSchedule.next(now(9), jam, hari4))
        assertTrue(SweepSchedule.isDue(now(9), jam, hari4))
    }

    @Test fun sebelumJamTapiSapuanHariIniSudahJalan() {
        assertEquals(LocalDateTime.of(2026, 11, 6, 8, 0), SweepSchedule.next(now(7), jam, hari5))
        assertFalse(SweepSchedule.isDue(now(7), jam, hari5))
    }

    @Test fun setelahJamDanSapuanHariIniSudahJalan() {
        assertFalse(SweepSchedule.isDue(now(9), jam, hari5))
        assertEquals(LocalDateTime.of(2026, 11, 6, 8, 0), SweepSchedule.next(now(9), jam, hari5))
    }

    @Test fun jamDiubahKeDepanHariIni() {
        assertEquals(
            LocalDateTime.of(2026, 11, 5, 10, 0),
            SweepSchedule.next(now(9), LocalTime.of(10, 0), null)
        )
    }

    @Test fun tepatPadaJamBelumPernahSapuan() {
        assertTrue(SweepSchedule.isDue(now(8), jam, null))
    }
}

