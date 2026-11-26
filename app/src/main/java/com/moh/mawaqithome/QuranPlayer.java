package com.moh.mawaqithome;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads a surah ayah by ayah: Arabic text from api.alquran.cloud (cached on the device) and one
 * audio file per ayah (everyayah.com style: BASE + SSSAAA.mp3). The text on screen therefore always
 * matches the ayah being recited. The next ayah is prepared while the current one plays.
 */
final class QuranPlayer {
    interface Cb {
        void onAyah(int ayah, String text, boolean bismillah, int index, int total);

        void onDone();

        void onError(String msg);
    }

    static final String[][] RECITERS = {
            {"Alafasy_128kbps", "مشاري العفاسي"},
            {"Abdul_Basit_Murattal_192kbps", "عبد الباسط عبد الصمد (مرتّل)"},
            {"Husary_128kbps", "محمود خليل الحصري"},
            {"Minshawy_Murattal_128kbps", "محمد صديق المنشاوي (مرتّل)"},
            {"Abdurrahmaan_As-Sudais_192kbps", "عبد الرحمن السديس"},
            {"Saood_ash-Shuraym_128kbps", "سعود الشريم"},
            {"MaherAlMuaiqly128kbps", "ماهر المعيقلي"},
            {"Hudhaify_128kbps", "علي الحذيفي"},
            {"Abu_Bakr_Ash-Shaatree_128kbps", "أبو بكر الشاطري"},
            {"Ahmed_ibn_Ali_al-Ajamy_128kbps_ketaab", "أحمد بن علي العجمي"},
            {"Hani_Rifai_192kbps", "هاني الرفاعي"},
            {"Muhammad_Ayyoub_128kbps", "محمد أيوب"},
            {"Yasser_Ad-Dussary_128kbps", "ياسر الدوسري"},
            {"Nasser_Alqatami_128kbps", "ناصر القطامي"},
            {"Ghamadi_40kbps", "سعد الغامدي"},
    };

    static String reciterName(String key) {
        for (String[] r : RECITERS) if (r[0].equals(key)) return r[1];
        return "قارئ مخصص";
    }

    private static final String BISMILLAH = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ";

    private static final class Item {
        int ayah;
        String text, file;
        boolean bism;
    }

    /** Other folder names the same reciter is known under on everyayah.com (tried when the first one fails). */
    static String[] altFolders(String key) {
        if ("Ahmed_ibn_Ali_al-Ajamy_128kbps_ketaab".equals(key))
            return new String[]{"Ahmed_ibn_Ali_al-Ajamy_128kbps_ketaab.net",
                    "Ahmed_ibn_Ali_al-Ajamy_64kbps_QuranExplorer.Com"};
        if ("MaherAlMuaiqly128kbps".equals(key)) return new String[]{"Maher_AlMuaiqly_64kbps"};
        if ("Maher_AlMuaiqly_128kbps".equals(key))                   // name used by an earlier version
            return new String[]{"MaherAlMuaiqly128kbps", "Maher_AlMuaiqly_64kbps"};
        return new String[0];
    }

    private String url(int i) {
        return bases.get(Math.min(baseIdx, bases.size() - 1)) + items.get(i).file;
    }

    private final Context ctx;
    private final int surah, from, to;
    private final List<String> bases = new ArrayList<>();
    private int baseIdx;
    private final float vol;
    private final Cb cb;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final List<Item> items = new ArrayList<>();
    private MediaPlayer cur, nxt;
    private int curIdx = -1;
    private boolean curReady, nxtReady, stopped, paused, started, curPlaying;
    private int retries;

    QuranPlayer(Context ctx, int surah, int from, int to, List<String> bases, float vol, Cb cb) {
        this.ctx = ctx.getApplicationContext();
        this.surah = surah;
        this.from = from;
        this.to = to;
        for (String b : bases) this.bases.add(b.endsWith("/") ? b : b + "/");
        this.vol = vol;
        this.cb = cb;
    }

    boolean isPaused() {
        return paused;
    }

    int total() {
        return items.size();
    }

    // -------------------------------------------------------------- text

    private static String norm(String s) {
        return s.replaceAll("[\\u064B-\\u065F\\u0670\\u0640]", "").replace('ٱ', 'ا');
    }

    private String loadText() throws Exception {
        File dir = new File(ctx.getFilesDir(), "quran");
        dir.mkdirs();
        File f = new File(dir, "s" + surah + ".json");
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(
                    "https://api.alquran.cloud/v1/surah/" + surah + "/quran-uthmani").openConnection();
            c.setRequestProperty("User-Agent", "MawaqitHome");
            c.setConnectTimeout(12000);
            c.setReadTimeout(20000);
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            try (InputStream in = c.getInputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            } finally {
                c.disconnect();
            }
            String body = b.toString("UTF-8");
            new JSONObject(body).getJSONObject("data").getJSONArray("ayahs");     // sanity check
            try (FileOutputStream o = new FileOutputStream(f)) {
                o.write(body.getBytes("UTF-8"));
            }
            return body;
        } catch (Exception net) {
            if (!f.exists()) throw net;
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            try (InputStream in = new FileInputStream(f)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            }
            return b.toString("UTF-8");
        }
    }

    private void build(String body) throws Exception {
        JSONArray ar = new JSONObject(body).getJSONObject("data").getJSONArray("ayahs");
        int last = ar.length();
        int a = Math.max(1, from), z = to <= 0 ? last : Math.min(last, to);
        if (a > last) a = 1;
        if (surah != 1 && surah != 9 && a == 1) {
            Item b = new Item();
            b.ayah = 0;
            b.bism = true;
            b.text = BISMILLAH;
            b.file = "001001.mp3";
            items.add(b);
        }
        for (int i = a; i <= z; i++) {
            JSONObject o = ar.getJSONObject(i - 1);
            String t = o.getString("text").trim();
            if (i == 1 && surah != 1 && surah != 9) {                 // the API prefixes the bismillah to ayah 1
                String nt = norm(t);
                if (nt.startsWith("بسم الله الرحمن الرحيم")) {
                    String[] w = t.split("\\s+");
                    StringBuilder sb = new StringBuilder();
                    for (int k = 4; k < w.length; k++) sb.append(sb.length() > 0 ? " " : "").append(w[k]);
                    if (sb.length() > 0) t = sb.toString();
                }
            }
            Item it = new Item();
            it.ayah = i;
            it.text = t;
            it.file = String.format(Locale.US, "%03d%03d.mp3", surah, i);
            items.add(it);
        }
    }

    // -------------------------------------------------------------- audio

    void start() {
        new Thread(() -> {
            try {
                final String body = loadText();
                h.post(() -> {
                    if (stopped) return;
                    try {
                        build(body);
                        if (items.isEmpty()) throw new Exception("no ayahs");
                        curIdx = 0;
                        cur = create(0);
                    } catch (Exception e) {
                        AppLog.error("QURAN", "build failed", e);
                        cb.onError("تعذّر تجهيز السورة");
                    }
                });
            } catch (Exception e) {
                AppLog.error("QURAN", "text failed", e);
                h.post(() -> {
                    if (!stopped) cb.onError("تعذّر جلب نص السورة — تحقق من الإنترنت");
                });
            }
        }, "quranload").start();
    }

    private MediaPlayer create(final int idx) {
        final Item it = items.get(idx);
        final String itUrl = url(idx);
        MediaPlayer m = new MediaPlayer();
        try {
            m.setAudioStreamType(AudioManager.STREAM_MUSIC);
            m.setDataSource(itUrl);
            m.setVolume(vol, vol);
            m.setOnPreparedListener(mp -> {
                if (stopped) return;
                if (mp == cur) {
                    curReady = true;
                    startCur();
                } else if (mp == nxt) {
                    nxtReady = true;
                }
            });
            m.setOnCompletionListener(mp -> {
                if (stopped || mp != cur) return;
                advance();
            });
            m.setOnErrorListener((mp, what, extra) -> {
                AppLog.info("QURAN", "audio error " + what + "/" + extra + " " + itUrl);
                if (stopped) return true;
                onFail(mp, idx);
                return true;
            });
            m.prepareAsync();
        } catch (Exception e) {
            AppLog.error("QURAN", "create failed " + itUrl, e);
            onFail(m, idx);
        }
        return m;
    }

    private void onFail(final MediaPlayer mp, final int idx) {
        try {
            mp.release();
        } catch (Exception ignored) {
        }
        if (retries >= 3) {
            if (!started && baseIdx + 1 < bases.size()) {           // first ayah never played: try the next folder name
                baseIdx++;
                retries = 0;
                AppLog.info("QURAN", "switching to folder " + bases.get(baseIdx));
                h.postDelayed(() -> {
                    if (stopped) return;
                    curReady = false;
                    cur = create(curIdx);
                }, 500L);
                return;
            }
            if (!stopped) cb.onError("تعذّر تحميل صوت الآية — تحقق من الإنترنت أو من القارئ (" + url(idx) + ")");
            return;
        }
        retries++;
        h.postDelayed(() -> {
            if (stopped) return;
            if (idx == curIdx) {
                curReady = false;
                cur = create(idx);
            } else if (idx == curIdx + 1) {
                nxtReady = false;
                nxt = create(idx);
            }
        }, 1500L);
    }

    private void startCur() {
        if (stopped || paused || !curReady || cur == null || curPlaying) return;
        try {
            curPlaying = true;
            retries = 0;
            Item it = items.get(curIdx);
            cb.onAyah(it.ayah, it.text, it.bism, curIdx, items.size());
            cur.start();
            started = true;
            if (nxt == null && curIdx + 1 < items.size()) {
                nxtReady = false;
                nxt = create(curIdx + 1);
            }
        } catch (Exception e) {
            AppLog.error("QURAN", "start failed", e);
            cb.onError("تعذّر التشغيل");
        }
    }

    private void advance() {
        try {
            if (cur != null) cur.release();
        } catch (Exception ignored) {
        }
        cur = null;
        curPlaying = false;
        curIdx++;
        if (curIdx >= items.size()) {
            cb.onDone();
            return;
        }
        cur = nxt;
        curReady = nxtReady;
        nxt = null;
        nxtReady = false;
        if (cur == null) cur = create(curIdx);
        else if (curReady) startCur();
    }

    void pause() {
        paused = true;
        try {
            if (cur != null && curPlaying) cur.pause();
        } catch (Exception ignored) {
        }
    }

    void resume() {
        paused = false;
        try {
            if (cur != null && curPlaying) cur.start();
            else startCur();
        } catch (Exception ignored) {
        }
    }

    void stop() {
        stopped = true;
        h.removeCallbacksAndMessages(null);
        try {
            if (cur != null) cur.release();
        } catch (Exception ignored) {
        }
        try {
            if (nxt != null) nxt.release();
        } catch (Exception ignored) {
        }
        cur = null;
        nxt = null;
    }
}
