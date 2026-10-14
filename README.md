# NetSpeed

[![Latest release](https://img.shields.io/github/v/release/N2K02/NetSpeed?label=download)](https://github.com/N2K02/NetSpeed/releases/latest) ![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white) ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white) [![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A tiny Android app that shows how much internet speed your phone is using **right now**, in the status bar next to the clock:

*Internet speed meter for Android: live download and upload speed in the status bar, with no notification.*

**[⬇ Download the latest APK](https://github.com/N2K02/NetSpeed/releases/latest)** · Android 8.0 or newer · Free and open source

```
20 Mb / 5 Kb
```

- Download first, upload second, for the whole phone (all apps), rounded to a whole number.
- **Kb** (kilobits/s) below 1000 Kb, **Mb** (megabits/s) from there on.
- Refreshes every **1 second** by default; change it to **2** or **3 seconds** in the app.
- Shows **0 Kb / 0 Kb** when there is no internet.
- Runs entirely in the background with **no notification**.
- The app screen shows the current **Download** and **Upload** speed.
- Horizontal and vertical sliders in the app to place it just left of your clock; text size and colour (white/black) are adjustable.
- Settings screen uses Jetpack Compose with Material 3 Expressive (same theme as VGS and PixelCast) with your wallpaper colours, and follows the phone's light/dark mode.
- Pauses while the screen is off to save battery, and comes back by itself after a restart.

## Install

1. Open the latest **Release** of this repo on your phone and download `NetSpeed-<version>.apk` (e.g. `NetSpeed-1.0.apk`).
2. Install it (allow "install unknown apps" for your browser if asked).
3. Open NetSpeed → **Allow display over other apps** → turn NetSpeed on → go back.
4. The speed appears at the top right, just below the status bar; use the Position sliders to move it. Tap **Let it run in the background** and allow it.

Tip: on Samsung, Xiaomi, Huawei and similar phones also set NetSpeed's battery usage to **Unrestricted** so the phone doesn't stop it.

## How it works

Android only lets an app run in the background without a notification while it has something visible on screen. Android also doesn't let normal apps add text to the real status bar, so NetSpeed draws a transparent overlay lined up exactly with the status bar row. That visible overlay ("display over other apps" permission) is also what lets it keep running, so it needs no foreground service and no notification. Every interval it reads the phone's total bytes received/sent (`TrafficStats`) and divides the change by the elapsed time.

## Updates

The app updates itself from this repo's Releases:

- When you open it (at most every 12 hours) it checks for a newer release. If there is one, a card shows **what's new** with **Update now**, **Later** or **Skip this version**.
- **Check for updates** is at the bottom of the main screen, next to the version number.
- The APK is downloaded inside the app, checked (size, SHA-256 checksum from GitHub, same app, newer version, same signing key) and installed with Android's own installer.
- The first time, Android asks you to allow installs from this app ("Install unknown apps"); after that you just confirm the update.

## Signing

Releases are signed with a private key that is kept encrypted in `signing/keys.tar.enc` and unlocked during the build with the `SIGNING_PASSPHRASE` repository secret (see [signing/README.md](signing/README.md)). Earlier builds used a public test key; Android's key rotation moves installed apps to the private key on the next update, without reinstalling.

## Build

Every push to `main` builds the APK with GitHub Actions and publishes it as a release named "NetSpeed <version>" (1.0, 1.1, 1.2 …, one decimal place) with release notes listing the changes since the previous release. To jump to a new number (e.g. 2.0), run the workflow by hand from the Actions tab and type the version. Locally: `./gradlew assembleRelease` (shrunk APK) (needs JDK 17 + Android SDK).

## License

MIT. See [LICENSE](LICENSE).
