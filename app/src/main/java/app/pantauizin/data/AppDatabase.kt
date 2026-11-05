package app.pantauizin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SantriEntity::class,
        IzinEntity::class,
        KonfirmasiEntity::class,
        NotifikasiEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun santriDao(): SantriDao
    abstract fun izinDao(): IzinDao
    abstract fun konfirmasiDao(): KonfirmasiDao
    abstract fun notifikasiDao(): NotifikasiDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "pantau_izin.db")
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.setForeignKeyConstraintsEnabled(true)
                    }
                })
                .build()
    }
}

