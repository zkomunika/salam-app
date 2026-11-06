package app.pantauizin.ui.form

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.pantauizin.data.AppDatabase
import app.pantauizin.data.BuktiStorage
import app.pantauizin.data.IzinInput
import app.pantauizin.data.IzinRepository
import app.pantauizin.data.SantriEntity
import app.pantauizin.data.SantriRepository
import app.pantauizin.data.SaveIzinResult
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.BatasJenis
import kotlinx.coroutines.flow.first
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
class IzinFormViewModelTest {
    private lateinit var db: AppDatabase
    private lateinit var santriRepo: SantriRepository
    private lateinit var izinRepo: IzinRepository
    private var zakyId: Long = 0
    private var rafiId: Long = 0

    private class NoopBukti : BuktiStorage {
        override suspend fun salin(source: Uri): String = "bukti/x"
        override fun hapus(path: String) {}
        override fun file(path: String): File = File(path)
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        santriRepo = SantriRepository(db.santriDao())
        izinRepo = IzinRepository(db.izinDao(), db.konfirmasiDao(), NoopBukti())

        kotlinx.coroutines.runBlocking {
            zakyId = db.santriDao().insert(SantriEntity(nama = "ZAKY FAUZI", kamar = "11", aktif = true))
            rafiId = db.santriDao().insert(SantriEntity(nama = "RAFI AHMAD", kamar = "10", aktif = true))
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun memilihKamarMempersempitSaranDanMemilihNamaMengisiKamar() = runTest {
        val vm = IzinFormViewModel(santriRepo, izinRepo, editIzinId = null) { LocalDate.of(2026, 11, 5) }
        vm.loadInitial()

        vm.pilihKamar("10")
        assertEquals(listOf("RAFI AHMAD"), vm.uiState.value.saranSantri.map { it.nama })

        vm.pilihKamar(null) // Semua kamar
        vm.pilihSantri(zakyId)
        assertEquals("11", vm.uiState.value.selectedKamar)
        assertEquals("ZAKY FAUZI", vm.uiState.value.namaQuery)
    }

    @Test
    fun memilihSantriYangMasihPunyaIzinAktifMenampilkanPeringatanDanMenahanSimpan() = runTest {
        val aktif = izinRepo.tambah(
            IzinInput(
                santriId = zakyId,
                tanggalPulang = LocalDate.of(2026, 11, 1),
                alasanJenis = AlasanJenis.SAKIT,
                alasanDetail = "Demam",
                batasJenis = BatasJenis.HARI_2,
                tanggalBatasPilihan = null,
                noWaWali = null
            )
        ) as SaveIzinResult.Ok

        val vm = IzinFormViewModel(santriRepo, izinRepo, editIzinId = null) { LocalDate.of(2026, 11, 5) }
        vm.loadInitial()
        vm.pilihSantri(zakyId)
        vm.ubahAlasanDetail("Flu")

        assertEquals(aktif.id, vm.uiState.value.activeIzinIdWarning)
        assertFalse(vm.simpan())
        assertEquals(1, izinRepo.observe().first().size)
    }

    @Test
    fun modeEditMemuatNilaiIzinYangAda() = runTest {
        val saved = izinRepo.tambah(
            IzinInput(
                santriId = rafiId,
                tanggalPulang = LocalDate.of(2026, 11, 1),
                alasanJenis = AlasanJenis.LAIN_LAIN,
                alasanDetail = "Acara keluarga",
                batasJenis = BatasJenis.HARI_3,
                tanggalBatasPilihan = null,
                noWaWali = "081234567890"
            )
        ) as SaveIzinResult.Ok

        val vm = IzinFormViewModel(santriRepo, izinRepo, editIzinId = saved.id) { LocalDate.of(2026, 11, 5) }
        vm.loadInitial()

        val state = vm.uiState.value
        assertEquals(rafiId, state.form.santriId)
        assertEquals("10", state.selectedKamar)
        assertEquals(AlasanJenis.LAIN_LAIN, state.form.alasanJenis)
        assertEquals("Acara keluarga", state.form.alasanDetail)
        assertEquals(BatasJenis.HARI_3, state.form.batasJenis)
        assertEquals("6281234567890", state.form.noWa)
        assertNull(state.activeIzinIdWarning)

        vm.ubahAlasanDetail("Acara nikahan kakak")
        assertTrue(vm.simpan())
        assertEquals("Acara nikahan kakak", izinRepo.get(saved.id)!!.alasanDetail)
        assertNotNull(izinRepo.get(saved.id))
    }
}
