package app.pantauizin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pantauizin.data.firebase.AppUpdateInfo
import app.pantauizin.data.firebase.AppUpdateManager
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    updateManager: AppUpdateManager,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var needsInstallPermission by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!updateInfo.forceUpdate && !isDownloading) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = "Pembaruan Tersedia (v${updateInfo.latestVersionName})",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Versi Anda saat ini: v${updateManager.currentVersionName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (updateInfo.releaseNotes.isNotBlank()) {
                    Text(
                        text = updateInfo.releaseNotes,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (isDownloading) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Mengunduh pembaruan... $progress%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (needsInstallPermission) {
                    Text(
                        text = "Aktifkan izin \"Izinkan dari sumber ini\" di Setelan agar aplikasi dapat memasang pembaruan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                errorText?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            if (downloadedFile != null && !updateManager.canInstallPackages()) {
                Button(
                    onClick = {
                        needsInstallPermission = true
                        updateManager.openInstallPermissionSettings()
                    }
                ) {
                    Text("Buka Setelan Izin")
                }
            } else if (downloadedFile != null) {
                Button(
                    onClick = {
                        updateManager.installApk(downloadedFile!!)
                    }
                ) {
                    Text("Pasang Sekarang")
                }
            } else {
                Button(
                    enabled = !isDownloading,
                    onClick = {
                        isDownloading = true
                        errorText = null
                        progress = 0
                        scope.launch {
                            val res = updateManager.downloadApk(updateInfo.apkUrl) { pct ->
                                progress = pct
                            }
                            isDownloading = false
                            res.fold(
                                onSuccess = { file ->
                                    downloadedFile = file
                                    if (updateManager.canInstallPackages()) {
                                        updateManager.installApk(file)
                                    } else {
                                        needsInstallPermission = true
                                        updateManager.openInstallPermissionSettings()
                                    }
                                },
                                onFailure = { err ->
                                    errorText = err.message ?: "Gagal mengunduh pembaruan"
                                }
                            )
                        }
                    }
                ) {
                    Text(if (isDownloading) "Mengunduh..." else "Unduh & Perbarui")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (errorText != null) {
                    OutlinedButton(
                        onClick = { updateManager.openInBrowser(updateInfo.apkUrl) }
                    ) {
                        Text("Buka di Browser")
                    }
                }
                if (!updateInfo.forceUpdate && !isDownloading) {
                    TextButton(onClick = onDismiss) {
                        Text("Nanti")
                    }
                }
            }
        }
    )
}
