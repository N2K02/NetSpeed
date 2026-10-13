# NetSpeed

A tiny Android app that shows how much internet speed your phone is using **right now**, as a small floating label on top of every app:

```
20 D/ 5 U
```

- **D** = download, **U** = upload, for the whole phone (all apps), rounded to a whole number.
- Refreshes every **1 second** by default; change it to **2** or **3 seconds** in the app.
- Shows **0 D/ 0 U** when there is no internet.
- Runs entirely in the background with **no notification**.
- Unit is **Mbps** by default (how internet speed is usually quoted); KB/s and MB/s are also available.
- Drag the label to move it, tap it to open settings, or lock it so touches pass through.
- Pauses while the screen is off to save battery, and comes back by itself after a restart.

## Install

1. Open the latest **Release** of this repo on your phone and download `NetSpeed.apk`.
2. Install it (allow "install unknown apps" for your browser if asked).
3. Open NetSpeed → **Allow display over other apps** → turn NetSpeed on → go back.
4. The meter appears. Tap **Let it run in the background** and allow it.

Tip: on Samsung, Xiaomi, Huawei and similar phones also set NetSpeed's battery usage to **Unrestricted** so the phone doesn't stop it.

## How it works

Android only lets an app run in the background without a notification while it has something visible on screen. NetSpeed's floating label is that visible thing (it uses the "display over other apps" permission), so it needs no foreground service and no notification. Every interval it reads the phone's total bytes received/sent (`TrafficStats`) and divides the change by the elapsed time.

## Build

Every push to `main` builds the APK with GitHub Actions and publishes it as a release. Locally: `./gradlew assembleDebug` (needs JDK 17 + Android SDK).
