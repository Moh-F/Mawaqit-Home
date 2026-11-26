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
