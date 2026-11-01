package com.catloft.launcher;

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * The device-admin Catloft needs for double-tap-to-lock: with {@code force-lock} it may call
 * {@link DevicePolicyManager#lockNow()}. The user grants it by hand, in Android's own
 * confirmation dialog — Catloft never enables itself silently, and the only policy it holds
 * is turning the screen off.
 */
public class CatloftDeviceAdmin extends DeviceAdminReceiver {

    /** Request code for {@link DevicePolicyManager#ACTION_ADD_DEVICE_ADMIN}. */
    static final int REQUEST_ENABLE = 41;

    static ComponentName component(Context c) {
        return new ComponentName(c.getPackageName(), CatloftDeviceAdmin.class.getName());
    }

    static boolean isActive(Context c) {
        DevicePolicyManager dpm =
                (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isAdminActive(component(c));
    }

    /**
     * Fires Android's "activate device administrator" screen. The answer comes back through
     * {@code onActivityResult} as RESULT_OK — only then may the caller store its preference.
     */
    static void request(Activity a) {
        Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component(a));
        i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Catloft uses this only to turn the screen off when you double-tap the home "
                        + "screen. It grants no other device policy.");
        try {
            a.startActivityForResult(i, REQUEST_ENABLE);
        } catch (Exception ignored) {
            // No system screen to answer the request — the caller stays off.
        }
    }
}
