package hu.quaternion.sleeptimer

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews

/**
 * Home-screen widget: three preset buttons when idle, a live countdown +
 * cancel button while a timer is running.
 */
class SleepTimerWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    companion object {
        // Preset durations shown on the widget (minutes). Double as request codes.
        private val PRESETS = intArrayOf(15, 30, 60)
        private const val REQ_CANCEL = 900

        /** Push a fresh RemoteViews to every placed widget. */
        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(
                ComponentName(context, SleepTimerWidget::class.java)
            )
            if (ids.isEmpty()) return
            val views = buildViews(context)
            for (id in ids) mgr.updateAppWidget(id, views)
        }

        private fun startPreset(context: Context, minutes: Int): PendingIntent {
            val i = Intent(context, TimerReceiver::class.java)
                .setAction(TimerController.ACTION_START)
                .putExtra(TimerController.EXTRA_MINUTES, minutes)
            return PendingIntent.getBroadcast(
                context, minutes, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun cancelIntent(context: Context): PendingIntent {
            val i = Intent(context, TimerReceiver::class.java)
                .setAction(TimerController.ACTION_CANCEL)
            return PendingIntent.getBroadcast(
                context, REQ_CANCEL, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_sleep_timer)

            // Tapping the title opens the app.
            val openApp = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, openApp)

            // Preset buttons (always wired; only visible when idle).
            views.setOnClickPendingIntent(R.id.widget_preset_1, startPreset(context, PRESETS[0]))
            views.setOnClickPendingIntent(R.id.widget_preset_2, startPreset(context, PRESETS[1]))
            views.setOnClickPendingIntent(R.id.widget_preset_3, startPreset(context, PRESETS[2]))
            views.setOnClickPendingIntent(R.id.widget_cancel, cancelIntent(context))

            if (Prefs.isRunning(context)) {
                views.setViewVisibility(R.id.widget_idle, View.GONE)
                views.setViewVisibility(R.id.widget_running, View.VISIBLE)

                // Convert wall-clock end time into the Chronometer's elapsedRealtime base.
                val endTime = Prefs.getEndTime(context)
                val base = SystemClock.elapsedRealtime() +
                    (endTime - System.currentTimeMillis())
                views.setChronometerCountDown(R.id.widget_chrono, true)
                views.setChronometer(R.id.widget_chrono, base, null, true)
            } else {
                views.setViewVisibility(R.id.widget_running, View.GONE)
                views.setViewVisibility(R.id.widget_idle, View.VISIBLE)
            }

            return views
        }
    }
}
