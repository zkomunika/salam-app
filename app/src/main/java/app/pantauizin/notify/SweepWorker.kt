package app.pantauizin.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.pantauizin.PantauIzinApp
import java.util.concurrent.TimeUnit

/**
 * Pengaman cadangan: bila alarm harian hilang (mis. dibersihkan manajer latar belakang HP),
 * pekerja ini tetap menjalankan sapuan begitu jam pengingat lewat dan memasang ulang alarm.
 */
class SweepWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? PantauIzinApp ?: return Result.success()
        return try {
            app.container.sweepRunner.runIfDue(SweepSource.WORKER)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "sweep_watchdog"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<SweepWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

