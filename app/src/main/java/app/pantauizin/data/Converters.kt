package app.pantauizin.data

import androidx.room.TypeConverter
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.BatasJenis
import app.pantauizin.domain.NotifJenis
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromAlasanJenis(value: AlasanJenis?): String? = value?.name

    @TypeConverter
    fun toAlasanJenis(value: String?): AlasanJenis? = value?.let(AlasanJenis::valueOf)

    @TypeConverter
    fun fromBatasJenis(value: BatasJenis?): String? = value?.name

    @TypeConverter
    fun toBatasJenis(value: String?): BatasJenis? = value?.let(BatasJenis::valueOf)

    @TypeConverter
    fun fromNotifJenis(value: NotifJenis?): String? = value?.name

    @TypeConverter
    fun toNotifJenis(value: String?): NotifJenis? = value?.let(NotifJenis::valueOf)
}

