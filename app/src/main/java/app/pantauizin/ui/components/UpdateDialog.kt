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
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pantauizin.data.firebase.AlreadyUpToDateApkException
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
    var downloadedFile by remember {
        mutableStateOf<File?>(updateManager.cachedApk(updateInfo.latestVersionCode))
    }
    var errorText by remember { mutableStateOf<String?>(null) }
    var needsInstallPermission by remember { mutableStateOf(false) }
    var canForceBypass by remember { mutableStateOf(false) }
    var waitingPermissionToInstall by remember { mutableStateOf(false) }
    var installerLaunched by remember { mutableStateOf(false) }

    // Segarkan status izin setiap kali user kembali dari layar Setelan.
    var resumeTick by remember { mutableIntStateOf(0) }
    val lifecycle = (LocalContext.current as? ComponentActivity)?.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    val canInstall = resumeTick.let { updateManager.canInstallPackages() }

    LaunchedEffect(resumeTick) {
        if (waitingPermissionToInstall && canInstall && downloadedFile != null) {
            waitingPermissionToInstall = false
            needsInstallPermission = false
            installerLaunched = true
            updateManager.installApk(downloadedFile!!).onFailure { err ->
                errorText = err.message ?: "Gagal membuka pemasang APK"
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if ((!updateInfo.forceUpdate || canForceBypass) && !isDownloading) {
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

                if (downloadedFile != null && canInstall) {
                    Text(
                        text = if (installerLaunched && errorText == null) {
                            "Jika layar pemasangan tertutup atau dibatalkan, ketuk \"Pasang Sekarang\" lalu pilih Perbarui/Install."
                        } else {
                            "Pembaruan sudah diunduh. Ketuk \"Pasang Sekarang\", lalu pilih Perbarui/Install."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
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
            if (downloadedFile != null && !canInstall) {
                Button(
                    onClick = {
                        needsInstallPermission = true
                        waitingPermissionToInstall = true
                        updateManager.openInstallPermissionSettings().onFailure { err ->
                            errorText = err.message ?: "Gagal membuka pengaturan izin"
                        }
                    }
                ) {
                    Text("Buka Setelan Izin")
                }
            } else if (downloadedFile != null) {
                Button(
                    onClick = {
                        errorText = null
                        installerLaunched = true
                        updateManager.installApk(downloadedFile!!).onFailure { err ->
                            errorText = err.message ?: "Gagal membuka pemasang APK"
                        }
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
                            val res = updateManager.downloadApk(updateInfo.apkUrl, updateInfo.latestVersionCode) { pct ->
                                progress = pct
                            }
                            isDownloading = false
                            res.fold(
                                onSuccess = { file ->
                                    downloadedFile = file
                                    if (updateManager.canInstallPackages()) {
                                        installerLaunched = true
                                        updateManager.installApk(file).onFailure { err ->
                                            errorText = err.message ?: "Gagal membuka pemasang APK"
                                        }
                                    } else {
                                        needsInstallPermission = true
                                        waitingPermissionToInstall = true
                                        updateManager.openInstallPermissionSettings().onFailure { err ->
                                            errorText = err.message ?: "Gagal membuka pengaturan izin"
                                        }
                                    }
                                },
                                onFailure = { err ->
                                    errorText = err.message ?: "Gagal mengunduh pembaruan"
                                    if (err is AlreadyUpToDateApkException) {
                                        canForceBypass = true
                                    }
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
                if ((errorText != null && !canForceBypass) || installerLaunched) {
                    OutlinedButton(
                        onClick = {
                            updateManager.openInBrowser(updateInfo.apkUrl).onFailure { err ->
                                errorText = err.message ?: "Gagal membuka browser"
                            }
                        }
                    ) {
                        Text("Buka di Browser")
                    }
                }
                if (canForceBypass && !isDownloading) {
                    TextButton(onClick = onDismiss) {
                        Text("Tutup")
                    }
                } else if (!updateInfo.forceUpdate && !isDownloading) {
                    TextButton(onClick = onDismiss) {
                        Text("Nanti")
                    }
                }
            }
        }
    )
}
