package hu.quaternion.sleeptimer

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews

/**
 * Home-screen widgets. Three sizes, one shared renderer:
 *  - [SleepTimerWidgetCompact] : a single row
 *  - [SleepTimerWidget]        : countdown, presets and the two toggles
 *  - [SleepTimerWidgetLarge]   : every control the main screen has
 */
class SleepTimerWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) =
        Widgets.push(mgr, ids, WidgetViews.standard(ctx))
}

class SleepTimerWidgetCompact : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) =
        Widgets.push(mgr, ids, WidgetViews.compact(ctx))
}

class SleepTimerWidgetLarge : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) =
        Widgets.push(mgr, ids, WidgetViews.large(ctx))
}

/** Keeps every placed widget, of any size, in sync with the current state. */
object Widgets {

    fun updateAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        refresh(ctx, mgr, SleepTimerWidgetCompact::class.java) { WidgetViews.compact(ctx) }
        refresh(ctx, mgr, SleepTimerWidget::class.java) { WidgetViews.standard(ctx) }
        refresh(ctx, mgr, SleepTimerWidgetLarge::class.java) { WidgetViews.large(ctx) }
    }

    internal fun push(mgr: AppWidgetManager, ids: IntArray, views: RemoteViews) {
        for (id in ids) mgr.updateAppWidget(id, views)
    }

    /** Only builds the views if a widget of that size is actually placed. */
    private fun refresh(
        ctx: Context,
        mgr: AppWidgetManager,
        provider: Class<*>,
        build: () -> RemoteViews
    ) {
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, provider))
        if (ids.isEmpty()) return
        push(mgr, ids, build())
    }
}
