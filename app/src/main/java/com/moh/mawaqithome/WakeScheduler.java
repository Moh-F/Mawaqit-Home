package com.moh.mawaqithome;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

/**
 * Prayer-time helpers shared by the activity and the receivers.
 * Times are stored in prefs ("prayers") as 5 comma-separated "minutes after midnight":
 * fajr, dhuhr, asr, maghrib, isha.
 */
public final class WakeScheduler {

    /** Wake the screen this many minutes BEFORE each adhan. */
    public static final int LEAD_MIN = 5;
    /** Keep the screen from dimming/blacking until this many minutes AFTER the adhan. */
    public static final int AFTER_MIN = 20;

    private WakeScheduler() {}

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("cfg", Context.MODE_PRIVATE);
    }

    public static int[] load(SharedPreferences p) {
        String s = p.getString("prayers", "");
        if (s.isEmpty()) return null;
        String[] parts = s.split(",");
        if (parts.length != 5) return null;
        int[] t = new int[5];
        try {
            for (int i = 0; i < 5; i++) t[i] = Integer.parseInt(parts[i]);
        } catch (NumberFormatException e) {
            return null;
        }
        return t;
    }

    public static void save(SharedPreferences p, int[] t, String source) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(t[i]);
        }
        p.edit().putString("prayers", sb.toString()).putString("prayers_src", source).apply();
    }

    /** True from LEAD_MIN before an adhan until AFTER_MIN after it. */
    public static boolean inWindow(int[] t) {
        if (t == null) return false;
        Calendar c = Calendar.getInstance();
        int now = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        for (int m : t) {
            if (now >= m - LEAD_MIN && now <= m + AFTER_MIN) return true;
        }
        return false;
    }

    /**
     * Schedules one exact alarm for the next "adhan minus LEAD_MIN" (today or tomorrow;
     * tomorrow uses today's times, which is accurate to a couple of minutes).
     * Returns the alarm time in ms, or 0 if prayer times are not known.
     */
    public static long scheduleNext(Context ctx) {
        SharedPreferences p = prefs(ctx);
        int[] t = load(p);
        if (t == null) return 0L;

        long now = System.currentTimeMillis();
        long best = Long.MAX_VALUE;
        for (int d = 0; d <= 1; d++) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            c.add(Calendar.DAY_OF_YEAR, d);
            for (int m : t) {
                long at = c.getTimeInMillis() + (long) (m - LEAD_MIN) * 60_000L;
                if (at > now + 3000L && at < best) best = at;
            }
        }
        if (best == Long.MAX_VALUE) return 0L;

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(ctx, WakeReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 7, i, PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, best, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, best, pi);
            }
        } catch (Exception e) {
            return 0L;
        }
        p.edit().putLong("next_wake", best).apply();
        return best;
    }
}
