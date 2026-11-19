package app.pantauizin.ui.izin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.IzinFilter
import app.pantauizin.domain.IzinStatus
import app.pantauizin.ui.components.EmptyStateBox
import app.pantauizin.ui.components.StatusBadge
import app.pantauizin.ui.konfirmasi.KonfirmasiRiwayatDialog
import app.pantauizin.ui.theme.StatusColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ID_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID"))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IzinListScreen(
    viewModel: IzinListViewModel,
    onAddIzin: () -> Unit,
    onEditIzin: (Long) -> Unit,
    onOpenKonfirmasi: (Long) -> Unit,
    canManageIzin: Boolean = true,
    modifier: Modifier = Modifier
) {
    val currentFilter by viewModel.filter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val kamarFilter by viewModel.kamarFilter.collectAsState()
    val kamarOptions by viewModel.kamarOptions.collectAsState()
    val filterCounts by viewModel.filterCounts.collectAsState()
    val items by viewModel.items.collectAsState()
    val scope = rememberCoroutineScope()

    var kamarExpanded by remember { mutableStateOf(false) }

    var dialogTandaiKembaliIzinId by remember { mutableStateOf<Long?>(null) }
    var dialogBatalKembaliIzinId by remember { mutableStateOf<Long?>(null) }
    var dialogRiwayatIzinId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (canManageIzin) {
                FloatingActionButton(
                    onClick = onAddIzin,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah izin")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.ubahSearchQuery(it) },
                placeholder = { Text("Cari nama, kamar, atau alasan...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.ubahSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Hapus pencarian")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = kamarExpanded,
                onExpandedChange = { kamarExpanded = !kamarExpanded }
            ) {
                OutlinedTextField(
                    value = kamarFilter?.let { "Kamar $it" } ?: "Semua kamar",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filter kamar") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kamarExpanded) },
                    shape = RoundedCornerShape(12.dp),
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
                            viewModel.pilihKamarFilter(null)
                            kamarExpanded = false
                        }
                    )
                    kamarOptions.forEach { kamar ->
                        DropdownMenuItem(
                            text = { Text("Kamar $kamar") },
                            onClick = {
                                viewModel.pilihKamarFilter(kamar)
                                kamarExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterLabels = listOf(
                    IzinFilter.SEMUA to "Semua",
                    IzinFilter.BERJALAN to "Izin berjalan",
                    IzinFilter.TERLAMBAT to "Terlambat",
                    IzinFilter.KEMBALI to "Sudah kembali"
                )
                for ((f, label) in filterLabels) {
                    val count = filterCounts[f] ?: 0
                    FilterChip(
                        selected = currentFilter == f,
                        onClick = { viewModel.pilihFilter(f) },
                        label = { Text("$label ($count)") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (items.isEmpty()) {
                val hasActiveFilter = searchQuery.isNotBlank() || currentFilter != IzinFilter.SEMUA || kamarFilter != null
                EmptyStateBox(
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    title = if (hasActiveFilter) "Tidak ada izin yang cocok" else "Belum ada data izin",
                    subtitle = when {
                        hasActiveFilter -> "Coba ubah kata kunci pencarian atau pilih kategori lain."
                        canManageIzin -> "Ketuk tombol + di kanan bawah untuk mencatat izin pulang santri."
                        else -> "Belum ada data izin pulang santri."
                    },
                    actionLabel = when {
                        hasActiveFilter -> "Reset Filter"
                        canManageIzin -> "Tambah Izin"
                        else -> null
                    },
                    onAction = when {
                        hasActiveFilter -> {
                            {
                                viewModel.ubahSearchQuery("")
                                viewModel.pilihFilter(IzinFilter.SEMUA)
                                viewModel.pilihKamarFilter(null)
                            }
                        }
                        canManageIzin -> onAddIzin
                        else -> null
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(items, key = { it.view.id }) { item ->
                        IzinCard(
                            item = item,
                            canManageIzin = canManageIzin,
                            onToggleKembali = {
                                if (item.view.tanggalKembali == null) {
                                    dialogTandaiKembaliIzinId = item.view.id
                                } else {
                                    dialogBatalKembaliIzinId = item.view.id
                                }
                            },
                            onOpenKonfirmasi = { onOpenKonfirmasi(item.view.id) },
                            onLihatKonfirmasi = { dialogRiwayatIzinId = item.view.id },
                            onEdit = { onEditIzin(item.view.id) }
                        )
                    }
                }
            }
        }
    }

    dialogTandaiKembaliIzinId?.let { izinId ->
        val todayMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = todayMillis)
        DatePickerDialog(
            onDismissRequest = { dialogTandaiKembaliIzinId = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = pickerState.selectedDateMillis ?: todayMillis
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        dialogTandaiKembaliIzinId = null
                        scope.launch {
                            viewModel.tandaiKembali(izinId, date)
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogTandaiKembaliIzinId = null }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    dialogRiwayatIzinId?.let { izinId ->
        val target = items.firstOrNull { it.view.id == izinId }
        if (target == null) {
            dialogRiwayatIzinId = null
        } else {
            KonfirmasiRiwayatDialog(
                namaSantri = target.view.nama,
                konfirmasi = target.view.konfirmasi,
                onDismiss = { dialogRiwayatIzinId = null }
            )
        }
    }

    dialogBatalKembaliIzinId?.let { izinId ->
        AlertDialog(
            onDismissRequest = { dialogBatalKembaliIzinId = null },
            title = { Text("Batalkan Status Kembali?") },
            text = { Text("Santri ini akan kembali tercatat belum kembali ke pondok.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialogBatalKembaliIzinId = null
                        scope.launch {
                            viewModel.batalKembali(izinId)
                        }
                    }
                ) {
                    Text("Ya, Batalkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogBatalKembaliIzinId = null }) {
                    Text("Tidak")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IzinCard(
    item: IzinListItemUi,
    canManageIzin: Boolean,
    onToggleKembali: () -> Unit,
    onOpenKonfirmasi: () -> Unit,
    onLihatKonfirmasi: () -> Unit,
    onEdit: () -> Unit
) {
    val style = styleOf(item.status)
    val alasanLabel = if (item.view.alasanJenis == AlasanJenis.SAKIT) "Sakit" else "Lain-lain"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.view.nama,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Kamar ${item.view.kamar}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canManageIzin) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.heightIn(min = 44.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$alasanLabel: ${item.view.alasanDetail}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Pulang: ${item.view.tanggalPulang.format(ID_DATE_FORMAT)}  •  Batas: ${item.batasAktif.format(ID_DATE_FORMAT)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!item.view.pemberiIzin.isNullOrBlank()) {
                Text(
                    text = "Pemberi izin: ${item.view.pemberiIzin}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusBadge(style = style)

                if (item.status == IzinStatus.KEMBALI_TERLAMBAT) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = StatusColors.AmberContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Terlambat ${item.hariTerlambat} hari",
                            color = StatusColors.AmberText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .let { if (canManageIzin) it.clickable(onClick = onToggleKembali) else it }
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.view.tanggalKembali != null,
                        enabled = canManageIzin,
                        onCheckedChange = if (canManageIzin) { { onToggleKembali() } } else null
                    )
                    Text(
                        text = "Sudah kembali",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (canManageIzin && item.tampilKotakKonfirmasi) {
                    OutlinedButton(
                        onClick = onOpenKonfirmasi,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (item.sudahKonfirmasi) "Tambah konfirmasi" else "Konfirmasi wali")
                    }
                }
            }

            if (item.view.konfirmasi.isNotEmpty()) {
                TextButton(
                    onClick = onLihatKonfirmasi,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Lihat konfirmasi (${item.view.konfirmasi.size})")
                }
            }
        }
    }
}

