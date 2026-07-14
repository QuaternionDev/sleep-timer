# Sleep Timer

A small, ad-free, personal sleep-timer for Android. Fades your music/podcast out
and then pauses whatever is playing (Spotify, YouTube, Jellyfin, any player),
with quick presets, notification controls, and a home-screen widget with a live
countdown.

Built as a clone of the classic *Sleep Timer (Turn music off)* behaviour —
softly lower the volume, then stop playback — minus the ads and the premium wall
on the widget.

- Package: `hu.quaternion.sleeptimer`
- minSdk 26 (Android 8.0) · targetSdk 34 · Kotlin · no third-party SDKs beyond AndroidX

## Features

- Dial (1–180 min) + one-tap presets (15 / 30 / 45 / 60 / 90).
- Adjustable fade-out length (0–20 s).
- **Screen-off mode** (optional): when the timer starts, a full-black screen at
  minimum brightness covers the display so the light from a video you dozed off
  to goes away while audio keeps playing. Tap anywhere — or let the timer end —
  to bring the screen back.
- When the timer ends: fade the media stream down, grab audio focus, dispatch a
  media-pause, then restore your original volume for next time.
- Ongoing notification with a live countdown, **+min** and **Cancel** actions.
- Home-screen widget: preset buttons when idle, a live `Chronometer` + Cancel
  while running.
- Survives reboots (re-arms the alarm on boot).

## Build it without eating your disk (recommended: GitHub Actions)

You don't need Android Studio or the ~10 GB SDK locally. The included workflow
builds the APK on GitHub's runners and hands you the file.

1. Create a repo (e.g. `QuaternionDev/sleep-timer`) and push this folder:
   ```bash
   git init
   git add .
   git commit -m "Sleep Timer"
   git branch -M main
   git remote add origin git@github.com:QuaternionDev/sleep-timer.git
   git push -u origin main
   ```
2. GitHub → **Actions** tab → the **Build APK** run starts automatically
   (or click *Run workflow*).
3. When it's green, open the run → **Artifacts** → download
   `SleepTimer-debug-apk`. Inside is `app-debug.apk`.
4. Copy the APK to your phone, enable "install unknown apps" for your file
   manager, tap it, install. Long-press the home screen → Widgets → Sleep Timer.

Local footprint: just this source (a few hundred KB) plus git. All the heavy
SDK/Gradle downloads live on GitHub's runners, not your machine.

### Other low-disk options
- **GitHub Codespaces / Gitpod**: open the repo in a cloud dev container and run
  `./gradlew assembleDebug` there — same idea, zero local SDK.
- **On-device with Termux** is possible but actually *heavier* than CI; skip it
  unless you specifically want to build on the phone.

## Building locally (if you ever have the space)

Needs JDK 17 and the Android SDK (via Android Studio or command-line tools):
```bash
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```
The debug APK is signed with the auto-generated debug key — fine for personal
sideloading. For a release build you'd add your own keystore and run
`assembleRelease`.

## Notes on reliability
- Pausing works by dispatching a media key + grabbing audio focus. The vast
  majority of players (Spotify, YouTube, YT Music, podcast apps) respect this.
  A few stubborn apps may only get muted via the fade as a fallback.
- Uses `USE_EXACT_ALARM` so the timer fires on time even in Doze. On Android
  12/12L, if you revoked exact-alarm access, the app nudges you to re-enable it;
  otherwise it falls back to an inexact alarm.

## Easy things to add later
- Shake-to-extend (register a `SensorManager` accelerometer listener in
  `TimerReceiver`/a small service).
- A Quick Settings tile (`TileService`) for a one-swipe start.
- Turn off Bluetooth/Wi-Fi on expiry.

## Credits

Sleep Timer v1.0 — built by **QuaternionDev** (https://github.com/QuaternionDev).
Tap the credit line at the bottom of the app to open the GitHub org.
