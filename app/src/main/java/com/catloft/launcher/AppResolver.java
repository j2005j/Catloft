package com.catloft.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Loading installed apps and resolving tile specs ("pkg:", "action:", "component:"). */
final class AppResolver {

    static final class AppEntry {
        final String pkg;
        final String label;
        final Drawable icon;
        final ComponentName component;
        final boolean system;

        AppEntry(String pkg, String label, Drawable icon, ComponentName component, boolean system) {
            this.pkg = pkg;
            this.label = label;
            this.icon = icon;
            this.component = component;
            this.system = system;
        }
    }

    private AppResolver() {
    }

    /** Apps with a launcher entry — the drawer contents (and tile/hider pickers). */
    static List<AppEntry> launchableApps(Context c) {
        PackageManager pm = c.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> ris = pm.queryIntentActivities(main, 0);
        Collator coll = Collator.getInstance(Locale.getDefault());
        List<AppEntry> out = new ArrayList<>();
        for (ResolveInfo ri : ris) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(c.getPackageName())) {
                continue; // never list Catloft inside Catloft
            }
            ComponentName cn = new ComponentName(pkg, ri.activityInfo.name);
            out.add(new AppEntry(
                    pkg,
                    ri.loadLabel(pm).toString(),
                    ri.loadIcon(pm),
                    cn,
                    (ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0));
        }
        Collections.sort(out, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry a, AppEntry b) {
                return coll.compare(a.label, b.label);
            }
        });
        return out;
    }

    /** Every installed app (system included) — for the killer lists. Excludes Catloft itself. */
    static List<AppEntry> allApps(Context c) {
        PackageManager pm = c.getPackageManager();
        List<ApplicationInfo> ais = pm.getInstalledApplications(0);
        Collator coll = Collator.getInstance(Locale.getDefault());
        List<AppEntry> out = new ArrayList<>();
        for (ApplicationInfo ai : ais) {
            if (ai.packageName.equals(c.getPackageName())) {
                continue;
            }
            boolean system = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            out.add(new AppEntry(
                    ai.packageName,
                    ai.loadLabel(pm).toString(),
                    ai.loadIcon(pm),
                    null,
                    system));
        }
        Collections.sort(out, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry a, AppEntry b) {
                return coll.compare(a.label, b.label);
            }
        });
        return out;
    }

    /** System apps only (FLAG_SYSTEM), for the red-warning picker. */
    static List<AppEntry> systemApps(Context c) {
        List<AppEntry> out = new ArrayList<>();
        for (AppEntry e : allApps(c)) {
            if (e.system) {
                out.add(e);
            }
        }
        return out;
    }

    // --- tile spec resolution ---

    /** Returns the component a tile spec points at, or null when it can't be resolved. */
    static ComponentName resolve(Context c, String spec) {
        if (spec == null) {
            return null;
        }
        PackageManager pm = c.getPackageManager();
        if (spec.startsWith("component:")) {
            return ComponentName.unflattenFromString(spec.substring("component:".length()));
        }
        if (spec.startsWith("action:")) {
            Intent i = new Intent(spec.substring("action:".length()));
            ResolveInfo ri = pm.resolveActivity(i, 0);
            return ri == null ? null : new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
        }
        if (spec.startsWith("pkg:")) {
            String pkg = spec.substring("pkg:".length());
            Intent li = pm.getLaunchIntentForPackage(pkg);
            if (li != null && li.getComponent() != null) {
                return li.getComponent();
            }
            // No LAUNCHER entry: fall back to whatever MAIN activity the package exposes.
            Intent main = new Intent(Intent.ACTION_MAIN);
            main.setPackage(pkg);
            List<ResolveInfo> ris = pm.queryIntentActivities(main, 0);
            if (!ris.isEmpty()) {
                return new ComponentName(pkg, ris.get(0).activityInfo.name);
            }
        }
        return null;
    }

    /** Launches a tile spec; false when nothing resolves. */
    static boolean launch(Context c, String spec) {
        ComponentName cn = resolve(c, spec);
        if (cn == null) {
            return false;
        }
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setComponent(cn);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            c.startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Display label for a tile spec; falls back to defaults, then the package tail. */
    static String label(Context c, String spec) {
        ComponentName cn = resolve(c, spec);
        PackageManager pm = c.getPackageManager();
        if (cn != null) {
            try {
                return pm.getActivityInfo(cn, 0).loadLabel(pm).toString();
            } catch (Exception ignored) {
                // fall through
            }
        }
        if ("pkg:com.flir.halo".equals(spec)) {
            return "MyFLIR";
        }
        if ("action:android.settings.SETTINGS".equals(spec)) {
            return "Android Settings";
        }
        if ("pkg:com.google.android.apps.photos".equals(spec)) {
            return "Google Photos";
        }
        if (spec != null && spec.startsWith("pkg:")) {
            String pkg = spec.substring(4);
            int dot = pkg.lastIndexOf('.');
            return dot >= 0 ? pkg.substring(dot + 1) : pkg;
        }
        return "Not installed";
    }

    /** Icon for a tile spec; placeholder when unresolvable. */
    static Drawable icon(Context c, String spec) {
        PackageManager pm = c.getPackageManager();
        ComponentName cn = resolve(c, spec);
        if (cn != null) {
            try {
                return pm.getActivityIcon(cn);
            } catch (Exception ignored) {
                // fall through
            }
        }
        return pm.getDefaultActivityIcon();
    }

    /** Convenience: labels for the three tiles, for settings summaries. */
    static String[] tileLabels(Context c) {
        String[] out = new String[3];
        for (int i = 0; i < 3; i++) {
            out[i] = label(c, Prefs.tile(c, i));
        }
        return out;
    }

    static Set<String> packageNames(List<AppEntry> apps) {
        Set<String> out = new HashSet<>();
        for (AppEntry e : apps) {
            out.add(e.pkg);
        }
        return out;
    }
}
