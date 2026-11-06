package app.pantauizin.ui.pengaturan

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.pantauizin.data.AppDatabase
import app.pantauizin.data.SantriEntity
import app.pantauizin.data.SantriRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataSantriViewModelTest {
    private lateinit var db: AppDatabase
    private lateinit var santriRepo: SantriRepository
    private var zakyId: Long = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        santriRepo = SantriRepository(db.santriDao())
        kotlinx.coroutines.runBlocking {
            zakyId = db.santriDao().insert(SantriEntity(nama = "ZAKY FAUZI", kamar = "11", aktif = true))
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun tambahDenganNamaKembarMemunculkanPesanNamaSudahAda() = runTest {
        val vm = DataSantriViewModel(santriRepo)

        assertFalse(vm.tambah("zaky fauzi", "12"))
        assertEquals("Nama sudah ada", vm.uiState.value.errorMessage)

        assertTrue(vm.tambah("RAFI AHMAD", "10"))
        assertNull(vm.uiState.value.errorMessage)
        assertEquals(2, santriRepo.observe().first().size)
    }

    @Test
    fun ubahDanSetAktifBerjalanSesuaiAturan() = runTest {
        val vm = DataSantriViewModel(santriRepo)

        assertTrue(vm.ubah(zakyId, "ZAKY FAUZI", "12"))
        assertEquals("12", santriRepo.observe().first().first { it.id == zakyId }.kamar)

        vm.setAktif(zakyId, false)
        assertFalse(santriRepo.observe().first().first { it.id == zakyId }.aktif)
    }
}

