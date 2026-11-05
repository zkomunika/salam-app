package app.pantauizin.notify

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.pantauizin.data.AppDatabase
import app.pantauizin.data.BuktiStorage
import app.pantauizin.data.IzinInput
import app.pantauizin.data.IzinRepository
import app.pantauizin.data.SantriEntity
import app.pantauizin.data.SettingsStore
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.BatasJenis
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class SweepRunnerTest {
    private lateinit var db: AppDatabase
    private lateinit var settings: SettingsStore
    private lateinit var repo: IzinRepository
    private lateinit var notifier: FakeNotifier
    private lateinit var scheduler: FakeScheduler
    private var currentTime: LocalDateTime = LocalDateTime.of(2026, 11, 5, 8, 0)
    private lateinit var runner: SweepRunner
    private var santriId: Long = 0

    private class FakeNotifier : Notifier {
        val shown = mutableListOf<Pair<Long, String>>()
        override fun show(izinId: Long, text: String) {
            shown += izinId to text
        }
    }

    private class FakeScheduler : Scheduler {
        val scheduled = mutableListOf<LocalDateTime>()
        override fun schedule(at: LocalDateTime) {
            scheduled += at
        }
    }

    private class NoopBuktiStorage : BuktiStorage {
        override suspend fun salin(source: Uri): String = "bukti/x"
        override fun hapus(path: String) {}
        override fun file(path: String): File = File(path)
    }

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settings = SettingsStore(context)
        repo = IzinRepository(db.izinDao(), db.konfirmasiDao(), NoopBuktiStorage())
        notifier = FakeNotifier()
        scheduler = FakeScheduler()
        runner = SweepRunner(
            izin = repo,
            notif = db.notifikasiDao(),
            settings = settings,
            notifier = notifier,
            scheduler = scheduler,
            clock = { currentTime }
        )
        santriId = db.santriDao().insert(SantriEntity(nama = "ZAKY", kamar = "11", aktif = true))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun tambahIzinTerlambat(): Long {
        val res = repo.tambah(
            IzinInput(
                santriId = santriId,
                tanggalPulang = LocalDate.of(2026, 11, 1),
                alasanJenis = AlasanJenis.SAKIT,
                alasanDetail = "Demam",
                batasJenis = BatasJenis.HARI_3,
                tanggalBatasPilihan = null,
                noWaWali = null
            )
        ) as app.pantauizin.data.SaveIzinResult.Ok
        return res.id
    }

    @Test
    fun sebelumJamPengingatTidakAdaNotifikasi() = runTest {
        tambahIzinTerlambat()
        currentTime = LocalDateTime.of(2026, 11, 5, 7, 30)

        assertFalse(runner.runIfDue())
        assertTrue(notifier.shown.isEmpty())
        assertEquals(LocalDateTime.of(2026, 11, 5, 8, 0), scheduler.scheduled.last())
    }

    @Test
    fun padaJam0800MendapatLewatBatasDanPanggilanKeduaFalse() = runTest {
        val id = tambahIzinTerlambat()
        currentTime = LocalDateTime.of(2026, 11, 5, 8, 0)

        assertTrue(runner.runIfDue())
        assertEquals(
            listOf(id to "ZAKY kamar 11 sudah melewati batas waktu izin"),
            notifier.shown
        )
        assertEquals(LocalDateTime.of(2026, 11, 6, 8, 0), scheduler.scheduled.last())

        assertFalse(runner.runIfDue())
        assertEquals(1, notifier.shown.size)
    }

    @Test
    fun hpMenyalaSiangPukul1300SapuanDikejarSekaliTanpaDuplikat() = runTest {
        val id = tambahIzinTerlambat()
        currentTime = LocalDateTime.of(2026, 11, 5, 13, 0)

        assertTrue(runner.runIfDue())
        assertEquals(
            listOf(id to "ZAKY kamar 11 sudah melewati batas waktu izin"),
            notifier.shown
        )

        currentTime = LocalDateTime.of(2026, 11, 5, 15, 0)
        assertFalse(runner.runIfDue())
        assertEquals(1, notifier.shown.size)
    }

    @Test
    fun santriSudahKembaliTidakDinotifikasiDanTambahTidakMemicuNotifier() = runTest {
        val id = tambahIzinTerlambat()
        // tambah izin tidak memanggil Notifier
        assertTrue(notifier.shown.isEmpty())

        repo.tandaiKembali(id, LocalDate.of(2026, 11, 5))
        currentTime = LocalDateTime.of(2026, 11, 5, 8, 0)

        assertTrue(runner.runIfDue())
        assertTrue(notifier.shown.isEmpty())
    }
}

