package hu.quaternion.sleeptimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.getSystemService
import kotlin.concurrent.thread

/**
 * Receives:
 *  - ACTION_EXPIRED : the alarm fired -> fade out & pause media (on a wakelocked thread)
 *  - ACTION_CANCEL  : stop the timer, leave music alone
 *  - ACTION_EXTEND  : add minutes to a running timer
 *  - ACTION_START   : start a preset timer (used by the widget buttons)
 */
class TimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TimerController.ACTION_EXPIRED -> handleExpired(context)
            TimerController.ACTION_CANCEL -> TimerController.cancel(context)
            TimerController.ACTION_EXTEND -> TimerController.extend(context)
            TimerController.ACTION_START -> {
                val minutes = intent.getIntExtra(TimerController.EXTRA_MINUTES, 30)
                TimerController.start(context, minutes)
            }
        }
    }

    private fun handleExpired(context: Context) {
        val fadeSeconds = Prefs.getFadeSeconds(context)
        val appContext = context.applicationContext
        val pendingResult = goAsync()

        // Keep the CPU awake while we fade + pause, even in Doze.
        val pm = appContext.getSystemService<PowerManager>()
        val wakeLock = pm?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SleepTimer:fade"
        )?.apply { acquire(fadeSeconds * 1000L + 4000L) }

        thread(name = "sleep-timer-fade") {
            try {
                MediaStopper.fadeAndStop(appContext, fadeSeconds)
            } finally {
                TimerController.onExpired(appContext)
                if (wakeLock?.isHeld == true) wakeLock.release()
                pendingResult.finish()
            }
        }
    }
}
