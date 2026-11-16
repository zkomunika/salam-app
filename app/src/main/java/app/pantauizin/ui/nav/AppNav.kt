package app.pantauizin.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import app.pantauizin.ui.notifikasi.NotifikasiScreen
import app.pantauizin.ui.notifikasi.NotifikasiViewModel
import app.pantauizin.ui.pengaturan.DataSantriScreen
import app.pantauizin.ui.pengaturan.DataSantriViewModel
import app.pantauizin.ui.pengaturan.PengaturanScreen
import app.pantauizin.ui.pengaturan.PengaturanViewModel
import app.pantauizin.ui.terlambat.TerlambatScreen
import app.pantauizin.ui.terlambat.TerlambatViewModel
import app.pantauizin.ui.terlambat.sortTerlambat
import kotlinx.coroutines.flow.map
import java.time.LocalDate

enum class MainTab(val route: String, val label: String, val icon: ImageVector) {
    IZIN("izin", "Izin", Icons.AutoMirrored.Filled.Assignment),
    TERLAMBAT("terlambat", "Terlambat", Icons.Default.Warning),
    NOTIFIKASI("notifikasi", "Notifikasi", Icons.Default.Notifications),
    PENGATURAN("pengaturan", "Pengaturan", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var showDataSantri by rememberSaveable { mutableStateOf(false) }

    if (container != null) {
        val currentUser by container.authManager.currentUser.collectAsState()
        val isAuthLoading by container.authManager.isLoading.collectAsState()

        if (isAuthLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
            return
        }

        if (currentUser == null) {
            val loginVm = remember(container) {
                app.pantauizin.ui.auth.LoginViewModel(container.authManager)
            }
            app.pantauizin.ui.auth.LoginScreen(
                viewModel = loginVm,
                onLoginSuccess = { }
            )
            return
        }
    }

    if (container != null && (showAddIzin || editIzinId != null)) {
        BackHandler {
            showAddIzin = false
            editIzinId = null
        }
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
        BackHandler { konfirmasiIzinId = null }
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

    if (container != null && showDataSantri) {
        BackHandler { showDataSantri = false }
        val vm = remember(container) {
            DataSantriViewModel(santriRepo = container.santriRepository)
        }
        DataSantriScreen(
            viewModel = vm,
            onBack = { showDataSantri = false }
        )
        return
    }

    val terlambatCount by if (container != null) {
        remember(container) {
            container.izinRepository.observe().map { list ->
                sortTerlambat(list, LocalDate.now()).size
            }
        }.collectAsState(initial = 0)
    } else {
        remember { mutableStateOf(0) }
    }

    val unreadNotifCount by if (container != null) {
        remember(container) {
            container.notifikasiRepository.observeUnreadCount()
        }.collectAsState(initial = 0)
    } else {
        remember { mutableStateOf(0) }
    }

    val topTitle = when (currentTab) {
        MainTab.IZIN -> "Daftar Izin Santri"
        MainTab.TERLAMBAT -> "Pantau Santri Terlambat"
        MainTab.NOTIFIKASI -> "Riwayat Notifikasi"
        MainTab.PENGATURAN -> "Pengaturan Aplikasi"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(topTitle) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                MainTab.entries.forEach { tab ->
                    val badgeCount = when (tab) {
                        MainTab.TERLAMBAT -> terlambatCount
                        MainTab.NOTIFIKASI -> unreadNotifCount
                        else -> 0
                    }
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (badgeCount > 0) {
                                        Badge {
                                            Text(if (badgeCount > 99) "99+" else badgeCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(tab.icon, contentDescription = tab.label)
                            }
                        },
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
                MainTab.NOTIFIKASI -> {
                    val vm = remember(container) {
                        NotifikasiViewModel(notifRepo = container.notifikasiRepository)
                    }
                    NotifikasiScreen(
                        viewModel = vm,
                        onOpenTerlambat = { currentTab = MainTab.TERLAMBAT },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                MainTab.PENGATURAN -> {
                    val vm = remember(container) {
                        PengaturanViewModel(
                            settings = container.settingsStore,
                            authManager = container.authManager,
                            syncManager = container.syncManager,
                            onTesNotifikasi = { container.sweepRunner.tesNotifikasi() },
                            onTesAlarm = { container.sweepRunner.scheduleTestAlarm() },
                            onReschedule = { container.sweepRunner.rescheduleOnly() }
                        )
                    }
                    PengaturanScreen(
                        viewModel = vm,
                        onOpenDataSantri = { showDataSantri = true },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
