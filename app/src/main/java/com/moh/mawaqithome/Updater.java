package com.moh.mawaqithome;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * In-app updater. The CI publishes every build to the GitHub release "latest" with
 * "versionCode=N" in the notes. We read it, download the APK, and (via the launcher's own
 * ADB connection) run "pm install -r", which updates in place and keeps every setting
 * because all builds are signed with the same key.
 */
public final class Updater {

    public static final String DEFAULT_REPO = "Moh-F/Mawaqit-Home";

    public static final class Info {
        public int code;
        public String name = "";
        public String apkUrl = "";
        public long size;
    }

    public interface CheckCb {
        /** info == null -> error (see err). */
        void done(Info info, String err);
    }

    public interface DownloadCb {
        void progress(int percent);

        void done(File apk, String err);
    }

    /** Device clock minus internet clock error (server time - device time), from HTTP Date. */
    public static volatile long skewMs = 0L, skewAt = 0L;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Updater() {}

    public static int currentCode(Context c) {
        try {
            PackageInfo p = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return p.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String currentName(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    public static File apkFile(Context c) {
        File dir = c.getExternalFilesDir(null);
        if (dir == null) dir = c.getFilesDir();
        return new File(dir, "update.apk");
    }

    public static void check(final Context c, final String repo, final CheckCb cb) {
        new Thread(new Runnable() {
            @Override public void run() {
                Info info = null;
                String err = null;
                try {
                    HttpURLConnection h = (HttpURLConnection) new URL(
                            "https://api.github.com/repos/" + repo + "/releases/tags/latest")
                            .openConnection();
                    h.setConnectTimeout(15000);
                    h.setReadTimeout(20000);
                    h.setRequestProperty("Accept", "application/vnd.github+json");
                    h.setRequestProperty("User-Agent", "MawaqitHome");
                    int code = h.getResponseCode();
                    long sd = h.getDate();
                    if (sd > 0) {
                        skewMs = sd - System.currentTimeMillis();
                        skewAt = System.currentTimeMillis();
                    }
                    if (code == 404) {
                        throw new Exception("لا يوجد إصدار منشور، أو المستودع خاص (404)");
                    }
                    if (code != 200) throw new Exception("HTTP " + code);
                    JSONObject j = new JSONObject(readAll(h.getInputStream()));
                    Matcher m = Pattern.compile("versionCode=(\\d+)")
                            .matcher(j.optString("body", ""));
                    if (!m.find()) throw new Exception("لم أجد رقم الإصدار في الإعلان");
                    Info i = new Info();
                    i.code = Integer.parseInt(m.group(1));
                    i.name = j.optString("name", "");
                    JSONArray assets = j.optJSONArray("assets");
                    if (assets != null) {
                        for (int k = 0; k < assets.length(); k++) {
                            JSONObject a = assets.getJSONObject(k);
                            if (a.optString("name").endsWith(".apk")) {
                                i.apkUrl = a.optString("browser_download_url");
                                i.size = a.optLong("size");
                                break;
                            }
                        }
                    }
                    if (i.apkUrl.isEmpty()) throw new Exception("لا يوجد ملف APK في الإصدار");
                    info = i;
                } catch (Exception e) {
                    err = e.getMessage() != null ? e.getMessage() : e.toString();
                    AppLog.info("UPDATE", "check failed: " + e);
                }
                final Info fi = info;
                final String fe = err;
                MAIN.post(new Runnable() {
                    @Override public void run() { cb.done(fi, fe); }
                });
            }
        }, "upd-check").start();
    }

    public static void download(final Context c, final Info info, final DownloadCb cb) {
        new Thread(new Runnable() {
            @Override public void run() {
                File out = apkFile(c);
                File tmp = new File(out.getParentFile(), "update.apk.part");
                String err = null;
                try {
                    HttpURLConnection h = (HttpURLConnection) new URL(info.apkUrl).openConnection();
                    h.setConnectTimeout(20000);
                    h.setReadTimeout(30000);
                    h.setRequestProperty("User-Agent", "MawaqitHome");
                    if (h.getResponseCode() != 200) throw new Exception("HTTP " + h.getResponseCode());
                    long total = h.getContentLength() > 0 ? h.getContentLength() : info.size;
                    InputStream in = h.getInputStream();
                    FileOutputStream fo = new FileOutputStream(tmp);
                    byte[] buf = new byte[16384];
                    long got = 0;
                    int n, last = -1;
                    while ((n = in.read(buf)) > 0) {
                        fo.write(buf, 0, n);
                        got += n;
                        final int pct = total > 0 ? (int) (got * 100 / total) : 0;
                        if (pct != last) {
                            last = pct;
                            MAIN.post(new Runnable() {
                                @Override public void run() { cb.progress(pct); }
                            });
                        }
                    }
                    fo.close();
                    in.close();
                    if (total > 0 && got != total) throw new Exception("التنزيل غير مكتمل");
                    if (out.exists()) out.delete();
                    if (!tmp.renameTo(out)) throw new Exception("تعذّر حفظ الملف");
                } catch (Exception e) {
                    err = e.getMessage() != null ? e.getMessage() : e.toString();
                    AppLog.info("UPDATE", "download failed: " + e);
                    tmp.delete();
                }
                final String fe = err;
                final File fo2 = out;
                MAIN.post(new Runnable() {
                    @Override public void run() { cb.done(fe == null ? fo2 : null, fe); }
                });
            }
        }, "upd-download").start();
    }

    /**
     * Installs the APK over the running app, then starts it again. The shell command keeps
     * running after the app process is replaced, so the relaunch still happens.
     */
    public static void install(Context c, File apk, long idleMs, AdbShell.Callback cb) {
        String path = apk.getAbsolutePath();
        String cmd = "pm install -r '" + path + "' && rm -f '" + path + "' && am start -n "
                + c.getPackageName() + "/.MainActivity --ez postupdate true"
                + (idleMs > 0 ? " --ez resume_saver true --el idle_ms " + idleMs : "");
        AppLog.info("UPDATE", "installing: " + cmd);
        AdbShell.run(c, cmd, cb);
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        return bos.toString("UTF-8");
    }
}
