package hu.quaternion.sleeptimer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Every tap target the widgets can offer, as PendingIntents.
 *
 * PendingIntent matching ignores extras, so each distinct payload gets its own request code;
 * the *_BASE ranges keep them from colliding with the notification's codes (100..103).
 */
object WidgetActions {

    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private const val REQ_OPEN = 900
    private const val REQ_CANCEL = 901
    private const val REQ_SCREEN_OFF = 902
    private const val REQ_LOCK = 903
    private const val REQ_START_BASE = 1_000   // + minutes
    private const val REQ_ADJUST_BASE = 2_000  // + minutes delta, offset to stay positive
    private const val REQ_FADE_BASE = 3_000    // + second delta, offset to stay positive
    private const val DELTA_OFFSET = 100

    fun openApp(ctx: Context): PendingIntent =
        PendingIntent.getActivity(
            ctx, REQ_OPEN,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            FLAGS
        )

    /**
     * Start a timer for [minutes].
     *
     * With screen-off mode on the tap has to land on DimActivity instead of the receiver:
     * a background broadcast receiver is not allowed to launch an activity, while the widget
     * tap that opens one directly is.
     */
    fun start(ctx: Context, minutes: Int): PendingIntent {
        val req = REQ_START_BASE + minutes
        return if (Prefs.isScreenOff(ctx)) {
            PendingIntent.getActivity(ctx, req, DimActivity.startIntent(ctx, minutes), FLAGS)
        } else {
            broadcast(ctx, TimerController.ACTION_START, req) {
                it.putExtra(TimerController.EXTRA_MINUTES, minutes)
            }
        }
    }

    /** Start the duration currently shown on the widget. */
    fun startSelected(ctx: Context): PendingIntent = start(ctx, Prefs.getLastMinutes(ctx))

    fun cancel(ctx: Context): PendingIntent =
        broadcast(ctx, TimerController.ACTION_CANCEL, REQ_CANCEL)

    /** Shifts the running timer, or the pending selection when idle, by [deltaMinutes]. */
    fun adjust(ctx: Context, deltaMinutes: Int): PendingIntent =
        broadcast(ctx, TimerController.ACTION_ADJUST, REQ_ADJUST_BASE + DELTA_OFFSET + deltaMinutes) {
            it.putExtra(TimerController.EXTRA_DELTA, deltaMinutes)
        }

    fun adjustFade(ctx: Context, deltaSeconds: Int): PendingIntent =
        broadcast(ctx, TimerController.ACTION_ADJUST_FADE, REQ_FADE_BASE + DELTA_OFFSET + deltaSeconds) {
            it.putExtra(TimerController.EXTRA_DELTA, deltaSeconds)
        }

    fun toggleScreenOff(ctx: Context): PendingIntent =
        broadcast(ctx, TimerController.ACTION_TOGGLE_SCREEN_OFF, REQ_SCREEN_OFF)

    /**
     * The lock option needs a device-admin grant, and only an activity can ask for it, so
     * before it is granted this tap opens the app instead of toggling anything.
     */
    fun toggleLock(ctx: Context): PendingIntent =
        if (ScreenLocker.isAdminActive(ctx)) {
            broadcast(ctx, TimerController.ACTION_TOGGLE_LOCK, REQ_LOCK)
        } else {
            openApp(ctx)
        }

    private fun broadcast(
        ctx: Context,
        action: String,
        req: Int,
        extras: (Intent) -> Unit = {}
    ): PendingIntent {
        val i = Intent(ctx, TimerReceiver::class.java).setAction(action).also(extras)
        return PendingIntent.getBroadcast(ctx, req, i, FLAGS)
    }
}
