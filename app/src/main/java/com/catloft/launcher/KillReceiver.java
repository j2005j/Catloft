package com.catloft.launcher;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.List;
import java.util.Set;

/**
 * Fires on the killer schedule: kills background processes of the targeted apps,
 * records the run and reschedules itself.
 *
 * Honest limits (documented in settings too): killBackgroundProcesses() only
 * touches background processes — foreground apps are safe, and anything with a
 * service/alarm gets restarted by Android later. No force-stop without root.
 */
public class KillReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        performKill(context);
        Prefs.setKillerLast(context, System.currentTimeMillis());
        KillScheduler.schedule(context); // next run
    }

    /** Returns how many packages were asked to die. */
    static int performKill(Context c) {
        PackageManager pm = c.getPackageManager();
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) {
            return 0;
        }

        boolean allMode = !"selected".equals(Prefs.killerMode(c));
        Set<String> selected = Prefs.killerApps(c);
        Set<String> sysAllowed = Prefs.killerSysApps(c);
        String self = c.getPackageName();

        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        int n = 0;
        for (ApplicationInfo ai : apps) {
            String pkg = ai.packageName;
            if (pkg.equals(self) || pkg.equals("android")) {
                continue;
            }
            boolean system = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            if (system) {
                // System apps only when explicitly listed: the list itself is the opt-in
                // (there is no separate warning tick any more). Empty list = none die.
                if (!sysAllowed.contains(pkg)) {
                    continue;
                }
            } else if (!allMode && !selected.contains(pkg)) {
                continue;
            }
            if ((ai.flags & ApplicationInfo.FLAG_PERSISTENT) != 0) {
                continue;
            }
            try {
                am.killBackgroundProcesses(pkg);
                n++;
            } catch (Exception ignored) {
                // package vanished between listing and killing
            }
        }
        return n;
    }
}
