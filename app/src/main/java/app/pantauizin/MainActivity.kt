package app.pantauizin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import app.pantauizin.notify.NotificationHelper
import app.pantauizin.ui.nav.AppNav
import app.pantauizin.ui.theme.PantauIzinTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var openTab by mutableStateOf<String?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        handleIntent(intent)

        val appContainer = (application as? PantauIzinApp)?.container
        setContent {
            PantauIzinTheme {
                AppNav(
                    container = appContainer,
                    initialTab = openTab
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        openTab = intent?.getStringExtra(NotificationHelper.EXTRA_OPEN_TAB)
        val izinId = intent?.getLongExtra(NotificationHelper.EXTRA_NOTIF_IZIN_ID, -1L) ?: -1L
        if (izinId > 0) {
            val appContainer = (application as? PantauIzinApp)?.container ?: return
            lifecycleScope.launch {
                appContainer.notifikasiRepository.markReadByIzin(izinId)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
