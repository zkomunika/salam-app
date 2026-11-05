package app.pantauizin.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.BatasJenis
import app.pantauizin.domain.NotifJenis
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
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun santriUnikTanpaBedaHuruf() = runTest {
        db.santriDao().insert(SantriEntity(nama = "ABC", kamar = "1", aktif = true))
        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking {
                db.santriDao().insert(SantriEntity(nama = "abc", kamar = "2", aktif = true))
            }
        }
    }

    @Test
    fun hapusIzinMenghapusKonfirmasiDanNotifikasiCascade() = runTest {
        val santriId = db.santriDao().insert(SantriEntity(nama = "ZAKY", kamar = "11", aktif = true))
        val izinId = db.izinDao().insert(
            IzinEntity(
                santriId = santriId,
                tanggalPulang = LocalDate.of(2026, 11, 1),
                alasanJenis = AlasanJenis.SAKIT,
                alasanDetail = "Demam",
                batasJenis = BatasJenis.HARI_3,
                batasAwal = LocalDate.of(2026, 11, 4),
                noWaWali = "6281234567890",
                tanggalKembali = null,
                dibuatPada = Instant.ofEpochMilli(1000L)
            )
        )
        db.konfirmasiDao().insert(
            KonfirmasiEntity(
                izinId = izinId,
                tanggalKembaliBaru = LocalDate.of(2026, 11, 6),
                catatan = "Masih istirahat",
                buktiPath = null,
                dibuatPada = Instant.ofEpochMilli(2000L)
            )
        )
        db.notifikasiDao().insertIgnore(
            NotifikasiEntity(
                izinId = izinId,
                jenis = NotifJenis.LEWAT_BATAS,
                pesan = "Pesan",
                tanggalKey = LocalDate.of(2026, 11, 5),
                dibuatPada = Instant.ofEpochMilli(3000L),
                dibaca = false
            )
        )

        assertNotNull(db.izinDao().get(izinId))
        assertEquals(1, db.notifikasiDao().forIzin(izinId).size)

        db.izinDao().delete(izinId)

        assertNull(db.izinDao().get(izinId))
        assertTrue(db.notifikasiDao().forIzin(izinId).isEmpty())
    }

    @Test
    fun insertIgnoreNotifikasiDuplikatMengembalikanMinusSatu() = runTest {
        val santriId = db.santriDao().insert(SantriEntity(nama = "ZAKY", kamar = "11", aktif = true))
        val izinId = db.izinDao().insert(
            IzinEntity(
                santriId = santriId,
                tanggalPulang = LocalDate.of(2026, 11, 1),
                alasanJenis = AlasanJenis.SAKIT,
                alasanDetail = "Demam",
                batasJenis = BatasJenis.HARI_3,
                batasAwal = LocalDate.of(2026, 11, 4),
                noWaWali = null,
                tanggalKembali = null,
                dibuatPada = Instant.ofEpochMilli(1000L)
            )
        )
        val notif = NotifikasiEntity(
            izinId = izinId,
            jenis = NotifJenis.LEWAT_BATAS,
            pesan = "ZAKY kamar 11 sudah melewati batas waktu izin",
            tanggalKey = LocalDate.of(2026, 11, 5),
            dibuatPada = Instant.ofEpochMilli(2000L),
            dibaca = false
        )

        val id1 = db.notifikasiDao().insertIgnore(notif)
        val id2 = db.notifikasiDao().insertIgnore(notif)
        assertTrue(id1 > 0)
        assertEquals(-1L, id2)
    }

    @Test
    fun findActiveBySantriMengembalikanIzinBelumKembaliSaja() = runTest {
        val santriId = db.santriDao().insert(SantriEntity(nama = "ZAKY", kamar = "11", aktif = true))
        val entity = IzinEntity(
            santriId = santriId,
            tanggalPulang = LocalDate.of(2026, 11, 1),
            alasanJenis = AlasanJenis.LAIN_LAIN,
            alasanDetail = "Acara keluarga",
            batasJenis = BatasJenis.HARI_2,
            batasAwal = LocalDate.of(2026, 11, 3),
            noWaWali = null,
            tanggalKembali = null,
            dibuatPada = Instant.ofEpochMilli(1000L)
        )
        val izinId = db.izinDao().insert(entity)

        val aktif = db.izinDao().findActiveBySantri(santriId)
        assertNotNull(aktif)
        assertEquals(izinId, aktif?.id)

        db.izinDao().update(aktif!!.copy(tanggalKembali = LocalDate.of(2026, 11, 3)))
        assertNull(db.izinDao().findActiveBySantri(santriId))

        val list = db.izinDao().observeAll().first()
        assertEquals(1, list.size)
        assertEquals("ZAKY", list[0].santri.nama)
    }
}

