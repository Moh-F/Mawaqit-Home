package com.moh.mawaqithome;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Finds free adhan / audio files on Wikimedia Commons (CC / public domain). */
final class AdhanSources {
    interface Cb {
        void done(List<String[]> found, String err);   // {title, url, license}
    }

    private AdhanSources() {}

    static void search(final String query, final Cb cb) {
        new Thread(() -> {
            try {
                String u = "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch="
                        + URLEncoder.encode(query + " filetype:audio", "UTF-8")
                        + "&gsrnamespace=6&gsrlimit=25&prop=imageinfo&iiprop=url%7Csize%7Cmime%7Cextmetadata&format=json";
                HttpURLConnection h = (HttpURLConnection) new URL(u).openConnection();
                h.setRequestProperty("User-Agent", "MawaqitHome/2 (https://github.com/Moh-F/Mawaqit-Home)");
                h.setConnectTimeout(12000);
                h.setReadTimeout(20000);
                String body;
                try (InputStream in = h.getInputStream()) {
                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
                    body = b.toString("UTF-8");
                } finally {
                    h.disconnect();
                }
                JSONObject q = new JSONObject(body).optJSONObject("query");
                List<String[]> out = new ArrayList<>();
                if (q != null) {
                    JSONObject pages = q.getJSONObject("pages");
                    for (Iterator<String> it = pages.keys(); it.hasNext(); ) {
                        JSONObject pg = pages.getJSONObject(it.next());
                        JSONObject ii = pg.getJSONArray("imageinfo").getJSONObject(0);
                        String mime = ii.optString("mime");
                        if (!mime.startsWith("audio") && !mime.contains("ogg")) continue;
                        String lic = "";
                        JSONObject em = ii.optJSONObject("extmetadata");
                        if (em != null && em.optJSONObject("LicenseShortName") != null)
                            lic = em.getJSONObject("LicenseShortName").optString("value");
                        String title = pg.optString("title", "").replaceFirst("^File:", "");
                        out.add(new String[]{title, ii.getString("url"), lic});
                    }
                }
                cb.done(out, null);
            } catch (Exception e) {
                cb.done(new ArrayList<>(), e.toString());
            }
        }, "adhansrc").start();
    }
}
