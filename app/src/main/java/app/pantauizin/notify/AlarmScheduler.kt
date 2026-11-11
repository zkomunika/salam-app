package app.pantauizin.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.pantauizin.MainActivity
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) : Scheduler {
    override fun schedule(at: LocalDateTime) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerMillis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(context, SweepReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val showIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_SHOW,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Alarm jenis "jam alarm": diperlakukan sistem sebagai alarm yang terlihat pengguna,
        // menembus Doze, tidak butuh izin alarm tepat, dan lebih jarang dihapus oleh manajer
        // latar belakang vendor dibanding setExactAndAllowWhileIdle.
        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerMillis, showIntent),
                pendingIntent
            )
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    companion object {
        private const val REQUEST_CODE = 1001
        private const val REQUEST_CODE_SHOW = 1002
    }
}
