package app.pantauizin.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SantriDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(l: List<SantriEntity>)

    @Query("SELECT COUNT(*) FROM santri")
    suspend fun count(): Int

    @Query("SELECT * FROM santri ORDER BY nama COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SantriEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(e: SantriEntity): Long

    @Update
    suspend fun update(e: SantriEntity)

    @Query("DELETE FROM santri WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM santri WHERE nama = :nama COLLATE NOCASE LIMIT 1")
    suspend fun findByNama(nama: String): SantriEntity?

    @Query("SELECT * FROM santri WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SantriEntity?
}

@Dao
interface IzinDao {
    @Insert
    suspend fun insert(e: IzinEntity): Long

    @Update
    suspend fun update(e: IzinEntity)

    @Query("DELETE FROM izin WHERE id = :id")
    suspend fun delete(id: Long)

    @Transaction
    @Query("SELECT * FROM izin ORDER BY dibuatPada DESC, id DESC")
    fun observeAll(): Flow<List<IzinWithKonfirmasi>>

    @Query("SELECT * FROM izin WHERE santriId = :santriId AND tanggalKembali IS NULL LIMIT 1")
    suspend fun findActiveBySantri(santriId: Long): IzinEntity?

    @Transaction
    @Query("SELECT * FROM izin WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): IzinWithKonfirmasi?

    @Query("SELECT * FROM izin")
    suspend fun getAllEntities(): List<IzinEntity>
}

@Dao
interface KonfirmasiDao {
    @Insert
    suspend fun insert(e: KonfirmasiEntity): Long
}

@Dao
interface NotifikasiDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(e: NotifikasiEntity): Long

    @Query("SELECT * FROM notifikasi WHERE izinId = :izinId ORDER BY tanggalKey ASC, id ASC")
    suspend fun forIzin(izinId: Long): List<NotifikasiEntity>

    @Query("SELECT * FROM notifikasi ORDER BY tanggalKey ASC, id ASC")
    suspend fun getAll(): List<NotifikasiEntity>

    @Query(
        """
        SELECT n.*, s.nama AS nama, s.kamar AS kamar
        FROM notifikasi n
        INNER JOIN izin i ON n.izinId = i.id
        INNER JOIN santri s ON i.santriId = s.id
        WHERE n.dibuatPada BETWEEN :fromMillis AND :toMillis
        ORDER BY n.dibuatPada DESC, n.id DESC
        """
    )
    fun observeBetween(fromMillis: Long, toMillis: Long): Flow<List<NotifikasiWithSantri>>

    @Query("UPDATE notifikasi SET dibaca = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE notifikasi SET dibaca = 1 WHERE izinId = :izinId")
    suspend fun markReadByIzin(izinId: Long)

    @Query("UPDATE notifikasi SET dibaca = 1")
    suspend fun markAllRead()
}

