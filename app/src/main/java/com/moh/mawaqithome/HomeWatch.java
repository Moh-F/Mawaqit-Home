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

    private static final Set<String> launchers = new HashSet<>();
    /** Activity classes that ARE the home screen (settings, app lists etc. of the same launcher are left alone). */
    private static final Set<String> homeClasses = new HashSet<>();
    private static final String[] KNOWN_HOME = {"com.amazon.tv.launcher.ui.HomeActivity_vNext",
            "com.amazon.tv.launcher.ui.HomeActivity", "com.google.android.apps.tv.launcherx.home.HomeActivity",
            "com.google.android.tvlauncher.MainActivity", "com.google.android.leanbacklauncher.MainActivity"};

    /** Last app (not us, not the system UI) whose window came to the front, and whether the service is running. */
    static volatile String lastForeign;
    static volatile boolean connected;

    static boolean isLauncher(String pkg) {
        if (pkg == null) return true;
        for (String k : KNOWN) if (k.equals(pkg)) return true;
        return launchers.contains(pkg);
    }

    /**
     * Whether going "back to the previous app" after the adhan / Quran makes sense: not when that app was the system
     * home screen (it would stay on top). Without the service we cannot tell, so the old behaviour stays.
     */
    static boolean previousIsRealApp() {
        if (!connected) return true;
        return lastForeign != null && !isLauncher(lastForeign);
    }
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
        for (String k : KNOWN_HOME) homeClasses.add(k);
        try {
            Intent h = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            List<ResolveInfo> l = getPackageManager().queryIntentActivities(h, 0);
            for (ResolveInfo r : l) {
                String p = r.activityInfo != null ? r.activityInfo.packageName : null;
                if (p == null || p.equals(getPackageName()) || p.equals("com.android.settings")
                        || p.contains("setupwizard") || p.contains("provision") || p.contains("settings")) continue;
                launchers.add(p);
                homeClasses.add(r.activityInfo.name);
            }
        } catch (Exception ignored) {
        }
        connected = true;
        AppLog.info("HOMEWATCH", "connected, watching " + launchers);
        if (SystemClock.elapsedRealtime() < 10 * 60_000L) bringBack("boot");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null || e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkg = e.getPackageName();
        if (pkg == null) return;
        String ps = pkg.toString();
        if (!ps.equals(getPackageName()) && !ps.equals("com.android.systemui") && !ps.contains("inputmethod")
                && !ps.contains(".hud")) lastForeign = ps;
        if (!launchers.contains(ps)) return;
        CharSequence cls = e.getClassName();
        if (cls == null || !homeClasses.contains(cls.toString())) return;   // e.g. the launcher's settings: leave it
        bringBack(ps);
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

    private long homeDownAt;
    private boolean homeLong;

    /**
     * The Home key itself is caught here, before the system acts on it: a short press opens Mawaqit Home at once (no
     * detour through the system home), a long press (≥ 0.7 s) opens the original home screen for 3 minutes.
     */
    @Override
    protected boolean onKeyEvent(android.view.KeyEvent e) {
        if (e.getKeyCode() != android.view.KeyEvent.KEYCODE_HOME) return false;
        SharedPreferences p = WakeScheduler.prefs(this);
        if (!p.getBoolean("home_watch", true) || !p.getBoolean("home_key", true)) return false;
        if (System.currentTimeMillis() < p.getLong("home_suspend_until", 0L)) return false;
        if (e.getAction() == android.view.KeyEvent.ACTION_DOWN) {
            if (e.getRepeatCount() == 0) {
                homeDownAt = e.getEventTime();
                homeLong = false;
            } else if (!homeLong && e.getEventTime() - homeDownAt > 700) {
                homeLong = true;
                suspend(this, 3 * 60_000L);
                AppLog.info("HOMEWATCH", "long Home press: original home for 3 minutes");
                performGlobalAction(GLOBAL_ACTION_HOME);
            }
            return true;
        }
        if (e.getAction() == android.view.KeyEvent.ACTION_UP) {
            if (!homeLong) {
                lastLaunch = 0L;
                bringBack("home key");
            }
            homeLong = false;
            return true;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        connected = false;
        super.onDestroy();
    }

    @Override
    public void onInterrupt() {
    }
}
