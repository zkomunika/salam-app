package app.pantauizin.data.firebase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AppUpdatePolicyTest {

    @Test
    fun isUpdateAvailableHanyaMunculJikaVersiServerLebihBaru() {
        val url = "https://github.com/zkomunika/salam-app/releases/latest/download/SALAM_APP_v1.5.apk"
        // Belum update -> muncul
        assertTrue(UpdatePolicy.isUpdateAvailable(currentVersionCode = 4, latestVersionCode = 5, apkUrl = url))
        // Sudah update ke v1.5 (kode 5) -> TIDAK muncul
        assertFalse(UpdatePolicy.isUpdateAvailable(currentVersionCode = 5, latestVersionCode = 5, apkUrl = url))
        // Terpasang lebih baru -> TIDAK muncul
        assertFalse(UpdatePolicy.isUpdateAvailable(currentVersionCode = 6, latestVersionCode = 5, apkUrl = url))
        // Admin rilis versi baru lagi (kode 6) -> muncul kembali
        assertTrue(
            UpdatePolicy.isUpdateAvailable(
                currentVersionCode = 5,
                latestVersionCode = 6,
                apkUrl = "https://github.com/zkomunika/salam-app/releases/latest/download/SALAM_APP_v1.6.apk"
            )
        )
        // URL kosong -> tidak muncul
        assertFalse(UpdatePolicy.isUpdateAvailable(currentVersionCode = 4, latestVersionCode = 5, apkUrl = "   "))
    }

    @Test
    fun validateApkMenolakApkLamaAtauPaketBerbedaDanMenerimaApkBaru() {
        val expectedPkg = "app.pantauizin"

        val valid = UpdatePolicy.validateApk(
            meta = ApkMetadata(expectedPkg, versionCode = 6, versionName = "1.6"),
            expectedPackage = expectedPkg,
            currentVersionCode = 5
        )
        assertEquals(ApkValidationResult.Valid, valid)

        val sameVersion = UpdatePolicy.validateApk(
            meta = ApkMetadata(expectedPkg, versionCode = 5, versionName = "1.5"),
            expectedPackage = expectedPkg,
            currentVersionCode = 5
        )
        assertEquals(ApkValidationResult.AlreadyInstalled(5, "1.5"), sameVersion)

        val olderVersion = UpdatePolicy.validateApk(
            meta = ApkMetadata(expectedPkg, versionCode = 4, versionName = "1.4"),
            expectedPackage = expectedPkg,
            currentVersionCode = 5
        )
        assertEquals(ApkValidationResult.AlreadyInstalled(4, "1.4"), olderVersion)

        val wrongPkg = UpdatePolicy.validateApk(
            meta = ApkMetadata("com.other.app", versionCode = 6, versionName = "1.6"),
            expectedPackage = expectedPkg,
            currentVersionCode = 5
        )
        assertEquals(ApkValidationResult.WrongPackage("com.other.app"), wrongPkg)

        assertEquals(
            ApkValidationResult.Unreadable,
            UpdatePolicy.validateApk(null, expectedPkg, currentVersionCode = 5)
        )
    }

    @Test
    fun defaultPublishConfigMenggunakanVersiSaatIniBukanPlusSatu() {
        assertEquals(5, UpdatePolicy.defaultPublishVersionCode(5))
        assertEquals("1.5", UpdatePolicy.defaultPublishVersionName("1.5"))
        assertEquals(
            "https://github.com/zkomunika/salam-app/releases/latest/download/SALAM_APP_v1.5.apk",
            UpdatePolicy.defaultApkUrl("1.5")
        )
    }

    @Test
    fun cleanupStaleApksMenghapusApkLamaDanPartSertaMempertahankanVersiLebihBaru() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        updatesDir.listFiles()?.forEach { it.delete() }

        val stale3 = File(updatesDir, "salam-update-3.apk").apply { writeText("old3") }
        val stale5 = File(updatesDir, "salam-update-5.apk").apply { writeText("old5") }
        val newer6 = File(updatesDir, "salam-update-6.apk").apply { writeText("new6") }
        val part6 = File(updatesDir, "salam-update-6.apk.part").apply { writeText("part") }
        val legacy = File(updatesDir, "salam-update.apk").apply { writeText("legacy") }

        AppUpdateManager(context).cleanupStaleApks(installedVersionCode = 5)

        assertFalse(stale3.exists())
        assertFalse(stale5.exists())
        assertFalse(part6.exists())
        assertFalse(legacy.exists())
        assertTrue(newer6.exists())

        updatesDir.listFiles()?.forEach { it.delete() }
    }
}
