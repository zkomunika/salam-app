package app.pantauizin.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class NotifRangeTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val today = LocalDate.of(2026, 11, 5)

    @Test
    fun hariIniMencakupAwalSampaiAkhirHari() {
        val (from, to) = NotifRange.HariIni.toInstants(today, zone)
        val expectedStart = ZonedDateTime.of(2026, 11, 5, 0, 0, 0, 0, zone).toInstant()
        val expectedEnd = ZonedDateTime.of(2026, 11, 5, 23, 59, 59, 999_000_000, zone).toInstant()

        assertEquals(expectedStart.toEpochMilli(), from.toEpochMilli())
        assertTrue(to.toEpochMilli() >= expectedEnd.toEpochMilli())
        val startBesok = ZonedDateTime.of(2026, 11, 6, 0, 0, 0, 0, zone).toInstant()
        assertTrue(to.isBefore(startBesok))
    }

    @Test
    fun tujuhHariMulaiEnamHariSebelumHariIni() {
        val (from, to) = NotifRange.TujuhHari.toInstants(today, zone)
        val expectedStart = ZonedDateTime.of(2026, 10, 30, 0, 0, 0, 0, zone).toInstant()
        val expectedEnd = ZonedDateTime.of(2026, 11, 5, 23, 59, 59, 999_000_000, zone).toInstant()

        assertEquals(expectedStart.toEpochMilli(), from.toEpochMilli())
        assertTrue(to.toEpochMilli() >= expectedEnd.toEpochMilli())
    }

    @Test
    fun tigaPuluhHariMulaiDuaPuluhSembilanHariSebelumHariIni() {
        val (from, to) = NotifRange.TigaPuluhHari.toInstants(today, zone)
        val expectedStart = ZonedDateTime.of(2026, 10, 7, 0, 0, 0, 0, zone).toInstant()
        val expectedEnd = ZonedDateTime.of(2026, 11, 5, 23, 59, 59, 999_000_000, zone).toInstant()

        assertEquals(expectedStart.toEpochMilli(), from.toEpochMilli())
        assertTrue(to.toEpochMilli() >= expectedEnd.toEpochMilli())
    }

    @Test(expected = IllegalArgumentException::class)
    fun kustomDenganDariLebihBesarDariSampaiDitolak() {
        NotifRange.Kustom(
            dari = LocalDate.of(2026, 11, 5),
            sampai = LocalDate.of(2026, 11, 1)
        ).toInstants(today, zone)
    }
}

