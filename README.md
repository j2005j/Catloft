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
- **⋮ menu** — app drawer, change wallpaper, launcher settings. **Swipe up** on
  the home screen also opens the drawer — an optional toggle, off by default.
- **Swipe down** on the home screen opens the notification shade — optional,
  off by default, and it needs Catloft's accessibility service switched on in
  Android's own settings (Android will not let an app open the shade itself).
- **Programmable Home / Back keys** — map the Home key (when Catloft is your
  default launcher) or Back-on-home to any app. Off by default: the empty
  mapping is today's behaviour.
- **Double-tap to lock** — turns the screen off from the home screen. Off by
  default; turning it on asks Android to activate Catloft as a device
  administrator (the only policy that grants is turning the screen off).
- **Portrait by default** — auto-rotation is a separate opt-in setting.
- **Wallpaper control** — pick from gallery or reuse the system wallpaper;
  dimness 0–100%, blur 0–100% (pre-rendered once, applied to home and/or
  lock screen). Default: pure black.
- **App drawer** — simple list, locale-sorted, with an **app hider**.
- **Automatic app killer** — kills *background* processes on a schedule
  (15 min – 24 h), all apps or a selected set. System apps only behind an
  explicit red warning plus per-app selection.
- **Color scheme** — Material palette picker with recents and an advanced
  H/S/V + hex dialog. Default `#FFCC01` (CAT yellow).
- **Slide-to-unlock** (off by default) — a ball you drag to get past, as a guard
  against buttdials. It covers Catloft itself, and other apps too once you grant
  "draw over other apps". Not a security feature: keep a system PIN.

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
| `SYSTEM_ALERT_WINDOW` | the optional "lock over other apps" — nothing is ever drawn over another app until you grant it in Android's "draw over other apps" screen |
| *Accessibility service* — not a permission; you switch it on yourself in Settings → Accessibility | swipe down opens the notification shade (Android refuses to let an app do this any other way) |

## Honest limits

- The app killer uses `killBackgroundProcesses()` — background processes only,
  never force-stop. Apps with services or alarms come back later.
- Slide-to-unlock cannot block hardware keys or the notification shade, and its
  system window sits **below** the system keyguard: with a PIN/pattern set,
  Android's own lock screen still appears first, and only after it Catloft's
  ball. Keep the system lock — it is the real one.
- The Home/Back mappings are scoped by where Android hands the keys: Home only
  reaches Catloft when Catloft is your default launcher (you'll see a brief
  flash of home before the mapped app opens), and Back only fires while Catloft
  is on screen — inside another app, Back belongs to that app.

## License

Personal project — no license granted.
