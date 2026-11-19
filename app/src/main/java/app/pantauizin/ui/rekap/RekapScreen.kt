package app.pantauizin.ui.rekap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.MessageTemplateBuilder
import app.pantauizin.domain.RekapFilter
import app.pantauizin.domain.RekapPulangLogic
import app.pantauizin.domain.SantriRekapItem
import app.pantauizin.ui.components.EmptyStateBox
import app.pantauizin.ui.theme.StatusColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    var kamarExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.ubahSearchQuery(it) },
            placeholder = { Text("Cari nama santri atau kamar...") },
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
                RekapFilter.SEMUA to "Semua",
                RekapFilter.SUDAH_PULANG_2_BULAN to "Sudah pulang <2 bln",
                RekapFilter.BELUM_PULANG_2_BULAN to "Aman (≥2 bln)"
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

        Text(
            text = "Aturan pesantren: batas pulang 2 bulan sekali (semua alasan dihitung).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        if (items.isEmpty()) {
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
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(items, key = { it.santri.id }) { item ->
                    RekapSantriCard(item = item)
                }
            }
        }
    }
}

@Composable
private fun RekapSantriCard(item: SantriRekapItem) {
    val borderColor = if (item.sudahPulangDalam2Bulan) {
        StatusColors.Amber.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = item.santri.nama,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Kamar ${item.santri.kamar}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (item.sudahPulangDalam2Bulan) {
                    val pillText = if (item.jumlahPulang2Bulan > 1) {
                        "Pulang ${item.jumlahPulang2Bulan}x (<2 bln)"
                    } else {
                        "Sudah pulang (<2 bln)"
                    }
                    Row(
                        modifier = Modifier
                            .background(
                                color = StatusColors.AmberContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StatusColors.AmberText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = pillText,
                            color = StatusColors.AmberText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .background(
                                color = StatusColors.HijauContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusColors.HijauText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aman (≥2 bln)",
                            color = StatusColors.HijauText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val riwayat = item.riwayatTerakhir
            if (riwayat != null) {
                val alasanLabel = if (riwayat.alasanJenis == AlasanJenis.SAKIT) "Sakit" else "Lain-lain"
                Text(
                    text = "Terakhir pulang: ${MessageTemplateBuilder.formatTanggal(riwayat.tanggalPulang)} (${RekapPulangLogic.formatSelisihHari(riwayat.hariSejakPulang)})",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Alasan: $alasanLabel — ${riwayat.alasanDetail}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!riwayat.pemberiIzin.isNullOrBlank()) {
                    Text(
                        text = "Pemberi izin: ${riwayat.pemberiIzin}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (item.sudahPulangDalam2Bulan) {
                    Text(
                        text = "Batas 2 bulan berikutnya: ${MessageTemplateBuilder.formatTanggal(riwayat.bolehPulangLagiMulai)} (sisa ${riwayat.sisaHariKe2Bulan} hari)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = StatusColors.AmberText
                    )
                } else {
                    Text(
                        text = "Total riwayat pulang: ${item.totalPulangSepanjangWaktu}x",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Belum pernah tercatat pulang",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
