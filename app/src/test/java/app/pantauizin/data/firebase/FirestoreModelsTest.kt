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
        assertFalse(pengurusUser.isPengurusInti)
        assertFalse(pengurusUser.canManageIzin)
        assertTrue(adminUser.canManageIzin)
    }

    @Test
    fun sepuluhPengurusIntiMendapatAksesKelolaDanAnggotaBiasaHanyaBaca() {
        val intiEmails = listOf(
            "miftahudin@asas.id",
            "mthoriqmusthafa@asas.id",
            "mfikrihambali@asas.id",
            "fikrimhilmi@asas.id",
            "mfajrulhikam@asas.id",
            "maulanahasyim@asas.id",
            "mohghiyatsmaulanaaripin@asas.id",
            "mazkaanggiri@asas.id",
            "mazmanabdulaziz@asas.id",
            "akmalmasykurhidayat@asas.id"
        )
        intiEmails.forEach { email ->
            val p = UserProfile(uid = email, email = email, nama = "", role = UserRole.PENGURUS.name)
            assertTrue("Harusnya pengurus inti: $email", p.isPengurusInti)
            assertTrue("Harusnya boleh kelola izin: $email", p.canManageIzin)
            assertTrue("Harusnya boleh kelola santri: $email", p.canManageSantri)
            assertTrue("Harusnya punya displayNama: $email", p.displayNama.isNotBlank())
        }

        // Alfan Maulana Adif mengandung kata "MAULANA" tetapi BUKAN pengurus inti
        val alfan = UserProfile(
            uid = "u_alfan",
            email = "alfanmaulanaadif@asas.id",
            nama = "ALFAN MAULANA ADIF",
            role = UserRole.PENGURUS.name
        )
        assertFalse(alfan.isPengurusInti)
        assertFalse(alfan.canManageIzin)
        assertFalse(alfan.canManageSantri)

        val imron = UserProfile(uid = "u_imron", email = "mimron@asas.id", nama = "M IMRON", role = UserRole.PENGURUS.name)
        assertFalse(imron.canManageIzin)

        val umum = UserProfile(uid = "u_umum", email = "pengurus@pesantren.id", nama = "Pengurus", role = UserRole.PENGURUS.name)
        assertFalse(umum.canManageIzin)
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
            pemberiIzin = "MIFTAHUDIN",
            konfirmasiList = listOf(konfirmasi)
        )

        assertEquals(100L, izin.id)
        assertEquals(5L, izin.santriId)
        assertEquals("MIFTAHUDIN", izin.pemberiIzin)
        assertEquals(1, izin.konfirmasiList.size)
        assertEquals("base64dummydata", izin.konfirmasiList[0].buktiBase64)
    }
}
