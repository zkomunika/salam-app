package app.pantauizin.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed interface NotifRange {
    data object HariIni : NotifRange
    data object TujuhHari : NotifRange
    data object TigaPuluhHari : NotifRange
    data class Kustom(val dari: LocalDate, val sampai: LocalDate) : NotifRange
}

fun NotifRange.toInstants(today: LocalDate, zone: ZoneId): Pair<Instant, Instant> {
    val (dari, sampai) = when (this) {
        NotifRange.HariIni -> today to today
        NotifRange.TujuhHari -> today.minusDays(6) to today
        NotifRange.TigaPuluhHari -> today.minusDays(29) to today
        is NotifRange.Kustom -> {
            require(!this.dari.isAfter(this.sampai)) {
                "Tanggal awal tidak boleh setelah tanggal akhir"
            }
            this.dari to this.sampai
        }
    }
    val start = dari.atStartOfDay(zone).toInstant()
    val end = sampai.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1)
    return start to end
}

