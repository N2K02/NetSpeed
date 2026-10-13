# NetSpeed

A tiny Android app that shows how much internet speed your phone is using **right now**, in the status bar next to the clock:

```
20 Mb / 5 Kb
```

- Download first, upload second, for the whole phone (all apps), rounded to a whole number.
- **Kb** (kilobits/s) below 1000 Kb, **Mb** (megabits/s) from there on.
- Refreshes every **1 second** by default; change it to **2** or **3 seconds** in the app.
- Shows **0 Kb / 0 Kb** when there is no internet.
- Runs entirely in the background with **no notification**.
- The app screen shows the current **Download** and **Upload** speed.
- Use the slider in the app to place it just left of your clock; text size and colour (white/black) are adjustable.
- Pauses while the screen is off to save battery, and comes back by itself after a restart.

## Install

1. Open the latest **Release** of this repo on your phone and download `NetSpeed.apk`.
2. Install it (allow "install unknown apps" for your browser if asked).
3. Open NetSpeed → **Allow display over other apps** → turn NetSpeed on → go back.
4. The speed appears in the status bar; move the slider until it sits left of your clock. Tap **Let it run in the background** and allow it.

Tip: on Samsung, Xiaomi, Huawei and similar phones also set NetSpeed's battery usage to **Unrestricted** so the phone doesn't stop it.

## How it works

Android only lets an app run in the background without a notification while it has something visible on screen. Android also doesn't let normal apps add text to the real status bar, so NetSpeed draws a transparent overlay lined up exactly with the status bar row. That visible overlay is also what ("display over other apps" permission) lets it run without a foreground service, so it needs no foreground service and no notification. Every interval it reads the phone's total bytes received/sent (`TrafficStats`) and divides the change by the elapsed time.

## Build

Every push to `main` builds the APK with GitHub Actions and publishes it as a release. Locally: `./gradlew assembleDebug` (needs JDK 17 + Android SDK).
