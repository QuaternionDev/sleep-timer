package hu.quaternion.sleeptimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * "Screen-off mode": a full-black screen at minimum brightness shown while the
 * timer runs, so the light from a video you fell asleep to goes away while audio
 * keeps playing. Tapping anywhere dismisses it; it also closes itself when the
 * timer ends or is cancelled.
 */
class DimActivity : AppCompatActivity() {

    private val ticker = Handler(Looper.getMainLooper())

    private val finishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    private val autoFinish = object : Runnable {
        override fun run() {
            if (!Prefs.isRunning(this@DimActivity)) {
                finish()
            } else {
                ticker.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over the lock screen and keep it up until dismissed.
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(false)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            )
        }

        setContentView(R.layout.activity_dim)

        // Force the display brightness to its minimum for this window only.
        window.attributes = window.attributes.apply {
            screenBrightness = 0.01f
        }

        findViewById<TextView>(R.id.dimHint).text = getString(R.string.screen_off_tap)

        // Any tap exits screen-off mode.
        findViewById<android.view.View>(R.id.dimRoot).setOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(ACTION_FINISH_DIM)
        if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.registerReceiver(
                this, finishReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(finishReceiver, filter)
        }
        ticker.post(autoFinish)
    }

    override fun onStop() {
        super.onStop()
        ticker.removeCallbacks(autoFinish)
        runCatching { unregisterReceiver(finishReceiver) }
    }

    companion object {
        const val ACTION_FINISH_DIM = "hu.quaternion.sleeptimer.FINISH_DIM"

        /** Called when the timer ends or is cancelled to drop the dim screen. */
        fun dismiss(ctx: Context) {
            ctx.sendBroadcast(Intent(ACTION_FINISH_DIM).setPackage(ctx.packageName))
        }
    }
}
