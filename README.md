# NetSpeed

[![Latest release](https://img.shields.io/github/v/release/N2K02/NetSpeed?label=download)](https://github.com/N2K02/NetSpeed/releases/latest) ![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white) ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white) [![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

NetSpeed is a free, open-source Android app that displays live download and upload speeds near the status-bar clock.

**[Download the latest APK](https://github.com/N2K02/NetSpeed/releases/latest)** · Android 8.0+

## Features

- Shows download and upload speeds for the whole phone.
- Updates every 1, 2, or 3 seconds.
- Lets you adjust the overlay position, text size, and text colour.
- Pauses while the screen is off to help save battery.
- Supports light and dark themes.
- Includes an in-app updater.

## Install

1. [Download the latest APK](https://github.com/N2K02/NetSpeed/releases/latest).
2. Install the APK. If prompted, allow your browser to install unknown apps.
3. Open NetSpeed and grant **Display over other apps** permission.
4. Enable NetSpeed and adjust its position in the app.
5. If your phone stops the overlay in the background, set NetSpeed's battery usage to **Unrestricted** in system settings.

## Update the app

Open **Updates** from the app menu to check for and install a newer release. Android may ask you to allow NetSpeed to install updates the first time. Review the installer prompt to complete the update.

## Build from source

Requirements: JDK 17 and the Android SDK.

Build a release APK locally:

```bash
./gradlew assembleRelease
```

GitHub Actions handles automated release builds. Release signing uses repository-managed secrets; never commit signing passwords or private keys.

## License

MIT. See [LICENSE](LICENSE).
