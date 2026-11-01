package com.catloft.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** All Catloft preferences in one place. */
public final class Prefs {

    private static final String NAME = "catloft_prefs";

    static final String ACCENT = "accent";
    static final String ICON_SCALE = "icon_scale";
    static final String DIM = "dim";
    static final String BLUR = "blur";
    static final String BLUR_HOME = "blur_home";
    static final String BLUR_LOCK = "blur_lock";

    static final String KILLER_ON = "killer_on";
    static final String KILLER_INTERVAL = "killer_interval";
    static final String KILLER_MODE = "killer_mode";
    static final String KILLER_APPS = "killer_apps";
    static final String KILLER_SYS_ON = "killer_sys_on";
    static final String KILLER_SYS_APPS = "killer_sys_apps";
    static final String KILLER_LAST = "killer_last";

    static final String HIDDEN = "hidden_apps";

    static final String TILE_1 = "tile_1";
    static final String TILE_2 = "tile_2";
    static final String TILE_3 = "tile_3";

    static final String SLIDE = "slide_unlock";
    static final String LOCK_APPLIED = "lock_wallpaper_applied";
    static final String RECENT_COLORS = "recent_colors";

    // Gestures & orientation — every one of these is opt-in.
    static final String SWIPE_UP_DRAWER = "swipe_up_drawer";
    static final String SWIPE_DOWN_SHADE = "swipe_down_shade";
    static final String AUTO_ROTATE = "auto_rotate";
    static final String DOUBLE_TAP_LOCK = "double_tap_lock";

    // Programmable hardware keys: tile spec of the app to launch, "" = leave alone.
    static final String HOME_KEY_APP = "home_key_app";
    static final String BACK_KEY_APP = "back_key_app";

    static final int DEFAULT_ACCENT = 0xFFFFCC01;
    static final long DEFAULT_INTERVAL = 60L * 60L * 1000L; // 1 hour

    private Prefs() {
    }

    static SharedPreferences p(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    // --- simple values ---

    static int accent(Context c) {
        return p(c).getInt(ACCENT, DEFAULT_ACCENT);
    }

    static void setAccent(Context c, int color) {
        p(c).edit().putInt(ACCENT, color).apply();
    }

    static float iconScale(Context c) {
        return p(c).getFloat(ICON_SCALE, 1f);
    }

    static void setIconScale(Context c, float v) {
        p(c).edit().putFloat(ICON_SCALE, v).apply();
    }

    static int dim(Context c) {
        return p(c).getInt(DIM, 0);
    }

    static void setDim(Context c, int v) {
        p(c).edit().putInt(DIM, v).apply();
    }

    static int blur(Context c) {
        return p(c).getInt(BLUR, 0);
    }

    static void setBlur(Context c, int v) {
        p(c).edit().putInt(BLUR, v).apply();
    }

    static boolean blurHome(Context c) {
        return p(c).getBoolean(BLUR_HOME, true);
    }

    static void setBlurHome(Context c, boolean v) {
        p(c).edit().putBoolean(BLUR_HOME, v).apply();
    }

    static boolean blurLock(Context c) {
        return p(c).getBoolean(BLUR_LOCK, false);
    }

    static void setBlurLock(Context c, boolean v) {
        p(c).edit().putBoolean(BLUR_LOCK, v).apply();
    }

    // --- app killer ---

    static boolean killerOn(Context c) {
        return p(c).getBoolean(KILLER_ON, true);
    }

    static void setKillerOn(Context c, boolean v) {
        p(c).edit().putBoolean(KILLER_ON, v).apply();
    }

    static long killerInterval(Context c) {
        return p(c).getLong(KILLER_INTERVAL, DEFAULT_INTERVAL);
    }

    static void setKillerInterval(Context c, long v) {
        p(c).edit().putLong(KILLER_INTERVAL, v).apply();
    }

    /** "all" or "selected" */
    static String killerMode(Context c) {
        return p(c).getString(KILLER_MODE, "all");
    }

    static void setKillerMode(Context c, String v) {
        p(c).edit().putString(KILLER_MODE, v).apply();
    }

    static Set<String> killerApps(Context c) {
        Set<String> def = new HashSet<>();
        def.add("com.flir.halo"); // MyFLIR out of RAM by default
        return strSet(c, KILLER_APPS, def);
    }

    static void setKillerApps(Context c, Set<String> v) {
        p(c).edit().putStringSet(KILLER_APPS, new HashSet<>(v)).apply();
    }

    static boolean killerSysOn(Context c) {
        return p(c).getBoolean(KILLER_SYS_ON, false);
    }

    static void setKillerSysOn(Context c, boolean v) {
        p(c).edit().putBoolean(KILLER_SYS_ON, v).apply();
    }

    static Set<String> killerSysApps(Context c) {
        return strSet(c, KILLER_SYS_APPS, new HashSet<String>());
    }

    static void setKillerSysApps(Context c, Set<String> v) {
        p(c).edit().putStringSet(KILLER_SYS_APPS, new HashSet<>(v)).apply();
    }

    static long killerLast(Context c) {
        return p(c).getLong(KILLER_LAST, 0L);
    }

    static void setKillerLast(Context c, long v) {
        p(c).edit().putLong(KILLER_LAST, v).apply();
    }

    // --- hider / tiles / security ---

    static Set<String> hiddenApps(Context c) {
        return strSet(c, HIDDEN, new HashSet<String>());
    }

    static void setHiddenApps(Context c, Set<String> v) {
        p(c).edit().putStringSet(HIDDEN, new HashSet<>(v)).apply();
    }

    static String tile(Context c, int index) {
        String def;
        if (index == 0) {
            def = "pkg:com.flir.halo";                    // MyFLIR
        } else if (index == 1) {
            def = "action:android.settings.SETTINGS";     // Android Settings
        } else {
            def = "pkg:com.google.android.apps.photos";   // Google Photos
        }
        return p(c).getString(tileKey(index), def);
    }

    static void setTile(Context c, int index, String spec) {
        p(c).edit().putString(tileKey(index), spec).apply();
    }

    private static String tileKey(int index) {
        if (index == 0) {
            return TILE_1;
        } else if (index == 1) {
            return TILE_2;
        }
        return TILE_3;
    }

    static boolean slideUnlock(Context c) {
        return p(c).getBoolean(SLIDE, false);
    }

    static void setSlideUnlock(Context c, boolean v) {
        p(c).edit().putBoolean(SLIDE, v).apply();
    }

    /** Swipe up on the home screen opens the app drawer. Off by default. */
    static boolean swipeUpDrawer(Context c) {
        return p(c).getBoolean(SWIPE_UP_DRAWER, false);
    }

    static void setSwipeUpDrawer(Context c, boolean v) {
        p(c).edit().putBoolean(SWIPE_UP_DRAWER, v).apply();
    }

    /** Swipe down on the home screen opens the notification drawer. Off by default. */
    static boolean swipeDownShade(Context c) {
        return p(c).getBoolean(SWIPE_DOWN_SHADE, false);
    }

    static void setSwipeDownShade(Context c, boolean v) {
        p(c).edit().putBoolean(SWIPE_DOWN_SHADE, v).apply();
    }

    /** Screen follows the sensor. Off by default: the launcher is locked to portrait. */
    static boolean autoRotate(Context c) {
        return p(c).getBoolean(AUTO_ROTATE, false);
    }

    static void setAutoRotate(Context c, boolean v) {
        p(c).edit().putBoolean(AUTO_ROTATE, v).apply();
    }

    /** Double-tap on the home screen turns the screen off. Off by default. */
    static boolean doubleTapLock(Context c) {
        return p(c).getBoolean(DOUBLE_TAP_LOCK, false);
    }

    static void setDoubleTapLock(Context c, boolean v) {
        p(c).edit().putBoolean(DOUBLE_TAP_LOCK, v).apply();
    }

    /** App launched when the Home key reaches Catloft. "" = do nothing (the default). */
    static String homeKeyApp(Context c) {
        return p(c).getString(HOME_KEY_APP, "");
    }

    static void setHomeKeyApp(Context c, String spec) {
        p(c).edit().putString(HOME_KEY_APP, spec == null ? "" : spec).apply();
    }

    /** App launched when Back is pressed on the home screen. "" = do nothing (default). */
    static String backKeyApp(Context c) {
        return p(c).getString(BACK_KEY_APP, "");
    }

    static void setBackKeyApp(Context c, String spec) {
        p(c).edit().putString(BACK_KEY_APP, spec == null ? "" : spec).apply();
    }

    static boolean lockApplied(Context c) {
        return p(c).getBoolean(LOCK_APPLIED, false);
    }

    static void setLockApplied(Context c, boolean v) {
        p(c).edit().putBoolean(LOCK_APPLIED, v).apply();
    }

    /** CSV of recent accent colors, newest first. */
    static String recentColors(Context c) {
        return p(c).getString(RECENT_COLORS, "");
    }

    static void setRecentColors(Context c, String csv) {
        p(c).edit().putString(RECENT_COLORS, csv).apply();
    }

    // --- helpers ---

    private static Set<String> strSet(Context c, String key, Set<String> def) {
        Set<String> s = p(c).getStringSet(key, null);
        if (s == null) {
            return def;
        }
        return new HashSet<>(s);
    }
}
