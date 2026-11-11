package app.pantauizin.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SweepReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val provider = context.applicationContext as? SweepRunnerProvider ?: return
        val isTestAlarm = intent?.getBooleanExtra(EXTRA_TEST_ALARM, false) == true
        Log.i(TAG, "onReceive dipanggil (isTestAlarm=$isTestAlarm)")
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isTestAlarm) {
                    provider.sweepRunner.runTestAlarm()
                    Log.i(TAG, "runTestAlarm selesai")
                } else {
                    provider.sweepRunner.runIfDue(SweepSource.ALARM)
                    Log.i(TAG, "runIfDue(ALARM) selesai")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SweepReceiver"
        const val EXTRA_TEST_ALARM = "extra_test_alarm"
    }
}

