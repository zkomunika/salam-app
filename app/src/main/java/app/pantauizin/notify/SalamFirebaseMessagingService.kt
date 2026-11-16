package app.pantauizin.notify

import android.util.Log
import app.pantauizin.domain.NotifJenis
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.time.LocalDate

class SalamFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed FCM token: $token")
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "SALAM APP"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Pemberitahuan izin santri"
        val izinId = remoteMessage.data["izinId"]?.toLongOrNull() ?: System.currentTimeMillis()

        val helper = NotificationHelper(applicationContext)
        helper.show(izinId = izinId, text = "$title - $body")
    }

    private fun saveTokenToFirestore(token: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val data = mapOf(
            "token" to token,
            "updatedAt" to System.currentTimeMillis(),
            "device" to "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        )
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("fcmTokens")
            .document(token)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.w("FCM", "Gagal menyimpan FCM token", e)
            }
    }
}
