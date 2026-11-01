package com.catloft.launcher;

import android.content.Context;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;

/**
 * The same {@link LockOverlayView}, hosted in a system window so slide-to-unlock also covers
 * <em>other</em> apps — only when the user has granted SYSTEM_ALERT_WINDOW, and only while
 * Catloft is not itself in the foreground (then the in-activity overlay owns the lock).
 *
 * <p>Two honest limits, both by Android's rules, not by choice: {@code TYPE_SYSTEM_ALERT}
 * sits <em>below</em> the system keyguard, so a stock swipe/PIN keyguard still appears
 * first whenever one is set; and the whole thing needs the "draw over other apps" grant,
 * which Android only gives through its own settings screen (see {@link #settingsIntent}).
 * If {@code addView} fails for any reason we catch it and fall back to exactly today's
 * behaviour: the in-activity overlay only.
 */
final class LockOverlayWindow {

    private static WindowManager windowManager;
    private static LockOverlayView view;
    private static boolean attached;

    /**
     * One "unlocked this wake" answer for the whole app, shared between this window and
     * {@link HomeActivity}'s in-activity overlay — otherwise unlocking one of them would
     * leave the other on screen.
     */
    private static boolean unlockedThisWake;

    private LockOverlayWindow() {
    }

    static boolean isUnlockedThisWake() {
        return unlockedThisWake;
    }

    static void setUnlockedThisWake(boolean v) {
        unlockedThisWake = v;
    }

    /** True while the system window is on screen. */
    static boolean isShowing() {
        return attached;
    }

    /** Android's own grant screen for "draw over other apps" (API 23). */
    static Uri overlaySettingsUri(String packageName) {
        return Uri.parse("package:" + packageName);
    }

    static boolean canDraw(Context c) {
        return Settings.canDrawOverlays(c);
    }

    /**
     * Puts the lock over whatever else is on screen. Idempotent, permission-guarded and
     * exception-guarded: on any failure we stay in today's behaviour rather than crash the
     * launcher over a lock screen.
     */
    static void show(Context c) {
        if (attached) {
            return;
        }
        if (!canDraw(c)) {
            return;
        }
        try {
            if (view == null) {
                view = new LockOverlayView(c.getApplicationContext());
                view.setListener(new LockOverlayView.Listener() {
                    @Override
                    public void onUnlocked() {
                        unlockedThisWake = true;
                        hide();
                    }
                });
            }
            view.setAccent(Prefs.accent(c));
            view.relock();
            windowManager = (WindowManager) c.getApplicationContext()
                    .getSystemService(Context.WINDOW_SERVICE);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | Gravity.START;
            windowManager.addView(view, lp);
            attached = true;
        } catch (Exception e) {
            // No overlay window: today's in-activity lock still works.
            attached = false;
        }
    }

    /** Takes the lock off screen (screen off, unlocked, or Catloft came to the front). */
    static void hide() {
        if (!attached) {
            return;
        }
        try {
            windowManager.removeViewImmediate(view);
        } catch (Exception ignored) {
            // Already gone — nothing to undo.
        }
        attached = false;
    }

    /**
     * Called when Catloft itself comes to the foreground: the in-activity overlay takes the
     * lock back from the system window, so the user only ever slides one ball.
     */
    static void release() {
        hide();
    }
}
