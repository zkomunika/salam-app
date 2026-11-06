package app.pantauizin.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.pantauizin.AppContainer
import app.pantauizin.notify.NotificationHelper

enum class MainTab(val route: String, val label: String, val icon: ImageVector) {
    IZIN("izin", "Izin", Icons.Default.Assignment),
    TERLAMBAT("terlambat", "Terlambat", Icons.Default.Warning),
    NOTIFIKASI("notifikasi", "Notifikasi", Icons.Default.Notifications),
    PENGATURAN("pengaturan", "Pengaturan", Icons.Default.Settings)
}

@Composable
fun AppNav(
    container: AppContainer? = null,
    initialTab: String? = null
) {
    val startTab = if (initialTab == NotificationHelper.TAB_TERLAMBAT) MainTab.TERLAMBAT else MainTab.IZIN
    var currentTab by rememberSaveable(initialTab) { mutableStateOf(startTab) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Layar ${currentTab.label}")
        }
    }
}

