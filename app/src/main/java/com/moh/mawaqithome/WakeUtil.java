package com.moh.mawaqithome;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

/** Turns the screen on from an alarm, and makes sure the launcher can come to the front even from the background. */
final class WakeUtil {
    private static PowerManager.WakeLock lock;

    private WakeUtil() { }

    /** Wakes the display (on a Fire TV / Google TV this also wakes the TV over HDMI-CEC when it supports it). */
    @SuppressWarnings("deprecation")
    static synchronized void wakeScreen(Context ctx, long ms) {
        try {
            PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            if (pm == null) return;
            boolean was = pm.isInteractive();
            if (lock != null && lock.isHeld()) lock.release();
            lock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.ON_AFTER_RELEASE, "mawaqithome:adhan");
            lock.setReferenceCounted(false);
            lock.acquire(ms);
            AppLog.info("WAKE", "wake lock taken, screen was " + (was ? "on" : "off"));
        } catch (Exception e) {
            AppLog.error("WAKE", "wake lock failed", e);
        }
    }

    /** Pauses whatever plays (YouTube, music…) with a media key; remembers it so it can be resumed afterwards. */
    static void pauseMedia(Context ctx) {
        try {
            android.media.AudioManager am = (android.media.AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (am == null || !am.isMusicActive()) {
                AppLog.info("MEDIA", "nothing is playing, no pause needed");
                return;
            }
            mediaKey(am, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE);
            WakeScheduler.prefs(ctx).edit().putLong("media_paused_at", System.currentTimeMillis()).apply();
            AppLog.info("MEDIA", "playback paused before the adhan");
        } catch (Exception e) {
            AppLog.error("MEDIA", "pause failed", e);
        }
    }

    /** Continues playback that was paused for the adhan (only if we paused it, and not long ago). */
    static void resumeMedia(Context ctx) {
        try {
            android.content.SharedPreferences p = WakeScheduler.prefs(ctx);
            long at = p.getLong("media_paused_at", 0L);
            if (at == 0L) return;
            p.edit().remove("media_paused_at").apply();
            if (System.currentTimeMillis() - at > 25 * 60_000L) return;
            android.media.AudioManager am = (android.media.AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return;
            mediaKey(am, android.view.KeyEvent.KEYCODE_MEDIA_PLAY);
            AppLog.info("MEDIA", "playback resumed after the adhan");
        } catch (Exception e) {
            AppLog.error("MEDIA", "resume failed", e);
        }
    }

    private static void mediaKey(android.media.AudioManager am, int code) {
        long t = android.os.SystemClock.uptimeMillis();
        am.dispatchMediaKeyEvent(new android.view.KeyEvent(t, t, android.view.KeyEvent.ACTION_DOWN, code, 0));
        am.dispatchMediaKeyEvent(new android.view.KeyEvent(t, t, android.view.KeyEvent.ACTION_UP, code, 0));
    }

    /** Where adb-to-self works: wake key + HDMI-CEC "one touch play" (turns the TV on and switches to this input). */
    static void adbWake(Context ctx) {
        if (AdbShell.blocked(ctx)) {
            AppLog.info("WAKE", "adb wake skipped: self-ADB is not available on this device");
            return;
        }
        AdbShell.run(ctx, "settings put global hdmi_control_auto_wakeup_enabled 1; input keyevent 224; "
                + "cmd hdmi_control onetouchplay", null);
    }

    static boolean canOverlay(Context ctx) {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(ctx);
    }

    /**
     * Starts the launcher with the given intent. Android 10+ blocks activity starts from the background unless the
     * app may draw over other apps; without that permission a full-screen notification is posted as a fallback.
     */
    static void bringToFront(Context ctx, Intent i, String title) {
        try {
            ctx.startActivity(i);
        } catch (Exception e) {
            AppLog.error("WAKE", "startActivity failed", e);
        }
        if (Build.VERSION.SDK_INT >= 29 && !canOverlay(ctx)) {
            try {
                NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm == null) return;
                String ch = "adhan";
                if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(ch) == null) {
                    NotificationChannel c = new NotificationChannel(ch, "Adhan", NotificationManager.IMPORTANCE_HIGH);
                    c.setSound(null, null);
                    nm.createNotificationChannel(c);
                }
                PendingIntent pi = PendingIntent.getActivity(ctx, 21, i, PendingIntent.FLAG_UPDATE_CURRENT);
                Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(ctx, ch)
                        : new Notification.Builder(ctx);
                b.setSmallIcon(R.mipmap.ic_launcher).setContentTitle(title).setContentIntent(pi)
                        .setFullScreenIntent(pi, true).setAutoCancel(true).setCategory(Notification.CATEGORY_ALARM);
                if (Build.VERSION.SDK_INT >= 26) b.setTimeoutAfter(180_000L);
                nm.notify(4401, b.build());
                AppLog.info("WAKE", "full-screen notification posted (no overlay permission)");
            } catch (Exception e) {
                AppLog.error("WAKE", "notification fallback failed", e);
            }
        }
    }
}
