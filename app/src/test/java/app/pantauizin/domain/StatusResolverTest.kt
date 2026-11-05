package app.pantauizin.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatusResolverTest {
    private val batas = LocalDate.of(2026, 11, 4)

    private fun d(day: Int) = LocalDate.of(2026, 11, day)

    private fun snap(kembali: LocalDate? = null, vararg baru: LocalDate?) = IzinSnapshot(
        izinId = 1,
        batasAwal = batas,
        tanggalKembali = kembali,
        konfirmasi = baru.map { KonfirmasiFact(it) }
    )

    @Test fun izinBerjalanPadaHariBatas() {
        assertEquals(IzinStatus.IZIN_BERJALAN, StatusResolver.status(snap(), d(4)))
    }

    @Test fun terlambatBelumKonfirmasiSetelahBatas() {
        assertEquals(IzinStatus.TERLAMBAT_BELUM_KONFIRMASI, StatusResolver.status(snap(), d(5)))
    }

    @Test fun terkonfirmasiSampaiTanggalBaru() {
        val s = snap(null, d(8))
        assertEquals(IzinStatus.TERLAMBAT_TERKONFIRMASI, StatusResolver.status(s, d(6)))
        assertEquals(IzinStatus.TERLAMBAT_TERKONFIRMASI, StatusResolver.status(s, d(8)))
    }

    @Test fun konfirmasiHabisSetelahTanggalBaru() {
        assertEquals(IzinStatus.TERLAMBAT_KONFIRMASI_HABIS, StatusResolver.status(snap(null, d(8)), d(9)))
    }

    @Test fun perpanjanganBerulang() {
        val s = snap(null, d(8), d(12))
        assertEquals(IzinStatus.TERLAMBAT_TERKONFIRMASI, StatusResolver.status(s, d(10)))
        assertEquals(d(12), StatusResolver.batasAktif(s))
    }

    @Test fun batasAktifTanpaKonfirmasiAdalahBatasAwal() {
        assertEquals(batas, StatusResolver.batasAktif(snap()))
    }

    @Test fun kembaliTepatWaktu() {
        assertEquals(IzinStatus.KEMBALI_TEPAT, StatusResolver.status(snap(d(4)), d(20)))
        assertEquals(IzinStatus.KEMBALI_TEPAT, StatusResolver.status(snap(d(3)), d(20)))
    }

    @Test fun kembaliTerlambat() {
        assertEquals(IzinStatus.KEMBALI_TERLAMBAT, StatusResolver.status(snap(d(6)), d(20)))
    }

    @Test fun perpanjanganTidakMengubahStatusTerlambat() {
        assertEquals(IzinStatus.KEMBALI_TERLAMBAT, StatusResolver.status(snap(d(6), d(10)), d(20)))
    }

    @Test fun hariTerlambat() {
        assertEquals(2, StatusResolver.hariTerlambat(snap(d(6)), d(20)))
        assertEquals(3, StatusResolver.hariTerlambat(snap(), d(7)))
        assertEquals(0, StatusResolver.hariTerlambat(snap(d(3)), d(20)))
    }
}

