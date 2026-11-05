package app.pantauizin.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object SweepSchedule {
    /**
     * Waktu sapuan berikutnya (spec 7.3): hari ini pada [jam] jika [jam] masih di depan [now]
     * dan [lastSweepDate] bukan hari ini; selain itu besok pada [jam].
     */
    fun next(now: LocalDateTime, jam: LocalTime, lastSweepDate: LocalDate?): LocalDateTime {
        val today = now.toLocalDate()
        return if (now.toLocalTime().isBefore(jam) && lastSweepDate != today) {
            LocalDateTime.of(today, jam)
        } else {
            LocalDateTime.of(today.plusDays(1), jam)
        }
    }

    /** Sapuan jatuh tempo bila jam sekarang >= [jam] dan sapuan hari ini belum jalan. */
    fun isDue(now: LocalDateTime, jam: LocalTime, lastSweepDate: LocalDate?): Boolean {
        val today = now.toLocalDate()
        return !now.toLocalTime().isBefore(jam) && lastSweepDate != today
    }
}

