package hu.quaternion.sleeptimer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.view.KeyEvent
import androidx.core.content.getSystemService

/**
 * Replicates the reference app's behaviour: gently fade the media stream down,
 * then pause whatever is playing (Spotify / YouTube / any player), then restore
 * the original volume so the next session starts at normal loudness.
 *
 * Runs on a background thread — keep total time under ~8s so it fits inside the
 * BroadcastReceiver's goAsync() window.
 */
object MediaStopper {

    fun fadeAndStop(ctx: Context, fadeSeconds: Int) {
        val am = ctx.getSystemService<AudioManager>() ?: return

        val originalVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)

        // --- Fade the stream volume down to zero ---
        val fadeMs = (fadeSeconds.coerceIn(0, 20)) * 1000L
        if (fadeMs > 0 && originalVolume > 0) {
            val steps = 20
            val stepDelay = fadeMs / steps
            for (i in 1..steps) {
                val v = originalVolume - (originalVolume * i / steps)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, v.coerceAtLeast(0), 0)
                try {
                    Thread.sleep(stepDelay)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }

        // --- Nudge well-behaved players to pause by grabbing audio focus ---
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .build()
        am.requestAudioFocus(focusRequest)

        // --- Explicitly pause the active media session ---
        dispatchPause(am)

        // Give players a moment to react, then let go of focus.
        try {
            Thread.sleep(400)
        } catch (_: InterruptedException) {
        }
        am.abandonAudioFocusRequest(focusRequest)

        // --- Restore original volume for next time ---
        am.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
    }

    private fun dispatchPause(am: AudioManager) {
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
    }
}
