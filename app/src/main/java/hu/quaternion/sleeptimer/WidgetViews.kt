package hu.quaternion.sleeptimer

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

/**
 * Builds the RemoteViews for each widget size. The three layouts share ids where they share
 * controls, but every binder below only touches ids that exist in the layout it is given —
 * RemoteViews throws when an action targets a missing view.
 */
object WidgetViews {

    private val PRESETS = intArrayOf(15, 30, 45, 60, 90)
    private val PRESET_IDS = intArrayOf(
        R.id.widget_preset_1, R.id.widget_preset_2, R.id.widget_preset_3,
        R.id.widget_preset_4, R.id.widget_preset_5
    )

    /** One line: presets when idle, countdown + extend + stop while running. */
    fun compact(ctx: Context): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_compact)
        val running = Prefs.isRunning(ctx)

        v.setOnClickPendingIntent(R.id.widget_icon, WidgetActions.openApp(ctx))
        bindPresets(ctx, v)
        v.setOnClickPendingIntent(R.id.widget_plus, WidgetActions.adjust(ctx, ADJUST_STEP))
        v.setOnClickPendingIntent(R.id.widget_cancel, WidgetActions.cancel(ctx))

        v.setViewVisibility(R.id.widget_idle, visibility(!running))
        v.setViewVisibility(R.id.widget_running, visibility(running))
        if (running) bindChronometer(ctx, v)

        return v
    }

    /** Header toggles, value or countdown, then presets / adjust + cancel. */
    fun standard(ctx: Context): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_sleep_timer)
        val running = Prefs.isRunning(ctx)

        bindHeader(ctx, v)
        bindPresets(ctx, v)
        bindValue(ctx, v, running)
        v.setOnClickPendingIntent(R.id.widget_minus, WidgetActions.adjust(ctx, -ADJUST_STEP))
        v.setOnClickPendingIntent(R.id.widget_plus, WidgetActions.adjust(ctx, ADJUST_STEP))
        v.setOnClickPendingIntent(R.id.widget_cancel, WidgetActions.cancel(ctx))

        v.setViewVisibility(R.id.widget_idle, visibility(!running))
        v.setViewVisibility(R.id.widget_running, visibility(running))

        return v
    }

    /** Everything: adjust, presets, fade length, both toggles and Start / Cancel. */
    fun large(ctx: Context): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_large)
        val running = Prefs.isRunning(ctx)

        bindHeader(ctx, v)
        bindPresets(ctx, v)
        bindValue(ctx, v, running)

        v.setOnClickPendingIntent(R.id.widget_minus, WidgetActions.adjust(ctx, -ADJUST_STEP))
        v.setOnClickPendingIntent(R.id.widget_plus, WidgetActions.adjust(ctx, ADJUST_STEP))
        v.setContentDescription(R.id.widget_minus, ctx.getString(R.string.widget_less))
        v.setContentDescription(R.id.widget_plus, ctx.getString(R.string.widget_more))

        val fade = Prefs.getFadeSeconds(ctx)
        v.setTextViewText(R.id.widget_fade_value, ctx.getString(R.string.widget_fade_value, fade))
        v.setOnClickPendingIntent(R.id.widget_fade_minus, WidgetActions.adjustFade(ctx, -1))
        v.setOnClickPendingIntent(R.id.widget_fade_plus, WidgetActions.adjustFade(ctx, 1))

        // The primary button is Start when idle and Cancel while running.
        if (running) {
            v.setTextViewText(R.id.widget_primary, ctx.getString(R.string.action_cancel))
            v.setInt(R.id.widget_primary, "setBackgroundResource", R.drawable.widget_cancel_bg)
            v.setTextColor(R.id.widget_primary, color(ctx, R.color.on_bg))
            v.setOnClickPendingIntent(R.id.widget_primary, WidgetActions.cancel(ctx))
        } else {
            v.setTextViewText(R.id.widget_primary, ctx.getString(R.string.start))
            v.setInt(R.id.widget_primary, "setBackgroundResource", R.drawable.widget_primary_bg)
            v.setTextColor(R.id.widget_primary, color(ctx, R.color.accent_on))
            v.setOnClickPendingIntent(R.id.widget_primary, WidgetActions.startSelected(ctx))
        }

        return v
    }

    // ---- Shared binders --------------------------------------------------------

    private fun bindHeader(ctx: Context, v: RemoteViews) {
        val openApp = WidgetActions.openApp(ctx)
        v.setOnClickPendingIntent(R.id.widget_icon, openApp)
        v.setOnClickPendingIntent(R.id.widget_title, openApp)

        bindToggle(
            ctx, v, R.id.widget_screen_off,
            on = Prefs.isScreenOff(ctx),
            label = ctx.getString(R.string.widget_screen_off),
            action = WidgetActions.toggleScreenOff(ctx)
        )
        // Without the device-admin grant the option cannot be on, so it always reads as off.
        bindToggle(
            ctx, v, R.id.widget_lock,
            on = Prefs.isLockScreen(ctx) && ScreenLocker.isAdminActive(ctx),
            label = ctx.getString(R.string.widget_lock),
            action = WidgetActions.toggleLock(ctx)
        )
    }

    private fun bindToggle(
        ctx: Context,
        v: RemoteViews,
        id: Int,
        on: Boolean,
        label: String,
        action: android.app.PendingIntent
    ) {
        v.setInt(
            id, "setBackgroundResource",
            if (on) R.drawable.widget_chip_on else R.drawable.widget_chip_off
        )
        v.setTextColor(ctx, id, if (on) R.color.accent_on else R.color.on_bg_dim)
        v.setContentDescription(
            id,
            ctx.getString(if (on) R.string.widget_toggle_on else R.string.widget_toggle_off, label)
        )
        v.setOnClickPendingIntent(id, action)
    }

    private fun bindPresets(ctx: Context, v: RemoteViews) {
        for (i in PRESETS.indices) {
            v.setOnClickPendingIntent(PRESET_IDS[i], WidgetActions.start(ctx, PRESETS[i]))
        }
    }

    /** Swaps the big number for the live countdown and relabels the caption under it. */
    private fun bindValue(ctx: Context, v: RemoteViews, running: Boolean) {
        v.setViewVisibility(R.id.widget_value, visibility(!running))
        v.setViewVisibility(R.id.widget_chrono, visibility(running))
        if (running) {
            bindChronometer(ctx, v)
            v.setTextViewText(R.id.widget_sub, ctx.getString(R.string.remaining))
        } else {
            v.setTextViewText(R.id.widget_value, Prefs.getLastMinutes(ctx).toString())
            v.setTextViewText(R.id.widget_sub, ctx.getString(R.string.minutes))
        }
    }

    /** The Chronometer counts in elapsedRealtime, so convert the wall-clock end time. */
    private fun bindChronometer(ctx: Context, v: RemoteViews) {
        val base = SystemClock.elapsedRealtime() +
            (Prefs.getEndTime(ctx) - System.currentTimeMillis())
        v.setChronometerCountDown(R.id.widget_chrono, true)
        v.setChronometer(R.id.widget_chrono, base, null, true)
    }

    private fun visibility(visible: Boolean) = if (visible) View.VISIBLE else View.GONE

    private fun color(ctx: Context, res: Int) = ContextCompat.getColor(ctx, res)

    private fun RemoteViews.setTextColor(ctx: Context, id: Int, res: Int) =
        setTextColor(id, color(ctx, res))

    /** Minutes added or removed by the widget's − / + buttons. */
    private const val ADJUST_STEP = 5
}
