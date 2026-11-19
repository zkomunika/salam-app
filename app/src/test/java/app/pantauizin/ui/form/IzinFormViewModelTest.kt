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

        // Awal terbuka (belum pilih kamar / ketik nama): saran harus kosong
        assertTrue(vm.uiState.value.saranSantri.isEmpty())

        // Ketik sebagian nama: muncul saran
        vm.ubahNamaQuery("zaky")
        assertEquals(listOf("ZAKY FAUZI"), vm.uiState.value.saranSantri.map { it.nama })

        // Kosongkan nama (kamar tetap null): saran kosong lagi
        vm.ubahNamaQuery("")
        assertTrue(vm.uiState.value.saranSantri.isEmpty())

        // Pilih kamar 10: muncul seluruh santri kamar 10
        vm.pilihKamar("10")
        assertEquals(listOf("RAFI AHMAD"), vm.uiState.value.saranSantri.map { it.nama })

        vm.pilihKamar(null) // Semua kamar
        vm.pilihSantri(zakyId)
        assertEquals("11", vm.uiState.value.selectedKamar)
        assertEquals("ZAKY FAUZI", vm.uiState.value.namaQuery)
        assertTrue(vm.uiState.value.saranSantri.isEmpty())
    }

    @Test
    fun memilihSantriBaruPulang2MingguLaluMenampilkanPopupTetapiTidakMengunciSimpanDanCatatPemberiIzin() = runTest {
        val rLama = izinRepo.tambah(
            IzinInput(
                santriId = zakyId,
                tanggalPulang = LocalDate.of(2026, 11, 5),
                alasanJenis = AlasanJenis.SAKIT,
                alasanDetail = "Demam",
                batasJenis = BatasJenis.HARI_2,
                tanggalBatasPilihan = null,
                noWaWali = null,
                pemberiIzin = "M THORIQ MUSTHAFA"
            )
        ) as SaveIzinResult.Ok
        izinRepo.tandaiKembali(rLama.id, LocalDate.of(2026, 11, 6))

        val vm = IzinFormViewModel(
            santriRepo = santriRepo,
            izinRepo = izinRepo,
            editIzinId = null,
            pemberiIzinNama = "MIFTAHUDIN"
        ) { LocalDate.of(2026, 11, 19) }
        vm.loadInitial()
        vm.pilihSantri(zakyId)

        val state = vm.uiState.value
        assertNotNull(state.recentPulangWarning)
        assertEquals("Demam", state.recentPulangWarning?.alasanDetail)
        assertTrue(state.showRecentPulangPopup)
        assertEquals("MIFTAHUDIN", state.pemberiIzin)

        vm.tutupPopupRecentPulang()
        assertFalse(vm.uiState.value.showRecentPulangPopup)

        // Tetap bisa disimpan meskipun belum genap 2 bulan
        vm.ubahAlasanDetail("Keperluan mendesak")
        assertTrue(vm.simpan())

        val terbaru = izinRepo.observe().first().first { it.id != rLama.id }
        assertEquals("MIFTAHUDIN", terbaru.pemberiIzin)
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
                noWaWali = "081234567890",
                pemberiIzin = "MIFTAHUDIN"
            )
        ) as SaveIzinResult.Ok

        val vm = IzinFormViewModel(
            santriRepo = santriRepo,
            izinRepo = izinRepo,
            editIzinId = saved.id,
            pemberiIzinNama = "AKMAL MASYKUR HIDAYAT"
        ) { LocalDate.of(2026, 11, 5) }
        vm.loadInitial()

        val state = vm.uiState.value
        assertEquals(rafiId, state.form.santriId)
        assertEquals("10", state.selectedKamar)
        assertEquals(AlasanJenis.LAIN_LAIN, state.form.alasanJenis)
        assertEquals("Acara keluarga", state.form.alasanDetail)
        assertEquals(BatasJenis.HARI_3, state.form.batasJenis)
        assertEquals("6281234567890", state.form.noWa)
        assertEquals("MIFTAHUDIN", state.pemberiIzin)
        assertNull(state.activeIzinIdWarning)

        vm.ubahAlasanDetail("Acara nikahan kakak")
        assertTrue(vm.simpan())
        val updated = izinRepo.get(saved.id)!!
        assertEquals("Acara nikahan kakak", updated.alasanDetail)
        assertEquals("MIFTAHUDIN", updated.pemberiIzin)
        assertNotNull(updated)
    }
}
