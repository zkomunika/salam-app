package app.pantauizin.ui.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.BatasJenis
import app.pantauizin.domain.FormField
import app.pantauizin.domain.MessageTemplateBuilder
import app.pantauizin.domain.RekapPulangLogic
import app.pantauizin.ui.components.DatePickerField
import app.pantauizin.ui.components.PillFilterButton
import app.pantauizin.ui.components.softOutlinedTextFieldColors
import app.pantauizin.ui.theme.BrandColors
import app.pantauizin.ui.theme.StatusColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID"))

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

    LaunchedEffect(Unit) {
        viewModel.loadInitial()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Custom Top Header matching prototype
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.25.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable(role = Role.Button, onClick = onDone)
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = if (state.isEditMode) "Ubah Izin Pulang" else "Catat Izin Santri",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center)
                )

                if (state.isEditMode) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(StatusColors.MerahContainer)
                            .clickable(role = Role.Button, onClick = { showDeleteConfirm = true })
                            .align(Alignment.CenterEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus izin",
                            tint = StatusColors.MerahText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Card 1: Pilih Santri
            FormSectionCard {
                Text(
                    text = "Pilih Santri",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                state.pemberiIzin?.takeIf { it.isNotBlank() }?.let { pengizin ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Pemberi izin: $pengizin",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

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
                        shape = RoundedCornerShape(16.dp),
                        colors = softOutlinedTextFieldColors(),
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
                    placeholder = { Text("Cari & pilih nama santri...") },
                    isError = state.errors.containsKey(FormField.SANTRI),
                    supportingText = {
                        state.errors[FormField.SANTRI]?.let { Text(it) }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = softOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.form.santriId == null && state.saranSantri.isNotEmpty()) {
                    val displayedSaran = if (state.selectedKamar != null) {
                        state.saranSantri
                    } else {
                        state.saranSantri.take(10)
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            displayedSaran.forEach { santri ->
                                val recentFlag = santri.id in state.recentPulangSantriIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clickable { viewModel.pilihSantri(santri.id) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = santri.nama,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 10.dp)
                                    )
                                    if (recentFlag) {
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 8.dp)
                                                .background(
                                                    color = StatusColors.AmberContainer,
                                                    shape = RoundedCornerShape(50)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "< 2 bln",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusColors.AmberText,
                                                softWrap = false,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Kamar ${santri.kamar}",
                                        style = MaterialTheme.typography.bodySmall,
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
                        shape = RoundedCornerShape(16.dp),
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
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Text("Buka izin tersebut")
                            }
                        }
                    }
                }

                state.recentPulangWarning?.let { w ->
                    val alasanLabel = if (w.alasanJenis == AlasanJenis.SAKIT) "Sakit" else "Lain-lain"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = StatusColors.AmberContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = StatusColors.AmberText
                                )
                                Text(
                                    text = "Belum genap 2 bulan sejak pulang terakhir",
                                    color = StatusColors.AmberText,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Terakhir pulang: ${MessageTemplateBuilder.formatTanggal(w.tanggalPulang)} (${RekapPulangLogic.formatSelisihHari(w.hariSejakPulang)})",
                                color = StatusColors.AmberText,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Alasan: $alasanLabel (${w.alasanDetail})",
                                color = StatusColors.AmberText,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (!w.pemberiIzin.isNullOrBlank()) {
                                Text(
                                    text = "Pemberi izin sebelumnya: ${w.pemberiIzin}",
                                    color = StatusColors.AmberText,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = "Batas 2 bulan berikutnya: ${MessageTemplateBuilder.formatTanggal(w.bolehPulangLagiMulai)} (sisa ${w.sisaHariKe2Bulan} hari)",
                                color = StatusColors.AmberText,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Hanya pemberitahuan — izin tetap dapat disimpan.",
                                color = StatusColors.AmberText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Card 2: Tanggal & Batas Kembali
            FormSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tanggal & Batas Kembali",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    state.preview?.let { prev ->
                        val pillBg = if (prev.hariLewat > 0) StatusColors.MerahContainer else BrandColors.SoftRoseContainer
                        val pillFg = if (prev.hariLewat > 0) StatusColors.MerahText else BrandColors.SoftRoseText
                        Box(
                            modifier = Modifier
                                .background(pillBg, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Batas: ${prev.batasAwal.format(SHORT_DATE_FORMAT)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = pillFg
                            )
                        }
                    }
                }

                DatePickerField(
                    value = MessageTemplateBuilder.formatTanggal(state.form.tanggalPulang),
                    label = "Tanggal pulang",
                    contentDescription = "Pilih tanggal pulang",
                    onClick = { showPulangDatePicker = true }
                )

                Text(
                    text = "Durasi Izin",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        BatasJenis.HARI_1 to "1 Hari",
                        BatasJenis.HARI_2 to "2 Hari",
                        BatasJenis.HARI_3 to "3 Hari"
                    ).forEach { (jenis, label) ->
                        PillFilterButton(
                            label = label,
                            selected = state.form.batasJenis == jenis,
                            onClick = { viewModel.ubahBatasJenis(jenis) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Dipertahankan khusus bila mengedit catatan lama berdurasi >3 hari
                if (state.form.batasJenis == BatasJenis.LEBIH_3_HARI) {
                    DatePickerField(
                        value = state.form.tanggalBatasPilihan?.let(MessageTemplateBuilder::formatTanggal) ?: "",
                        label = "Tanggal kembali (data lama >3 hari)",
                        contentDescription = "Pilih tanggal kembali",
                        isError = state.errors.containsKey(FormField.BATAS_TANGGAL),
                        errorText = state.errors[FormField.BATAS_TANGGAL],
                        onClick = { showBatasDatePicker = true }
                    )
                }

                state.preview?.let { prev ->
                    if (prev.hariLewat > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StatusColors.MerahContainer, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = StatusColors.MerahText,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Sudah lewat ${prev.hariLewat} hari, santri akan langsung tercatat terlambat.",
                                color = StatusColors.MerahText,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Card 3: Alasan Izin
            FormSectionCard {
                Text(
                    text = "Alasan Izin",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isSakit = state.form.alasanJenis == AlasanJenis.SAKIT
                    val isLain = state.form.alasanJenis == AlasanJenis.LAIN_LAIN
                    PillFilterButton(
                        label = "Sakit / Berobat",
                        selected = isSakit,
                        onClick = { viewModel.ubahAlasanJenis(AlasanJenis.SAKIT) },
                        modifier = Modifier.weight(1f)
                    )
                    PillFilterButton(
                        label = "Lain-lain",
                        selected = isLain,
                        onClick = { viewModel.ubahAlasanJenis(AlasanJenis.LAIN_LAIN) },
                        modifier = Modifier.weight(1f)
                    )
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
                    placeholder = {
                        Text(
                            if (state.form.alasanJenis == AlasanJenis.SAKIT) {
                                "Contoh: Demam & perlu istirahat"
                            } else {
                                "Contoh: Acara keluarga / urusan rumah"
                            }
                        )
                    },
                    isError = state.errors.containsKey(FormField.ALASAN_DETAIL),
                    supportingText = {
                        state.errors[FormField.ALASAN_DETAIL]?.let { Text(it) }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = softOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Card 4: Nomor WhatsApp Wali
            FormSectionCard {
                Text(
                    text = "Nomor WhatsApp Wali",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = state.form.noWa,
                    onValueChange = { viewModel.ubahNoWa(it) },
                    label = { Text("Nomor WA wali (opsional)") },
                    placeholder = { Text("0812-xxxx-xxxx") },
                    isError = state.errors.containsKey(FormField.NO_WA),
                    supportingText = {
                        state.errors[FormField.NO_WA]?.let { Text(it) }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = softOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    scope.launch {
                        if (viewModel.simpan()) {
                            onDone()
                        }
                    }
                },
                enabled = state.activeIzinIdWarning == null,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (state.isEditMode) "Simpan Perubahan" else "Simpan Izin",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.isEditMode) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusColors.MerahText),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Hapus Izin", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
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
            initialDate = state.form.tanggalBatasPilihan ?: state.form.tanggalPulang.plusDays(3),
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

    if (state.showRecentPulangPopup && state.recentPulangWarning != null) {
        val w = state.recentPulangWarning!!
        val alasanLabel = if (w.alasanJenis == AlasanJenis.SAKIT) "Sakit" else "Lain-lain"
        AlertDialog(
            onDismissRequest = { viewModel.tutupPopupRecentPulang() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = StatusColors.AmberText
                )
            },
            title = { Text("Pemberitahuan: Belum 2 Bulan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${state.namaQuery} sudah pernah pulang dalam 2 bulan terakhir.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Terakhir pulang: ${MessageTemplateBuilder.formatTanggal(w.tanggalPulang)} (${RekapPulangLogic.formatSelisihHari(w.hariSejakPulang)})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Alasan: $alasanLabel — ${w.alasanDetail}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!w.pemberiIzin.isNullOrBlank()) {
                        Text(
                            text = "Pemberi izin: ${w.pemberiIzin}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Jadwal ideal berikutnya: ${MessageTemplateBuilder.formatTanggal(w.bolehPulangLagiMulai)} (sisa ${w.sisaHariKe2Bulan} hari).",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusColors.AmberText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Catatan: Ini hanya pemberitahuan. Izin tetap dapat disimpan jika diperlukan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.tutupPopupRecentPulang() }) {
                    Text("Mengerti, Lanjutkan")
                }
            }
        )
    }
}

@Composable
private fun FormSectionCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
