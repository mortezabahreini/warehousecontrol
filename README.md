# Transparent Weather Widget

A fully transparent 4x2 Android home-screen clock and weather widget.

- 12-hour clock with AM/PM and dd/MM/yyyy date format.
- Automatic device clock/time zone via Android TextClock.
- Location-based weather from Open-Meteo.
- 30-minute WorkManager periodic refresh; tap for settings and immediate refresh.
- Android 8+; min SDK 26, target and compile SDK 35.
- Location updates in the background require Allow all the time (Android settings).

## Build

GitHub Actions runs on pushes to this branch and uploads the directly installable, debug-key-signed release APK as a workflow artifact.

You may also run `gradle :app:assembleRelease` with Android SDK 35 and Java 17.

## Installation

Download the GitHub Actions artifact, extract the APK, open it on your phone and grant permission to install from this source. Launch Transparent Weather, grant location, enable background location, then long-press your home screen > Widgets > Transparent Weather.
