package com.moh.mawaqithome;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dadb.AdbKeyPair;
import dadb.AdbShellResponse;
import dadb.Dadb;

/**
 * Lets the launcher talk to the device's OWN adb daemon (127.0.0.1:5555), so it can run
 * a few shell commands that a normal app is not allowed to (open hidden settings screens,
 * put the device to sleep). Needs "ADB debugging" on; the first connection shows an
 * "Allow USB debugging?" prompt on the TV, answer "Always allow".
 */
public final class AdbShell {

    public interface Callback {
        void done(boolean ok, String output);
    }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static Dadb dadb;

    private AdbShell() {}

    private static synchronized Dadb connect(Context c) throws Exception {
        if (dadb != null) return dadb;
        File priv = new File(c.getFilesDir(), "adbkey");
        File pub = new File(c.getFilesDir(), "adbkey.pub");
        if (!priv.exists() || !pub.exists()) AdbKeyPair.generate(priv, pub);
        AdbKeyPair pair = AdbKeyPair.read(priv, pub);
        java.util.List<String> hosts = new java.util.ArrayList<>();
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
        Throwable last = null;
        // EOF usually means the "Allow USB debugging?" prompt was just answered (adbd drops the
        // first connection), so try a few rounds before giving up.
        for (int round = 1; round <= 3; round++) {
            for (String h : hosts) {
                try {
                    Dadb d = Dadb.create(h, 5555, pair);
                    d.shell("echo ok");           // forces the connection / auth now
                    dadb = d;
                    AppLog.info("ADB", "connected via " + h + " (round " + round + ")");
                    return d;
                } catch (Throwable t) {
                    last = t;
                    AppLog.info("ADB", "round " + round + " " + h + " failed: " + t);
                }
            }
            if (round < 3) {
                try { Thread.sleep(2500); } catch (InterruptedException ignored) { }
            }
        }
        throw new Exception(String.valueOf(last), last);
    }

    /** Deletes this app's ADB key so the next connection asks for authorisation again. */
    public static synchronized void resetKey(Context c) {
        reset();
        new File(c.getFilesDir(), "adbkey").delete();
        new File(c.getFilesDir(), "adbkey.pub").delete();
        AppLog.info("ADB", "key reset");
    }

    private static synchronized void reset() {
        try {
            if (dadb != null) dadb.close();
        } catch (Exception ignored) {
        }
        dadb = null;
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
                    AdbShellResponse r = connect(app).shell(cmd);
                    ok = r.getExitCode() == 0;
                    out = r.getAllOutput().trim();
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
