package app.pantauizin.ui.terlambat

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.pantauizin.domain.AlasanJenis
import app.pantauizin.domain.IzinStatus
import app.pantauizin.domain.MessageTemplateBuilder
import app.pantauizin.ui.components.DetailInfoRow
import app.pantauizin.ui.components.EmptyStateBox
import app.pantauizin.ui.components.HeroGradientCard
import app.pantauizin.ui.components.MetaTag
import app.pantauizin.ui.components.PillFilterButton
import app.pantauizin.ui.components.StatusBadge
import app.pantauizin.ui.components.isAppDarkTheme
import app.pantauizin.ui.izin.styleOf
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

private val SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID"))
private val SHORT_DATETIME = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.forLanguageTag("id-ID"))
private val ID_DATETIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TerlambatScreen(
    viewModel: TerlambatViewModel,
    onOpenKonfirmasi: (Long) -> Unit,
    canManageIzin: Boolean = true,
    currentUserName: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentFilter by viewModel.filter.collectAsState()
    val allItems by viewModel.allItems.collectAsState()
    val items by viewModel.items.collectAsState()
    val jamPengingat by viewModel.jamPengingat.collectAsState()
    val scope = rememberCoroutineScope()

    var notifEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    var dialogTandaiKembaliIzinId by remember { mutableStateOf<Long?>(null) }
    var detailIzinId by remember { mutableStateOf<Long?>(null) }
    var waConfirmItem by remember { mutableStateOf<TerlambatItemUi?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notifEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val sudahKonfirmasiCount = remember(allItems) {
        allItems.count { it.status == IzinStatus.TERLAMBAT_TERKONFIRMASI }
    }
    val belumKonfirmasiCount = remember(allItems) {
        allItems.count {
            it.status == IzinStatus.TERLAMBAT_BELUM_KONFIRMASI ||
                it.status == IzinStatus.TERLAMBAT_KONFIRMASI_HABIS
        }
    }

    val executeWaSend: (TerlambatItemUi) -> Unit = { target ->
        val action = viewModel.buildSendAction(target)
        TemplateSender.execute(context, action)
        scope.launch {
            viewModel.catatHubungiWa(target.view.id, currentUserName)
        }
    }

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
                        text = "Terlambat & WA Wali",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Pantau keterlambatan & konfirmasi wali",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = BrandColors.SoftRoseContainer,
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${allItems.size} Kasus",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandColors.SoftRoseText
                    )
                }
            }
        }

        // 2. Notification disabled warning
        if (!notifEnabled) {
            item(key = "notif_banner") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = "Izin notifikasi mati",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pengingat harian tidak akan muncul karena izin notifikasi ditolak.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    // Abaikan bila pengaturan tidak ditemukan
                                }
                            },
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Buka Pengaturan Notifikasi")
                        }
                    }
                }
            }
        }

        // 3. Hero Progress Card
        item(key = "hero") {
            val total = allItems.size.coerceAtLeast(1)
            val progressFraction = if (allItems.isEmpty()) 1f else sudahKonfirmasiCount.toFloat() / total
            HeroGradientCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status Tindak Lanjut Hari Ini",
                        style = MaterialTheme.typography.bodySmall,
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
                            text = "Pengingat ${jamPengingat.format(TIME_FORMAT)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${allItems.size} Santri Lewat Batas",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$sudahKonfirmasiCount Sudah Konfirmasi",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "$belumKonfirmasiCount Perlu Dihubungi",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // 4. Filter Pills (FlowRow — zero cutoff)
        item(key = "filters") {
            val filterOptions = listOf(
                TerlambatFilter.SEMUA to "Semua",
                TerlambatFilter.BELUM_KONFIRMASI to "Belum Konfirmasi",
                TerlambatFilter.PERPANJANGAN to "Perpanjangan"
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { (f, label) ->
                    PillFilterButton(
                        label = label,
                        selected = currentFilter == f,
                        onClick = { viewModel.pilihFilter(f) }
                    )
                }
            }
        }

        // 5. Cards or Empty State
        if (items.isEmpty()) {
            item(key = "empty") {
                EmptyStateBox(
                    icon = Icons.Default.CheckCircle,
                    iconTint = StatusColors.Hijau,
                    title = if (allItems.isEmpty()) "Semua Tepat Waktu" else "Tidak Ada Data di Filter Ini",
                    subtitle = if (allItems.isEmpty()) {
                        "Alhamdulillah, tidak ada santri yang terlambat saat ini."
                    } else {
                        "Pilih filter Semua untuk melihat seluruh kasus keterlambatan."
                    },
                    actionLabel = if (allItems.isNotEmpty()) "Tampilkan Semua" else null,
                    onAction = if (allItems.isNotEmpty()) { { viewModel.pilihFilter(TerlambatFilter.SEMUA) } } else null
                )
            }
        } else {
            items(items, key = { it.view.id }) { item ->
                TerlambatCard(
                    item = item,
                    canManageIzin = canManageIzin,
                    onOpenDetail = { detailIzinId = item.view.id },
                    onKirimTemplate = {
                        val sudahDihubungi = item.view.terakhirWaPada != null || item.view.konfirmasi.isNotEmpty()
                        if (sudahDihubungi) {
                            waConfirmItem = item
                        } else {
                            executeWaSend(item)
                        }
                    },
                    onKonfirmasi = { onOpenKonfirmasi(item.view.id) },
                    onTandaiKembali = { dialogTandaiKembaliIzinId = item.view.id }
                )
            }
        }
    }

    waConfirmItem?.let { target ->
        val waAt = target.view.terakhirWaPada
        val waOleh = target.view.terakhirWaOleh?.takeIf { it.isNotBlank() } ?: "Pengurus"
        val bodyText = buildString {
            if (waAt != null) {
                append("Wali santri ${target.view.nama} sudah dihubungi melalui WhatsApp oleh $waOleh pada ${waAt.atZone(ZoneId.systemDefault()).format(ID_DATETIME)}.")
            }
            if (target.view.konfirmasi.isNotEmpty()) {
                if (isNotEmpty()) append("\n\n")
                append("Wali santri juga sudah tercatat melakukan konfirmasi (${target.view.konfirmasi.size}x).")
            }
            append("\n\nMohon pastikan kembali agar tidak terjadi pesan berulang (spam) ke nomor wali santri.")
        }

        AlertDialog(
            onDismissRequest = { waConfirmItem = null },
            title = {
                Text(if (waAt != null) "Wali Sudah Dihubungi" else "Wali Sudah Konfirmasi")
            },
            text = { Text(bodyText) },
            confirmButton = {
                Button(
                    onClick = {
                        val selected = target
                        waConfirmItem = null
                        executeWaSend(selected)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandColors.WhatsAppGreen,
                        contentColor = BrandColors.WhatsAppOnGreen
                    )
                ) {
                    Text("Tetap Kirim WA")
                }
            },
            dismissButton = {
                TextButton(onClick = { waConfirmItem = null }) {
                    Text("Batal")
                }
            }
        )
    }

    detailIzinId?.let { targetId ->
        val target = items.firstOrNull { it.view.id == targetId }
        if (target == null) {
            detailIzinId = null
        } else {
            TerlambatDetailBottomSheet(
                item = target,
                canManageIzin = canManageIzin,
                onDismiss = { detailIzinId = null },
                onKirimTemplate = {
                    detailIzinId = null
                    val sudahDihubungi = target.view.terakhirWaPada != null || target.view.konfirmasi.isNotEmpty()
                    if (sudahDihubungi) {
                        waConfirmItem = target
                    } else {
                        executeWaSend(target)
                    }
                },
                onKonfirmasi = {
                    detailIzinId = null
                    onOpenKonfirmasi(target.view.id)
                },
                onTandaiKembali = {
                    detailIzinId = null
                    dialogTandaiKembaliIzinId = target.view.id
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TerlambatCard(
    item: TerlambatItemUi,
    canManageIzin: Boolean,
    onOpenDetail: () -> Unit,
    onKirimTemplate: () -> Unit,
    onKonfirmasi: () -> Unit,
    onTandaiKembali: () -> Unit
) {
    val style = styleOf(item.status)
    val isDark = isAppDarkTheme()
    val greenBg = if (isDark) BrandColors.SoftGreenPillBgDark else BrandColors.SoftGreenPillBg
    val greenText = if (isDark) BrandColors.SoftGreenPillTextDark else BrandColors.SoftGreenPillText

    val waLabel = item.view.noWaWali?.takeIf { it.isNotBlank() } ?: "Belum diisi (pilih di WA)"
    val alasanLabel = if (item.view.alasanJenis == AlasanJenis.SAKIT) "Sakit" else "Lain-lain"
    val fullStatusLabel = when (item.status) {
        IzinStatus.TERLAMBAT_BELUM_KONFIRMASI -> "Belum Konfirmasi"
        IzinStatus.TERLAMBAT_TERKONFIRMASI -> "Perpanjangan"
        IzinStatus.TERLAMBAT_KONFIRMASI_HABIS -> "Konfirmasi Habis"
        else -> style.label
    }
    val sudahWa = item.view.terakhirWaPada != null
    val sudahKonfirmasi = item.view.konfirmasi.isNotEmpty()
    val sudahDihubungiAtauKonfirmasi = sudahWa || sudahKonfirmasi

    Card(
        onClick = onOpenDetail,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Header Row: +Xh badge + Full Name + Status & Structured Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = StatusColors.MerahContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+${item.hariTerlambat}h",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusColors.MerahText
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.view.nama,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = style.containerColor,
                                    shape = RoundedCornerShape(50)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = fullStatusLabel,
                                color = style.contentColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                        MetaTag("Kamar ${item.view.kamar}")
                        MetaTag("Batas ${item.batasAktif.format(SHORT_DATE)}")
                        MetaTag(alasanLabel)
                    }
                }
            }

            // 2. Anti-Spam WA Wali & Konfirmasi Notice Strip
            if (sudahDihubungiAtauKonfirmasi) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = greenBg,
                    border = BorderStroke(1.dp, BrandColors.WhatsAppGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = greenText,
                            modifier = Modifier.size(17.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            item.view.terakhirWaPada?.let { waAt ->
                                val oleh = item.view.terakhirWaOleh?.takeIf { it.isNotBlank() } ?: "Pengurus"
                                val waktu = waAt.atZone(ZoneId.systemDefault()).format(SHORT_DATETIME)
                                Text(
                                    text = "Sudah di-WA oleh $oleh ($waktu)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = greenText
                                )
                            }
                            if (sudahKonfirmasi) {
                                Text(
                                    text = "Wali sudah konfirmasi (${item.view.konfirmasi.size}x) — lihat detail",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = greenText
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Concise WA Info + Detail Button Strip
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nomor WA Wali",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = waLabel,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.view.noWaWali.isNullOrBlank()) {
                                StatusColors.AmberText
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
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
                            text = if (sudahKonfirmasi) {
                                "Lihat detail, alasan & riwayat konfirmasi"
                            } else {
                                "Lihat detail & alasan lengkap"
                            },
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

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Primary Action Row: WA Wali + Konfirmasi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (item.view.tanggalKembali == null) {
                    if (sudahDihubungiAtauKonfirmasi) {
                        OutlinedButton(
                            onClick = onKirimTemplate,
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.25.dp, BrandColors.WhatsAppGreen),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = greenBg,
                                contentColor = greenText
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (sudahWa) "Sudah di-WA" else "WA Wali",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    } else {
                        Button(
                            onClick = onKirimTemplate,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandColors.WhatsAppGreen,
                                contentColor = BrandColors.WhatsAppOnGreen
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WA Wali",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (canManageIzin) {
                    Button(
                        onClick = onKonfirmasi,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Konfirmasi",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                }
            }

            // 5. Distinct, Harmonious Pill Button for Tandai Sudah Kembali
            if (canManageIzin) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    onClick = onTandaiKembali,
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = greenText,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tandai Sudah Kembali",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
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
private fun TerlambatDetailBottomSheet(
    item: TerlambatItemUi,
    canManageIzin: Boolean,
    onDismiss: () -> Unit,
    onKirimTemplate: () -> Unit,
    onKonfirmasi: () -> Unit,
    onTandaiKembali: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val style = styleOf(item.status)
    val isDark = isAppDarkTheme()
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
                    text = item.view.nama,
                    style = MaterialTheme.typography.titleLarge
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(style = style)
                    MetaTag(
                        text = "+${item.hariTerlambat} hari terlambat",
                        containerColor = StatusColors.MerahContainer,
                        contentColor = StatusColors.MerahText,
                        borderColor = StatusColors.Merah.copy(alpha = 0.35f)
                    )
                }
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
                    DetailInfoRow(
                        label = "Batas Aktif",
                        value = MessageTemplateBuilder.formatTanggal(item.batasAktif)
                    )
                    if (!item.view.pemberiIzin.isNullOrBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DetailInfoRow(label = "Pemberi Izin", value = item.view.pemberiIzin)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailInfoRow(
                        label = "Nomor WA Wali",
                        value = item.view.noWaWali?.takeIf { it.isNotBlank() } ?: "Belum diisi (pilih di WA)",
                        valueColor = if (item.view.noWaWali.isNullOrBlank()) StatusColors.AmberText else MaterialTheme.colorScheme.onSurface
                    )
                    DetailInfoRow(
                        label = "Status Hubungi WA Wali",
                        value = item.view.terakhirWaPada?.let { waAt ->
                            val oleh = item.view.terakhirWaOleh?.takeIf { it.isNotBlank() } ?: "Pengurus"
                            val waktu = waAt.atZone(ZoneId.systemDefault()).format(ID_DATETIME)
                            "Sudah di-WA oleh $oleh pada $waktu"
                        } ?: "Belum pernah dihubungi",
                        valueColor = if (item.view.terakhirWaPada != null) greenText else MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onKirimTemplate,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandColors.WhatsAppGreen,
                            contentColor = BrandColors.WhatsAppOnGreen
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (item.view.terakhirWaPada != null) "WA Lagi" else "WA Wali")
                    }

                    if (canManageIzin) {
                        Button(
                            onClick = onKonfirmasi,
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
                            Text("Konfirmasi")
                        }
                    }
                }

                if (canManageIzin) {
                    Surface(
                        onClick = onTandaiKembali,
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = greenText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tandai Sudah Kembali",
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
