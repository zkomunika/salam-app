package app.pantauizin

import android.app.Application
import android.os.Build
import app.pantauizin.notify.SweepRunner
import app.pantauizin.notify.SweepRunnerProvider
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
        container.santriSeeder.seedIfEmpty()
        container.sweepRunner.runIfDue()
    }

    override fun onTerminate() {
        appScope.cancel()
        super.onTerminate()
    }
}
