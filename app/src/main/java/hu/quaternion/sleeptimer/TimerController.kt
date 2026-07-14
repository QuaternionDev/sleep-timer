package hu.quaternion.sleeptimer

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService

/**
 * Single source of truth for starting / cancelling / extending the sleep timer.
 * Called from the UI, the widget, notification actions and the boot receiver.
 */
object TimerController {

    const val ACTION_EXPIRED = "hu.quaternion.sleeptimer.EXPIRED"
    const val ACTION_CANCEL = "hu.quaternion.sleeptimer.CANCEL"
    const val ACTION_EXTEND = "hu.quaternion.sleeptimer.EXTEND"
    const val ACTION_START = "hu.quaternion.sleeptimer.START"
    const val EXTRA_MINUTES = "minutes"

    private const val CHANNEL_ID = "sleep_timer"
    private const val NOTIF_ID = 1
    private const val REQ_ALARM = 100
    private const val REQ_CANCEL = 101
    private const val REQ_EXTEND = 102
    private const val REQ_CONTENT = 103

    /** Start (or restart) a timer for [minutes] minutes from now. */
    fun start(ctx: Context, minutes: Int) {
        val safeMinutes = minutes.coerceIn(1, 720)
        val endTime = System.currentTimeMillis() + safeMinutes * 60_000L
        Prefs.setEndTime(ctx, endTime)
        Prefs.setLastMinutes(ctx, safeMinutes)
        scheduleAlarm(ctx, endTime)
        showNotification(ctx, endTime)
        SleepTimerWidget.updateAll(ctx)
    }

    /** Add [addMinutes] to a running timer (or the default extend amount). */
    fun extend(ctx: Context, addMinutes: Int = Prefs.getExtendMinutes(ctx)) {
        if (!Prefs.isRunning(ctx)) return
        val endTime = Prefs.getEndTime(ctx) + addMinutes * 60_000L
        Prefs.setEndTime(ctx, endTime)
        scheduleAlarm(ctx, endTime)
        showNotification(ctx, endTime)
        SleepTimerWidget.updateAll(ctx)
    }

    /** Cancel a running timer without touching the music. */
    fun cancel(ctx: Context) {
        cancelAlarm(ctx)
        Prefs.setEndTime(ctx, 0L)
        NotificationManagerCompat.from(ctx).cancel(NOTIF_ID)
        DimActivity.dismiss(ctx)
        SleepTimerWidget.updateAll(ctx)
    }

    /** Called by TimerReceiver once the alarm fires and the music has been stopped. */
    fun onExpired(ctx: Context) {
        Prefs.setEndTime(ctx, 0L)
        NotificationManagerCompat.from(ctx).cancel(NOTIF_ID)
        DimActivity.dismiss(ctx)
        SleepTimerWidget.updateAll(ctx)
    }

    /** Re-arm the alarm after a reboot if a timer was still pending. */
    fun rescheduleIfNeeded(ctx: Context) {
        if (Prefs.isRunning(ctx)) {
            val endTime = Prefs.getEndTime(ctx)
            scheduleAlarm(ctx, endTime)
            showNotification(ctx, endTime)
        } else {
            Prefs.setEndTime(ctx, 0L)
        }
        SleepTimerWidget.updateAll(ctx)
    }

    // ---- Alarm plumbing ------------------------------------------------------

    private fun expiredPendingIntent(ctx: Context): PendingIntent {
        val i = Intent(ctx, TimerReceiver::class.java).setAction(ACTION_EXPIRED)
        return PendingIntent.getBroadcast(
            ctx, REQ_ALARM, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleAlarm(ctx: Context, endTime: Long) {
        val am = ctx.getSystemService<AlarmManager>() ?: return
        val pi = expiredPendingIntent(ctx)
        val canExact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime, pi)
        } else {
            // Fall back to an inexact alarm if the user revoked exact-alarm access.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime, pi)
        }
    }

    private fun cancelAlarm(ctx: Context) {
        val am = ctx.getSystemService<AlarmManager>() ?: return
        am.cancel(expiredPendingIntent(ctx))
    }

    // ---- Notification --------------------------------------------------------

    private fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService<NotificationManager>() ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                ctx.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = ctx.getString(R.string.channel_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(ch)
        }
    }

    private fun broadcast(ctx: Context, action: String, req: Int): PendingIntent {
        val i = Intent(ctx, TimerReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            ctx, req, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showNotification(ctx: Context, endTime: Long) {
        ensureChannel(ctx)

        val contentPi = PendingIntent.getActivity(
            ctx, REQ_CONTENT,
            Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val extendMin = Prefs.getExtendMinutes(ctx)

        val n: Notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(ctx.getString(R.string.notif_title))
            .setContentText(ctx.getString(R.string.notif_text))
            .setContentIntent(contentPi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(endTime)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setColor(ctx.getColor(R.color.accent))
            .addAction(
                0,
                ctx.getString(R.string.action_extend, extendMin),
                broadcast(ctx, ACTION_EXTEND, REQ_EXTEND)
            )
            .addAction(
                0,
                ctx.getString(R.string.action_cancel),
                broadcast(ctx, ACTION_CANCEL, REQ_CANCEL)
            )
            .build()

        // POST_NOTIFICATIONS may not be granted; NotificationManagerCompat handles it gracefully.
        NotificationManagerCompat.from(ctx).notify(NOTIF_ID, n)
    }
}
