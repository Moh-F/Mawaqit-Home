package com.moh.mawaqithome;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps Mawaqit Home as the screen behind the Home button, also on Fire TV where a third-party Home app cannot be set:
 * whenever the system launcher (Fire TV / Google TV / Android TV home) comes to the front, Mawaqit Home is brought
 * back on top within a moment. Switched on as an accessibility service (by the user, ADB, or WRITE_SECURE_SETTINGS).
 */
public class HomeWatch extends AccessibilityService {

    private static final String[] KNOWN = {"com.amazon.tv.launcher", "com.amazon.firelauncher",
            "com.google.android.apps.tv.launcherx", "com.google.android.tvlauncher",
            "com.google.android.leanbacklauncher", "com.android.tv.launcher", "com.google.android.tungsten.setupwraith"};

    private final Set<String> launchers = new HashSet<>();
    private long lastLaunch;

    static ComponentName component(Context c) {
        return new ComponentName(c.getPackageName(), HomeWatch.class.getName());
    }

    static boolean enabled(Context c) {
        try {
            String s = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return s != null && (s.contains(component(c).flattenToString()) || s.contains(component(c).flattenToShortString()));
        } catch (Exception e) {
            return false;
        }
    }

    static boolean canWriteSecure(Context c) {
        return c.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED;
    }

    /** The secure-settings command line that switches the service on, keeping the services already enabled. */
    static String enableValue(Context c) {
        String me = component(c).flattenToString();
        String s = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (s == null || s.trim().isEmpty()) return me;
        if (s.contains(me)) return s;
        return s + ":" + me;
    }

    /** Switches the service on by itself when the app was granted WRITE_SECURE_SETTINGS (adb shell pm grant …). */
    static boolean enableSelf(Context c) {
        if (enabled(c)) return true;
        if (!canWriteSecure(c)) return false;
        try {
            Settings.Secure.putString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, enableValue(c));
            Settings.Secure.putString(c.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, "1");
            AppLog.info("HOMEWATCH", "enabled through WRITE_SECURE_SETTINGS");
            return true;
        } catch (Exception e) {
            AppLog.error("HOMEWATCH", "self enable failed", e);
            return false;
        }
    }

    /** Pauses the watch, e.g. while the person deliberately opens the original home screen. */
    static void suspend(Context c, long ms) {
        WakeScheduler.prefs(c).edit().putLong("home_suspend_until", System.currentTimeMillis() + ms).apply();
    }

    @Override
    protected void onServiceConnected() {
        AppLog.init(this);
        launchers.clear();
        for (String k : KNOWN) launchers.add(k);
        try {
            Intent h = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            List<ResolveInfo> l = getPackageManager().queryIntentActivities(h, 0);
            for (ResolveInfo r : l) {
                String p = r.activityInfo != null ? r.activityInfo.packageName : null;
                if (p == null || p.equals(getPackageName()) || p.equals("com.android.settings")
                        || p.contains("setupwizard") || p.contains("provision")) continue;
                launchers.add(p);
            }
        } catch (Exception ignored) {
        }
        AppLog.info("HOMEWATCH", "connected, watching " + launchers);
        if (SystemClock.elapsedRealtime() < 10 * 60_000L) bringBack("boot");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null || e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkg = e.getPackageName();
        if (pkg == null || !launchers.contains(pkg.toString())) return;
        bringBack(pkg.toString());
    }

    private void bringBack(String why) {
        SharedPreferences p = WakeScheduler.prefs(this);
        if (!p.getBoolean("home_watch", true)) return;
        long now = System.currentTimeMillis();
        if (now < p.getLong("home_suspend_until", 0L)) return;
        if (now - lastLaunch < 1200L) return;
        lastLaunch = now;
        AppLog.info("HOMEWATCH", "system home shown (" + why + "), bringing Mawaqit Home to the front");
        try {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            i.putExtra("from_home", true);
            startActivity(i);
        } catch (Exception ex) {
            AppLog.error("HOMEWATCH", "start failed", ex);
        }
    }

    @Override
    public void onInterrupt() {
    }
}
