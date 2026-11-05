package app.pantauizin.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NotificationPlannerTest {
    private fun d(day: Int) = LocalDate.of(2026, 11, day)

    // batasAwal = 11-04
    private fun snap(id: Long = 1, kembali: LocalDate? = null, vararg baru: LocalDate?) = IzinSnapshot(
        izinId = id,
        batasAwal = d(4),
        tanggalKembali = kembali,
        konfirmasi = baru.map { KonfirmasiFact(it) }
    )

    @Test fun lewatBatasTanpaKonfirmasiDanRiwayat() {
        val hasil = NotificationPlanner.plan(d(5), listOf(snap()), emptyList())
        assertEquals(listOf(PlannedNotif(1, NotifJenis.LEWAT_BATAS)), hasil)
    }

    @Test fun pengingatHarianSetelahAdaRiwayat() {
        val hasil = NotificationPlanner.plan(d(6), listOf(snap()), listOf(ExistingNotif(1, d(5))))
        assertEquals(listOf(PlannedNotif(1, NotifJenis.PENGINGAT_HARIAN)), hasil)
    }

    @Test fun tidakGandaPadaHariYangSama() {
        val hasil = NotificationPlanner.plan(d(5), listOf(snap()), listOf(ExistingNotif(1, d(5))))
        assertTrue(hasil.isEmpty())
    }

    @Test fun sudahKembaliTidakDinotifikasi() {
        val hasil = NotificationPlanner.plan(d(10), listOf(snap(kembali = d(6))), emptyList())
        assertTrue(hasil.isEmpty())
    }

    @Test fun belumLewatTanggalBaruTidakDinotifikasi() {
        val riwayat = listOf(ExistingNotif(1, d(5)), ExistingNotif(1, d(6)))
        val hasil = NotificationPlanner.plan(d(7), listOf(snap(1, null, d(8))), riwayat)
        assertTrue(hasil.isEmpty())
    }

    @Test fun lewatTanggalBaruSetelahKonfirmasi() {
        val riwayat = listOf(ExistingNotif(1, d(5)), ExistingNotif(1, d(6)))
        val hasil = NotificationPlanner.plan(d(9), listOf(snap(1, null, d(8))), riwayat)
        assertEquals(listOf(PlannedNotif(1, NotifJenis.LEWAT_TANGGAL_BARU)), hasil)
    }

    @Test fun pengingatHarianSetelahLewatTanggalBaru() {
        val riwayat = listOf(ExistingNotif(1, d(5)), ExistingNotif(1, d(6)), ExistingNotif(1, d(9)))
        val hasil = NotificationPlanner.plan(d(10), listOf(snap(1, null, d(8))), riwayat)
        assertEquals(listOf(PlannedNotif(1, NotifJenis.PENGINGAT_HARIAN)), hasil)
    }

    @Test fun batalCentangKembaliDinotifikasiLagi() {
        // riwayat lama dari sebelum santri dicentang kembali; tanggalKembali kini null lagi
        val riwayat = listOf(ExistingNotif(1, d(5)))
        val hasil = NotificationPlanner.plan(d(7), listOf(snap()), riwayat)
        assertEquals(listOf(PlannedNotif(1, NotifJenis.PENGINGAT_HARIAN)), hasil)
    }

    @Test fun belumLewatBatasAwalTidakDinotifikasi() {
        assertTrue(NotificationPlanner.plan(d(4), listOf(snap()), emptyList()).isEmpty())
    }
}

