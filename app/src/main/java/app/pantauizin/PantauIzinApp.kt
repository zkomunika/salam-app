package app.pantauizin

import android.app.Application
import android.os.Build
import app.pantauizin.notify.SweepRunner
import app.pantauizin.notify.SweepRunnerProvider
import app.pantauizin.notify.SweepSource
import app.pantauizin.notify.SweepWorker
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PantauIzinApp : Application(), SweepRunnerProvider {
    lateinit var container: AppContainer
        private set

    override val sweepRunner: SweepRunner
        get() = container.sweepRunner

    private val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, _ -> }
    )

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if (Build.FINGERPRINT != "robolectric") {
            appScope.launch {
                runStartupTasks()
            }
        }
    }

    suspend fun runStartupTasks() {
        container.syncManager.startRealtimeSync(appScope)
        container.santriSeeder.seedIfEmpty()
        SweepWorker.enqueue(this)
        container.sweepRunner.runIfDue(SweepSource.APP_OPEN)
    }

    override fun onTerminate() {
        container.syncManager.stopRealtimeSync()
        appScope.cancel()
        super.onTerminate()
    }
}
