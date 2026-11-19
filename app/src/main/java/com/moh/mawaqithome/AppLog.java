package com.moh.mawaqithome;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Tiny file logger. The log lives next to the backup file so it can be pulled with adb:
 * /sdcard/Android/data/com.moh.mawaqithome/files/launcher.log
 * Crashes are written to it too.
 */
public final class AppLog {
    private static final long MAX_BYTES = 256 * 1024;
    private static File file;

    private AppLog() {}

    public static synchronized void init(Context c) {
        if (file != null) return;
        File dir = c.getExternalFilesDir(null);
        if (dir == null) dir = c.getFilesDir();
        file = new File(dir, "launcher.log");

        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable ex) {
                error("CRASH", "thread " + t.getName(), ex);
                if (prev != null) prev.uncaughtException(t, ex);
            }
        });
        info("APP", "---- start ----");
    }

    public static synchronized File file() {
        return file;
    }

    public static synchronized void info(String tag, String msg) {
        if (file == null) return;
        try {
            if (file.length() > MAX_BYTES) {
                File old = new File(file.getParentFile(), "launcher.log.1");
                //noinspection ResultOfMethodCallIgnored
                old.delete();
                //noinspection ResultOfMethodCallIgnored
                file.renameTo(old);
            }
            String line = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())
                    + " " + tag + ": " + msg + "\n";
            FileOutputStream out = new FileOutputStream(file, true);
            out.write(line.getBytes("UTF-8"));
            out.close();
        } catch (Exception ignored) {
            // logging must never crash the app
        }
    }

    public static void error(String tag, String msg, Throwable t) {
        StringWriter sw = new StringWriter();
        if (t != null) t.printStackTrace(new PrintWriter(sw));
        info(tag, msg + (t != null ? "\n" + sw : ""));
    }

    public static synchronized String tail(int maxLines) {
        if (file == null || !file.exists()) return "(السجل فارغ)";
        try {
            FileInputStream in = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            int n = 0, r;
            while (n < data.length && (r = in.read(data, n, data.length - n)) > 0) n += r;
            in.close();
            String[] lines = new String(data, 0, n, "UTF-8").split("\n");
            List<String> out = new ArrayList<>();
            for (int i = Math.max(0, lines.length - maxLines); i < lines.length; i++) out.add(lines[i]);
            return android.text.TextUtils.join("\n", out);
        } catch (Exception e) {
            return "(تعذّرت قراءة السجل)";
        }
    }

    public static synchronized void clear() {
        if (file != null) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
        info("APP", "log cleared");
    }
}
