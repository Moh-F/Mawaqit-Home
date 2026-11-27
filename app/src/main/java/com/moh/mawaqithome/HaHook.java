package com.moh.mawaqithome;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;

/**
 * Home Assistant link: posts small JSON events to a webhook (prayer soon, adhan start/end, iqama, Quran…), so a Home
 * Assistant automation can turn the TV on, switch its HDMI input, mute other speakers, etc.
 */
public final class HaHook {
    private HaHook() {}

    static final String[] KEYS = {"fajr", "dhuhr", "asr", "maghrib", "isha"};
    static final String[] NAMES = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};

    static boolean on(SharedPreferences p) {
        return p.getBoolean("ha_on", false) && p.getString("ha_url", "").startsWith("http");
    }

    static String deviceName(SharedPreferences p) {
        String n = p.getString("sync_name", "");
        return n.isEmpty() ? (Build.MANUFACTURER + " " + Build.MODEL).trim() : n;
    }

    /** Sends one event in the background. extra may be null. */
    static void send(final Context ctx, final String event, final int prayer, final JSONObject extra) {
        final SharedPreferences p = WakeScheduler.prefs(ctx);
        if (!on(p) || !p.getBoolean("ha_ev_" + event, true)) return;
        final String url = p.getString("ha_url", "");
        new Thread(() -> {
            try {
                JSONObject o = extra != null ? extra : new JSONObject();
                o.put("event", event);
                o.put("device", deviceName(p));
                o.put("ts", System.currentTimeMillis() / 1000L);
                if (prayer >= 0 && prayer < 5) {
                    o.put("prayer", KEYS[prayer]);
                    o.put("prayer_ar", NAMES[prayer]);
                    o.put("friday", prayer == 1 && Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY);
                }
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(6000);
                c.setReadTimeout(6000);
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] b = o.toString().getBytes("UTF-8");
                try (OutputStream os = c.getOutputStream()) {
                    os.write(b);
                }
                int code = c.getResponseCode();
                c.disconnect();
                AppLog.info("HA", event + " -> HTTP " + code);
                p.edit().putString("ha_last", event + " • HTTP " + code + " • "
                        + String.format(Locale.US, "%tR", Calendar.getInstance())).apply();
            } catch (Exception e) {
                AppLog.error("HA", "send " + event + " failed", e);
                p.edit().putString("ha_last", event + " • فشل: " + e.getClass().getSimpleName()).apply();
            }
        }).start();
    }

    /** Arms the "prayer_soon" alarm a little before the next prayer. */
    static void schedule(Context ctx) {
        try {
            SharedPreferences p = WakeScheduler.prefs(ctx);
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(ctx, Receiver.class);
            if (!on(p) || !p.getBoolean("ha_ev_prayer_soon", true)) {
                am.cancel(PendingIntent.getBroadcast(ctx, 10, i, PendingIntent.FLAG_UPDATE_CURRENT));
                return;
            }
            int[] t = WakeScheduler.load(p);
            if (t == null) return;
            long lead = Math.max(10, Math.min(900, p.getInt("ha_lead", 60))) * 1000L;
            long now = System.currentTimeMillis(), best = Long.MAX_VALUE;
            int bi = -1;
            for (int d = 0; d <= 1; d++) {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                c.add(Calendar.DAY_OF_YEAR, d);
                for (int k = 0; k < 5; k++) {
                    long at = c.getTimeInMillis() + (long) t[k] * 60_000L;
                    if (at - lead > now + 1000L && at < best) {
                        best = at;
                        bi = k;
                    }
                }
            }
            if (bi < 0) return;
            i.putExtra("idx", bi);
            i.putExtra("at", best);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, 10, i, PendingIntent.FLAG_UPDATE_CURRENT);
            if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, best - lead, pi);
            else am.setExact(AlarmManager.RTC_WAKEUP, best - lead, pi);
        } catch (Exception e) {
            AppLog.error("HA", "schedule failed", e);
        }
    }

    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            AppLog.init(ctx);
            int idx = intent.getIntExtra("idx", -1);
            long at = intent.getLongExtra("at", 0L);
            try {
                JSONObject x = new JSONObject();
                x.put("in_seconds", Math.max(0, (at - System.currentTimeMillis()) / 1000L));
                x.put("time", String.format(Locale.US, "%tR", at));
                send(ctx, "prayer_soon", idx, x);
            } catch (Exception ignored) {
            }
            schedule(ctx);
        }
    }
}
