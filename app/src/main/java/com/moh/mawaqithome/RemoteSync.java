package com.moh.mawaqithome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Settings sync and remote control through a private GitHub repository the user owns.
 *   sync/settings.json  {rev, by, name, at, settings{...}}   - written by devices and by the web panel
 *   sync/cmds.json      {cmds:[{id,t,to,arg}]}               - written by the web panel, executed by devices
 * Devices poll once a minute (conditional requests, so quiet polls are cheap), push their own changes,
 * apply newer remote revisions and run new commands.
 */
final class RemoteSync {
    interface Host {
        /** Settings to share (already without device-bound / screen-size keys). */
        JSONObject snapshot() throws Exception;

        /** Apply remote settings (runs on the main thread). */
        void apply(JSONObject settings) throws Exception;

        /** Run a remote command (runs on the main thread). */
        void command(JSONObject cmd);
    }

    private static final long POLL_MS = 60_000L;
    private final Context ctx;
    private final SharedPreferences p;
    private final Host host;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Thread worker;
    private volatile boolean running;
    private String shaSettings, etagSettings, etagCmds;
    volatile String status = "—";

    RemoteSync(Context ctx, SharedPreferences p, Host host) {
        this.ctx = ctx.getApplicationContext();
        this.p = p;
        this.host = host;
    }

    static boolean configured(SharedPreferences p) {
        return p.getBoolean("sync_on", false) && p.getString("sync_repo", "").matches("[\\w.-]+/[\\w.-]+")
                && !p.getString("sync_token", "").isEmpty();
    }

    String deviceId() {
        String id = p.getString("sync_id", "");
        if (id.isEmpty()) {
            id = UUID.randomUUID().toString().substring(0, 8);
            p.edit().putString("sync_id", id).apply();
        }
        return id;
    }

    String deviceName() {
        String n = p.getString("sync_name", "");
        return n.isEmpty() ? "جهاز " + deviceId() : n;
    }

    void start() {
        if (running) return;
        running = true;
        worker = new Thread(() -> {
            while (running) {
                tickSafe();
                try {
                    Thread.sleep(POLL_MS);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "remotesync");
        worker.setDaemon(true);
        worker.start();
    }

    void stop() {
        running = false;
        if (worker != null) worker.interrupt();
    }

    /** Immediate round (from a button). */
    void syncNow() {
        new Thread(this::tickSafe, "remotesync-now").start();
    }

    private synchronized void tickSafe() {
        if (!configured(p)) {
            status = "المزامنة غير مفعّلة";
            return;
        }
        try {
            pullSettings();
            pullCmds();
            pushIfChanged();
            status = "✓ " + new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(new java.util.Date()) + " — تعمل (rev " + p.getInt("sync_rev", 0) + ")";
        } catch (Exception e) {
            status = "✗ " + e.getMessage();
            AppLog.info("SYNC", "round failed: " + e);
        }
    }

    // ------------------------------------------------------------ HTTP

    private static final class Resp {
        int code;
        String body = "", etag = "";
    }

    private Resp api(String method, String path, String json, String etag) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("https://api.github.com/repos/"
                + p.getString("sync_repo", "") + "/contents/" + path).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(12000);
        c.setReadTimeout(20000);
        c.setRequestProperty("User-Agent", "MawaqitHome");
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("Authorization", "Bearer " + p.getString("sync_token", ""));
        if (etag != null && !etag.isEmpty()) c.setRequestProperty("If-None-Match", etag);
        if (json != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.getOutputStream().write(json.getBytes("UTF-8"));
        }
        Resp r = new Resp();
        r.code = c.getResponseCode();
        r.etag = c.getHeaderField("ETag") == null ? "" : c.getHeaderField("ETag");
        InputStream in = r.code >= 400 ? c.getErrorStream() : c.getInputStream();
        if (in != null) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            r.body = b.toString("UTF-8");
            in.close();
        }
        c.disconnect();
        if (r.code == 401 || r.code == 403)
            throw new Exception("المفتاح مرفوض (" + r.code + ") — تحقق من صلاحياته");
        if (r.code == 404 && !path.startsWith("sync/")) throw new Exception("المستودع غير موجود");
        return r;
    }

    private static String decode(String contentApiJson) throws Exception {
        String b64 = new JSONObject(contentApiJson).getString("content").replace("\n", "");
        return new String(Base64.decode(b64, Base64.DEFAULT), "UTF-8");
    }

    // ------------------------------------------------------------ settings

    private void pullSettings() throws Exception {
        Resp r = api("GET", "sync/settings.json", null, etagSettings);
        if (r.code == 304) return;
        if (r.code == 404) {
            shaSettings = null;
            return;
        }
        if (r.code != 200) throw new Exception("HTTP " + r.code);
        etagSettings = r.etag;
        shaSettings = new JSONObject(r.body).getString("sha");
        JSONObject doc = new JSONObject(decode(r.body));
        int rev = doc.optInt("rev", 0);
        String by = doc.optString("by", "");
        boolean mine = by.equals(deviceId());
        boolean firstJoin = p.getInt("sync_rev", 0) == 0;
        if (rev > p.getInt("sync_rev", 0) && (!mine || firstJoin)) {
            final JSONObject s = doc.getJSONObject("settings");
            final int fr = rev;
            final Exception[] err = new Exception[1];
            final Object lock = new Object();
            final boolean[] done = {false};
            main.post(() -> {
                try {
                    host.apply(s);
                    p.edit().putInt("sync_rev", fr).putString("sync_hash", hash(host.snapshot())).apply();
                } catch (Exception e) {
                    err[0] = e;
                }
                synchronized (lock) {
                    done[0] = true;
                    lock.notifyAll();
                }
            });
            synchronized (lock) {
                long end = System.currentTimeMillis() + 8000L;
                while (!done[0] && System.currentTimeMillis() < end) lock.wait(500);
            }
            if (err[0] != null) throw err[0];
            AppLog.info("SYNC", "applied remote revision " + rev + " from " + doc.optString("name", by));
        } else if (rev > p.getInt("sync_rev", 0)) {
            p.edit().putInt("sync_rev", rev).apply();
        }
    }

    static String hash(JSONObject o) throws Exception {
        TreeMap<String, String> m = new TreeMap<>();
        Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String k = it.next();
            m.put(k, String.valueOf(o.get(k)));
        }
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(m.toString().getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private void pushIfChanged() throws Exception {
        if (p.getInt("sync_mode", 1) != 1) return;                     // receive-only device
        JSONObject snap = host.snapshot();
        String h = hash(snap);
        if (h.equals(p.getString("sync_hash", ""))) return;
        if (System.currentTimeMillis() - p.getLong("sync_pushed", 0L) < 20_000L) return;
        int rev = p.getInt("sync_rev", 0) + 1;
        JSONObject doc = new JSONObject().put("rev", rev).put("by", deviceId()).put("name", deviceName())
                .put("at", System.currentTimeMillis()).put("settings", snap);
        JSONObject body = new JSONObject().put("message", "settings rev " + rev + " from " + deviceName())
                .put("content", Base64.encodeToString(doc.toString(1).getBytes("UTF-8"), Base64.NO_WRAP));
        if (shaSettings != null) body.put("sha", shaSettings);
        Resp r = api("PUT", "sync/settings.json", body.toString(), null);
        if (r.code == 200 || r.code == 201) {
            shaSettings = new JSONObject(r.body).getJSONObject("content").getString("sha");
            etagSettings = null;
            p.edit().putInt("sync_rev", rev).putString("sync_hash", h).putLong("sync_pushed", System.currentTimeMillis()).apply();
            AppLog.info("SYNC", "pushed revision " + rev);
        } else if (r.code == 409 || r.code == 422) {
            etagSettings = null;                                          // somebody wrote first: pull next round
            AppLog.info("SYNC", "push conflict, will pull first");
        } else throw new Exception("HTTP " + r.code + " " + r.body);
    }

    // ------------------------------------------------------------ commands

    private void pullCmds() throws Exception {
        Resp r = api("GET", "sync/cmds.json", null, etagCmds);
        if (r.code == 304 || r.code == 404) return;
        if (r.code != 200) throw new Exception("HTTP " + r.code);
        etagCmds = r.etag;
        JSONArray cmds = new JSONObject(decode(r.body)).optJSONArray("cmds");
        if (cmds == null) return;
        long last = p.getLong("sync_cmd", -1L);
        long max = last;
        if (last < 0) {                                                  // first run: never replay old commands
            for (int i = 0; i < cmds.length(); i++) max = Math.max(max, cmds.getJSONObject(i).optLong("id"));
            p.edit().putLong("sync_cmd", max).apply();
            return;
        }
        for (int i = 0; i < cmds.length(); i++) {
            final JSONObject c = cmds.getJSONObject(i);
            long id = c.optLong("id");
            if (id <= last) continue;
            max = Math.max(max, id);
            String to = c.optString("to", "*");
            if (!"*".equals(to) && !to.equals(deviceId()) && !to.equals(deviceName())) continue;
            if (System.currentTimeMillis() - id > 15 * 60_000L) continue;   // too old to still make sense
            AppLog.info("SYNC", "command " + c.optString("t"));
            main.post(() -> host.command(c));
        }
        if (max != last) p.edit().putLong("sync_cmd", max).apply();
    }
}
