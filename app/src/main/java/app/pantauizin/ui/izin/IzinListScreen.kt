package app.pantauizin.ui.izin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.IzinFilter
import app.pantauizin.domain.IzinStatus
import app.pantauizin.ui.konfirmasi.KonfirmasiRiwayatDialog
import app.pantauizin.ui.theme.StatusColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ID_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID"))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IzinListScreen(
    viewModel: IzinListViewModel,
    onAddIzin: () -> Unit,
    onEditIzin: (Long) -> Unit,
    onOpenKonfirmasi: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentFilter by viewModel.filter.collectAsState()
    val items by viewModel.items.collectAsState()
    val scope = rememberCoroutineScope()

    var dialogTandaiKembaliIzinId by remember { mutableStateOf<Long?>(null) }
    var dialogBatalKembaliIzinId by remember { mutableStateOf<Long?>(null) }
    var dialogRiwayatIzinId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddIzin,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah izin")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
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
                    FilterChip(
                        selected = currentFilter == f,
                        onClick = { viewModel.pilihFilter(f) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada data izin pada kategori ini.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(items, key = { it.view.id }) { item ->
                        IzinCard(
                            item = item,
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

@Composable
private fun IzinCard(
    item: IzinListItemUi,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Kamar ${item.view.kamar}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$alasanLabel: ${item.view.alasanDetail}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Batas aktif: ${item.batasAktif.format(ID_DATE_FORMAT)}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            color = style.color.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = style.label,
                        tint = style.color,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = style.label,
                        color = style.color,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (item.status == IzinStatus.KEMBALI_TERLAMBAT) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = StatusColors.Oranye,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Terlambat ${item.hariTerlambat} hari",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = item.view.tanggalKembali != null,
                        onCheckedChange = { onToggleKembali() }
                    )
                    Text(
                        text = "Sudah kembali",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (item.tampilKotakKonfirmasi) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = item.sudahKonfirmasi,
                            onCheckedChange = { onOpenKonfirmasi() }
                        )
                        Text(
                            text = "Konfirmasi wali",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (item.view.konfirmasi.isNotEmpty()) {
                TextButton(onClick = onLihatKonfirmasi) {
                    Text("Lihat konfirmasi (${item.view.konfirmasi.size})")
                }
            }
        }
    }
}

