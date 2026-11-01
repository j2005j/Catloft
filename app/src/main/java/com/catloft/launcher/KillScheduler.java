package com.catloft.launcher;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/** Schedules the periodic background-process cleanup. */
public final class KillScheduler {

    private static final int REQ = 1001;

    private KillScheduler() {
    }

    private static PendingIntent pi(Context c) {
        Intent i = new Intent(c, KillReceiver.class);
        return PendingIntent.getBroadcast(
                c, REQ, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        if (!Prefs.killerOn(c)) {
            am.cancel(pi(c));
            return;
        }
        long interval = Prefs.killerInterval(c);
        // One-shot + reschedule-from-receiver: survives Doze via setAndAllowWhileIdle.
        am.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, pi(c));
    }

    static void cancel(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(pi(c));
        }
    }
}
