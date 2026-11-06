package app.pantauizin

import android.app.Application
import app.pantauizin.notify.SweepRunner
import app.pantauizin.notify.SweepRunnerProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PantauIzinApp : Application(), SweepRunnerProvider {
    lateinit var container: AppContainer
        private set

    override val sweepRunner: SweepRunner
        get() = container.sweepRunner

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            container.santriSeeder.seedIfEmpty()
            container.sweepRunner.runIfDue()
        }
    }
}

