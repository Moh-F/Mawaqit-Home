package com.moh.mawaqithome;

import android.content.Context;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Downloads a background picture from Bing, Wikimedia Commons or the user's own links. */
final class Backgrounds {
    static final int BING = 1, WIKI = 2, OWN = 4;
    private static final String UA = "MawaqitHome/2 (https://github.com/Moh-F/Mawaqit-Home)";
    private static final long MAX_BYTES = 14L * 1024 * 1024;

    interface Cb {
        void done(boolean ok, String credit, String err);
    }

    static File file(Context c) {
        return new File(c.getFilesDir(), "bg.jpg");
    }

    /** Runs on a background thread; the callback comes back on that thread too. */
    static void fetch(final Context c, final int sources, final String ownLinks, final Cb cb) {
        new Thread(() -> {
            List<Integer> order = new ArrayList<>();
            if ((sources & BING) != 0) order.add(BING);
            if ((sources & WIKI) != 0) order.add(WIKI);
            if ((sources & OWN) != 0) order.add(OWN);
            Collections.shuffle(order);
            String lastErr = "no source selected";
            for (int src : order) {
                try {
                    String[] r = pick(src, ownLinks);          // {url, credit}
                    if (r == null) { lastErr = "source " + src + ": nothing found"; continue; }
                    File tmp = new File(c.getFilesDir(), "bg_new.jpg");
                    download(r[0], tmp);
                    BitmapFactory.Options o = new BitmapFactory.Options();
                    o.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(tmp.getAbsolutePath(), o);
                    if (o.outWidth < 400 || o.outHeight < 300) {
                        tmp.delete();
                        lastErr = "not a usable picture: " + r[0];
                        continue;
                    }
                    File dst = file(c);
                    if (dst.exists()) dst.delete();
                    if (!tmp.renameTo(dst)) throw new Exception("rename failed");
                    cb.done(true, r[1], null);
                    return;
                } catch (Exception e) {
                    lastErr = "source " + src + ": " + e;
                }
            }
            cb.done(false, null, lastErr);
        }, "bg-fetch").start();
    }

    private static String[] pick(int src, String own) throws Exception {
        Random rnd = new Random();
        if (src == BING) {
            String j = get("https://www.bing.com/HPImageArchive.aspx?format=js&idx=" + rnd.nextInt(8)
                    + "&n=1&mkt=en-US");
            JSONObject im = new JSONObject(j).getJSONArray("images").getJSONObject(0);
            String u = im.getString("url");
            if (!u.startsWith("http")) u = "https://www.bing.com" + u;
            return new String[]{u, "Bing — " + im.optString("copyright", "")};
        }
        if (src == WIKI) {
            for (int attempt = 0; attempt < 2; attempt++) {
                int off = attempt == 0 ? rnd.nextInt(300) : 0;
                String j = get("https://commons.wikimedia.org/w/api.php?action=query&generator=search"
                        + "&gsrsearch=" + java.net.URLEncoder.encode(
                        "incategory:\"Featured_pictures_on_Wikimedia_Commons\" landscape", "UTF-8")
                        + "&gsrnamespace=6&gsrlimit=30&gsroffset=" + off
                        + "&prop=imageinfo&iiprop=url%7Csize%7Cextmetadata&iiurlwidth=1920&format=json");
                JSONObject q = new JSONObject(j).optJSONObject("query");
                if (q == null) continue;
                JSONObject pages = q.getJSONObject("pages");
                List<String[]> ok = new ArrayList<>();
                for (Iterator<String> it = pages.keys(); it.hasNext(); ) {
                    JSONObject ii = pages.getJSONObject(it.next()).getJSONArray("imageinfo").getJSONObject(0);
                    if (ii.optInt("width") < 1600 || ii.optInt("width") <= ii.optInt("height")) continue;
                    String u = ii.optString("thumburl", ii.optString("url"));
                    JSONObject em = ii.optJSONObject("extmetadata");
                    String artist = em == null || em.optJSONObject("Artist") == null ? ""
                            : em.getJSONObject("Artist").optString("value", "").replaceAll("<[^>]*>", "").trim();
                    String lic = em == null || em.optJSONObject("LicenseShortName") == null ? ""
                            : em.getJSONObject("LicenseShortName").optString("value", "");
                    ok.add(new String[]{u, "Wikimedia Commons — " + artist + (lic.isEmpty() ? "" : " (" + lic + ")")});
                }
                if (!ok.isEmpty()) return ok.get(rnd.nextInt(ok.size()));
            }
            return null;
        }
        List<String> links = links(own);
        if (links.isEmpty()) return null;
        String u = links.get(rnd.nextInt(links.size()));
        return new String[]{u, "رابط خاص"};
    }

    static List<String> links(String raw) {
        List<String> l = new ArrayList<>();
        if (raw == null) return l;
        for (String t : raw.split("[\\s,;]+")) if (t.startsWith("https://") || t.startsWith("http://")) l.add(t);
        return l;
    }

    private static HttpURLConnection open(String u) throws Exception {
        for (int hop = 0; hop < 5; hop++) {
            HttpURLConnection h = (HttpURLConnection) new URL(u).openConnection();
            h.setConnectTimeout(15000);
            h.setReadTimeout(30000);
            h.setInstanceFollowRedirects(false);
            h.setRequestProperty("User-Agent", UA);
            int code = h.getResponseCode();
            if (code >= 300 && code < 400 && h.getHeaderField("Location") != null) {
                u = new URL(new URL(u), h.getHeaderField("Location")).toString();
                h.disconnect();
                continue;
            }
            if (code != 200) throw new Exception("HTTP " + code + " " + u);
            return h;
        }
        throw new Exception("too many redirects");
    }

    private static String get(String u) throws Exception {
        HttpURLConnection h = open(u);
        try (InputStream in = h.getInputStream()) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            return b.toString("UTF-8");
        } finally {
            h.disconnect();
        }
    }

    private static void download(String u, File to) throws Exception {
        HttpURLConnection h = open(u);
        try (InputStream in = h.getInputStream(); FileOutputStream out = new FileOutputStream(to)) {
            byte[] buf = new byte[16384];
            long tot = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                tot += n;
                if (tot > MAX_BYTES) throw new Exception("picture too large");
                out.write(buf, 0, n);
            }
        } finally {
            h.disconnect();
        }
    }
}
