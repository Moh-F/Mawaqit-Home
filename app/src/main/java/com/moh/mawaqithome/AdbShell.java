package com.moh.mawaqithome;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lets the launcher talk to the device's OWN adb daemon (port 5555) so it can run shell commands
 * a normal app is not allowed to (install updates silently, put the device to sleep).
 * Needs "ADB debugging" on; the first connection shows an "Allow USB debugging?" prompt.
 */
public final class AdbShell {

    public interface Callback {
        void done(boolean ok, String output);
    }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static MiniAdb conn;

    private AdbShell() {}

    private static List<String> hosts() {
        List<String> hosts = new ArrayList<>();
        hosts.add("127.0.0.1");
        try {
            java.util.Enumeration<java.net.NetworkInterface> nis =
                    java.net.NetworkInterface.getNetworkInterfaces();
            while (nis != null && nis.hasMoreElements()) {
                java.util.Enumeration<java.net.InetAddress> as = nis.nextElement().getInetAddresses();
                while (as.hasMoreElements()) {
                    java.net.InetAddress ad = as.nextElement();
                    if (ad instanceof java.net.Inet4Address && !ad.isLoopbackAddress()) {
                        hosts.add(ad.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return hosts;
    }

    private static synchronized MiniAdb connect(Context c) throws Exception {
        if (conn != null) return conn;
        MiniAdb.Keys keys = MiniAdb.loadKeys(c.getFilesDir());
        Throwable last = null;
        for (int round = 1; round <= 2; round++) {
            for (String h : hosts()) {
                try {
                    conn = MiniAdb.connect(h, 5555, keys);
                    AppLog.info("ADB", "connected via " + h + " (round " + round + ")");
                    return conn;
                } catch (Throwable t) {
                    last = t;
                    AppLog.info("ADB", "round " + round + " " + h + " failed: " + t);
                }
            }
            if (round < 2) {
                try { Thread.sleep(2500); } catch (InterruptedException ignored) { }
            }
        }
        throw new Exception(String.valueOf(last), last);
    }

    /** Deletes this app's ADB key so the next connection asks for authorisation again. */
    public static synchronized void resetKey(Context c) {
        reset();
        MiniAdb.deleteKeys(c.getFilesDir());
        new File(c.getFilesDir(), "adbkey").delete();
        new File(c.getFilesDir(), "adbkey.pub").delete();
        AppLog.info("ADB", "key reset");
    }

    private static synchronized void reset() {
        if (conn != null) conn.close();
        conn = null;
    }

    /** Runs a shell command in the background; the callback is called on the main thread. */
    public static void run(Context c, final String cmd, final Callback cb) {
        final Context app = c.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());
        EXEC.execute(new Runnable() {
            @Override public void run() {
                boolean ok;
                boolean connFail = false;
                String out;
                try {
                    Object[] r = connect(app).shell(cmd);
                    ok = ((Integer) r[0]) == 0;
                    out = (String) r[1];
                } catch (Throwable t) {
                    reset();
                    connFail = true;
                    ok = false;
                    out = String.valueOf(t);
                }
                app.getSharedPreferences("cfg", Context.MODE_PRIVATE).edit()
                        .putInt("adb_state", connFail ? 2 : 1)
                        .putLong("adb_ts", System.currentTimeMillis()).apply();
                AppLog.info("ADB", cmd + " -> " + (ok ? "ok" : "FAIL") + " " + out);
                final boolean fok = ok;
                final String fout = out;
                if (cb != null) main.post(new Runnable() {
                    @Override public void run() { cb.done(fok, fout); }
                });
            }
        });
    }
}
