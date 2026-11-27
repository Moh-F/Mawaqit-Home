package com.moh.mawaqithome;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Quran reading schedule. Items live in prefs "quran_items" as a JSON array:
 * {id, s: surah, a: from ayah (0 = start), b: to ayah (0 = end), r: reciter key ("" = default),
 *  t: "fix" | "pre" | "post", h: minutes after midnight (fix) or offset minutes (pre/post),
 *  p: prayer index 0..4 (pre/post), d: day mask (bit0 = Sunday ... bit6 = Saturday), on: boolean}
 */
public final class QuranSched {
    private QuranSched() {}

    public static JSONArray items(SharedPreferences p) {
        try {
            return new JSONArray(p.getString("quran_items", "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    /** Built-in adhkar sessions (morning / evening / sleep), as schedule items with "z" = the adhkar set key. */
    public static JSONArray azkarItems(SharedPreferences p) {
        JSONArray ar = new JSONArray();
        try {
            if (p.getBoolean("azk_m_on", true))
                ar.put(new JSONObject().put("id", 9000001L).put("z", "ms").put("zt", "أذكار الصباح").put("t", "post")
                        .put("p", 0).put("h", p.getInt("azk_m_off", 30)).put("d", 127).put("on", true));
            if (p.getBoolean("azk_e_on", true))
                ar.put(new JSONObject().put("id", 9000002L).put("z", "ms").put("zt", "أذكار المساء").put("t", "post")
                        .put("p", 2).put("h", p.getInt("azk_e_off", 30)).put("d", 127).put("on", true));
            if (p.getBoolean("azk_s_on", false))
                ar.put(new JSONObject().put("id", 9000003L).put("z", "sleep").put("zt", "أذكار النوم").put("t", "post")
                        .put("p", 4).put("h", p.getInt("azk_s_off", 120)).put("d", 127).put("on", true));
        } catch (Exception ignored) {
        }
        return ar;
    }

    public static JSONObject find(SharedPreferences p, long id) {
        JSONArray az = azkarItems(p);
        for (int i = 0; i < az.length(); i++) {
            JSONObject o = az.optJSONObject(i);
            if (o != null && o.optLong("id") == id) return o;
        }
        JSONArray ar = items(p);
        for (int i = 0; i < ar.length(); i++) {
            JSONObject o = ar.optJSONObject(i);
            if (o != null && o.optLong("id") == id) return o;
        }
        return null;
    }

    /** Minutes after midnight at which the item fires on a normal day, or -1 if unknown. */
    public static int minutesOf(JSONObject o, int[] prayers) {
        String t = o.optString("t", "fix");
        if ("fix".equals(t)) return o.optInt("h", 0);
        if (prayers == null) return -1;
        int p = Math.max(0, Math.min(4, o.optInt("p", 0)));
        int off = o.optInt("h", 0);
        return prayers[p] + ("pre".equals(t) ? -off : off);
    }

    /** Arms one exact alarm for the nearest enabled item (looks up to 8 days ahead). */
    public static void schedule(Context ctx) {
        try {
            SharedPreferences p = WakeScheduler.prefs(ctx);
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(ctx, QuranReceiver.class);
            JSONArray ar = p.getBoolean("quran_on", true) ? items(p) : new JSONArray();
            JSONArray az = azkarItems(p);
            for (int k = 0; k < az.length(); k++) ar.put(az.opt(k));
            int[] pt = WakeScheduler.load(p);
            long now = System.currentTimeMillis();
            long best = Long.MAX_VALUE, bid = -1L;
            for (int d = 0; d <= 8; d++) {
                Calendar day = Calendar.getInstance();
                day.set(Calendar.HOUR_OF_DAY, 0);
                day.set(Calendar.MINUTE, 0);
                day.set(Calendar.SECOND, 0);
                day.set(Calendar.MILLISECOND, 0);
                day.add(Calendar.DAY_OF_YEAR, d);
                int dow = day.get(Calendar.DAY_OF_WEEK) - 1;          // 0 = Sunday
                for (int k = 0; k < ar.length(); k++) {
                    JSONObject o = ar.optJSONObject(k);
                    if (o == null || !o.optBoolean("on", true)) continue;
                    if ((o.optInt("d", 127) & (1 << dow)) == 0) continue;
                    int m = minutesOf(o, pt);
                    if (m < 0) continue;
                    Calendar x = (Calendar) day.clone();
                    x.add(Calendar.MINUTE, m);
                    long at = x.getTimeInMillis();
                    if (at > now + 1000L && at < best) {
                        best = at;
                        bid = o.optLong("id");
                    }
                }
            }
            if (bid < 0) {
                am.cancel(PendingIntent.getBroadcast(ctx, 9, i, PendingIntent.FLAG_UPDATE_CURRENT));
                p.edit().remove("next_quran").apply();
                return;
            }
            i.putExtra("id", bid);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, 9, i, PendingIntent.FLAG_UPDATE_CURRENT);
            if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, best, pi);
            else am.setExact(AlarmManager.RTC_WAKEUP, best, pi);
            p.edit().putLong("next_quran", best).putLong("next_quran_id", bid).apply();
        } catch (Exception e) {
            AppLog.error("QURAN", "schedule failed", e);
        }
    }
}
