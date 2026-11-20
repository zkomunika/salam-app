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

    @Test
    fun izinMenyimpanPemberiIzinDanMigrasi1Ke2Berhasil() = runTest {
        val santriId = db.santriDao().insert(SantriEntity(nama = "BUDI", kamar = "5", aktif = true))
        val izinId = db.izinDao().insert(
            IzinEntity(
                santriId = santriId,
                tanggalPulang = LocalDate.of(2026, 11, 10),
                alasanJenis = AlasanJenis.SAKIT,
                alasanDetail = "Flu",
                batasJenis = BatasJenis.HARI_2,
                batasAwal = LocalDate.of(2026, 11, 11),
                noWaWali = null,
                tanggalKembali = null,
                dibuatPada = Instant.ofEpochMilli(1000L),
                pemberiIzin = "MIFTAHUDIN"
            )
        )
        assertEquals("MIFTAHUDIN", db.izinDao().get(izinId)?.izin?.pemberiIzin)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("test_migration_1_2.db")
        dbFile.parentFile?.mkdirs()
        if (dbFile.exists()) dbFile.delete()

        val v1Db = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        v1Db.version = 1
        v1Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `santri` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `nama` TEXT NOT NULL COLLATE NOCASE,
                `kamar` TEXT NOT NULL,
                `aktif` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        v1Db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_santri_nama` ON `santri` (`nama`)")
        v1Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `izin` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `santriId` INTEGER NOT NULL,
                `tanggalPulang` INTEGER NOT NULL,
                `alasanJenis` TEXT NOT NULL,
                `alasanDetail` TEXT NOT NULL,
                `batasJenis` TEXT NOT NULL,
                `batasAwal` INTEGER NOT NULL,
                `noWaWali` TEXT,
                `tanggalKembali` INTEGER,
                `dibuatPada` INTEGER NOT NULL,
                FOREIGN KEY(`santriId`) REFERENCES `santri`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent()
        )
        v1Db.execSQL("CREATE INDEX IF NOT EXISTS `index_izin_santriId` ON `izin` (`santriId`)")
        v1Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `konfirmasi` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `izinId` INTEGER NOT NULL,
                `tanggalKembaliBaru` INTEGER,
                `catatan` TEXT,
                `buktiPath` TEXT,
                `dibuatPada` INTEGER NOT NULL,
                FOREIGN KEY(`izinId`) REFERENCES `izin`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        v1Db.execSQL("CREATE INDEX IF NOT EXISTS `index_konfirmasi_izinId` ON `konfirmasi` (`izinId`)")
        v1Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notifikasi` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `izinId` INTEGER NOT NULL,
                `jenis` TEXT NOT NULL,
                `pesan` TEXT NOT NULL,
                `tanggalKey` INTEGER NOT NULL,
                `dibuatPada` INTEGER NOT NULL,
                `dibaca` INTEGER NOT NULL,
                FOREIGN KEY(`izinId`) REFERENCES `izin`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        v1Db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_notifikasi_izinId_tanggalKey` ON `notifikasi` (`izinId`, `tanggalKey`)")
        v1Db.execSQL("INSERT INTO `santri` (`id`, `nama`, `kamar`, `aktif`) VALUES (1, 'AHMAD', '10', 1)")
        v1Db.execSQL(
            """
            INSERT INTO `izin` (`id`, `santriId`, `tanggalPulang`, `alasanJenis`, `alasanDetail`, `batasJenis`, `batasAwal`, `noWaWali`, `tanggalKembali`, `dibuatPada`)
            VALUES (1, 1, 20000, 'SAKIT', 'Demam', 'HARI_1', 20000, NULL, NULL, 1000)
            """.trimIndent()
        )
        v1Db.close()

        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbFile.name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        try {
            val migratedIzin = migratedDb.izinDao().get(1L)
            assertNotNull(migratedIzin)
            assertEquals("Demam", migratedIzin?.izin?.alasanDetail)
            assertNull(migratedIzin?.izin?.pemberiIzin)
            assertNull(migratedIzin?.izin?.terakhirWaPada)
            assertNull(migratedIzin?.izin?.terakhirWaOleh)
        } finally {
            migratedDb.close()
            dbFile.delete()
        }
    }

    @Test
    fun izinMenyimpanStatusWaWaliDanMigrasi2Ke3Berhasil() = runTest {
        val waTime = Instant.ofEpochMilli(1795172400000L)
        val santriId = db.santriDao().insert(SantriEntity(nama = "HASAN", kamar = "3", aktif = true))
        val izinId = db.izinDao().insert(
            IzinEntity(
                santriId = santriId,
                tanggalPulang = LocalDate.of(2026, 11, 10),
                alasanJenis = AlasanJenis.SAKIT,
                alasanDetail = "Demam",
                batasJenis = BatasJenis.HARI_2,
                batasAwal = LocalDate.of(2026, 11, 11),
                noWaWali = "628123456789",
                tanggalKembali = null,
                dibuatPada = Instant.ofEpochMilli(1000L),
                pemberiIzin = "MIFTAHUDIN",
                terakhirWaPada = waTime,
                terakhirWaOleh = "M FIKRI HAMBALI"
            )
        )
        val saved = db.izinDao().get(izinId)?.izin
        assertEquals(waTime, saved?.terakhirWaPada)
        assertEquals("M FIKRI HAMBALI", saved?.terakhirWaOleh)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("test_migration_2_3.db")
        dbFile.parentFile?.mkdirs()
        if (dbFile.exists()) dbFile.delete()

        val v2Db = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        v2Db.version = 2
        v2Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `santri` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `nama` TEXT NOT NULL COLLATE NOCASE,
                `kamar` TEXT NOT NULL,
                `aktif` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        v2Db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_santri_nama` ON `santri` (`nama`)")
        v2Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `izin` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `santriId` INTEGER NOT NULL,
                `tanggalPulang` INTEGER NOT NULL,
                `alasanJenis` TEXT NOT NULL,
                `alasanDetail` TEXT NOT NULL,
                `batasJenis` TEXT NOT NULL,
                `batasAwal` INTEGER NOT NULL,
                `noWaWali` TEXT,
                `tanggalKembali` INTEGER,
                `dibuatPada` INTEGER NOT NULL,
                `pemberiIzin` TEXT,
                FOREIGN KEY(`santriId`) REFERENCES `santri`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent()
        )
        v2Db.execSQL("CREATE INDEX IF NOT EXISTS `index_izin_santriId` ON `izin` (`santriId`)")
        v2Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `konfirmasi` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `izinId` INTEGER NOT NULL,
                `tanggalKembaliBaru` INTEGER,
                `catatan` TEXT,
                `buktiPath` TEXT,
                `dibuatPada` INTEGER NOT NULL,
                FOREIGN KEY(`izinId`) REFERENCES `izin`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        v2Db.execSQL("CREATE INDEX IF NOT EXISTS `index_konfirmasi_izinId` ON `konfirmasi` (`izinId`)")
        v2Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notifikasi` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `izinId` INTEGER NOT NULL,
                `jenis` TEXT NOT NULL,
                `pesan` TEXT NOT NULL,
                `tanggalKey` INTEGER NOT NULL,
                `dibuatPada` INTEGER NOT NULL,
                `dibaca` INTEGER NOT NULL,
                FOREIGN KEY(`izinId`) REFERENCES `izin`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        v2Db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_notifikasi_izinId_tanggalKey` ON `notifikasi` (`izinId`, `tanggalKey`)")
        v2Db.execSQL("INSERT INTO `santri` (`id`, `nama`, `kamar`, `aktif`) VALUES (1, 'UMAR', '7', 1)")
        v2Db.execSQL(
            """
            INSERT INTO `izin` (`id`, `santriId`, `tanggalPulang`, `alasanJenis`, `alasanDetail`, `batasJenis`, `batasAwal`, `noWaWali`, `tanggalKembali`, `dibuatPada`, `pemberiIzin`)
            VALUES (1, 1, 20000, 'SAKIT', 'Flu berat', 'HARI_2', 20001, '6281234', NULL, 1000, 'MIFTAHUDIN')
            """.trimIndent()
        )
        v2Db.close()

        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbFile.name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        try {
            val migratedIzin = migratedDb.izinDao().get(1L)
            assertNotNull(migratedIzin)
            assertEquals("Flu berat", migratedIzin?.izin?.alasanDetail)
            assertEquals("MIFTAHUDIN", migratedIzin?.izin?.pemberiIzin)
            assertNull(migratedIzin?.izin?.terakhirWaPada)
            assertNull(migratedIzin?.izin?.terakhirWaOleh)
        } finally {
            migratedDb.close()
            dbFile.delete()
        }
    }
}

