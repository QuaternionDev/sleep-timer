package hu.quaternion.sleeptimer

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.getSystemService

/**
 * Locks the screen when the timer ends. Android only lets an app engage the keyguard if it is
 * an active device admin, so the user has to activate [LockAdminReceiver] once; the requested
 * policy set is force-lock only.
 */
object ScreenLocker {

    /**
     * Grace period so the screen-off overlay (which draws over the keyguard) is really gone
     * before the lock kicks in.
     */
    private const val LOCK_DELAY_MS = 400L

    private fun admin(ctx: Context) = ComponentName(ctx, LockAdminReceiver::class.java)

    fun isAdminActive(ctx: Context): Boolean {
        val dpm = ctx.getSystemService<DevicePolicyManager>() ?: return false
        return dpm.isAdminActive(admin(ctx))
    }

    /** Intent that opens the system screen where the user activates the lock permission. */
    fun activationIntent(ctx: Context): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin(ctx))
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                ctx.getString(R.string.lock_admin_explanation)
            )

    /** Give the privilege back when the user turns the option off. */
    fun releaseAdmin(ctx: Context) {
        val dpm = ctx.getSystemService<DevicePolicyManager>() ?: return
        val admin = admin(ctx)
        if (dpm.isAdminActive(admin)) {
            runCatching { dpm.removeActiveAdmin(admin) }
        }
    }

    /**
     * Lock the device if the user enabled the option and the admin is still active.
     * Sleeps briefly, so call this off the main thread.
     */
    fun lockIfEnabled(ctx: Context) {
        if (!Prefs.isLockScreen(ctx)) return
        val dpm = ctx.getSystemService<DevicePolicyManager>() ?: return
        if (!dpm.isAdminActive(admin(ctx))) return
        SystemClock.sleep(LOCK_DELAY_MS)
        // Throws if the admin was revoked between the check and here.
        runCatching { dpm.lockNow() }
    }
}
