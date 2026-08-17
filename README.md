# Sleep Timer

A small, ad-free Android sleep timer. It fades your audio or video out and then
pauses whatever is playing. It has quick presets, notification controls, 
an optional screen-off mode, and a home-screen widget.

## Features

- Duration dial (1–180 min) and one-tap presets (15 / 30 / 45 / 60 / 90).
- Adjustable fade-out length (0–20 s).
- On expiry: fade the media stream down, take audio focus, dispatch a media
  pause, then restore your original volume for next time.
- Screen-off mode: a full-black screen at minimum brightness covers the display
  while the timer runs, so the light from a video goes away while audio keeps
  playing. Tap anywhere, or let the timer end, to bring the screen back.
- Optional screen lock when the timer ends. Android only allows this for an active
  device admin, so the switch asks for that permission once; the app requests the
  force-lock policy and nothing else, and turning the switch off revokes it.
- Ongoing notification with a live countdown, +min and Cancel buttons.
- Three home-screen widgets, all of them live — the app itself is only needed for
  the one-time screen-lock permission:
  - **One line** (4x1): five presets when idle; countdown, +5 and Stop while running.
  - **Standard** (4x2): the screen-off and lock toggles, the selected duration or a
    live countdown, then presets when idle / −5, +5 and Cancel while running.
  - **Full** (4x4): all of the above plus fine adjust around the value, the
    fade-out length, and a Start / Cancel button.
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

## AI Notice

The app was written with the help of Claude by Anthropic. This app is purely vibe-coded, cause I wanted an app for my own needs.

## How to contribute?

If u have a new feature in mind, feel free to fork the repo and add a feature or fix a bug u found, then open a pull request for me to review it.
