package app.pantauizin.ui.izin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.IzinFilter
import app.pantauizin.domain.IzinStatus
import app.pantauizin.domain.MessageTemplateBuilder
import app.pantauizin.ui.components.CircularIconBadge
import app.pantauizin.ui.components.DetailInfoRow
import app.pantauizin.ui.components.EmptyStateBox
import app.pantauizin.ui.components.HeroGradientCard
import app.pantauizin.ui.components.KamarFilterSelector
import app.pantauizin.ui.components.MetaTag
import app.pantauizin.ui.components.PillFilterButton
import app.pantauizin.ui.components.StatusBadge
import app.pantauizin.ui.components.isAppDarkTheme
import app.pantauizin.ui.components.userInitials
import app.pantauizin.ui.konfirmasi.KonfirmasiRiwayatSection
import app.pantauizin.ui.theme.BrandColors
import app.pantauizin.ui.theme.StatusColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID"))
private val HERO_DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.forLanguageTag("id-ID"))
private val ID_DATETIME_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IzinListScreen(
    viewModel: IzinListViewModel,
    onAddIzin: () -> Unit,
    onEditIzin: (Long) -> Unit,
    onOpenKonfirmasi: (Long) -> Unit,
    canManageIzin: Boolean = true,
    userName: String = "Pengurus",
    isSyncing: Boolean = false,
    onSyncClick: () -> Unit = {},
    onOpenProfil: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentFilter by viewModel.filter.collectAsState()
    val kamarFilter by viewModel.kamarFilter.collectAsState()
    val kamarOptions by viewModel.kamarOptions.collectAsState()
    val filterCounts by viewModel.filterCounts.collectAsState()
    val heroSummary by viewModel.heroSummary.collectAsState()
    val items by viewModel.items.collectAsState()
    val scope = rememberCoroutineScope()

    var dialogTandaiKembaliIzinId by remember { mutableStateOf<Long?>(null) }
    var dialogBatalKembaliIzinId by remember { mutableStateOf<Long?>(null) }
    var detailIzinId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (canManageIzin) {
                FloatingActionButton(
                    onClick = onAddIzin,
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah izin")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Greeting Header
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Assalamu'alaikum,",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = userName.ifBlank { "Pengurus" },
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.25.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .clickable(enabled = !isSyncing, role = Role.Button, onClick = onSyncClick),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sinkronkan data",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .border(1.25.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .clickable(role = Role.Button, onClick = onOpenProfil),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userInitials(userName),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 2. Hero Maroon Gradient Summary Card
            item(key = "hero") {
                val todayLabel = remember {
                    LocalDate.now().format(HERO_DATE_FORMAT).uppercase(Locale.forLanguageTag("id-ID"))
                }
                HeroGradientCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Santri Sedang Izin",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color.White.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(50)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = todayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${heroSummary.sedangIzin}",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Santri",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HeroStatPill(
                                dotColor = StatusColors.Amber,
                                label = "${heroSummary.berjalan} Berjalan"
                            )
                            HeroStatPill(
                                dotColor = Color(0xFFFF8A8A),
                                label = "${heroSummary.terlambatAktif} Terlambat"
                            )
                        }
                        Text(
                            text = "${heroSummary.kembali} Kembali",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // 3. Symmetrical Kamar Filter Selector
            item(key = "kamar_filter") {
                KamarFilterSelector(
                    selectedKamar = kamarFilter,
                    kamarOptions = kamarOptions,
                    onSelectKamar = { viewModel.pilihKamarFilter(it) }
                )
            }

            // 4. Filter Pills (wrapped FlowRow so all filters are clearly visible)
            item(key = "filter_pills") {
                val filterLabels = listOf(
                    IzinFilter.SEMUA to "Semua",
                    IzinFilter.BERJALAN to "Berjalan",
                    IzinFilter.TERLAMBAT to "Terlambat",
                    IzinFilter.KEMBALI to "Kembali"
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterLabels.forEach { (f, label) ->
                        val count = filterCounts[f] ?: 0
                        PillFilterButton(
                            label = "$label ($count)",
                            selected = currentFilter == f,
                            onClick = { viewModel.pilihFilter(f) }
                        )
                    }
                }
            }

            // 5. Section Header
            item(key = "section_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Izin Santri",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${items.size} data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 6. Empty State or List Items
            if (items.isEmpty()) {
                item(key = "empty") {
                    val hasActiveFilter = currentFilter != IzinFilter.SEMUA || kamarFilter != null
                    EmptyStateBox(
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        title = if (hasActiveFilter) "Tidak ada izin yang cocok" else "Belum ada data izin",
                        subtitle = when {
                            hasActiveFilter -> "Coba pilih kamar atau kategori status lain."
                            canManageIzin -> "Ketuk tombol + di kanan bawah untuk mencatat izin pulang santri."
                            else -> "Belum ada data izin pulang santri."
                        },
                        actionLabel = when {
                            hasActiveFilter -> "Reset Filter"
                            canManageIzin -> "Catat Izin"
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
                }
            } else {
                items(items, key = { it.view.id }) { item ->
                    IzinCard(
                        item = item,
                        canManageIzin = canManageIzin,
                        onOpenDetail = { detailIzinId = item.view.id },
                        onToggleKembali = {
                            if (item.view.tanggalKembali == null) {
                                dialogTandaiKembaliIzinId = item.view.id
                            } else {
                                dialogBatalKembaliIzinId = item.view.id
                            }
                        }
                    )
                }
            }
        }
    }

    detailIzinId?.let { targetId ->
        val target = items.firstOrNull { it.view.id == targetId }
        if (target == null) {
            detailIzinId = null
        } else {
            IzinDetailBottomSheet(
                item = target,
                canManageIzin = canManageIzin,
                onDismiss = { detailIzinId = null },
                onEdit = {
                    detailIzinId = null
                    onEditIzin(target.view.id)
                },
                onOpenKonfirmasi = {
                    detailIzinId = null
                    onOpenKonfirmasi(target.view.id)
                },
                onToggleKembali = {
                    detailIzinId = null
                    if (target.view.tanggalKembali == null) {
                        dialogTandaiKembaliIzinId = target.view.id
                    } else {
                        dialogBatalKembaliIzinId = target.view.id
                    }
                }
            )
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
private fun HeroStatPill(
    dotColor: Color,
    label: String
) {
    Row(
        modifier = Modifier
            .background(
                color = Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IzinCard(
    item: IzinListItemUi,
    canManageIzin: Boolean,
    onOpenDetail: () -> Unit,
    onToggleKembali: () -> Unit
) {
    val style = styleOf(item.status)
    val isDark = isAppDarkTheme()
    val greenPillBg = if (isDark) BrandColors.SoftGreenPillBgDark else BrandColors.SoftGreenPillBg
    val greenPillText = if (isDark) BrandColors.SoftGreenPillTextDark else BrandColors.SoftGreenPillText

    val alasanLabel = if (item.view.alasanJenis == AlasanJenis.SAKIT) "Sakit" else "Lain-lain"
    val statusBadgeLabel = when (item.status) {
        IzinStatus.IZIN_BERJALAN -> "Berjalan"
        IzinStatus.TERLAMBAT_BELUM_KONFIRMASI -> "Terlambat ${item.hariTerlambat}h"
        IzinStatus.TERLAMBAT_TERKONFIRMASI -> "Perpanjangan +${item.hariTerlambat}h"
        IzinStatus.TERLAMBAT_KONFIRMASI_HABIS -> "Lewat Batas ${item.hariTerlambat}h"
        IzinStatus.KEMBALI_TEPAT -> "Kembali"
        IzinStatus.KEMBALI_TERLAMBAT -> "Kembali +${item.hariTerlambat}h"
    }

    Card(
        onClick = onOpenDetail,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Header Row: Icon + Full Name + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                CircularIconBadge(
                    icon = style.icon,
                    containerColor = style.containerColor,
                    iconTint = style.contentColor
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = item.view.nama,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    color = style.containerColor,
                                    shape = RoundedCornerShape(50)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = statusBadgeLabel,
                                color = style.contentColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Structured Metadata Tags (Zero middots)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetaTag("Kamar ${item.view.kamar}")
                        MetaTag(alasanLabel)
                        MetaTag("${item.view.tanggalPulang.format(SHORT_DATE_FORMAT)} – ${item.batasAktif.format(SHORT_DATE_FORMAT)}")
                        if (item.view.konfirmasi.isNotEmpty()) {
                            MetaTag(
                                text = "${item.view.konfirmasi.size} Konfirmasi",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary,
                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            )
                        }
                        if (item.view.terakhirWaPada != null && item.view.tanggalKembali == null) {
                            MetaTag(
                                text = "Sudah di-WA",
                                containerColor = greenPillBg,
                                contentColor = greenPillText,
                                borderColor = BrandColors.WhatsAppGreen.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Symmetrical Bottom Action Bar
            if (canManageIzin) {
                val isReturned = item.view.tanggalKembali != null
                val kembaliBg = if (isReturned) StatusColors.Hijau else greenPillBg
                val kembaliFg = if (isReturned) Color.White else greenPillText
                val kembaliBorder = if (isReturned) null else BorderStroke(1.25.dp, BrandColors.WhatsAppGreen.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = onOpenDetail,
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Detail",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        onClick = onToggleKembali,
                        shape = RoundedCornerShape(14.dp),
                        color = kembaliBg,
                        border = kembaliBorder,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = kembaliFg,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isReturned) "Sudah Kembali" else "Kembali",
                                style = MaterialTheme.typography.labelLarge,
                                color = kembaliFg,
                                maxLines = 1
                            )
                        }
                    }
                }
            } else {
                Surface(
                    onClick = onOpenDetail,
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Lihat Detail",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IzinDetailBottomSheet(
    item: IzinListItemUi,
    canManageIzin: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onOpenKonfirmasi: () -> Unit,
    onToggleKembali: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val style = styleOf(item.status)
    val isDark = isAppDarkTheme()
    val greenText = if (isDark) BrandColors.SoftGreenPillTextDark else BrandColors.SoftGreenPillText
    val isReturned = item.view.tanggalKembali != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.view.nama,
                    style = MaterialTheme.typography.titleLarge
                )
                StatusBadge(style = style)
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailInfoRow(label = "Kamar", value = "Kamar ${item.view.kamar}")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailInfoRow(
                        label = "Kategori Alasan",
                        value = if (item.view.alasanJenis == AlasanJenis.SAKIT) "Sakit / Berobat" else "Lain-lain"
                    )
                    DetailInfoRow(label = "Alasan Lengkap", value = item.view.alasanDetail)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailInfoRow(
                        label = "Tanggal Pulang",
                        value = MessageTemplateBuilder.formatTanggal(item.view.tanggalPulang)
                    )
                    DetailInfoRow(
                        label = "Batas Awal Kembali",
                        value = MessageTemplateBuilder.formatTanggal(item.view.batasAwal)
                    )
                    if (item.batasAktif != item.view.batasAwal) {
                        DetailInfoRow(
                            label = "Batas Aktif (Perpanjangan)",
                            value = MessageTemplateBuilder.formatTanggal(item.batasAktif),
                            valueColor = StatusColors.AmberText
                        )
                    }
                    DetailInfoRow(
                        label = "Status Kepulangan",
                        value = item.view.tanggalKembali?.let {
                            "Sudah kembali pada ${MessageTemplateBuilder.formatTanggal(it)}"
                        } ?: "Belum kembali ke pondok",
                        valueColor = if (isReturned) greenText else MaterialTheme.colorScheme.onSurface
                    )
                    if (!item.view.pemberiIzin.isNullOrBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DetailInfoRow(label = "Pemberi Izin", value = item.view.pemberiIzin)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailInfoRow(
                        label = "Nomor WA Wali",
                        value = item.view.noWaWali?.takeIf { it.isNotBlank() } ?: "Belum diisi",
                        valueColor = if (item.view.noWaWali.isNullOrBlank()) StatusColors.AmberText else MaterialTheme.colorScheme.onSurface
                    )
                    item.view.terakhirWaPada?.let { waAt ->
                        val waktuStr = waAt.atZone(ZoneId.systemDefault()).format(ID_DATETIME_FORMAT)
                        val olehStr = item.view.terakhirWaOleh?.takeIf { it.isNotBlank() } ?: "Pengurus"
                        DetailInfoRow(
                            label = "Status Hubungi WA Wali",
                            value = "Sudah di-WA oleh $olehStr pada $waktuStr",
                            valueColor = greenText
                        )
                    }
                }
            }

            if (item.view.konfirmasi.isNotEmpty()) {
                Text(
                    text = "Riwayat Konfirmasi Wali (${item.view.konfirmasi.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                KonfirmasiRiwayatSection(konfirmasi = item.view.konfirmasi)
            }

            if (canManageIzin) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEdit,
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Izin")
                        }

                        if (item.tampilKotakKonfirmasi) {
                            Button(
                                onClick = onOpenKonfirmasi,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 46.dp)
                            ) {
                                Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (item.sudahKonfirmasi) "Tambah Konfirmasi" else "Konfirmasi")
                            }
                        }
                    }

                    Surface(
                        onClick = onToggleKembali,
                        shape = RoundedCornerShape(24.dp),
                        color = if (isReturned) MaterialTheme.colorScheme.surfaceVariant else BrandColors.WhatsAppGreen,
                        contentColor = if (isReturned) MaterialTheme.colorScheme.onSurface else BrandColors.WhatsAppOnGreen,
                        border = if (isReturned) BorderStroke(1.25.dp, MaterialTheme.colorScheme.outline) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isReturned) MaterialTheme.colorScheme.onSurface else BrandColors.WhatsAppOnGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isReturned) "Batalkan Status Kembali" else "Tandai Sudah Kembali",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
