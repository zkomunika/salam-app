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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.pantauizin.AppContainer
import app.pantauizin.notify.NotificationHelper
import app.pantauizin.ui.form.IzinFormScreen
import app.pantauizin.ui.form.IzinFormViewModel
import app.pantauizin.ui.izin.IzinListScreen
import app.pantauizin.ui.izin.IzinListViewModel
import app.pantauizin.ui.konfirmasi.KonfirmasiFormScreen
import app.pantauizin.ui.konfirmasi.KonfirmasiFormViewModel
import app.pantauizin.ui.terlambat.TerlambatScreen
import app.pantauizin.ui.terlambat.TerlambatViewModel

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

    var showAddIzin by rememberSaveable { mutableStateOf(false) }
    var editIzinId by rememberSaveable { mutableStateOf<Long?>(null) }
    var konfirmasiIzinId by rememberSaveable { mutableStateOf<Long?>(null) }

    if (container != null && (showAddIzin || editIzinId != null)) {
        val currentEditId = editIzinId
        val vm = remember(currentEditId) {
            IzinFormViewModel(
                santriRepo = container.santriRepository,
                izinRepo = container.izinRepository,
                editIzinId = currentEditId
            )
        }
        IzinFormScreen(
            viewModel = vm,
            onDone = {
                showAddIzin = false
                editIzinId = null
            },
            onOpenActiveIzin = { activeId ->
                showAddIzin = false
                editIzinId = activeId
            }
        )
        return
    }

    if (container != null && konfirmasiIzinId != null) {
        val targetIzinId = konfirmasiIzinId!!
        val vm = remember(targetIzinId) {
            KonfirmasiFormViewModel(
                izinRepo = container.izinRepository,
                izinId = targetIzinId
            )
        }
        KonfirmasiFormScreen(
            viewModel = vm,
            onSaved = { konfirmasiIzinId = null },
            onBack = { konfirmasiIzinId = null }
        )
        return
    }

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
        if (container == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Layar ${currentTab.label}")
            }
        } else {
            when (currentTab) {
                MainTab.IZIN -> {
                    val vm = remember(container) {
                        IzinListViewModel(izinRepo = container.izinRepository)
                    }
                    IzinListScreen(
                        viewModel = vm,
                        onAddIzin = { showAddIzin = true },
                        onEditIzin = { id -> editIzinId = id },
                        onOpenKonfirmasi = { id -> konfirmasiIzinId = id },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                MainTab.TERLAMBAT -> {
                    val vm = remember(container) {
                        TerlambatViewModel(
                            izinRepo = container.izinRepository,
                            settingsStore = container.settingsStore
                        )
                    }
                    TerlambatScreen(
                        viewModel = vm,
                        onOpenKonfirmasi = { id -> konfirmasiIzinId = id },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                MainTab.NOTIFIKASI,
                MainTab.PENGATURAN -> {
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
        }
    }
}
