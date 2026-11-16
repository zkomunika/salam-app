package app.pantauizin.data.firebase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreModelsTest {

    @Test
    fun userProfileIsAdminBerfungsiBenar() {
        val adminUser = UserProfile(
            uid = "admin123",
            email = "admin@pesantren.id",
            nama = "Admin Santri",
            role = UserRole.ADMIN.name
        )
        assertTrue(adminUser.isAdmin)

        val pengurusUser = UserProfile(
            uid = "pengurus123",
            email = "ustadz@pesantren.id",
            nama = "Ustadz Ahmad",
            role = UserRole.PENGURUS.name
        )
        assertFalse(pengurusUser.isAdmin)
    }

    @Test
    fun firestoreIzinDanKonfirmasiMenyimpanDataDenganValid() {
        val konfirmasi = FirestoreKonfirmasi(
            id = 1L,
            tanggalKembaliBaru = "2026-11-20",
            catatan = "Perpanjangan izin sakit",
            buktiBase64 = "base64dummydata",
            dibuatPada = 1731700000000L
        )

        val izin = FirestoreIzin(
            id = 100L,
            santriId = 5L,
            tanggalPulang = "2026-11-15",
            alasanJenis = "SAKIT",
            alasanDetail = "Demam tinggi",
            batasJenis = "TIGA_HARI",
            batasAwal = "2026-11-18",
            noWaWali = "628123456789",
            tanggalKembali = null,
            dibuatPada = 1731600000000L,
            konfirmasiList = listOf(konfirmasi)
        )

        assertEquals(100L, izin.id)
        assertEquals(5L, izin.santriId)
        assertEquals(1, izin.konfirmasiList.size)
        assertEquals("base64dummydata", izin.konfirmasiList[0].buktiBase64)
    }
}
