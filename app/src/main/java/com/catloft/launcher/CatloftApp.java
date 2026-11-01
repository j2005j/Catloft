package com.catloft.launcher;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

/**
 * Installs the crash recorder before any activity is created, so a failure anywhere in the
 * launcher leaves a readable trace behind.
 *
 * <p>It also owns the two pieces of process-wide state the system-wide lock needs:
 * a started-activity counter (so we know whether Catloft itself is in the foreground) and
 * the screen on/off receiver that drives that lock — a context-registered receiver is the
 * only way {@code ACTION_SCREEN_ON}/{@code ACTION_SCREEN_OFF} can be delivered at all, and
 * registering it here keeps the lock alive even when no Catloft activity exists.
 */
public class CatloftApp extends Application {

    /** How many Catloft activities are started; >0 means Catloft is the foreground app. */
    private static int startedActivities;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                LockOverlayWindow.setUnlockedThisWake(false);
                LockOverlayWindow.hide();
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                LockOverlayWindow.setUnlockedThisWake(false);
                // Only over other apps: when Catloft is in front, HomeActivity's own
                // overlay is the lock (and takes this one back the moment it resumes).
                if (Prefs.slideUnlock(context) && !isForeground()) {
                    LockOverlayWindow.show(context);
                }
            }
        }
    };

    /** True while any Catloft activity is started — i.e. Catloft owns the screen. */
    static boolean isForeground() {
        return startedActivities > 0;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CrashLog.install(this);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(Activity activity) {
                if (isCatloft(activity)) {
                    startedActivities++;
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                if (isCatloft(activity)) {
                    startedActivities = Math.max(0, startedActivities - 1);
                }
            }

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, filter);
    }

    private boolean isCatloft(Activity activity) {
        if (activity == null) {
            return false;
        }
        try {
            return getPackageName().equals(activity.getPackageName());
        } catch (Exception e) {
            return false;
        }
    }
}
