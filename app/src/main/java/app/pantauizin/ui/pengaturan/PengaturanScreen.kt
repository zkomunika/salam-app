package app.pantauizin.ui.pengaturan

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pantauizin.data.firebase.UpdatePolicy
import app.pantauizin.domain.TemplateVariant
import app.pantauizin.notify.SweepSource
import app.pantauizin.ui.components.UpdateDialog
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var showTimePicker by remember { mutableStateOf(false) }
    var showPublishUpdateDialog by remember { mutableStateOf(false) }
    var selectedTemplateTab by rememberSaveable { mutableStateOf(TemplateVariant.A) }
    var draftTemplateA by remember { mutableStateOf(state.templateA) }
    var draftTemplateB by remember { mutableStateOf(state.templateB) }

    LaunchedEffect(state.templateA) {
        draftTemplateA = state.templateA
    }
    LaunchedEffect(state.templateB) {
        draftTemplateB = state.templateB
    }

    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val cardElevation = CardDefaults.cardElevation(defaultElevation = 1.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. Akun Pengurus & Cloud Sync
        if (state.currentUser != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = cardBorder,
                elevation = cardElevation
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.currentUser?.email ?: "Pengurus",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    state.currentUser?.isAdmin == true -> "Role: Admin Asrama"
                                    state.currentUser?.isPengurusInti == true -> "Role: Pengurus Inti"
                                    else -> "Role: Anggota (Hanya Baca)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.currentUser?.canManageIzin == true) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Sinkronisasi Cloud:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { viewModel.sinkronkanSekarang() },
                        enabled = !state.isSyncing,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (state.isSyncing) "Menyinkronkan..." else "Sinkronkan / Refresh Data")
                    }
                    if (state.syncResult != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.syncResult ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (state.currentUser?.isAdmin == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.migrasiDataKeCloud() },
                            enabled = !state.isMigrating,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (state.isMigrating) (state.migrationProgress ?: "Mengunggah...") else "Unggah Data Lokal ke Cloud")
                        }
                        if (state.migrationResult != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = state.migrationResult ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Keluar Akun (Logout)")
                    }
                }
            }
        }

        // 1. Jam pengingat harian
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = cardBorder,
            elevation = cardElevation
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Jam Pengingat Harian",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sapuan otomatis dijalankan setiap hari pada pukul ${state.jamPengingat.format(TIME_FORMAT)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Ubah Jam (${state.jamPengingat.format(TIME_FORMAT)})")
                }
            }
        }

        // 1b. Status pengingat (diagnosis)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = cardBorder,
            elevation = cardElevation
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Status Pengingat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                val belum = "belum pernah"
                Text(
                    text = "Alarm berikutnya: ${state.nextAlarmAt?.format(STATUS_FORMAT) ?: "belum dijadwalkan"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Alarm terakhir berbunyi: ${state.lastAlarmFiredAt?.format(STATUS_FORMAT) ?: belum}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Sapuan terakhir: ${state.lastRunAt?.format(STATUS_FORMAT) ?: belum}" +
                        (state.lastRunSource?.let { " (dari ${labelSumber(it)})" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Jika \"Alarm terakhir berbunyi\" tetap kosong padahal jam pengingat sudah lewat saat app ditutup, " +
                        "alarm dihentikan HP. Aktifkan Mulai Otomatis untuk app ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.testAlarmAt?.let { testAt ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Alarm uji dipasang untuk ${testAt.format(STATUS_FORMAT)} — segera tutup app dari Recent dan tunggu 10 detik.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.tesNotifikasi() },
                        modifier = Modifier.heightIn(min = 48.dp)
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
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Tes alarm (10 dtk)")
                    }
                    OutlinedButton(
                        onClick = { bukaMulaiOtomatis(context) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Mulai Otomatis")
                    }
                }
            }
        }

        // 2. Data santri (hanya untuk Admin & pengurus yang ditunjuk)
        if (state.currentUser?.canManageSantri == true) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = cardBorder,
                elevation = cardElevation
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Data Master Santri",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Kelola daftar santri, pindah kamar, atau nonaktifkan santri yang sudah lulus/boyong.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onOpenDataSantri,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Kelola Data Santri")
                    }
                }
            }
        }

        // 3. Template pesan A dan B (hanya Admin)
        if (state.currentUser?.isAdmin == true) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = cardBorder,
                elevation = cardElevation
            ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Template Pesan Wali",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Penampung: {nama}, {kamar}, {batas_awal}, {batas_aktif}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTemplateTab == TemplateVariant.A,
                        onClick = { selectedTemplateTab = TemplateVariant.A },
                        label = { Text("Varian A (Belum Konfirmasi)") }
                    )
                    FilterChip(
                        selected = selectedTemplateTab == TemplateVariant.B,
                        onClick = { selectedTemplateTab = TemplateVariant.B },
                        label = { Text("Varian B (Sudah Konfirmasi)") }
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
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Simpan $variantName")
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                viewModel.kembalikanTemplate(selectedTemplateTab)
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Kembalikan bawaan")
                    }
                }
            }
            }
        }

        // 4. Pembaruan Aplikasi (In-App Update)
        if (viewModel.updateManager != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = cardBorder,
                elevation = cardElevation
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pembaruan Aplikasi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Versi terpasang saat ini: v${viewModel.updateManager.currentVersionName} (kode ${viewModel.updateManager.currentVersionCode})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    state.updateCheckMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.cekPembaruan() },
                            enabled = !state.isCheckingUpdate,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(if (state.isCheckingUpdate) "Memeriksa..." else "Cek Pembaruan")
                        }
                        if (state.currentUser?.isAdmin == true) {
                            Button(
                                onClick = { showPublishUpdateDialog = true },
                                enabled = !state.isPublishingUpdate,
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Rilis Update (Admin)")
                            }
                        }
                    }
                }
            }
        }
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

