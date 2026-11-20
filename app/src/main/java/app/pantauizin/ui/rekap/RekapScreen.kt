package app.pantauizin.ui.rekap

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.MessageTemplateBuilder
import app.pantauizin.domain.RekapFilter
import app.pantauizin.domain.RekapPulangLogic
import app.pantauizin.domain.SantriRekapItem
import app.pantauizin.ui.components.CircularIconBadge
import app.pantauizin.ui.components.DetailInfoRow
import app.pantauizin.ui.components.EmptyStateBox
import app.pantauizin.ui.components.KamarFilterSelector
import app.pantauizin.ui.components.MetaTag
import app.pantauizin.ui.components.PillFilterButton
import app.pantauizin.ui.components.SoftSearchBar
import app.pantauizin.ui.components.isAppDarkTheme
import app.pantauizin.ui.theme.BrandColors
import app.pantauizin.ui.theme.StatusColors
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID"))

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RekapScreen(
    viewModel: RekapViewModel,
    modifier: Modifier = Modifier
) {
    val currentFilter by viewModel.filter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val kamarFilter by viewModel.kamarFilter.collectAsState()
    val kamarOptions by viewModel.kamarOptions.collectAsState()
    val filterCounts by viewModel.filterCounts.collectAsState()
    val items by viewModel.items.collectAsState()

    var detailSantriId by remember { mutableStateOf<Long?>(null) }

    val flaggedCount = filterCounts[RekapFilter.SUDAH_PULANG_2_BULAN] ?: 0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        text = "Rekap Pulang",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Aturan pesantren: batas pulang 2 bulan sekali",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = StatusColors.AmberContainer,
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$flaggedCount Belum 2 Bln",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusColors.AmberText
                    )
                }
            }
        }

        // 2. Search & Kamar Filter
        item(key = "search_and_kamar") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftSearchBar(
                    value = searchQuery,
                    onValueChange = { viewModel.ubahSearchQuery(it) },
                    placeholder = "Cari nama santri atau kamar..."
                )

                KamarFilterSelector(
                    selectedKamar = kamarFilter,
                    kamarOptions = kamarOptions,
                    onSelectKamar = { viewModel.pilihKamarFilter(it) }
                )
            }
        }

        // 3. Filter Pills (FlowRow — zero cutoff)
        item(key = "filter_pills") {
            val filterLabels = listOf(
                RekapFilter.SEMUA to "Semua",
                RekapFilter.SUDAH_PULANG_2_BULAN to "Belum 2 Bulan",
                RekapFilter.BELUM_PULANG_2_BULAN to "Aman"
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

        // 4. List or Empty State
        if (items.isEmpty()) {
            item(key = "empty") {
                val hasActiveFilter = searchQuery.isNotBlank() || currentFilter != RekapFilter.SEMUA || kamarFilter != null
                EmptyStateBox(
                    icon = Icons.AutoMirrored.Filled.FactCheck,
                    title = if (hasActiveFilter) "Tidak ada santri yang cocok" else "Belum ada data santri",
                    subtitle = if (hasActiveFilter) {
                        "Coba ubah kata kunci pencarian, kamar, atau kategori filter."
                    } else {
                        "Data master santri akan tampil di sini beserta rekap kepulangannya."
                    },
                    actionLabel = if (hasActiveFilter) "Reset Filter" else null,
                    onAction = if (hasActiveFilter) {
                        {
                            viewModel.ubahSearchQuery("")
                            viewModel.pilihFilter(RekapFilter.SEMUA)
                            viewModel.pilihKamarFilter(null)
                        }
                    } else null
                )
            }
        } else {
            items(items, key = { it.santri.id }) { item ->
                RekapSantriCard(
                    item = item,
                    onOpenDetail = { detailSantriId = item.santri.id }
                )
            }
        }
    }

    detailSantriId?.let { targetId ->
        val target = items.firstOrNull { it.santri.id == targetId }
        if (target == null) {
            detailSantriId = null
        } else {
            RekapDetailBottomSheet(
                item = target,
                onDismiss = { detailSantriId = null }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RekapSantriCard(
    item: SantriRekapItem,
    onOpenDetail: () -> Unit
) {
    val flagged = item.sudahPulangDalam2Bulan
    val isDark = isAppDarkTheme()
    val greenBg = if (isDark) BrandColors.SoftGreenPillBgDark else BrandColors.SoftGreenPillBg
    val greenText = if (isDark) BrandColors.SoftGreenPillTextDark else BrandColors.SoftGreenPillText

    val borderColor = if (flagged) {
        StatusColors.Amber.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val pillBg = if (flagged) StatusColors.AmberContainer else greenBg
    val pillFg = if (flagged) StatusColors.AmberText else greenText
    val pillText = if (flagged) {
        if (item.jumlahPulang2Bulan > 1) "Pulang ${item.jumlahPulang2Bulan}x" else "Belum 2 Bulan"
    } else {
        "Aman"
    }

    Card(
        onClick = onOpenDetail,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.25.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                CircularIconBadge(
                    icon = if (flagged) Icons.Default.Warning else Icons.Default.CheckCircle,
                    containerColor = pillBg,
                    iconTint = pillFg
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = item.santri.nama,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .background(pillBg, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = pillText,
                                color = pillFg,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val riwayat = item.riwayatTerakhir
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetaTag("Kamar ${item.santri.kamar}")
                        if (riwayat != null) {
                            MetaTag("Pulang ${riwayat.tanggalPulang.format(SHORT_DATE)}")
                            if (flagged) {
                                MetaTag(
                                    text = "Sisa ${riwayat.sisaHariKe2Bulan} hari",
                                    containerColor = StatusColors.AmberContainer,
                                    contentColor = StatusColors.AmberText,
                                    borderColor = StatusColors.Amber.copy(alpha = 0.45f)
                                )
                            }
                        } else {
                            MetaTag("Belum pernah pulang")
                        }
                    }
                }
            }

            if (item.riwayatTerakhir != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenDetail)
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detail riwayat kepulangan",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RekapDetailBottomSheet(
    item: SantriRekapItem,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val flagged = item.sudahPulangDalam2Bulan
    val isDark = isAppDarkTheme()
    val greenBg = if (isDark) BrandColors.SoftGreenPillBgDark else BrandColors.SoftGreenPillBg
    val greenText = if (isDark) BrandColors.SoftGreenPillTextDark else BrandColors.SoftGreenPillText

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
                    text = item.santri.nama,
                    style = MaterialTheme.typography.titleLarge
                )
                MetaTag(
                    text = if (flagged) "Belum Genap 2 Bulan" else "Aman (Sudah ≥ 2 Bulan / Belum Pulang)",
                    containerColor = if (flagged) StatusColors.AmberContainer else greenBg,
                    contentColor = if (flagged) StatusColors.AmberText else greenText,
                    borderColor = if (flagged) StatusColors.Amber.copy(alpha = 0.5f) else BrandColors.WhatsAppGreen.copy(alpha = 0.4f)
                )
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
                    DetailInfoRow(label = "Kamar", value = "Kamar ${item.santri.kamar}")
                    DetailInfoRow(label = "Total Riwayat Pulang", value = "${item.totalPulangSepanjangWaktu} kali")

                    val riwayat = item.riwayatTerakhir
                    if (riwayat != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DetailInfoRow(
                            label = "Terakhir Pulang",
                            value = "${MessageTemplateBuilder.formatTanggal(riwayat.tanggalPulang)} (${RekapPulangLogic.formatSelisihHari(riwayat.hariSejakPulang)})"
                        )
                        DetailInfoRow(
                            label = "Kategori Alasan",
                            value = if (riwayat.alasanJenis == AlasanJenis.SAKIT) "Sakit / Berobat" else "Lain-lain"
                        )
                        DetailInfoRow(label = "Alasan Lengkap", value = riwayat.alasanDetail)
                        if (!riwayat.pemberiIzin.isNullOrBlank()) {
                            DetailInfoRow(label = "Pemberi Izin", value = riwayat.pemberiIzin)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        if (flagged) {
                            DetailInfoRow(
                                label = "Batas 2 Bulan Berikutnya",
                                value = "${MessageTemplateBuilder.formatTanggal(riwayat.bolehPulangLagiMulai)} (sisa ${riwayat.sisaHariKe2Bulan} hari)",
                                valueColor = StatusColors.AmberText
                            )
                        } else {
                            DetailInfoRow(
                                label = "Status Aturan 2 Bulan",
                                value = "Sudah melewati batas minimal 2 bulan (${MessageTemplateBuilder.formatTanggal(riwayat.bolehPulangLagiMulai)})",
                                valueColor = greenText
                            )
                        }
                    } else {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DetailInfoRow(
                            label = "Riwayat Kepulangan",
                            value = "Belum pernah tercatat pulang"
                        )
                    }
                }
            }
        }
    }
}
