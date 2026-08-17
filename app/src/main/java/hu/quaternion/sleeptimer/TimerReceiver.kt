package hu.quaternion.sleeptimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.getSystemService
import kotlin.concurrent.thread

/**
 * Receives:
 *  - ACTION_EXPIRED : the alarm fired -> fade out, pause media and optionally lock the screen
 *                     (on a wakelocked thread)
 *  - ACTION_CANCEL  : stop the timer, leave music alone
 *  - ACTION_EXTEND  : add minutes to a running timer
 *  - ACTION_START   : start a preset timer (used by the widget buttons)
 *  - ACTION_ADJUST  : widget − / +, shifts the running timer or the pending duration
 *  - ACTION_ADJUST_FADE / ACTION_TOGGLE_* : the settings the widgets expose
 */
class TimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TimerController.ACTION_EXPIRED -> handleExpired(context)
            TimerController.ACTION_CANCEL -> TimerController.cancel(context)
            TimerController.ACTION_EXTEND -> TimerController.extend(context)
            TimerController.ACTION_START -> {
                val minutes = intent.getIntExtra(
                    TimerController.EXTRA_MINUTES, Prefs.getLastMinutes(context)
                )
                TimerController.start(context, minutes)
            }

            TimerController.ACTION_ADJUST ->
                TimerController.adjust(context, intent.getIntExtra(TimerController.EXTRA_DELTA, 0))

            TimerController.ACTION_ADJUST_FADE ->
                TimerController.adjustFade(
                    context, intent.getIntExtra(TimerController.EXTRA_DELTA, 0)
                )

            TimerController.ACTION_TOGGLE_SCREEN_OFF -> TimerController.toggleScreenOff(context)
            TimerController.ACTION_TOGGLE_LOCK -> TimerController.toggleLock(context)
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
                // After the dim overlay is dismissed, so the keyguard is what stays on screen.
                ScreenLocker.lockIfEnabled(appContext)
                if (wakeLock?.isHeld == true) wakeLock.release()
                pendingResult.finish()
            }
        }
    }
}
