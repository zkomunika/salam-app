package app.pantauizin.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.pantauizin.domain.SantriSearch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SantriRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: SantriRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = SantriRepository(db.santriDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun tambahMerapikanSpasi() = runTest {
        val res = repo.tambah("  zaky  fauzi ", " 5 ")
        assertEquals(SantriResult.Ok, res)
        val items = repo.observe().first()
        assertEquals(1, items.size)
        assertEquals("zaky fauzi", items[0].nama)
        assertEquals("5", items[0].kamar)
    }

    @Test
    fun tambahKosongDitolak() = runTest {
        assertEquals(SantriResult.Invalid, repo.tambah("ABC", ""))
        assertEquals(SantriResult.Invalid, repo.tambah("   ", "1"))
    }

    @Test
    fun tambahNamaKembarBedaHurufDitolak() = runTest {
        assertEquals(SantriResult.Ok, repo.tambah("ZAKY FAUZI", "5"))
        assertEquals(SantriResult.NamaKembar, repo.tambah("zaky fauzi", "6"))
    }

    @Test
    fun ubahDanSetAktif() = runTest {
        assertEquals(SantriResult.Ok, repo.tambah("ZAKY FAUZI", "5"))
        assertEquals(SantriResult.Ok, repo.tambah("BUDI SANTOSO", "6"))
        val zaky = repo.observe().first().first { it.nama == "ZAKY FAUZI" }

        assertEquals(SantriResult.NamaKembar, repo.ubah(zaky.id, "budi santoso", "5"))
        assertEquals(SantriResult.Ok, repo.ubah(zaky.id, "ZAKY FAUZI", "11"))
        assertEquals("11", repo.observe().first().first { it.id == zaky.id }.kamar)

        repo.setAktif(zaky.id, false)
        val all = repo.observe().first()
        assertTrue(SantriSearch.filter(all, "zaky").isEmpty())
    }
}

