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
        dadb = Dadb.create("127.0.0.1", 5555, pair);
        return dadb;
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
