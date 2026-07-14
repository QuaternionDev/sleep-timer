package hu.quaternion.sleeptimer

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var bigTime: TextView
    private lateinit var minutesLabel: TextView
    private lateinit var seekMinutes: SeekBar
    private lateinit var seekFade: SeekBar
    private lateinit var fadeLabel: TextView
    private lateinit var startStop: MaterialButton
    private lateinit var switchScreenOff: MaterialSwitch
    private lateinit var status: TextView

    private var selectedMinutes = 30

    private val ticker = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            refreshRunningDisplay()
            if (Prefs.isRunning(this@MainActivity)) {
                ticker.postDelayed(this, 1000L)
            }
        }
    }

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bigTime = findViewById(R.id.bigTime)
        minutesLabel = findViewById(R.id.minutesLabel)
        seekMinutes = findViewById(R.id.seekMinutes)
        seekFade = findViewById(R.id.seekFade)
        fadeLabel = findViewById(R.id.fadeLabel)
        startStop = findViewById(R.id.startStop)
        switchScreenOff = findViewById(R.id.switchScreenOff)
        status = findViewById(R.id.status)

        switchScreenOff.isChecked = Prefs.isScreenOff(this)
        switchScreenOff.setOnCheckedChangeListener { _, checked ->
            Prefs.setScreenOff(this, checked)
        }

        selectedMinutes = Prefs.getLastMinutes(this)

        // Minutes dial: SeekBar range 0..179 maps to 1..180 minutes.
        seekMinutes.max = 179
        seekMinutes.progress = (selectedMinutes - 1).coerceIn(0, 179)
        seekMinutes.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedMinutes = progress + 1
                Prefs.setLastMinutes(this@MainActivity, selectedMinutes)
                if (!Prefs.isRunning(this@MainActivity)) showIdleDuration()
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Fade dial: 0..20 seconds.
        seekFade.max = 20
        seekFade.progress = Prefs.getFadeSeconds(this)
        updateFadeLabel(seekFade.progress)
        seekFade.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                Prefs.setFadeSeconds(this@MainActivity, progress)
                updateFadeLabel(progress)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // One-tap presets start immediately, matching the reference app.
        wirePreset(R.id.preset15, 15)
        wirePreset(R.id.preset30, 30)
        wirePreset(R.id.preset45, 45)
        wirePreset(R.id.preset60, 60)
        wirePreset(R.id.preset90, 90)

        startStop.setOnClickListener {
            if (Prefs.isRunning(this)) {
                TimerController.cancel(this)
            } else {
                ensureExactAlarmAllowed()
                TimerController.start(this, selectedMinutes)
                maybeStartScreenOff()
            }
            syncUi()
        }

        requestNotificationPermissionIfNeeded()
    }

    private fun maybeStartScreenOff() {
        if (Prefs.isScreenOff(this)) {
            startActivity(Intent(this, DimActivity::class.java))
        }
    }

    private fun wirePreset(id: Int, minutes: Int) {
        findViewById<Button>(id).setOnClickListener {
            ensureExactAlarmAllowed()
            TimerController.start(this, minutes)
            maybeStartScreenOff()
            syncUi()
        }
    }

    override fun onResume() {
        super.onResume()
        syncUi()
    }

    override fun onPause() {
        super.onPause()
        ticker.removeCallbacks(tick)
    }

    private fun syncUi() {
        if (Prefs.isRunning(this)) {
            startStop.setText(R.string.stop)
            status.setText(R.string.status_running)
            ticker.removeCallbacks(tick)
            ticker.post(tick)
        } else {
            startStop.setText(R.string.start)
            status.setText(R.string.status_idle)
            ticker.removeCallbacks(tick)
            showIdleDuration()
        }
    }

    private fun showIdleDuration() {
        bigTime.text = selectedMinutes.toString()
        minutesLabel.setText(R.string.minutes)
    }

    private fun refreshRunningDisplay() {
        val remaining = Prefs.remainingMillis(this)
        if (remaining <= 0L) {
            syncUi()
            return
        }
        bigTime.text = formatRemaining(remaining)
        minutesLabel.setText(R.string.remaining)
    }

    private fun formatRemaining(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    }

    private fun updateFadeLabel(seconds: Int) {
        fadeLabel.text = getString(R.string.fade_label, seconds)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /** On Android 12/12L, exact-alarm access can be revoked by the user. Nudge them. */
    private fun ensureExactAlarmAllowed() {
        if (Build.VERSION.SDK_INT in 31..32) {
            val am = getSystemService<AlarmManager>() ?: return
            if (!am.canScheduleExactAlarms()) {
                Toast.makeText(this, R.string.exact_alarm_hint, Toast.LENGTH_LONG).show()
                runCatching {
                    startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
            }
        }
    }
}
