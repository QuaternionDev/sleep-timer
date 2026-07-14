package hu.quaternion.sleeptimer

import android.content.Context

/** Tiny wrapper over SharedPreferences holding all timer state. */
object Prefs {
    private const val FILE = "sleep_timer_prefs"

    private const val KEY_END_TIME = "end_time"        // epoch millis when music stops (0 = idle)
    private const val KEY_LAST_MIN = "last_minutes"    // last duration chosen on the main screen
    private const val KEY_FADE_SEC = "fade_seconds"    // fade-out length in seconds
    private const val KEY_EXTEND_MIN = "extend_min"    // minutes added by the "+" action
    private const val KEY_SCREEN_OFF = "screen_off"    // dim the screen to black while running

    private fun sp(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getEndTime(ctx: Context): Long = sp(ctx).getLong(KEY_END_TIME, 0L)

    fun setEndTime(ctx: Context, value: Long) =
        sp(ctx).edit().putLong(KEY_END_TIME, value).apply()

    fun isRunning(ctx: Context): Boolean =
        getEndTime(ctx) > System.currentTimeMillis()

    fun remainingMillis(ctx: Context): Long =
        (getEndTime(ctx) - System.currentTimeMillis()).coerceAtLeast(0L)

    fun getLastMinutes(ctx: Context): Int = sp(ctx).getInt(KEY_LAST_MIN, 30)
    fun setLastMinutes(ctx: Context, m: Int) =
        sp(ctx).edit().putInt(KEY_LAST_MIN, m).apply()

    fun getFadeSeconds(ctx: Context): Int = sp(ctx).getInt(KEY_FADE_SEC, 5)
    fun setFadeSeconds(ctx: Context, s: Int) =
        sp(ctx).edit().putInt(KEY_FADE_SEC, s).apply()

    fun getExtendMinutes(ctx: Context): Int = sp(ctx).getInt(KEY_EXTEND_MIN, 5)
    fun setExtendMinutes(ctx: Context, m: Int) =
        sp(ctx).edit().putInt(KEY_EXTEND_MIN, m).apply()

    fun isScreenOff(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_SCREEN_OFF, false)
    fun setScreenOff(ctx: Context, on: Boolean) =
        sp(ctx).edit().putBoolean(KEY_SCREEN_OFF, on).apply()
}
