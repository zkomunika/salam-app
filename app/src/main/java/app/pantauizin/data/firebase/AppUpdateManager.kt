package app.pantauizin.data.firebase

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.Keep
import androidx.core.content.FileProvider
import app.pantauizin.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@Keep
data class AppUpdateInfo(
    val latestVersionCode: Int = 0,
    val latestVersionName: String = "",
    val apkUrl: String = "",
    val releaseNotes: String = "",
    val forceUpdate: Boolean = false
)

class AppUpdateManager(
    private val context: Context,
    private val firestoreProvider: () -> FirebaseFirestore? = {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else null
        } catch (_: Exception) {
            null
        }
    }
) {
    private val firestore: FirebaseFirestore? get() = firestoreProvider()

    val currentVersionCode: Int get() = BuildConfig.VERSION_CODE
    val currentVersionName: String get() = BuildConfig.VERSION_NAME

    fun suggestNextVersionName(): String {
        val parts = currentVersionName.split(".")
        return if (parts.size >= 2) {
            val major = parts[0]
            val minor = parts[1].toIntOrNull() ?: 0
            "$major.${minor + 1}"
        } else {
            "${currentVersionName}.1"
        }
    }

    /**
     * Mengambil konfigurasi update terakhir yang tersimpan di Firestore untuk auto-fill form rilis.
     */
    suspend fun getLatestConfig(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext null
        try {
            val snap = db.collection("app_config").document("update").get().await()
            if (!snap.exists()) return@withContext null
            AppUpdateInfo(
                latestVersionCode = (snap.getLong("latestVersionCode") ?: 0L).toInt(),
                latestVersionName = snap.getString("latestVersionName") ?: "",
                apkUrl = snap.getString("apkUrl") ?: "",
                releaseNotes = snap.getString("releaseNotes") ?: "",
                forceUpdate = snap.getBoolean("forceUpdate") ?: false
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Mengecek apakah ada versi baru di Firestore (`app_config/update`).
     * Mengembalikan AppUpdateInfo jika ada versi lebih baru, atau null jika sudah terbaru.
     */
    suspend fun checkForUpdate(): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase belum terhubung"))
        try {
            val snap = db.collection("app_config").document("update").get().await()
            if (!snap.exists()) {
                return@withContext Result.success(null)
            }
            val latestCode = (snap.getLong("latestVersionCode") ?: 0L).toInt()
            val latestName = snap.getString("latestVersionName") ?: ""
            val apkUrl = snap.getString("apkUrl") ?: ""
            val notes = snap.getString("releaseNotes") ?: ""
            val force = snap.getBoolean("forceUpdate") ?: false

            if (latestCode > currentVersionCode && apkUrl.isNotBlank()) {
                Result.success(
                    AppUpdateInfo(
                        latestVersionCode = latestCode,
                        latestVersionName = latestName,
                        apkUrl = apkUrl,
                        releaseNotes = notes,
                        forceUpdate = force
                    )
                )
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mempublikasikan info versi baru ke Firestore (hanya untuk Admin).
     */
    suspend fun publishUpdateInfo(info: AppUpdateInfo): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firebase belum terhubung"))
        try {
            val data = mapOf(
                "latestVersionCode" to info.latestVersionCode,
                "latestVersionName" to info.latestVersionName,
                "apkUrl" to info.apkUrl.trim(),
                "releaseNotes" to info.releaseNotes.trim(),
                "forceUpdate" to info.forceUpdate,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("app_config").document("update").set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengunduh APK dari URL langsung ke folder cache/updates.
     */
    private fun updatesDir(): File = File(context.cacheDir, "updates").apply { mkdirs() }

    private fun isApkFile(f: File): Boolean {
        if (!f.exists() || f.length() < 1024) return false
        val header = ByteArray(2)
        f.inputStream().use { if (it.read(header) < 2) return false }
        return header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
    }

    /**
     * APK pembaruan yang sudah selesai diunduh untuk versi tertentu (bertahan walau app
     * di-restart Android saat user mengaktifkan izin "pasang dari sumber ini").
     */
    fun cachedApk(versionCode: Int): File? {
        val f = File(updatesDir(), "salam-update-$versionCode.apk")
        return if (isApkFile(f)) f else null
    }

    suspend fun downloadApk(
        apkUrl: String,
        versionCode: Int,
        onProgress: (percent: Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = updatesDir()
            updatesDir.listFiles()?.forEach { it.delete() }
            val finalFile = File(updatesDir, "salam-update-$versionCode.apk")
            val outFile = File(updatesDir, "salam-update-$versionCode.apk.part")

            var currentUrl = apkUrl.trim()
            var connection: HttpURLConnection? = null
            var redirects = 0

            while (redirects < 5) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 15000
                    readTimeout = 30000
                    requestMethod = "GET"
                }
                val code = connection.responseCode
                if (code in 300..399) {
                    val loc = connection.getHeaderField("Location") ?: break
                    connection.disconnect()
                    currentUrl = URL(url, loc).toString()
                    redirects++
                } else {
                    break
                }
            }

            val conn = connection ?: return@withContext Result.failure(IllegalStateException("Gagal membuka koneksi"))
            if (conn.responseCode !in 200..299) {
                val errCode = conn.responseCode
                conn.disconnect()
                return@withContext Result.failure(IllegalStateException("Gagal mengunduh (HTTP $errCode)"))
            }

            val totalBytes = conn.contentLengthLong
            var downloadedBytes = 0L

            conn.inputStream.use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var lastPercent = -1
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val percent = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                    output.flush()
                }
            }
            conn.disconnect()

            // Validasi sederhana: APK adalah file ZIP yang diawali dengan byte 'P', 'K' (0x50, 0x4B)
            val header = ByteArray(2)
            outFile.inputStream().use { it.read(header) }
            if (header[0] != 0x50.toByte() || header[1] != 0x4B.toByte()) {
                outFile.delete()
                return@withContext Result.failure(
                    IllegalStateException("Link bukan file APK langsung. Gunakan tombol Buka di Browser.")
                )
            }

            if (!outFile.renameTo(finalFile)) {
                outFile.delete()
                return@withContext Result.failure(IllegalStateException("Gagal menyimpan berkas pembaruan"))
            }
            onProgress(100)
            Result.success(finalFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    fun openInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.trim())).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
