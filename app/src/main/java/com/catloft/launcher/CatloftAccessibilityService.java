package com.catloft.launcher;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

/**
 * Catloft's accessibility service — Nova Launcher's own mechanism for
 * "swipe down to open the notification shade". Programmatic shade expansion is
 * signature-gated ({@code STATUS_BAR_SERVICE} is not available to third-party apps), so on
 * an unrooted API 23 device this is the only route: the user switches it on by hand in
 * Settings → Accessibility, and can switch it off there at any time.
 *
 * <p>The service itself does nothing: it listens for no window content
 * ({@code canRetrieveWindowContent=false}) and exists only so that
 * {@link #GLOBAL_ACTION_NOTIFICATIONS} is permitted. Off until enabled — nothing here
 * runs unless the user has granted it.
 */
public class CatloftAccessibilityService extends AccessibilityService {

    /** Set while the service is connected; lives in this process, which we share. */
    private static CatloftAccessibilityService instance;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onDestroy() {
        if (instance == this) {
            instance = null;
        }
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Intentionally empty: no event is read and no content is ever retrieved.
    }

    @Override
    public void onInterrupt() {
        // Nothing to interrupt — the service never talks.
    }

    /** True when the user has switched Catloft's service on in system settings. */
    static boolean isEnabled(Context c) {
        AccessibilityManager am =
                (AccessibilityManager) c.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) {
            return false;
        }
        ComponentName me = new ComponentName(c.getPackageName(),
                CatloftAccessibilityService.class.getName());
        List<AccessibilityServiceInfo> enabled =
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        if (enabled == null) {
            return false;
        }
        for (AccessibilityServiceInfo info : enabled) {
            if (info == null || info.getId() == null) {
                continue;
            }
            ComponentName that = ComponentName.unflattenFromString(info.getId());
            if (me.equals(that)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Opens the notification shade. False when the service isn't running yet — the caller
     * reports that instead of pretending the swipe did something.
     */
    static boolean openNotifications() {
        CatloftAccessibilityService service = instance;
        if (service == null) {
            return false;
        }
        return service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
    }
}
