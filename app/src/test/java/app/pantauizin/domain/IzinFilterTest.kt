package app.pantauizin.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IzinFilterTest {
    @Test
    fun semuaMencocokkanSemuaStatus() {
        for (s in IzinStatus.entries) {
            assertTrue(IzinFilter.SEMUA.matches(s))
        }
    }

    @Test
    fun berjalanHanyaIzinBerjalan() {
        assertTrue(IzinFilter.BERJALAN.matches(IzinStatus.IZIN_BERJALAN))
        for (s in IzinStatus.entries.filter { it != IzinStatus.IZIN_BERJALAN }) {
            assertFalse(IzinFilter.BERJALAN.matches(s))
        }
    }

    @Test
    fun terlambatMemuatTigaStatusTerlambatDanKembaliTerlambat() {
        assertTrue(IzinFilter.TERLAMBAT.matches(IzinStatus.TERLAMBAT_BELUM_KONFIRMASI))
        assertTrue(IzinFilter.TERLAMBAT.matches(IzinStatus.TERLAMBAT_TERKONFIRMASI))
        assertTrue(IzinFilter.TERLAMBAT.matches(IzinStatus.TERLAMBAT_KONFIRMASI_HABIS))
        assertTrue(IzinFilter.TERLAMBAT.matches(IzinStatus.KEMBALI_TERLAMBAT))
        assertFalse(IzinFilter.TERLAMBAT.matches(IzinStatus.IZIN_BERJALAN))
        assertFalse(IzinFilter.TERLAMBAT.matches(IzinStatus.KEMBALI_TEPAT))
    }

    @Test
    fun kembaliMemuatKembaliTepatDanKembaliTerlambat() {
        assertTrue(IzinFilter.KEMBALI.matches(IzinStatus.KEMBALI_TEPAT))
        assertTrue(IzinFilter.KEMBALI.matches(IzinStatus.KEMBALI_TERLAMBAT))
        assertFalse(IzinFilter.KEMBALI.matches(IzinStatus.IZIN_BERJALAN))
        assertFalse(IzinFilter.KEMBALI.matches(IzinStatus.TERLAMBAT_BELUM_KONFIRMASI))
        assertFalse(IzinFilter.KEMBALI.matches(IzinStatus.TERLAMBAT_TERKONFIRMASI))
        assertFalse(IzinFilter.KEMBALI.matches(IzinStatus.TERLAMBAT_KONFIRMASI_HABIS))
    }
}

