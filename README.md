# Sleep Timer

A small, ad-free Android sleep timer. It fades your audio or video out and then
pauses whatever is playing — Spotify, YouTube, Jellyfin, podcasts, any player —
with quick presets, notification controls, an optional screen-off mode, and a
home-screen widget.

- Package: `hu.quaternion.sleeptimer`
- minSdk 26 (Android 8.0), targetSdk 34
- Kotlin, no third-party SDKs beyond AndroidX and Material

## Features

- Duration dial (1–180 min) and one-tap presets (15 / 30 / 45 / 60 / 90).
- Adjustable fade-out length (0–20 s).
- On expiry: fade the media stream down, take audio focus, dispatch a media
  pause, then restore your original volume for next time.
- Screen-off mode: a full-black screen at minimum brightness covers the display
  while the timer runs, so the light from a video goes away while audio keeps
  playing. Tap anywhere, or let the timer end, to bring the screen back.
- Ongoing notification with a live countdown, +min and Cancel actions.
- Home-screen widget: presets when idle, a live countdown and Cancel while
  running.
- Survives reboots and fires on time in Doze.

## Building

Requires JDK 17 and the Android SDK.

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk` and is signed
with the debug key, which is fine for personal sideloading. For a release build,
add your own keystore and run `assembleRelease`.

## Reliability notes

- Pausing works by dispatching a media key and taking audio focus, which almost
  every player respects. A few players may only be muted by the fade as a
  fallback.
- The app uses exact alarms so the timer fires on time in Doze. On Android 12
  and 12L, if exact-alarm access was revoked, the app prompts you to re-enable
  it and otherwise falls back to an inexact alarm.

## Credits

Sleep Timer v1.0, built by QuaternionDev (https://github.com/QuaternionDev).

## AI Notice

The app was written with the help of Claude by Anthropic.