package app.pantauizin.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.BatasJenis
import app.pantauizin.domain.NotifJenis
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "santri",
    indices = [Index(value = ["nama"], unique = true)]
)
data class SantriEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val nama: String,
    val kamar: String,
    val aktif: Boolean = true
)

@Entity(
    tableName = "izin",
    foreignKeys = [
        ForeignKey(
            entity = SantriEntity::class,
            parentColumns = ["id"],
            childColumns = ["santriId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("santriId")]
)
data class IzinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val santriId: Long,
    val tanggalPulang: LocalDate,
    val alasanJenis: AlasanJenis,
    val alasanDetail: String,
    val batasJenis: BatasJenis,
    val batasAwal: LocalDate,
    val noWaWali: String?,
    val tanggalKembali: LocalDate?,
    val dibuatPada: Instant,
    val pemberiIzin: String? = null
)

@Entity(
    tableName = "konfirmasi",
    foreignKeys = [
        ForeignKey(
            entity = IzinEntity::class,
            parentColumns = ["id"],
            childColumns = ["izinId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("izinId")]
)
data class KonfirmasiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val izinId: Long,
    val tanggalKembaliBaru: LocalDate?,
    val catatan: String?,
    val buktiPath: String?,
    val dibuatPada: Instant
)

@Entity(
    tableName = "notifikasi",
    foreignKeys = [
        ForeignKey(
            entity = IzinEntity::class,
            parentColumns = ["id"],
            childColumns = ["izinId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["izinId", "tanggalKey"], unique = true)]
)
data class NotifikasiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val izinId: Long,
    val jenis: NotifJenis,
    val pesan: String,
    val tanggalKey: LocalDate,
    val dibuatPada: Instant,
    val dibaca: Boolean = false
)

data class IzinWithKonfirmasi(
    @Embedded val izin: IzinEntity,
    @Relation(parentColumn = "santriId", entityColumn = "id")
    val santri: SantriEntity,
    @Relation(parentColumn = "id", entityColumn = "izinId")
    val konfirmasi: List<KonfirmasiEntity>
)

data class NotifikasiWithSantri(
    @Embedded val notifikasi: NotifikasiEntity,
    val nama: String,
    val kamar: String
)

