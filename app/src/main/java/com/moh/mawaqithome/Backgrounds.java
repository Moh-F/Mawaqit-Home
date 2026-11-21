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

    // ------------------------------------------------------------------ video

    static final int V_WIKI = 1, V_PEXELS = 2, V_OWN = 4, V_PIXABAY = 8;

    interface VCb {
        void done(boolean ok, String url, String credit, String err);
    }

    /** Picks a short free video; nothing is downloaded, the page streams the returned https URL. */
    static void fetchVideo(final int sources, final String ownLinks, final String pexelsKey, final String pixabayKey,
                           final String topic, final VCb cb) {
        new Thread(() -> {
            List<Integer> order = new ArrayList<>();
            if ((sources & V_WIKI) != 0) order.add(V_WIKI);
            if ((sources & V_PEXELS) != 0 && pexelsKey != null && pexelsKey.trim().length() > 8) order.add(V_PEXELS);
            if ((sources & V_PIXABAY) != 0 && pixabayKey != null && pixabayKey.trim().length() > 8) order.add(V_PIXABAY);
            if ((sources & V_OWN) != 0) order.add(V_OWN);
            Collections.shuffle(order);
            String lastErr = "no video source selected (or key missing)";
            for (int src : order) {
                try {
                    String[] r = pickVideo(src, ownLinks, src == V_PIXABAY ? pixabayKey : pexelsKey, topic);
                    if (r == null) { lastErr = "video source " + src + ": nothing found"; continue; }
                    cb.done(true, r[0], r[1], null);
                    return;
                } catch (Exception e) {
                    lastErr = "video source " + src + ": " + e;
                }
            }
            cb.done(false, null, null, lastErr);
        }, "bg-video").start();
    }

    private static String[] pickVideo(int src, String own, String key, String topic) throws Exception {
        Random rnd = new Random();
        if (src == V_OWN) {
            List<String> links = links(own);
            if (links.isEmpty()) return null;
            return new String[]{links.get(rnd.nextInt(links.size())), "رابط خاص"};
        }
        if (src == V_PIXABAY) {
            String j = get("https://pixabay.com/api/videos/?key=" + java.net.URLEncoder.encode(key.trim(), "UTF-8")
                    + "&q=" + java.net.URLEncoder.encode(topic, "UTF-8")
                    + "&video_type=film&safesearch=true&per_page=50&page=" + (1 + rnd.nextInt(2)));
            JSONArray hs = new JSONObject(j).optJSONArray("hits");
            if (hs == null) return null;
            List<String[]> ok = new ArrayList<>();
            for (int i = 0; i < hs.length(); i++) {
                JSONObject h = hs.getJSONObject(i);
                if (h.optInt("duration") > 90) continue;
                JSONObject vv = h.optJSONObject("videos");
                if (vv == null) continue;
                JSONObject f = vv.optJSONObject("medium");
                if (f == null || f.optString("url").isEmpty()) f = vv.optJSONObject("large");
                if (f == null || f.optString("url").isEmpty()) continue;
                ok.add(new String[]{f.getString("url"), "Pixabay — " + h.optString("user")});
            }
            return ok.isEmpty() ? null : ok.get(rnd.nextInt(ok.size()));
        }
        if (src == V_PEXELS) {
            HttpURLConnection h = openAuth("https://api.pexels.com/videos/search?query="
                    + java.net.URLEncoder.encode(topic, "UTF-8")
                    + "&orientation=landscape&size=medium&per_page=40&page=" + (1 + rnd.nextInt(3)), key.trim());
            String j;
            try (InputStream in = h.getInputStream()) { j = readAll(in); } finally { h.disconnect(); }
            JSONArray vs = new JSONObject(j).optJSONArray("videos");
            if (vs == null) return null;
            List<String[]> ok = new ArrayList<>();
            for (int i = 0; i < vs.length(); i++) {
                JSONObject v = vs.getJSONObject(i);
                if (v.optInt("duration") > 90) continue;
                JSONArray fl = v.optJSONArray("video_files");
                String best = null;
                int bw = 0;
                for (int k = 0; fl != null && k < fl.length(); k++) {
                    JSONObject f = fl.getJSONObject(k);
                    int w = f.optInt("width");
                    if (!"video/mp4".equals(f.optString("file_type")) || w > 1920 || w < 1280) continue;
                    if (w > bw) { bw = w; best = f.optString("link"); }
                }
                if (best != null) ok.add(new String[]{best, "Pexels — " + v.getJSONObject("user").optString("name")});
            }
            return ok.isEmpty() ? null : ok.get(rnd.nextInt(ok.size()));
        }
        // Wikimedia Commons: short nature/landscape clips, smallest VP9/WebM transcode >= 480p
        String j = get("https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch="
                + java.net.URLEncoder.encode("filetype:video " + topic, "UTF-8")
                + "&gsrnamespace=6&gsrlimit=40&gsroffset=" + rnd.nextInt(80)
                + "&prop=videoinfo&viprop=url%7Csize%7Cderivatives%7Cextmetadata&format=json");
        JSONObject q = new JSONObject(j).optJSONObject("query");
        if (q == null) return null;
        JSONObject pages = q.getJSONObject("pages");
        List<String[]> ok = new ArrayList<>();
        for (Iterator<String> it = pages.keys(); it.hasNext(); ) {
            JSONObject pg = pages.getJSONObject(it.next());
            JSONArray vi = pg.optJSONArray("videoinfo");
            if (vi == null || vi.length() == 0) continue;
            JSONObject v = vi.getJSONObject(0);
            double dur = v.optDouble("duration", 0);
            if (dur < 4 || dur > 150) continue;
            JSONArray dv = v.optJSONArray("derivatives");
            String best = null;
            int bh = 0;
            for (int k = 0; dv != null && k < dv.length(); k++) {
                JSONObject d = dv.getJSONObject(k);
                String ty = d.optString("type");
                int hgt = d.optInt("height");
                if (!ty.startsWith("video/webm") || hgt < 480 || hgt > 1080) continue;
                if (best == null || hgt < bh) { bh = hgt; best = d.optString("src"); }
            }
            if (best == null) continue;
            if (best.startsWith("//")) best = "https:" + best;
            JSONObject em = v.optJSONObject("extmetadata");
            String artist = em == null || em.optJSONObject("Artist") == null ? ""
                    : em.getJSONObject("Artist").optString("value", "").replaceAll("<[^>]*>", "").trim();
            ok.add(new String[]{best, "Wikimedia Commons — " + artist});
        }
        return ok.isEmpty() ? null : ok.get(rnd.nextInt(ok.size()));
    }

    private static HttpURLConnection openAuth(String u, String key) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(u).openConnection();
        h.setConnectTimeout(15000);
        h.setReadTimeout(30000);
        h.setRequestProperty("User-Agent", UA);
        h.setRequestProperty("Authorization", key);
        int code = h.getResponseCode();
        if (code != 200) throw new Exception("HTTP " + code + (code == 401 ? " (مفتاح Pexels غير صحيح)" : ""));
        return h;
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
        return b.toString("UTF-8");
    }
}
