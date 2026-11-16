package app.pantauizin.ui.notifikasi

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import app.pantauizin.data.NotifikasiView
import app.pantauizin.domain.NotifRange
import app.pantauizin.ui.components.EmptyStateBox
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ID_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID"))
private val ID_DATETIME_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotifikasiScreen(
    viewModel: NotifikasiViewModel,
    onOpenTerlambat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentRange by viewModel.range.collectAsState()
    val items by viewModel.items.collectAsState()
    val scope = rememberCoroutineScope()

    var showCustomDialog by remember { mutableStateOf(false) }
    var customDari by remember { mutableStateOf(LocalDate.now().minusDays(6)) }
    var customSampai by remember { mutableStateOf(LocalDate.now()) }
    var customError by remember { mutableStateOf<String?>(null) }
    var pickingDari by remember { mutableStateOf(false) }
    var pickingSampai by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = currentRange is NotifRange.HariIni,
                onClick = { viewModel.pilihRange(NotifRange.HariIni) },
                label = { Text("Hari ini") }
            )
            FilterChip(
                selected = currentRange is NotifRange.TujuhHari,
                onClick = { viewModel.pilihRange(NotifRange.TujuhHari) },
                label = { Text("7 hari") }
            )
            FilterChip(
                selected = currentRange is NotifRange.TigaPuluhHari,
                onClick = { viewModel.pilihRange(NotifRange.TigaPuluhHari) },
                label = { Text("30 hari") }
            )
            val kustomLabel = (currentRange as? NotifRange.Kustom)?.let {
                "${it.dari.format(ID_DATE_FORMAT)} – ${it.sampai.format(ID_DATE_FORMAT)}"
            } ?: "Pilih tanggal"
            FilterChip(
                selected = currentRange is NotifRange.Kustom,
                onClick = {
                    val existing = currentRange as? NotifRange.Kustom
                    if (existing != null) {
                        customDari = existing.dari
                        customSampai = existing.sampai
                    }
                    customError = null
                    showCustomDialog = true
                },
                label = { Text(kustomLabel) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val unreadCount = remember(items) { items.count { !it.dibaca } }
        if (unreadCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$unreadCount belum dibaca",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { scope.launch { viewModel.tandaiSemuaDibaca() } },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Tandai semua dibaca")
                }
            }
        }

        if (items.isEmpty()) {
            EmptyStateBox(
                icon = Icons.Default.NotificationsNone,
                title = "Belum Ada Notifikasi",
                subtitle = "Belum ada riwayat notifikasi pada rentang waktu ini."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    NotifikasiItemCard(
                        item = item,
                        onClick = {
                            scope.launch {
                                viewModel.tandaiDibaca(item.id)
                                onOpenTerlambat()
                            }
                        }
                    )
                }
            }
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Rentang Tanggal Notifikasi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text("Dari tanggal", style = MaterialTheme.typography.labelMedium)
                        OutlinedButton(
                            onClick = { pickingDari = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(customDari.format(ID_DATE_FORMAT))
                        }
                    }
                    Column {
                        Text("Sampai tanggal", style = MaterialTheme.typography.labelMedium)
                        OutlinedButton(
                            onClick = { pickingSampai = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(customSampai.format(ID_DATE_FORMAT))
                        }
                    }
                    customError?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customDari.isAfter(customSampai)) {
                            customError = "Tanggal awal tidak boleh setelah tanggal akhir"
                        } else {
                            viewModel.pilihRange(NotifRange.Kustom(customDari, customSampai))
                            showCustomDialog = false
                        }
                    }
                ) {
                    Text("Terapkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (pickingDari) {
        val initMillis = customDari.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        DatePickerDialog(
            onDismissRequest = { pickingDari = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = pickerState.selectedDateMillis ?: initMillis
                        customDari = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        customError = null
                        pickingDari = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingDari = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (pickingSampai) {
        val initMillis = customSampai.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        DatePickerDialog(
            onDismissRequest = { pickingSampai = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = pickerState.selectedDateMillis ?: initMillis
                        customSampai = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        customError = null
                        pickingSampai = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingSampai = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun NotifikasiItemCard(
    item: NotifikasiView,
    onClick: () -> Unit
) {
    val waktuStr = item.dibuatPada
        .atZone(ZoneId.systemDefault())
        .format(ID_DATETIME_FORMAT)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!item.dibaca) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!item.dibaca) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.pesan,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (!item.dibaca) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = waktuStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

