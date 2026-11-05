package app.pantauizin.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SweepReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val provider = context.applicationContext as? SweepRunnerProvider ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                provider.sweepRunner.runIfDue()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

