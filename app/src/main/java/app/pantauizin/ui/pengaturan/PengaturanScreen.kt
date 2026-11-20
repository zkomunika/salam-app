package app.pantauizin.ui.pengaturan

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.pantauizin.data.firebase.UpdatePolicy
import app.pantauizin.domain.TemplateVariant
import app.pantauizin.notify.SweepSource
import app.pantauizin.ui.components.CircularIconBadge
import app.pantauizin.ui.components.PillFilterButton
import app.pantauizin.ui.components.ThemeToggleButton
import app.pantauizin.ui.components.UpdateDialog
import app.pantauizin.ui.components.softOutlinedTextFieldColors
import app.pantauizin.ui.components.userInitials
import app.pantauizin.ui.theme.BrandColors
import app.pantauizin.ui.theme.StatusColors
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
private val STATUS_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss")

private fun labelSumber(s: SweepSource): String = when (s) {
    SweepSource.ALARM -> "alarm"
    SweepSource.WORKER -> "pekerja cadangan"
    SweepSource.APP_OPEN -> "app dibuka"
    SweepSource.BOOT -> "HP menyala"
}

private fun bukaMulaiOtomatis(context: Context) {
    val kandidat = listOf(
        ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
        ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
        ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")
    )
    for (komponen in kandidat) {
        val intent = Intent().apply {
            component = komponen
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            return
        } catch (_: Exception) {
            // coba kandidat berikutnya
        }
    }
    val detail = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(detail)
    } catch (_: Exception) {
        Toast.makeText(context, "Buka manual: Pengaturan > Aplikasi > Mulai otomatis", Toast.LENGTH_LONG).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PengaturanScreen(
    viewModel: PengaturanViewModel,
    onOpenDataSantri: () -> Unit,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var showTimePicker by remember { mutableStateOf(false) }
    var showPublishUpdateDialog by remember { mutableStateOf(false) }
    var showTemplateSection by rememberSaveable { mutableStateOf(false) }
    var showSyncSection by rememberSaveable { mutableStateOf(false) }
    var showDiagnosticSection by rememberSaveable { mutableStateOf(false) }
    var selectedTemplateTab by rememberSaveable { mutableStateOf(TemplateVariant.A) }
    var draftTemplateA by remember { mutableStateOf(state.templateA) }
    var draftTemplateB by remember { mutableStateOf(state.templateB) }

    LaunchedEffect(state.templateA) {
        draftTemplateA = state.templateA
    }
    LaunchedEffect(state.templateB) {
        draftTemplateB = state.templateB
    }

    val cardBorder = BorderStroke(1.25.dp, MaterialTheme.colorScheme.outlineVariant)
    val user = state.currentUser
    val displayName = user?.displayNama?.ifBlank { "Pengurus" } ?: "Pengurus"
    val roleLabel = when {
        user?.isAdmin == true -> "Admin Asrama"
        user?.isPengurusInti == true -> "Pengurus Inti"
        else -> "Anggota (Hanya Baca)"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Pengaturan & Profil",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Center)
            )
            ThemeToggleButton(
                isDarkTheme = isDarkTheme,
                onToggle = onToggleDarkTheme,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        // 2. Centered Profile Hero
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userInitials(displayName),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user?.email ?: "pengurus@asas.id",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Text(
                    text = roleLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }
        }

        // 3. 2x2 Bento Stats Grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BentoStatCard(
                    icon = Icons.Default.People,
                    iconBg = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    value = "${state.totalSantriAktif} Santri",
                    subtitle = "Santri Aktif",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = if (user?.canManageSantri == true) onOpenDataSantri else null
                )
                BentoStatCard(
                    icon = Icons.Default.CheckCircle,
                    iconBg = StatusColors.HijauContainer,
                    iconTint = StatusColors.HijauText,
                    value = state.persenTepatWaktu?.let { "$it% Tepat" } ?: "Belum ada",
                    subtitle = "Disiplin Kembali",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = null
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BentoStatCard(
                    icon = Icons.Default.AccessTime,
                    iconBg = StatusColors.AmberContainer,
                    iconTint = StatusColors.AmberText,
                    value = "${state.jamPengingat.format(TIME_FORMAT)} WIB",
                    subtitle = "Pengingat Harian",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = { showTimePicker = true }
                )
                BentoStatCard(
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    iconBg = StatusColors.MerahContainer,
                    iconTint = StatusColors.MerahText,
                    value = "${state.izinAktifCount} Sedang Izin",
                    subtitle = "${state.terlambatCount} Terlambat",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = null
                )
            }
        }

        // 4. Grouped Settings Menu Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = cardBorder,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                var needsDivider = false

                if (user?.canManageSantri == true) {
                    MenuRowItem(
                        icon = Icons.Default.People,
                        iconBg = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Kelola Data Santri",
                        subtitle = "Daftar santri, pindah kamar & status aktif",
                        onClick = onOpenDataSantri,
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    needsDivider = true
                }

                if (user?.isAdmin == true) {
                    if (needsDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    MenuRowItem(
                        icon = Icons.AutoMirrored.Filled.Message,
                        iconBg = StatusColors.HijauContainer,
                        iconTint = StatusColors.HijauText,
                        title = "Template Pesan WhatsApp",
                        subtitle = "Format pengingat otomatis wali (Varian A & B)",
                        onClick = { showTemplateSection = !showTemplateSection },
                        trailing = {
                            Icon(
                                imageVector = if (showTemplateSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    if (showTemplateSection) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Penampung: {nama}, {kamar}, {batas_awal}, {batas_aktif}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PillFilterButton(
                                    label = "Varian A",
                                    selected = selectedTemplateTab == TemplateVariant.A,
                                    onClick = { selectedTemplateTab = TemplateVariant.A }
                                )
                                PillFilterButton(
                                    label = "Varian B",
                                    selected = selectedTemplateTab == TemplateVariant.B,
                                    onClick = { selectedTemplateTab = TemplateVariant.B }
                                )
                            }
                            val isA = selectedTemplateTab == TemplateVariant.A
                            val draft = if (isA) draftTemplateA else draftTemplateB
                            val errorText = if (isA) state.pesanErrorTemplateA else state.pesanErrorTemplateB
                            val variantName = if (isA) "Varian A" else "Varian B"

                            OutlinedTextField(
                                value = draft,
                                onValueChange = { updated ->
                                    if (isA) draftTemplateA = updated else draftTemplateB = updated
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = softOutlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4
                            )
                            errorText?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            if (viewModel.simpanTemplate(selectedTemplateTab, draft)) {
                                                Toast.makeText(
                                                    context,
                                                    "Template $variantName disimpan",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.heightIn(min = 44.dp)
                                ) {
                                    Text("Simpan $variantName")
                                }
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            viewModel.kembalikanTemplate(selectedTemplateTab)
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.heightIn(min = 44.dp)
                                ) {
                                    Text("Kembalikan bawaan")
                                }
                            }
                        }
                    }
                    needsDivider = true
                }

                if (needsDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                MenuRowItem(
                    icon = Icons.Default.AccessTime,
                    iconBg = StatusColors.AmberContainer,
                    iconTint = StatusColors.AmberText,
                    title = "Jadwal Pengingat Harian",
                    subtitle = "Sapuan otomatis dijalankan setiap hari",
                    onClick = { showTimePicker = true },
                    trailing = {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = state.jamPengingat.format(TIME_FORMAT),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                MenuRowItem(
                    icon = Icons.Default.Sync,
                    iconBg = StatusColors.BiruContainer,
                    iconTint = StatusColors.BiruText,
                    title = "Sinkronisasi Cloud",
                    subtitle = state.syncResult ?: "Tarik data terbaru dari server",
                    onClick = { showSyncSection = !showSyncSection },
                    trailing = {
                        Icon(
                            imageVector = if (showSyncSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                if (showSyncSection) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.sinkronkanSekarang() },
                            enabled = !state.isSyncing,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (state.isSyncing) "Menyinkronkan..." else "Sinkronkan / Refresh Data")
                        }
                        if (state.syncResult != null) {
                            Text(
                                text = state.syncResult.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (user?.isAdmin == true) {
                            Button(
                                onClick = { viewModel.migrasiDataKeCloud() },
                                enabled = !state.isMigrating,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (state.isMigrating) (state.migrationProgress ?: "Mengunggah...") else "Unggah Data Lokal ke Cloud")
                            }
                            if (state.migrationResult != null) {
                                Text(
                                    text = state.migrationResult.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                MenuRowItem(
                    icon = Icons.Default.NotificationsActive,
                    iconBg = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Status & Uji Pengingat",
                    subtitle = "Diagnosis alarm, tes notifikasi & Mulai Otomatis",
                    onClick = { showDiagnosticSection = !showDiagnosticSection },
                    trailing = {
                        Icon(
                            imageVector = if (showDiagnosticSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                if (showDiagnosticSection) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val belum = "belum pernah"
                        Text(
                            text = "Alarm berikutnya: ${state.nextAlarmAt?.format(STATUS_FORMAT) ?: "belum dijadwalkan"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Alarm terakhir berbunyi: ${state.lastAlarmFiredAt?.format(STATUS_FORMAT) ?: belum}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Sapuan terakhir: ${state.lastRunAt?.format(STATUS_FORMAT) ?: belum}" +
                                (state.lastRunSource?.let { " (dari ${labelSumber(it)})" } ?: ""),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Jika alarm tetap kosong padahal jam pengingat sudah lewat saat app ditutup, aktifkan Mulai Otomatis untuk app ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.testAlarmAt?.let { testAt ->
                            Text(
                                text = "Alarm uji dipasang untuk ${testAt.format(STATUS_FORMAT)} — segera tutup app dari Recent dan tunggu 10 detik.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.tesNotifikasi() },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Text("Tes notifikasi")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.tesAlarm()
                                    Toast.makeText(
                                        context,
                                        "Alarm uji dipasang 10 detik lagi. Tutup app dari Recent sekarang.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Text("Tes alarm (10 dtk)")
                            }
                            OutlinedButton(
                                onClick = { bukaMulaiOtomatis(context) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Text("Mulai Otomatis")
                            }
                        }
                    }
                }

                if (viewModel.updateManager != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularIconBadge(
                                icon = Icons.Default.SystemUpdate,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.primary,
                                size = 40.dp,
                                iconSize = 20.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pembaruan Aplikasi",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Versi terpasang: v${viewModel.updateManager.currentVersionName} (kode ${viewModel.updateManager.currentVersionCode})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        state.updateCheckMessage?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.cekPembaruan() },
                                enabled = !state.isCheckingUpdate,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Text(if (state.isCheckingUpdate) "Memeriksa..." else "Cek Pembaruan")
                            }
                            if (user?.isAdmin == true) {
                                Button(
                                    onClick = { showPublishUpdateDialog = true },
                                    enabled = !state.isPublishingUpdate,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.heightIn(min = 44.dp)
                                ) {
                                    Text("Rilis Update (Admin)")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Full-width Logout CTA Pill
        if (user != null) {
            Button(
                onClick = { viewModel.logout() },
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Keluar dari Akun Pengurus",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    state.foundUpdate?.let { updateInfo ->
        viewModel.updateManager?.let { um ->
            UpdateDialog(
                updateInfo = updateInfo,
                updateManager = um,
                onDismiss = { viewModel.tutupDialogUpdate() }
            )
        }
    }

    if (showPublishUpdateDialog && viewModel.updateManager != null) {
        val um = viewModel.updateManager
        val defaultCode = UpdatePolicy.defaultPublishVersionCode(um.currentVersionCode).toString()
        val defaultName = UpdatePolicy.defaultPublishVersionName(um.currentVersionName)
        val defaultUrl = UpdatePolicy.defaultApkUrl(defaultName)
        var verCodeStr by remember { mutableStateOf(defaultCode) }
        var verNameStr by remember { mutableStateOf(defaultName) }
        var apkUrlStr by remember { mutableStateOf(defaultUrl) }
        var notesStr by remember { mutableStateOf("Pembaruan sistem, peningkatan kestabilan, dan perbaikan antarmuka.") }
        var forceChecked by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val last = um.getLatestConfig()
            if (last != null && last.latestVersionCode == um.currentVersionCode) {
                if (last.apkUrl.isNotBlank()) {
                    apkUrlStr = last.apkUrl
                }
                if (last.releaseNotes.isNotBlank()) {
                    notesStr = last.releaseNotes
                }
            }
        }

        val typedCode = verCodeStr.toIntOrNull()

        AlertDialog(
            onDismissRequest = { showPublishUpdateDialog = false },
            title = { Text("Publikasikan Info Update") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Versi di HP Anda saat ini: v${um.currentVersionName} (kode ${um.currentVersionCode})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = verCodeStr,
                        onValueChange = { verCodeStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Kode Versi (sesuai APK, misal: ${um.currentVersionCode})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = verNameStr,
                        onValueChange = { verNameStr = it },
                        label = { Text("Nama Versi (sesuai APK, misal: ${um.currentVersionName})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = apkUrlStr,
                        onValueChange = { apkUrlStr = it },
                        label = { Text("Link Download Langsung APK (.apk)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notesStr,
                        onValueChange = { notesStr = it },
                        label = { Text("Catatan Pembaruan") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (typedCode != null && typedCode > um.currentVersionCode) {
                        Text(
                            text = "Perhatian: Kode versi ($typedCode) lebih besar dari versi HP Anda (${um.currentVersionCode}). Pastikan APK yang diunggah benar-benar berkode $typedCode, kalau tidak pop-up update akan muncul terus-menerus.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = forceChecked,
                            onCheckedChange = { forceChecked = it }
                        )
                        Text("Wajib update (tidak bisa dilewati)")
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = verCodeStr.isNotBlank() && verNameStr.isNotBlank() && apkUrlStr.isNotBlank(),
                    onClick = {
                        val code = verCodeStr.toIntOrNull() ?: return@Button
                        viewModel.publikasikanUpdate(
                            versionCode = code,
                            versionName = verNameStr.trim(),
                            apkUrl = apkUrlStr.trim(),
                            releaseNotes = notesStr.trim(),
                            forceUpdate = forceChecked
                        )
                        showPublishUpdateDialog = false
                    }
                ) {
                    Text("Publikasikan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublishUpdateDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = state.jamPengingat.hour,
            initialMinute = state.jamPengingat.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pilih Jam Pengingat (24 Jam)") },
            text = {
                TimePicker(state = pickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTime = LocalTime.of(pickerState.hour, pickerState.minute)
                        showTimePicker = false
                        scope.launch {
                            viewModel.ubahJam(newTime)
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun BentoStatCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.let {
            if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it
        },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularIconBadge(
                icon = icon,
                containerColor = iconBg,
                iconTint = iconTint,
                size = 36.dp,
                iconSize = 18.dp
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MenuRowItem(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularIconBadge(
            icon = icon,
            containerColor = iconBg,
            iconTint = iconTint,
            size = 40.dp,
            iconSize = 20.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing()
    }
}
