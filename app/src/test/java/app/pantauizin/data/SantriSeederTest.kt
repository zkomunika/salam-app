package app.pantauizin.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SantriSeederTest {
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
    fun seederMemuat275BarisDanIdempotent() = runTest {
        val assetFile = sequenceOf(
            File("src/main/assets/santri_master.csv"),
            File("app/src/main/assets/santri_master.csv"),
            File("../seed/santri_master.csv"),
            File("seed/santri_master.csv")
        ).first { it.exists() }

        val seeder = SantriSeeder(db.santriDao()) { assetFile.readText(Charsets.UTF_8) }

        val dimuatPertama = seeder.seedIfEmpty()
        assertEquals(275, dimuatPertama)
        assertEquals(275, db.santriDao().count())

        val dimuatKedua = seeder.seedIfEmpty()
        assertEquals(0, dimuatKedua)
        assertEquals(275, db.santriDao().count())
    }
}

