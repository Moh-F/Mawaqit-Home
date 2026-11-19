package com.moh.mawaqithome;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Time settings shared by the activity and the alarm receivers: 12/24 h display and an optional
 * fixed time zone (DST is applied automatically by the zone rules). Setting the process-wide
 * default zone also makes the wake-up alarm maths use that zone.
 */
public final class TimeCfg {
    private TimeCfg() {}

    public static void apply(Context c) {
        SharedPreferences p = WakeScheduler.prefs(c);
        String id = p.getString("tz", "");
        if (id == null || id.isEmpty()) {
            TimeZone.setDefault(null);                    // follow the device
        } else {
            TimeZone z = TimeZone.getTimeZone(id);        // unknown id -> GMT, guard below
            if (!z.getID().equals("GMT") || id.equals("GMT")) TimeZone.setDefault(z);
        }
    }

    public static boolean valid(String id) {
        return TimeZone.getTimeZone(id).getID().equals(id);
    }

    private static String ampm(String s) {
        return s.replace("AM", "ص").replace("PM", "م");
    }

    public static String time(SharedPreferences p, long ms) {
        boolean h24 = p.getBoolean("h24", true);
        return ampm(new SimpleDateFormat(h24 ? "HH:mm" : "hh:mm a", Locale.US).format(new Date(ms)));
    }

    public static String hm(SharedPreferences p, int minutes) {
        boolean h24 = p.getBoolean("h24", true);
        if (h24) return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60);
        int h = minutes / 60;
        String suffix = h >= 12 ? "م" : "ص";
        h = h % 12 == 0 ? 12 : h % 12;
        return String.format(Locale.US, "%d:%02d %s", h, minutes % 60, suffix);
    }

    public static String date(SharedPreferences p, long ms) {
        return new SimpleDateFormat("dd.MM.yyyy", Locale.US).format(new Date(ms));
    }

    public static String dateTime(SharedPreferences p, long ms) {
        return time(p, ms) + "  " + date(p, ms);      // 14:18  19.11.2026
    }

    public static String zoneId() {
        return TimeZone.getDefault().getID();
    }

    public static boolean dst() {
        return TimeZone.getDefault().inDaylightTime(new Date());
    }
}
