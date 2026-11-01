<p align="center">
  <img src="art/icon.svg" width="128" height="128" alt="Catloft icon">
</p>

<h1 align="center">Catloft</h1>

<p align="center">
  A minimalist home-screen launcher for the CAT S60 (Android 6.0.1).
</p>

---

**Catloft** strips the launcher down to what you actually look at: the date, the
time, the battery, and three big shortcuts. Black by default, one accent color,
no clutter. Built in pure Java with **zero dependencies** — just the Android
framework.

## Features

- **Big clock** — date, time (12/24h follows system) and battery percentage;
  turns red at ≤ 15%.
- **Hidden status bar** — non-immersive fullscreen, so a swipe down from the
  top still opens the notification drawer.
- **3 customizable tiles** — defaults: MyFLIR, Android Settings, Google
  Photos. Tap to launch, long-press to reassign any app.
- **⋮ menu** — app drawer, change wallpaper, launcher settings. Swipe up on
  the home screen for the drawer too.
- **Wallpaper control** — pick from gallery or reuse the system wallpaper;
  dimness 0–100%, blur 0–100% (pre-rendered once, applied to home and/or
  lock screen). Default: pure black.
- **App drawer** — simple list, locale-sorted, with an **app hider**.
- **Automatic app killer** — kills *background* processes on a schedule
  (15 min – 24 h), all apps or a selected set. System apps only behind an
  explicit red warning plus per-app selection.
- **Color scheme** — Material palette picker with recents and an advanced
  H/S/V + hex dialog. Default `#FFCC01` (CAT yellow).
- **Slide-to-unlock** (off by default) — launcher-level guard against
  buttdials on the home screen. Not a security feature: keep a system PIN.

## Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then press Home and pick Catloft.

## Build

```bash
./gradlew assembleDebug
```

Requires JDK 17 and an Android SDK (compileSdk 34; `local.properties` pins
`sdk.dir`). `minSdk`/`targetSdk` are 23 — the device runs Marshmallow.

## Permissions

| Permission | Why |
| --- | --- |
| `KILL_BACKGROUND_PROCESSES` | the automatic app killer |
| `SET_WALLPAPER` | "apply blur to lock screen" (Android 6 has no separate lock wallpaper) |
| `RECEIVE_BOOT_COMPLETED` | re-arm the killer schedule after a reboot |

## Honest limits

- The app killer uses `killBackgroundProcesses()` — background processes only,
  never force-stop. Apps with services or alarms come back later.
- Cache cleanup for other apps is impossible without root/device-owner, so
  that feature deliberately does not exist.
- Slide-to-unlock lives in the launcher only; it cannot block hardware keys or
  the system keyguard.

## License

Personal project — no license granted.
