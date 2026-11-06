package app.pantauizin.ui.izin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.pantauizin.domain.IzinStatus
import app.pantauizin.ui.theme.StatusColors

data class StatusStyle(
    val label: String,
    val icon: ImageVector,
    val color: Color
)

fun styleOf(s: IzinStatus): StatusStyle = when (s) {
    IzinStatus.IZIN_BERJALAN -> StatusStyle(
        label = "Izin berjalan",
        icon = Icons.Default.Schedule,
        color = StatusColors.Biru
    )
    IzinStatus.TERLAMBAT_BELUM_KONFIRMASI -> StatusStyle(
        label = "Terlambat - belum konfirmasi",
        icon = Icons.Default.Warning,
        color = StatusColors.Merah
    )
    IzinStatus.TERLAMBAT_TERKONFIRMASI -> StatusStyle(
        label = "Terlambat - terkonfirmasi",
        icon = Icons.Default.Info,
        color = StatusColors.Amber
    )
    IzinStatus.TERLAMBAT_KONFIRMASI_HABIS -> StatusStyle(
        label = "Terlambat - konfirmasi habis",
        icon = Icons.Default.Error,
        color = StatusColors.Merah
    )
    IzinStatus.KEMBALI_TEPAT -> StatusStyle(
        label = "Kembali tepat waktu",
        icon = Icons.Default.CheckCircle,
        color = StatusColors.Hijau
    )
    IzinStatus.KEMBALI_TERLAMBAT -> StatusStyle(
        label = "Kembali terlambat",
        icon = Icons.Default.History,
        color = StatusColors.Hijau
    )
}

