package app.pantauizin.ui.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.BatasJenis
import app.pantauizin.domain.FormField
import app.pantauizin.domain.MessageTemplateBuilder
import app.pantauizin.ui.components.DatePickerField
import app.pantauizin.ui.theme.StatusColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IzinFormScreen(
    viewModel: IzinFormViewModel,
    onDone: () -> Unit,
    onOpenActiveIzin: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var showPulangDatePicker by remember { mutableStateOf(false) }
    var showBatasDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var kamarExpanded by remember { mutableStateOf(false) }
    var alasanExpanded by remember { mutableStateOf(false) }
    var batasExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadInitial()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Ubah Izin Pulang" else "Tambah Izin Pulang") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (state.isEditMode) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Hapus izin",
                                tint = StatusColors.MerahText
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Data Santri & Alasan
            FormSectionCard(title = "1. Data Santri & Alasan") {
                ExposedDropdownMenuBox(
                    expanded = kamarExpanded,
                    onExpandedChange = { kamarExpanded = !kamarExpanded }
                ) {
                    OutlinedTextField(
                        value = state.selectedKamar?.let { "Kamar $it" } ?: "Semua kamar",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kamar") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kamarExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = kamarExpanded,
                        onDismissRequest = { kamarExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Semua kamar") },
                            onClick = {
                                viewModel.pilihKamar(null)
                                kamarExpanded = false
                            }
                        )
                        state.kamarOptions.forEach { kamar ->
                            DropdownMenuItem(
                                text = { Text("Kamar $kamar") },
                                onClick = {
                                    viewModel.pilihKamar(kamar)
                                    kamarExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.namaQuery,
                    onValueChange = { viewModel.ubahNamaQuery(it) },
                    label = { Text("Nama santri") },
                    isError = state.errors.containsKey(FormField.SANTRI),
                    supportingText = {
                        state.errors[FormField.SANTRI]?.let { Text(it) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.form.santriId == null && state.saranSantri.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            state.saranSantri.take(8).forEach { santri ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clickable { viewModel.pilihSantri(santri.id) }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = santri.nama,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 12.dp)
                                    )
                                    Text(
                                        text = "Kamar ${santri.kamar}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                state.activeIzinIdWarning?.let { activeId ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = StatusColors.MerahContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = StatusColors.MerahText
                                )
                                Text(
                                    "Santri ini masih tercatat izin dan belum kembali",
                                    color = StatusColors.MerahText,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            OutlinedButton(
                                onClick = { onOpenActiveIzin(activeId) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Buka izin tersebut")
                            }
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = alasanExpanded,
                    onExpandedChange = { alasanExpanded = !alasanExpanded }
                ) {
                    val labelAlasan = when (state.form.alasanJenis) {
                        AlasanJenis.SAKIT -> "Sakit"
                        AlasanJenis.LAIN_LAIN -> "Lain-lain"
                        null -> ""
                    }
                    OutlinedTextField(
                        value = labelAlasan,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Alasan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = alasanExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = alasanExpanded,
                        onDismissRequest = { alasanExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sakit") },
                            onClick = {
                                viewModel.ubahAlasanJenis(AlasanJenis.SAKIT)
                                alasanExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Lain-lain") },
                            onClick = {
                                viewModel.ubahAlasanJenis(AlasanJenis.LAIN_LAIN)
                                alasanExpanded = false
                            }
                        )
                    }
                }

                val detailLabel = if (state.form.alasanJenis == AlasanJenis.SAKIT) {
                    "Nama penyakit"
                } else {
                    "Keperluan pulang"
                }
                OutlinedTextField(
                    value = state.form.alasanDetail,
                    onValueChange = { viewModel.ubahAlasanDetail(it) },
                    label = { Text(detailLabel) },
                    isError = state.errors.containsKey(FormField.ALASAN_DETAIL),
                    supportingText = {
                        state.errors[FormField.ALASAN_DETAIL]?.let { Text(it) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Jadwal & Batas Kembali
            FormSectionCard(title = "2. Jadwal & Batas Kembali") {
                DatePickerField(
                    value = MessageTemplateBuilder.formatTanggal(state.form.tanggalPulang),
                    label = "Tanggal pulang",
                    contentDescription = "Pilih tanggal pulang",
                    onClick = { showPulangDatePicker = true }
                )

                ExposedDropdownMenuBox(
                    expanded = batasExpanded,
                    onExpandedChange = { batasExpanded = !batasExpanded }
                ) {
                    val labelBatas = when (state.form.batasJenis) {
                        BatasJenis.HARI_1 -> "1 hari"
                        BatasJenis.HARI_2 -> "2 hari"
                        BatasJenis.HARI_3 -> "3 hari"
                        BatasJenis.LEBIH_3_HARI -> ">3 hari"
                        null -> ""
                    }
                    OutlinedTextField(
                        value = labelBatas,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Batas waktu") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = batasExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = batasExpanded,
                        onDismissRequest = { batasExpanded = false }
                    ) {
                        listOf(
                            BatasJenis.HARI_1 to "1 hari",
                            BatasJenis.HARI_2 to "2 hari",
                            BatasJenis.HARI_3 to "3 hari",
                            BatasJenis.LEBIH_3_HARI to ">3 hari"
                        ).forEach { (jenis, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.ubahBatasJenis(jenis)
                                    batasExpanded = false
                                }
                            )
                        }
                    }
                }

                if (state.form.batasJenis == BatasJenis.LEBIH_3_HARI) {
                    DatePickerField(
                        value = state.form.tanggalBatasPilihan?.let(MessageTemplateBuilder::formatTanggal) ?: "",
                        label = "Tanggal kembali",
                        contentDescription = "Pilih tanggal kembali",
                        isError = state.errors.containsKey(FormField.BATAS_TANGGAL),
                        errorText = state.errors[FormField.BATAS_TANGGAL],
                        onClick = { showBatasDatePicker = true }
                    )
                }

                state.preview?.let { prev ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (prev.hariLewat > 0) {
                                StatusColors.MerahContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Batas kembali: ${MessageTemplateBuilder.formatTanggal(prev.batasAwal)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (prev.hariLewat > 0) {
                                    StatusColors.MerahText
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            if (prev.hariLewat > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = StatusColors.MerahText
                                    )
                                    Text(
                                        "Sudah lewat ${prev.hariLewat} hari, santri akan langsung tercatat terlambat",
                                        color = StatusColors.MerahText,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Kontak Wali
            FormSectionCard(title = "3. Kontak Wali") {
                OutlinedTextField(
                    value = state.form.noWa,
                    onValueChange = { viewModel.ubahNoWa(it) },
                    label = { Text("Nomor WA wali (opsional)") },
                    isError = state.errors.containsKey(FormField.NO_WA),
                    supportingText = {
                        state.errors[FormField.NO_WA]?.let { Text(it) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        if (viewModel.simpan()) {
                            onDone()
                        }
                    }
                },
                enabled = state.activeIzinIdWarning == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("Simpan")
            }

            if (state.isEditMode) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusColors.MerahText),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text("Hapus Izin")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showPulangDatePicker) {
        LocalDatePickerDialog(
            initialDate = state.form.tanggalPulang,
            onDismiss = { showPulangDatePicker = false },
            onConfirm = { picked ->
                viewModel.ubahTanggalPulang(picked)
                showPulangDatePicker = false
            }
        )
    }

    if (showBatasDatePicker) {
        LocalDatePickerDialog(
            initialDate = state.form.tanggalBatasPilihan ?: state.form.tanggalPulang.plusDays(4),
            onDismiss = { showBatasDatePicker = false },
            onConfirm = { picked ->
                viewModel.ubahTanggalBatasPilihan(picked)
                showBatasDatePicker = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Izin?") },
            text = { Text("Menghapus izin juga akan menghapus seluruh konfirmasi, riwayat notifikasi, dan bukti file santri ini.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            if (viewModel.hapus()) {
                                showDeleteConfirm = false
                                onDone()
                            }
                        }
                    }
                ) {
                    Text("Hapus", color = StatusColors.MerahText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun FormSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val initialMillis = initialDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        onConfirm(date)
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Pilih")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

