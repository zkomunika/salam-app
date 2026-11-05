package app.pantauizin.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.pantauizin.MainActivity

class NotificationHelper(private val context: Context) : Notifier {
    init {
        ensureChannel()
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Pengingat santri yang melewati batas waktu izin pulang"
        }
        manager.createNotificationChannel(channel)
    }

    override fun show(izinId: Long, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TAB, TAB_TERLAMBAT)
            putExtra(EXTRA_NOTIF_IZIN_ID, izinId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            izinId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(CHANNEL_NAME)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(izinId.toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "santri_terlambat"
        const val CHANNEL_NAME = "Santri terlambat"
        const val EXTRA_OPEN_TAB = "open_tab"
        const val TAB_TERLAMBAT = "terlambat"
        const val EXTRA_NOTIF_IZIN_ID = "notif_izin_id"
    }
}

