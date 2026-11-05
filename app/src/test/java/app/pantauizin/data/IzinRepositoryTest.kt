package app.pantauizin.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.BatasJenis
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class IzinRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var buktiStorage: FakeBuktiStorage
    private lateinit var repo: IzinRepository
    private var santriId: Long = 0

    private class FakeBuktiStorage : BuktiStorage {
        var shouldThrow = false
        private var counter = 0
        val deletedPaths = mutableListOf<String>()

        override suspend fun salin(source: Uri): String {
            if (shouldThrow) throw IOException("Gagal menyalin file bukti")
            counter++
            return "bukti/file_$counter.jpg"
        }

        override fun hapus(path: String) {
            deletedPaths += path
        }

        override fun file(path: String): File = File(path)
    }

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        buktiStorage = FakeBuktiStorage()
        repo = IzinRepository(db.izinDao(), db.konfirmasiDao(), buktiStorage)
        santriId = db.santriDao().insert(SantriEntity(nama = "ZAKY", kamar = "11", aktif = true))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun input(
        pulang: LocalDate = LocalDate.of(2026, 11, 1),
        batas: BatasJenis = BatasJenis.HARI_3,
        pilihan: LocalDate? = null,
        wa: String? = "0812-3456-7890"
    ) = IzinInput(
        santriId = santriId,
        tanggalPulang = pulang,
        alasanJenis = AlasanJenis.SAKIT,
        alasanDetail = "Demam",
        batasJenis = batas,
        tanggalBatasPilihan = pilihan,
        noWaWali = wa
    )

    @Test
    fun satuIzinAktifPerSantriDanBolehLagiSetelahTandaiKembali() = runTest {
        val r1 = repo.tambah(input()) as SaveIzinResult.Ok
        val r2 = repo.tambah(input())
        assertEquals(SaveIzinResult.SantriSudahPunyaIzinAktif(r1.id), r2)

        repo.tandaiKembali(r1.id, LocalDate.of(2026, 11, 3))
        val r3 = repo.tambah(input(pulang = LocalDate.of(2026, 11, 4)))
        assertTrue(r3 is SaveIzinResult.Ok)
    }

    @Test
    fun tanggalPulangLampauDiterimaDanBatasAwalBenar() = runTest {
        val r = repo.tambah(input(pulang = LocalDate.of(2026, 10, 20), batas = BatasJenis.HARI_2)) as SaveIzinResult.Ok
        val view = repo.get(r.id)!!
        assertEquals(LocalDate.of(2026, 10, 22), view.batasAwal)
        assertEquals("6281234567890", view.noWaWali)
    }

    @Test
    fun ubahMenghitungUlangBatasAwalDanMempertahankanKonfirmasi() = runTest {
        val r = repo.tambah(input(pulang = LocalDate.of(2026, 11, 1), batas = BatasJenis.HARI_2)) as SaveIzinResult.Ok
        repo.tambahKonfirmasi(r.id, LocalDate.of(2026, 11, 6), "Masih sakit", Uri.parse("content://test/1"))

        val ubahRes = repo.ubah(r.id, input(pulang = LocalDate.of(2026, 11, 2), batas = BatasJenis.HARI_3, wa = null))
        assertEquals(SaveIzinResult.Ok(r.id), ubahRes)

        val view = repo.get(r.id)!!
        assertEquals(LocalDate.of(2026, 11, 5), view.batasAwal)
        assertNull(view.noWaWali)
        assertEquals(1, view.konfirmasi.size)
        assertEquals("Masih sakit", view.konfirmasi[0].catatan)
    }

    @Test
    fun validasiNomorWaDanBatasLebihTigaHari() = runTest {
        assertEquals(SaveIzinResult.NomorWaTidakValid, repo.tambah(input(wa = "abc")))
        assertEquals(
            SaveIzinResult.BatasTidakValid,
            repo.tambah(input(batas = BatasJenis.LEBIH_3_HARI, pilihan = null))
        )
        assertEquals(
            SaveIzinResult.BatasTidakValid,
            repo.tambah(input(pulang = LocalDate.of(2026, 11, 1), batas = BatasJenis.LEBIH_3_HARI, pilihan = LocalDate.of(2026, 11, 4)))
        )
    }

    @Test
    fun batalKembaliMengembalikanTanggalKembaliNull() = runTest {
        val r = repo.tambah(input()) as SaveIzinResult.Ok
        repo.tandaiKembali(r.id, LocalDate.of(2026, 11, 4))
        assertNotNull(repo.get(r.id)!!.tanggalKembali)

        repo.batalKembali(r.id)
        assertNull(repo.get(r.id)!!.tanggalKembali)
    }

    @Test
    fun hapusMenghapusKonfirmasiDanSemuaFileBukti() = runTest {
        val r = repo.tambah(input()) as SaveIzinResult.Ok
        repo.tambahKonfirmasi(r.id, LocalDate.of(2026, 11, 6), "K1", Uri.parse("content://test/1"))
        repo.tambahKonfirmasi(r.id, LocalDate.of(2026, 11, 8), "K2", Uri.parse("content://test/2"))

        repo.hapus(r.id)

        assertNull(repo.get(r.id))
        assertEquals(listOf("bukti/file_1.jpg", "bukti/file_2.jpg"), buktiStorage.deletedPaths)
        assertTrue(repo.observe().first().isEmpty())
    }

    @Test
    fun tambahKonfirmasiGagalSalinBuktiTidakMenambahBaris() = runTest {
        val r = repo.tambah(input()) as SaveIzinResult.Ok
        buktiStorage.shouldThrow = true

        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking {
                repo.tambahKonfirmasi(r.id, LocalDate.of(2026, 11, 6), "Gagal", Uri.parse("content://test/err"))
            }
        }
        assertTrue(repo.get(r.id)!!.konfirmasi.isEmpty())
    }
}

