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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
import app.pantauizin.data.NotifikasiView
import app.pantauizin.domain.NotifJenis
import app.pantauizin.domain.NotifRange
import app.pantauizin.ui.components.CircularIconBadge
import app.pantauizin.ui.components.EmptyStateBox
import app.pantauizin.ui.components.MetaTag
import app.pantauizin.ui.components.PillFilterButton
import app.pantauizin.ui.theme.BrandColors
import app.pantauizin.ui.theme.StatusColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ID_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID"))
private val SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID"))
private val TIME_ONLY_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

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

    val unreadCount = remember(items) { items.count { !it.dibaca } }
    val today = remember { LocalDate.now() }
    val groupedItems = remember(items) {
        items.groupBy { it.dibuatPada.atZone(ZoneId.systemDefault()).toLocalDate() }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Page Header
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Notifikasi",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Pengingat harian & peringatan batas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (unreadCount > 0) {
                    Surface(
                        onClick = { scope.launch { viewModel.tandaiSemuaDibaca() } },
                        shape = RoundedCornerShape(50),
                        color = BrandColors.SoftRoseContainer
                    ) {
                        Text(
                            text = "Tandai Dibaca",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandColors.SoftRoseText,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 2. Range Filter Pills (FlowRow — zero cutoff)
        item(key = "range_pills") {
            val kustomLabel = (currentRange as? NotifRange.Kustom)?.let {
                "${it.dari.format(SHORT_DATE_FORMAT)} – ${it.sampai.format(SHORT_DATE_FORMAT)}"
            } ?: "Pilih tanggal"

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PillFilterButton(
                    label = "Hari ini",
                    selected = currentRange is NotifRange.HariIni,
                    onClick = { viewModel.pilihRange(NotifRange.HariIni) }
                )
                PillFilterButton(
                    label = "7 hari",
                    selected = currentRange is NotifRange.TujuhHari,
                    onClick = { viewModel.pilihRange(NotifRange.TujuhHari) }
                )
                PillFilterButton(
                    label = "30 hari",
                    selected = currentRange is NotifRange.TigaPuluhHari,
                    onClick = { viewModel.pilihRange(NotifRange.TigaPuluhHari) }
                )
                PillFilterButton(
                    label = kustomLabel,
                    selected = currentRange is NotifRange.Kustom,
                    onClick = {
                        val existing = currentRange as? NotifRange.Kustom
                        if (existing != null) {
                            customDari = existing.dari
                            customSampai = existing.sampai
                        }
                        customError = null
                        showCustomDialog = true
                    }
                )
            }
        }

        // 3. Content
        if (items.isEmpty()) {
            item(key = "empty") {
                EmptyStateBox(
                    icon = Icons.Default.NotificationsNone,
                    title = "Belum Ada Notifikasi",
                    subtitle = "Belum ada riwayat notifikasi pada rentang waktu ini."
                )
            }
        } else {
            groupedItems.forEach { (date, dateItems) ->
                val dateLabel = when (date) {
                    today -> "Hari Ini"
                    today.minusDays(1) -> "Kemarin"
                    else -> date.format(ID_DATE_FORMAT)
                }
                item(key = "header_$date") {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                items(dateItems, key = { it.id }) { item ->
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
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { pickingDari = true },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(customDari.format(ID_DATE_FORMAT))
                        }
                    }
                    Column {
                        Text("Sampai tanggal", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { pickingSampai = true },
                            shape = RoundedCornerShape(16.dp),
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
    val timeStr = item.dibuatPada
        .atZone(ZoneId.systemDefault())
        .format(TIME_ONLY_FORMAT)

    val (icon, badgeBg, iconTint, categoryLabel) = when (item.jenis) {
        NotifJenis.LEWAT_BATAS -> Quad(
            Icons.Default.Warning,
            StatusColors.MerahContainer,
            StatusColors.MerahText,
            "LEWAT BATAS"
        )
        NotifJenis.LEWAT_TANGGAL_BARU -> Quad(
            Icons.Default.Schedule,
            StatusColors.AmberContainer,
            StatusColors.AmberText,
            "LEWAT TANGGAL BARU"
        )
        NotifJenis.PENGINGAT_HARIAN -> Quad(
            Icons.Default.Notifications,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.primary,
            "PENGINGAT HARIAN"
        )
    }

    val borderColor = if (!item.dibaca) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.25.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            CircularIconBadge(
                icon = icon,
                containerColor = badgeBg,
                iconTint = iconTint
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = categoryLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.6.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!item.dibaca) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.nama,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (!item.dibaca) FontWeight.Bold else FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    MetaTag("Kamar ${item.kamar}")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.pesan,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
