package app.pantauizin.ui.konfirmasi

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.pantauizin.data.AppDatabase
import app.pantauizin.data.BuktiStorage
import app.pantauizin.data.IzinInput
import app.pantauizin.data.IzinRepository
import app.pantauizin.data.SantriEntity
import app.pantauizin.data.SaveIzinResult
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.BatasJenis
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class KonfirmasiFormViewModelTest {
    private lateinit var db: AppDatabase
    private lateinit var izinRepo: IzinRepository
    private var santriBelumKembaliIzinId: Long = 0
    private var santriSudahKembaliIzinId: Long = 0
    private val today = LocalDate.of(2026, 11, 5)

    private class FakeBukti : BuktiStorage {
        override suspend fun salin(source: Uri): String = "bukti/salinan.jpg"
        override fun hapus(path: String) {}
        override fun file(path: String): File = File(path)
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        izinRepo = IzinRepository(db.izinDao(), db.konfirmasiDao(), FakeBukti())

        kotlinx.coroutines.runBlocking {
            val s1 = db.santriDao().insert(SantriEntity(nama = "ZAKY", kamar = "11", aktif = true))
            val s2 = db.santriDao().insert(SantriEntity(nama = "RAFI", kamar = "10", aktif = true))

            santriBelumKembaliIzinId = (izinRepo.tambah(
                IzinInput(
                    santriId = s1,
                    tanggalPulang = LocalDate.of(2026, 11, 1),
                    alasanJenis = AlasanJenis.SAKIT,
                    alasanDetail = "Demam",
                    batasJenis = BatasJenis.HARI_2,
                    tanggalBatasPilihan = null,
                    noWaWali = null
                )
            ) as SaveIzinResult.Ok).id

            santriSudahKembaliIzinId = (izinRepo.tambah(
                IzinInput(
                    santriId = s2,
                    tanggalPulang = LocalDate.of(2026, 10, 28),
                    alasanJenis = AlasanJenis.SAKIT,
                    alasanDetail = "Flu",
                    batasJenis = BatasJenis.HARI_2,
                    tanggalBatasPilihan = null,
                    noWaWali = null
                )
            ) as SaveIzinResult.Ok).id
            izinRepo.tandaiKembali(santriSudahKembaliIzinId, LocalDate.of(2026, 11, 3))
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun tanggalBaruWajibJikaBelumKembaliDanTidakBolehSebelumHariIni() = runTest {
        val vm = KonfirmasiFormViewModel(izinRepo, santriBelumKembaliIzinId) { today }
        vm.load()

        assertTrue(vm.uiState.value.perluTanggalBaru)
        // Default belum diisi -> gagal simpan
        assertFalse(vm.simpan())
        assertNotNull(vm.uiState.value.tanggalError)

        // Tanggal sebelum hari ini -> gagal simpan
        vm.ubahTanggalBaru(today.minusDays(1))
        assertFalse(vm.simpan())
        assertNotNull(vm.uiState.value.tanggalError)

        // Tanggal hari ini, tanpa bukti & catatan -> berhasil simpan
        vm.ubahTanggalBaru(today)
        assertTrue(vm.simpan())
        assertNull(vm.uiState.value.tanggalError)

        val izin = izinRepo.get(santriBelumKembaliIzinId)!!
        assertEquals(1, izin.konfirmasi.size)
        assertEquals(today, izin.konfirmasi.first().tanggalKembaliBaru)
        assertNull(izin.konfirmasi.first().catatan)
        assertNull(izin.konfirmasi.first().buktiPath)
    }

    @Test
    fun santriSudahKembaliTidakMembutuhkanTanggalBaruDanBuktiCatatanOpsional() = runTest {
        val vm = KonfirmasiFormViewModel(izinRepo, santriSudahKembaliIzinId) { today }
        vm.load()

        assertFalse(vm.uiState.value.perluTanggalBaru)
        vm.ubahCatatan("Konfirmasi susulan dari wali")
        assertTrue(vm.simpan())

        val izin = izinRepo.get(santriSudahKembaliIzinId)!!
        assertEquals(1, izin.konfirmasi.size)
        assertNull(izin.konfirmasi.first().tanggalKembaliBaru)
        assertEquals("Konfirmasi susulan dari wali", izin.konfirmasi.first().catatan)
    }
}

