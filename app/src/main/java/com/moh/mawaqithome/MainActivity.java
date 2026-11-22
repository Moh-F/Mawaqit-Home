package com.moh.mawaqithome;

import android.annotation.TargetApi;
import android.content.res.ColorStateList;
import android.graphics.drawable.LayerDrawable;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Mawaqit Home launcher.
 *
 * The live Mawaqit page fills the top part of the screen; a strip of app tiles (1-3 rows)
 * sits underneath; a floating category menu slides in from the left or right edge. After a
 * while without remote activity the tiles slide away and the page grows to the full screen
 * (screen saver), then the screen dims, goes black, and can put the device to sleep.
 */
public class MainActivity extends Activity {

    // ---------------------------------------------------------------- Config

    private static final String DEFAULT_URL =
            "https://mawaqit.net/ar/byty-saarbrucken-66115-germany";

    private static final double DEFAULT_LAT = 49.2402;
    private static final double DEFAULT_LON = 6.9969;
    private static final String DEFAULT_CITY = "Saarbrücken";

    private static final long RELOAD_EVERY_MS = 3L * 60 * 60 * 1000;
    private static final long RETRY_AFTER_ERROR_MS = 20_000L;
    private static final long WEATHER_EVERY_MS = 10L * 60 * 1000;
    private static final long WEATHER_RETRY_MS = 2L * 60 * 1000;
    private static final long PRAYER_EVERY_MS = 60L * 60 * 1000;
    private static final long PRAYER_RETRY_MS = 10L * 60 * 1000;
    private static final long PIXEL_SHIFT_EVERY_MS = 60_000L;
    private static final long UPDATE_EVERY_MS = 6L * 60 * 60 * 1000;
    private static final long BUBBLE_MS = 1800L;

    // Tab kinds. The tab list is: favorites, TV, mobile, all, <custom categories>, settings
    private static final int K_FAV = 0, K_TV = 1, K_MOBILE = 2, K_ALL = 3, K_CUSTOM = 4, K_SYS = 5;

    private static final String[] STAGE_NAMES = {
            "إخفاء الأيقونات وتكبير الصفحة", "تعتيم الشاشة", "شاشة سوداء", "نوم الجهاز (يطفئ التلفزيون)"
    };
    private static final int[] STAGE_DEFAULTS = {10, 15, 20, 0};
    private static final int[] MINUTE_OPTIONS = {0, 1, 2, 3, 4, 5, 6, 8, 10, 12, 15, 20, 25, 30,
            40, 45, 60, 90, 120};
    private static final int[] TOP_PCT_OPTIONS = {70, 75, 78, 80, 82, 85, 88, 90};
    private static final float DIM_ALPHA = 0.65f;

    private static final int[][] SHIFTS = {
            {0, 0}, {3, 0}, {3, 3}, {0, 3}, {-3, 3}, {-3, 0}, {-3, -3}, {0, -3}, {3, -3}
    };

    private static final String MOVE_HINT = "↔ وضع التحريك: يمين/يسار للتحريك • OK للتثبيت";
    private static final String APP_DETAILS_ACTION = "android.settings.APPLICATION_DETAILS_SETTINGS";
    private static final String[] PRAYER_NAMES = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};
    private static final String[] MONTHS = {"كانون الثاني", "شباط", "آذار", "نيسان", "أيار",
            "حزيران", "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول"};
    private static final String[] CAT_ICONS = {"📁", "📡", "🎬", "🛠", "🎮", "📚", "🎵", "⭐",
            "🏠", "🌐", "📺", "🔧"};

    private static final String JS_TIMES =
            "(function(){try{var c=(typeof confData!=='undefined')?confData:window.confData;"
                    + "if(!c||!c.times)return null;return JSON.stringify(c.times);}catch(e){return null;}})()";

    private static class Shortcut {
        final String icon, label;
        final String[] actions;

        Shortcut(String icon, String label, String... actions) {
            this.icon = icon;
            this.label = label;
            this.actions = actions;
        }
    }

    // "#..." entries are handled inside the launcher itself.
    private static final Shortcut[] SHORTCUTS = {
            new Shortcut("🕌", "إعدادات Mawaqit Home", "#settings"),
            new Shortcut("⚙️", "إعدادات الجهاز", "android.settings.SETTINGS"),
            new Shortcut("📶", "الشبكة / Wi-Fi", "android.settings.WIFI_SETTINGS",
                    "android.settings.WIRELESS_SETTINGS"),
            new Shortcut("🖥", "الشاشة", "android.settings.DISPLAY_SETTINGS",
                    "android.settings.SETTINGS"),
            new Shortcut("🔊", "الصوت", "android.settings.SOUND_SETTINGS",
                    "android.settings.SETTINGS"),
            new Shortcut("🔵", "البلوتوث والأجهزة", "android.settings.BLUETOOTH_SETTINGS",
                    "android.settings.SETTINGS"),
            new Shortcut("📦", "إدارة التطبيقات", "android.settings.MANAGE_ALL_APPLICATIONS_SETTINGS",
                    "android.settings.APPLICATION_SETTINGS", "android.settings.SETTINGS"),
            new Shortcut("🗄", "التخزين", "android.settings.INTERNAL_STORAGE_SETTINGS",
                    "android.settings.SETTINGS"),
            new Shortcut("🛠", "خيارات المطوّر", "android.settings.APPLICATION_DEVELOPMENT_SETTINGS",
                    "android.settings.SETTINGS"),
            new Shortcut("⏰", "فحص أوقات الصلاة والإيقاظ", "#prayer"),
            new Shortcut("👁", "التطبيقات المخفية", "#hidden"),
            new Shortcut("📤", "نسخ احتياطي للإعدادات", "#backup"),
            new Shortcut("📥", "استعادة الإعدادات", "#restore"),
            new Shortcut("🔄", "تحديث الصفحة", "#reload")
    };

    // ---------------------------------------------------------------- State

    private static class AppEntry {
        final String pkg, cls, label;
        final Drawable icon;
        final boolean tv;
        Drawable banner;                 // TV banner (only loaded for the rectangular styles)

        AppEntry(String pkg, String cls, String label, Drawable icon, boolean tv) {
            this.pkg = pkg;
            this.cls = cls;
            this.label = label;
            this.icon = icon;
            this.tv = tv;
        }
    }

    private static class Tab {
        String id, name, icon;
        int kind;
        Cat cat;   // for K_CUSTOM
    }

    private static class Cat {
        String id, name, icon;
        final List<String> pkgs = new ArrayList<>();
    }

    private static class Day {
        String icon = "☁️", date = "";
        long max, min, hum = -1, rain = -1, wind = -1, uv = -1;
    }

    private static class WeatherData {
        final Day[] days = new Day[3];
        long curTemp, curHum, curFeels = -999, curWind = -1;
        String curIcon = "☁️";
        int phase = 0;            // 0 day, 1 dusk/dawn, 2 night
        int code = -1;            // WMO weather code of the current conditions
    }

    private interface Pick {
        void on(int index);
    }

    private interface TextCb {
        void on(String text);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean destroyed = false;

    private FrameLayout root, shiftLayer;
    private WebView webView;
    private String url;

    // weather card
    private LinearLayout weatherCard;
    private TextView weatherCity, weatherAge;
    private final TextView[] wDay = new TextView[3], wIcon = new TextView[3],
            wTemp = new TextView[3], wSub = new TextView[3];
    private View wSep;
    private boolean wMove = false;
    private LinearLayout daysBox;
    private final LinearLayout[] dayView = new LinearLayout[3];
    private final Map<String, Integer> lastPos = new HashMap<>();
    private ServerSocket logSrv;
    private WeatherData weather;
    private long weatherAt = 0L;
    private boolean weatherFailed = false;

    // bottom strip
    private LinearLayout strip, topRow, badge, grid;
    private TextView badgeIcon, nameLabel;
    private HorizontalScrollView rowScroll;
    private View hair;
    private final List<View> tiles = new ArrayList<>();

    // floating category menu
    private class CapScroll extends ScrollView {
        int maxH = 0;

        CapScroll(Context c) { super(c); }

        @Override protected void onMeasure(int w, int h) {
            if (maxH > 0) h = MeasureSpec.makeMeasureSpec(maxH, MeasureSpec.AT_MOST);
            super.onMeasure(w, h);
        }
    }

    private CapScroll railScroll;
    private LinearLayout railList;
    private final List<TextView> railItems = new ArrayList<>();
    private boolean railOpen = false;

    private TextView bubble;
    private boolean bubbleSticky = false;
    private View overlay;

    private final List<AppEntry> entries = new ArrayList<>();
    private final Map<String, AppEntry> byPkg = new HashMap<>();
    private final List<String> favs = new ArrayList<>();
    private final List<String> order = new ArrayList<>();
    private final List<String> hidden = new ArrayList<>();
    private final List<Cat> cats = new ArrayList<>();
    private final List<Tab> tabs = new ArrayList<>();
    private String lastSig = "";
    private int currentTab = 0;

    // geometry (computed in onSized)
    private int sizeW = 0, sizeH = 0, topH = 0, stripH = 0, tilePx = 0, iconPx = 0, railW = 0;
    private int rows = 1;
    private float pageRatio = 0.82f;
    private boolean fillMode = false;
    private boolean railRight = false;

    private boolean moveMode = false;
    private String movePkg = null;

    private static boolean splashDone = false;      // shown once per process start
    private FrameLayout splash;
    private TextView splashPct;
    private android.widget.ProgressBar splashBar;
    private int splashShown = 0;
    private boolean pageMain = true;                // false while the adhan / iqama screen is up
    private int stage = 0;                 // 0 normal, 1 page full-screen, 2 dimmed, 3 black
    private boolean sleepTried = false;
    private long lastInput = SystemClock.elapsedRealtime();
    private int shiftIdx = 0;
    private int lastDay = -1;
    private boolean resumeSaverPending = false;

    // health
    private boolean pageOk = false, errorThisLoad = false, resumed = false;
    private long pageLoadedAt = 0L;
    private String wdLast = "";
    private int wdSame = 0;
    private boolean wdPending = false;
    private long wdRecoverAt = 0L;
    private int pageColor = 0xFF0E3B2C;

    // updates
    private Updater.Info pendingInfo;
    private File readyApk;
    private boolean updBusy = false;

    private SharedPreferences prefs;
    private AudioManager audio;

    // ---------------------------------------------------------------- Tasks

    private final Runnable retryTask = new Runnable() {
        @Override public void run() { webView.loadUrl(url); }
    };

    private final Runnable reloadTask = new Runnable() {
        @Override public void run() {
            AppLog.info("PAGE", "scheduled reload");
            webView.reload();
            handler.postDelayed(this, RELOAD_EVERY_MS);
        }
    };

    private final Runnable weatherTask = new Runnable() {
        @Override public void run() { fetchWeather(); }
    };

    private final Runnable prayerTask = new Runnable() {
        @Override public void run() { extractPrayerTimes(); }
    };

    private final Runnable hideBubbleTask = new Runnable() {
        @Override public void run() { hideBubble(); }
    };

    private final Runnable railCloseTask = new Runnable() {
        @Override public void run() { closeRail(true); }
    };

    private final Runnable updateTask = new Runnable() {
        @Override public void run() {
            handler.removeCallbacks(this);
            handler.postDelayed(this, UPDATE_EVERY_MS);
            if (prefs.getInt("upd_mode", 1) != 0) checkUpdate(false);
        }
    };

    private static final String JS_HB =
            "(function(){try{return document.body.innerText.replace(/\\s+/g,' ').slice(0,4000);}"
                    + "catch(e){return 'x';}})()";

    /** Every 90 s: if the page text (it contains the ticking seconds) stops changing, reload. */
    private final Runnable watchdogTask = new Runnable() {
        @Override public void run() {
            handler.postDelayed(this, 90_000L);
            if (!prefs.getBoolean("watchdog", true) || !pageOk || !resumed) {
                wdSame = 0;
                return;
            }
            wdPending = true;
            webView.evaluateJavascript(JS_HB, v -> {
                wdPending = false;
                if (v == null) return;
                if (v.equals(wdLast)) wdSame++;
                else {
                    wdSame = 0;
                    wdLast = v;
                }
                if (wdSame >= 3) recoverPage("page content frozen");
            });
            handler.postDelayed(() -> {
                if (wdPending) {
                    wdPending = false;
                    recoverPage("page not responding");
                }
            }, 20_000L);
        }
    };

    private final Runnable pageColorTask = new Runnable() {
        @Override public void run() {
            handler.removeCallbacks(this);
            handler.postDelayed(this, 5L * 60 * 1000);
            samplePageColor();
        }
    };

    private final BroadcastReceiver netRx = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (!online()) {
                if (!pageOk) showBubble("⚠ لا يوجد اتصال بالإنترنت — بانتظار الشبكة…", 0L);
                return;
            }
            if (!pageOk || weatherFailed) {
                AppLog.info("NET", "back online -> refreshing");
                if (!pageOk) webView.loadUrl(url);
                if (weatherFailed) fetchWeather();
            }
        }
    };

    private final Runnable tickTask = new Runnable() {
        @Override public void run() {
            updateWeatherAge();
            int d = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            if (lastDay != -1 && d != lastDay) {
                AppLog.info("PRAYER", "day changed -> re-reading prayer times");
                extractPrayerTimes();
                fetchWeather();
            }
            if (d != lastDay) autoBackup();
            lastDay = d;
            maybeShowNotice();
            handler.postDelayed(this, 30_000L);
        }
    };

    private void applyOverscan() {
        int sw = shiftLayer.getWidth(), sh = shiftLayer.getHeight();
        if (sw < 100 || sh < 100) return;
        int mx = 0, my = 0;
        for (int[] sft : SHIFTS) { mx = Math.max(mx, Math.abs(dp(sft[0]))); my = Math.max(my, Math.abs(dp(sft[1]))); }
        shiftLayer.setPivotX(sw / 2f);
        shiftLayer.setPivotY(sh / 2f);
        shiftLayer.setScaleX(1f + 2f * (mx + dp(1)) / sw);     // overscan: shifting never exposes the root colour
        shiftLayer.setScaleY(1f + 2f * (my + dp(1)) / sh);
    }

    private final Runnable shiftTask = new Runnable() {
        @Override public void run() {
            boolean on = prefs.getBoolean("px_shift", true);
            shiftIdx = on ? (shiftIdx + 1) % SHIFTS.length : 0;
            applyOverscan();
            shiftLayer.setTranslationX(dp(SHIFTS[shiftIdx][0]));
            shiftLayer.setTranslationY(dp(SHIFTS[shiftIdx][1]));
            handler.postDelayed(this, PIXEL_SHIFT_EVERY_MS);
        }
    };

    private final Runnable idleTask = new Runnable() {
        @Override public void run() {
            if (openDialogs > 0) lastInput = SystemClock.elapsedRealtime();   // a menu is in use
            long idleMs = SystemClock.elapsedRealtime() - lastInput;
            int t1 = stageMinutes(0), t2 = stageMinutes(1), t3 = stageMinutes(2), t4 = stageMinutes(3);
            int target = 0;
            if (t3 > 0 && idleMs >= t3 * 60_000L) target = 3;
            else if (t2 > 0 && idleMs >= t2 * 60_000L) target = 2;
            else if (t1 > 0 && idleMs >= t1 * 60_000L) target = 1;

            boolean prayerNow = WakeScheduler.inWindow(WakeScheduler.load(prefs));
            if (prayerNow && target > 1) target = 1;
            if (target != stage && sizeH > 0) applyStage(target, true);

            // a downloaded update is installed only while nobody is watching the launcher
            if (readyApk != null && stage >= 1 && !prayerNow && !updBusy) installUpdate(true);

            if (t4 > 0 && idleMs >= t4 * 60_000L && !prayerNow && !sleepTried) sleepDevice();
            handler.postDelayed(this, 10_000L);
        }
    };

    // ------------------------------------------------------------ Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLog.init(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        prefs = getSharedPreferences("cfg", MODE_PRIVATE);
        audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        TimeCfg.apply(this);
        resolveUrl(getIntent());
        loadLists();
        rows = Math.max(1, Math.min(3, prefs.getInt("rows", 1)));
        railRight = prefs.getInt("rail_side", 0) == 1;
        checkVersionChange();

        loadFonts();
        root = new FrameLayout(this);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF05100C, 0xFF0B2A20}));

        shiftLayer = new FrameLayout(this);
        shiftLayer.setClipChildren(false);
        root.addView(shiftLayer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        webView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int p) {
                setSplash(25 + p * 75 / 100);
            }

            @Override public boolean onConsoleMessage(android.webkit.ConsoleMessage cm) {
                String m = cm.message();
                if (m != null && m.startsWith("MHVID")) {
                    AppLog.info("VIDEO", m);
                    if (m.startsWith("MHVID playing")) videoErrors = 0;
                    else if (m.startsWith("MHVID error") && ++videoErrors <= 4 && bgVideo()) {
                        handler.postDelayed(() -> bgFetch(false), 4000L);     // try another clip
                    }
                }
                return true;
            }
        });
        webView.setFocusable(false);
        webView.setFocusableInTouchMode(false);
        configureWebView();
        shiftLayer.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        webView.loadUrl(url);                      // start the network as early as possible
        buildWeatherCard();
        buildStrip();
        buildBubble();
        buildRail();

        overlay = new View(this);
        overlay.setBackgroundColor(Color.BLACK);
        overlay.setAlpha(0f);
        overlay.setFocusable(false);
        root.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (!splashDone) buildSplash();
        setContentView(root);
        applySounds(root);
        applyFont(root);
        getWindow().getDecorView().setSoundEffectsEnabled(prefs.getBoolean("sounds", true));
        hideSystemUi();

        reloadApps();
        buildTabs();
        String savedId = prefs.getString("tab_id", "");
        currentTab = tabIndexById(savedId);
        if (currentTab < 0) currentTab = favs.isEmpty() ? tabIndexByKind(K_ALL) : 0;

        // Came back after a background update while the screen saver was running
        Intent in = getIntent();
        if (in != null && in.getBooleanExtra("resume_saver", false)) {
            resumeSaverPending = true;
            lastInput = SystemClock.elapsedRealtime() - in.getLongExtra("idle_ms", 0L);
            stage = 1;
        }

        root.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override public void onGlobalLayout() {
                        int w = root.getWidth(), h = root.getHeight();
                        if (w > 0 && h > 0 && (w != sizeW || h != sizeH)) {
                            sizeW = w;
                            sizeH = h;
                            onSized(w, h);
                        }
                    }
                });

        setSplash(25);
        handler.postDelayed(reloadTask, RELOAD_EVERY_MS);
        handler.postDelayed(pageStateTask, 4000L);
        handler.post(weatherTask);
        handler.post(tickTask);
        handler.postDelayed(idleTask, 10_000L);
        handler.postDelayed(shiftTask, PIXEL_SHIFT_EVERY_MS);
        handler.postDelayed(updateTask, 20_000L);
        handler.postDelayed(bgTask, 15_000L);
        handler.postDelayed(watchdogTask, 120_000L);
        try {
            registerReceiver(netRx, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        } catch (Exception e) {
            AppLog.error("NET", "register failed", e);
        }
        if (!online()) showBubble("⚠ لا يوجد اتصال بالإنترنت — بانتظار الشبكة…", 0L);
        WakeScheduler.scheduleNext(this);
        AppLog.info("APP", "created v" + Updater.currentName(this) + " url=" + url);

        if (getIntent().getBooleanExtra("wake", false)) doWake();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String old = url;
        resolveUrl(intent);
        if (!url.equals(old)) webView.loadUrl(url);
        if (intent.getBooleanExtra("wake", false)) doWake();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        webView.onResume();
        webView.resumeTimers();
        resumeNativeVideo();
        if (resumeSaverPending) {
            resumeSaverPending = false;       // keep the saver running after an update restart
        } else {
            lastInput = SystemClock.elapsedRealtime();
            sleepTried = false;
            if (sizeH > 0) applyStage(0, false);
        }
        if (reloadApps() && tilePx > 0) {
            buildTabs();
            showTab(currentTab, -1);
        }
        if (root.findFocus() == null) focusFirst();
        hideSystemUi();
        long ts = prefs.getLong("prayers_ts", 0L);
        if (System.currentTimeMillis() - ts > 30L * 60 * 1000) {
            handler.removeCallbacks(prayerTask);
            handler.postDelayed(prayerTask, 4000L);
        }
    }

    @Override
    protected void onPause() {
        resumed = false;
        try {
            pauseNativeVideo();
        } catch (Exception ignored) {
        }
        webView.onPause();
        webView.pauseTimers();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        try {
            unregisterReceiver(netRx);
        } catch (Exception ignored) {
        }
        handler.removeCallbacksAndMessages(null);
        stopNativeVideo();
        stopLinkServer();
        shiftLayer.removeView(webView);
        webView.destroy();
        AppLog.info("APP", "destroyed");
        phoneSrv.stop();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // Home screen: Back does nothing.
    }

    private static final String JS_STATE = "(function(){function vis(e){if(!e)return false;var r=e.getBoundingClientRect();"
            + "var c=getComputedStyle(e);return r.width>0&&r.height>0&&c.visibility!=='hidden'&&c.display!=='none';}"
            + "var cover='';function shown(e){if(!vis(e))return false;"
            + "for(var n=e;n&&n.nodeType===1;n=n.parentElement){if(parseFloat(getComputedStyle(n).opacity)<0.2)return false;}"
            + "var r=e.getBoundingClientRect(),pts=[[.5,.5],[.25,.3],[.75,.7],[.5,.15]],hid=0;"
            + "for(var i=0;i<pts.length;i++){var t=document.elementFromPoint(r.left+r.width*pts[i][0],r.top+r.height*pts[i][1]);"
            + "if(t&&!e.contains(t)&&!t.contains(e)){hid++;cover=String(t.className||t.id||t.tagName).slice(0,40);}}"
            + "return hid<2;}"
            + "var p=document.querySelector('.prayers');"
            + "function iq(){var all=document.querySelectorAll('body *');for(var i=0;i<all.length;i++){var e=all[i];"
            + "if(e.children.length>0)continue;var t=(e.textContent||'').trim();if(!t||t.length>60)continue;"
            + "if(/(الإقامة|الاقامة|إقامة|اقامة|iqama|iqamah)/i.test(t)&&vis(e)){var f=parseFloat(getComputedStyle(e).fontSize)||0;"
            + "if(f>=36)return true;}}return false;}"
            + "if(!shown(p))return '0:'+cover;return iq()?'2':'1';})()";

    private final Runnable pageStateTask = new Runnable() {
        @Override public void run() {
            handler.postDelayed(this, 2000L);
            if (!pageOk || stage != 0) return;
            webView.evaluateJavascript(JS_STATE, v -> {
                boolean main = v == null || v.startsWith("\"1");
                if (main != pageMain) {
                    pageMain = main;
                    AppLog.info("PAGE", main ? "main screen (prayer times visible)"
                            : "prayer times hidden/covered (adhan, iqama, athkar, hadith…) state=" + v);
                    applyWeatherVis(true);
                }
            });
        }
    };

    private void applyWeatherVis(boolean animate) {
        if (weatherCard == null || !prefs.getBoolean("w_show", true)) return;
        boolean visible = (pageMain || !prefs.getBoolean("w_hide_adhan", true));
        int pcl = prefs.getInt("sv_clear", 0);
        float full = (stage >= 1 && stage <= 2 && pcl > 0) ? 1f - pcl / 100f : 1f;
        weatherCard.animate().alpha(visible ? full : 0f).setDuration(animate ? ms(600L) : 0L).start();
    }

    private void doWake() {
        AppLog.info("WAKE", "woken by the pre-adhan alarm");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        sleepTried = false;
        lastInput = SystemClock.elapsedRealtime();
        if (sizeH > 0) applyStage(0, false);
        WakeScheduler.scheduleNext(this);
    }

    /** Remembers the running version; after an update it queues a "updated" notice. */
    private void checkVersionChange() {
        int now = Updater.currentCode(this);
        int last = prefs.getInt("last_code", 0);
        if (last != 0 && last != now) {
            String n = "✓ تم تحديث التطبيق إلى الإصدار " + Updater.currentName(this);
            prefs.edit().putString("notice", n).apply();
            AppLog.info("UPDATE", "updated " + last + " -> " + now);
        }
        prefs.edit().putInt("last_code", now).apply();
    }

    private void maybeShowNotice() {
        if (stage != 0 || bubbleSticky) return;
        String n = prefs.getString("notice", "");
        if (n != null && !n.isEmpty() && sizeH > 0) {
            prefs.edit().remove("notice").apply();
            showBubble(n, 9000L);
        }
    }

    // ------------------------------------------------------------------ URL

    private void buildSplash() {
        splash = new FrameLayout(this);
        splash.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF05100C, 0xFF0B2A20}));
        splash.setClickable(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        android.widget.ImageView logo = new android.widget.ImageView(this);
        logo.setImageResource(R.drawable.banner);
        logo.setAdjustViewBounds(true);
        col.addView(logo, new LinearLayout.LayoutParams(dp(300), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView nm = text(22, true);
        nm.setText("Mawaqit Home");
        nm.setGravity(Gravity.CENTER);
        nm.setPadding(0, dp(14), 0, dp(14));
        col.addView(nm, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        splashBar = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        splashBar.setMax(100);
        splashBar.getProgressDrawable().setColorFilter(0xFFF4C95D, android.graphics.PorterDuff.Mode.SRC_IN);
        col.addView(splashBar, new LinearLayout.LayoutParams(dp(300), dp(8)));
        splashPct = text(16, true);
        splashPct.setGravity(Gravity.CENTER);
        splashPct.setTextColor(0xFFF4C95D);
        splashPct.setPadding(0, dp(10), 0, 0);
        col.addView(splashPct, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        splash.addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        root.addView(splash, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setSplash(5);
        handler.postDelayed(this::hideSplash, 30_000L);      // never block the screen for good
    }

    private void setSplash(int pct) {
        if (splash == null) return;
        pct = Math.max(splashShown, Math.min(100, pct));
        splashShown = pct;
        splashBar.setProgress(pct);
        splashPct.setText(pct + "%");
    }

    private void hideSplash() {
        if (splash == null) return;
        final View sp = splash;
        splash = null;
        splashDone = true;
        sp.animate().alpha(0f).setDuration(450).withEndAction(() -> root.removeView(sp)).start();
    }

    private void resolveUrl(Intent intent) {
        String extra = intent != null ? intent.getStringExtra("url") : null;
        if (extra != null && extra.startsWith("https://")) {
            prefs.edit().putString("url_v2", extra).apply();
        }
        url = prefs.getString("url_v2", DEFAULT_URL);
    }

    // -------------------------------------------------------------- WebView

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUserAgentString(desktopUserAgent(s.getUserAgentString()));
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String target) {
                return false;
            }

            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view,
                                                                              android.webkit.WebResourceRequest r) {
                String p = r.getUrl().getPath();
                if (p != null && p.startsWith("/__mhbg/bg_")) {
                    try {
                        File bf = Backgrounds.file(MainActivity.this,
                                p.endsWith("1.jpg") ? 1 : 0);
                        if (bf.exists()) {
                            android.webkit.WebResourceResponse resp = new android.webkit.WebResourceResponse(
                                    "image/jpeg", null, new java.io.FileInputStream(bf));
                            java.util.Map<String, String> hd = new HashMap<>();
                            hd.put("Cache-Control", "no-store");
                            resp.setResponseHeaders(hd);
                            return resp;
                        }
                    } catch (Exception e) {
                        AppLog.info("BG", "serve failed: " + e);
                    }
                }
                if (p != null && p.startsWith("/__mhfont/") && p.endsWith(".ttf")) {
                    try {
                        String name = p.substring("/__mhfont/".length());
                        if (name.contains("/") || name.contains("..")) return null;
                        android.webkit.WebResourceResponse resp = new android.webkit.WebResourceResponse(
                                "font/ttf", null, getAssets().open("fonts/" + name));
                        java.util.Map<String, String> hd = new HashMap<>();
                        hd.put("Access-Control-Allow-Origin", "*");
                        resp.setResponseHeaders(hd);
                        return resp;
                    } catch (Exception e) {
                        AppLog.info("FONT", "page font not found: " + p);
                    }
                }
                return null;
            }

            @Override
            public void onPageStarted(WebView view, String u, Bitmap favicon) {
                errorThisLoad = false;
                setSplash(30);
            }

            @Override
            public void onPageFinished(WebView view, String finishedUrl) {
                if (errorThisLoad) return;          // Android also calls this after a failed load
                pageOk = true;
                setSplash(100);
                handler.postDelayed(MainActivity.this::hideSplash, 500L);
                pageLoadedAt = System.currentTimeMillis();
                wdSame = 0;
                wdLast = "";
                handler.removeCallbacks(retryTask);
                handler.removeCallbacks(pageColorTask);
                handler.postDelayed(pageColorTask, 6000L);
                if (bubbleSticky) hideBubble();
                handler.removeCallbacks(prayerTask);
                handler.postDelayed(prayerTask, 5000L);
                applyPageCss();
                handler.postDelayed(MainActivity.this::applyPageCss, 2500L);
            }

            @TargetApi(23)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (request.isForMainFrame()) pageFailed(String.valueOf(error.getDescription()));
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                if (failingUrl != null && failingUrl.equals(url)) pageFailed(description);
            }
        });
    }

    private void pageFailed(String why) {
        AppLog.info("PAGE", "load failed: " + why);
        errorThisLoad = true;
        pageOk = false;
        showBubble(online() ? "⚠ تعذّر تحميل الصفحة… إعادة المحاولة"
                : "⚠ لا يوجد اتصال بالإنترنت — بانتظار الشبكة…", 0L);
        handler.removeCallbacks(retryTask);
        handler.postDelayed(retryTask, RETRY_AFTER_ERROR_MS);
    }

    private static String desktopUserAgent(String ua) {
        if (ua == null) ua = "";
        String chrome = "120.0.0.0";
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("Chrome/([\\d.]+)").matcher(ua);
        if (m.find()) chrome = m.group(1);
        return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/" + chrome + " Safari/537.36";
    }

    // ------------------------------------------------------- Network helpers

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        return bos.toString("UTF-8");
    }

    private static String httpGet(String u) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        try {
            c.setConnectTimeout(15000);
            c.setReadTimeout(15000);
            c.setRequestProperty("User-Agent", "MawaqitHome/2.1");
            int code = c.getResponseCode();
            if (code != 200) throw new IOException("HTTP " + code);
            return readAll(c.getInputStream());
        } finally {
            c.disconnect();
        }
    }

    // ------------------------------------------------------------- Location

    private boolean manualLocation() {
        return "manual".equals(prefs.getString("loc_mode", "auto"));
    }

    private String currentCity() {
        String c = manualLocation() ? prefs.getString("city", "") : prefs.getString("ip_city", "");
        return c == null || c.isEmpty() ? DEFAULT_CITY : c;
    }

    private double[] resolveLocation() {
        try {
            if (manualLocation()) {
                return new double[]{Double.parseDouble(prefs.getString("lat", "")),
                        Double.parseDouble(prefs.getString("lon", ""))};
            }
            long age = System.currentTimeMillis() - prefs.getLong("ip_ts", 0L);
            if (!prefs.contains("ip_lat") || !prefs.getBoolean("ip_fix", false) || age > 12L * 60 * 60 * 1000) ipLookup();
            if (prefs.contains("ip_lat")) {
                return new double[]{Double.parseDouble(prefs.getString("ip_lat", "")),
                        Double.parseDouble(prefs.getString("ip_lon", ""))};
            }
        } catch (Exception e) {
            AppLog.error("LOC", "resolve failed", e);
        }
        return new double[]{DEFAULT_LAT, DEFAULT_LON};
    }

    /** IP services return ASCII ("Saarbrucken"); the geocoder gives the real spelling (ü ä ö ß). */
    private String properName(String name, double lat, double lon) {
        if (name == null || name.isEmpty()) return name;
        try {
            String u = "https://geocoding-api.open-meteo.com/v1/search?name="
                    + URLEncoder.encode(name, "UTF-8") + "&count=10&language=de&format=json";
            JSONArray r = new JSONObject(httpGet(u)).optJSONArray("results");
            double best = 1e9;
            String bn = name;
            for (int i = 0; r != null && i < r.length(); i++) {
                JSONObject o = r.getJSONObject(i);
                double d = Math.abs(o.getDouble("latitude") - lat) + Math.abs(o.getDouble("longitude") - lon);
                if (d < best) {
                    best = d;
                    bn = o.optString("name", name);
                }
            }
            if (best < 0.5) return bn;
        } catch (Exception e) {
            AppLog.info("LOC", "name lookup failed: " + e);
        }
        return name;
    }

    private void ipLookup() {
        String[] urls = {"https://ipwho.is/?fields=success,latitude,longitude,city",
                "https://ipapi.co/json/"};
        for (String u : urls) {
            try {
                JSONObject j = new JSONObject(httpGet(u));
                if (j.has("success") && !j.optBoolean("success", true)) continue;
                double lat = j.getDouble("latitude"), lon = j.getDouble("longitude");
                String city = properName(j.optString("city", ""), lat, lon);
                prefs.edit().putString("ip_lat", Double.toString(lat))
                        .putString("ip_lon", Double.toString(lon))
                        .putString("ip_city", city).putBoolean("ip_fix", true)
                        .putLong("ip_ts", System.currentTimeMillis()).apply();
                AppLog.info("LOC", "IP location: " + city + " " + lat + "," + lon);
                return;
            } catch (Exception e) {
                AppLog.info("LOC", "IP lookup failed (" + u + "): " + e);
            }
        }
    }

    // -------------------------------------------------------------- Weather

    private static String weatherIcon(int c, boolean day) {
        if (!day) {
            if (c == 0 || c == 1) return "🌙";
            if (c == 2) return "☁️";
        }
        return weatherIcon(c);
    }

    private static String weatherIcon(int c) {
        if (c == 0) return "☀️";
        if (c == 1) return "🌤";
        if (c == 2) return "⛅";
        if (c == 3) return "☁️";
        if (c == 45 || c == 48) return "🌫";
        if (c >= 51 && c <= 57) return "🌦";
        if (c >= 61 && c <= 67) return "🌧";
        if (c >= 71 && c <= 77) return "❄️";
        if (c >= 80 && c <= 82) return "🌦";
        if (c == 85 || c == 86) return "🌨";
        if (c >= 95) return "⛈";
        return "☁️";
    }

    private void fetchWeather() {
        handler.removeCallbacks(weatherTask);
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final WeatherData wd = loadWeather();
                    handler.post(new Runnable() {
                        @Override public void run() {
                            if (destroyed) return;
                            weather = wd;
                            weatherAt = System.currentTimeMillis();
                            weatherFailed = false;
                            updateWeatherUi();
                        }
                    });
                    if (!destroyed) handler.postDelayed(weatherTask, WEATHER_EVERY_MS);
                } catch (Exception e) {
                    AppLog.error("WEATHER", "fetch failed", e);
                    handler.post(new Runnable() {
                        @Override public void run() {
                            weatherFailed = true;
                            updateWeatherAge();
                        }
                    });
                    if (!destroyed) handler.postDelayed(weatherTask, WEATHER_RETRY_MS);
                }
            }
        }, "weather").start();
    }

    private WeatherData loadWeather() throws Exception {
        double[] loc = resolveLocation();
        String head = "https://api.open-meteo.com/v1/forecast?latitude=" + loc[0]
                + "&longitude=" + loc[1]
                + "&current=temperature_2m,relative_humidity_2m,weather_code";
        String tail = "&timezone=auto&forecast_days=3";
        String curX = ",apparent_temperature,wind_speed_10m,is_day";
        String basic = "weather_code,temperature_2m_max,temperature_2m_min";
        String body;
        try {
            body = httpGet(head + curX + "&daily=" + basic + ",relative_humidity_2m_mean,"
                    + "precipitation_probability_max,wind_speed_10m_max,uv_index_max,sunrise,sunset" + tail);
        } catch (IOException e) {
            try {
                body = httpGet(head + "&daily=" + basic + ",relative_humidity_2m_mean,"
                        + "precipitation_probability_max" + tail);
            } catch (IOException e2) {
                body = httpGet(head + "&daily=" + basic + tail);     // fall back to the basic fields
            }
        }
        JSONObject j = new JSONObject(body);
        WeatherData wd = new WeatherData();

        JSONObject cur = j.getJSONObject("current");
        wd.curTemp = Math.round(cur.getDouble("temperature_2m"));
        wd.curHum = Math.round(cur.getDouble("relative_humidity_2m"));
        boolean isDay = !cur.has("is_day") || cur.optInt("is_day", 1) == 1;
        wd.curIcon = weatherIcon(cur.getInt("weather_code"), isDay);
        wd.code = cur.getInt("weather_code");
        wd.phase = isDay ? 0 : 2;
        if (cur.has("apparent_temperature") && !cur.isNull("apparent_temperature"))
            wd.curFeels = Math.round(cur.getDouble("apparent_temperature"));
        if (cur.has("wind_speed_10m") && !cur.isNull("wind_speed_10m"))
            wd.curWind = Math.round(cur.getDouble("wind_speed_10m"));

        JSONObject d = j.getJSONObject("daily");
        JSONArray wc = d.getJSONArray("weather_code");
        JSONArray mx = d.getJSONArray("temperature_2m_max");
        JSONArray mn = d.getJSONArray("temperature_2m_min");
        JSONArray hm = d.optJSONArray("relative_humidity_2m_mean");
        JSONArray rn = d.optJSONArray("precipitation_probability_max");
        JSONArray tm = d.optJSONArray("time");
        JSONArray wn = d.optJSONArray("wind_speed_10m_max");
        JSONArray uvA = d.optJSONArray("uv_index_max");
        try {                                   // dusk/dawn = within 50 min of sunrise/sunset
            String now = cur.optString("time", "");
            JSONArray sr = d.optJSONArray("sunrise"), ss = d.optJSONArray("sunset");
            if (now.length() >= 16 && sr != null && ss != null) {
                int n = hm(now), r = hm(sr.getString(0)), t = hm(ss.getString(0));
                if (Math.abs(n - r) <= 50 || Math.abs(n - t) <= 50) wd.phase = 1;
                else wd.phase = n >= r && n < t ? 0 : 2;
            }
        } catch (Exception ignored) {
        }
        for (int i = 0; i < 3; i++) {
            Day day = new Day();
            day.icon = weatherIcon(wc.getInt(i));
            day.max = Math.round(mx.getDouble(i));
            day.min = Math.round(mn.getDouble(i));
            day.hum = (hm != null && !hm.isNull(i)) ? Math.round(hm.getDouble(i)) : -1;
            day.rain = (rn != null && !rn.isNull(i)) ? Math.round(rn.getDouble(i)) : -1;
            day.wind = (wn != null && !wn.isNull(i)) ? Math.round(wn.getDouble(i)) : -1;
            day.uv = (uvA != null && !uvA.isNull(i)) ? Math.round(uvA.getDouble(i)) : -1;
            if (tm != null) {
                String[] p = tm.getString(i).split("-");       // 2026-11-19
                if (p.length == 3) {
                    day.date = String.format(Locale.US, "%02d/%02d",
                            Integer.parseInt(p[2]), Integer.parseInt(p[1]));   // day/month
                }
            }
            wd.days[i] = day;
        }
        return wd;
    }

    private static int hm(String iso) {        // "2026-11-21T07:47" -> minutes of day
        return Integer.parseInt(iso.substring(11, 13)) * 60 + Integer.parseInt(iso.substring(14, 16));
    }

    private float unit() {
        int w = sizeW > 0 ? sizeW : getResources().getDisplayMetrics().widthPixels;
        return Math.max(0.6f, Math.min(2.4f, w / 1280f));
    }

    /** Weather-card pixel size: scales with the screen width and the "card size" setting. */
    private int wpad() {
        return tgt(prefs.getInt("w_pad", 5));
    }

    /** A gap, shrunk by the "tighten" setting (0-100%). */
    private int tgt(float v) {
        return Math.round(wsc(v) * (100 - Math.max(0, Math.min(100, prefs.getInt("w_tight", 40)))) / 100f);
    }

    /** Negative margin that trims the font's own leading above/below a line. */
    private int lead(TextView t) {
        return -Math.round(t.getTextSize() * 0.14f * Math.max(0, Math.min(100, prefs.getInt("w_tight", 40))) / 100f);
    }

    private int wsc(float v) {
        return Math.round(v * unit() * prefs.getInt("w_size", 100) / 100f);
    }

    private void setPx(TextView t, float size) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, wsc(size) * Math.max(60, Math.min(200, prefs.getInt("font_pct", 100)))
                / 100f * Math.max(50, Math.min(250, prefs.getInt("w_font", 100))) / 100f);
    }

    private void buildWeatherCard() {
        weatherCard = new LinearLayout(this);
        weatherCard.setOrientation(LinearLayout.VERTICAL);
        weatherCard.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        weatherCity = text(12, true);
        weatherCity.setTextColor(0xFFF4C95D);
        weatherCity.setTextDirection(View.TEXT_DIRECTION_RTL);
        weatherCity.setGravity(Gravity.CENTER);
        weatherCity.setSingleLine(true);
        weatherCity.setEllipsize(null);
        weatherCard.addView(weatherCity, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        wSep = new View(this);
        wSep.setBackgroundColor(0x22FFFFFF);
        weatherCard.addView(wSep, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));

        daysBox = new LinearLayout(this);
        weatherCard.addView(daysBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        for (int i = 0; i < 3; i++) {
            LinearLayout row = new LinearLayout(this);
            dayView[i] = row;
            row.setOrientation(LinearLayout.VERTICAL);
            LinearLayout main = new LinearLayout(this);
            main.setOrientation(LinearLayout.HORIZONTAL);

            wDay[i] = text(12, true);
            wDay[i].setTextDirection(View.TEXT_DIRECTION_RTL);
            wIcon[i] = text(26, false);
            wIcon[i].setGravity(Gravity.CENTER);
            wIcon[i].setText("—");
            wTemp[i] = text(15, true);
            wTemp[i].setGravity(Gravity.CENTER);
            wTemp[i].setText("--°");
            wSub[i] = text(10, false);
            wSub[i].setTextColor(0xE6FFFFFF);
            wSub[i].setTextDirection(View.TEXT_DIRECTION_RTL);
            wSub[i].setText("💧 —");

            main.addView(wDay[i]);
            main.addView(wIcon[i]);
            main.addView(wTemp[i]);
            row.addView(main, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            row.addView(wSub[i], new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            daysBox.addView(row);
        }

        weatherAge = text(10, false);
        weatherAge.setTextColor(0x99FFFFFF);
        weatherAge.setTextDirection(View.TEXT_DIRECTION_RTL);
        weatherAge.setGravity(Gravity.CENTER);
        weatherAge.setText("جارٍ التحميل…");
        weatherCard.addView(weatherAge, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        shiftLayer.addView(weatherCard, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.LEFT));
        layoutWeather();
        applyWeatherStyle();
        updateWeatherHeader();
    }

    /** Sizes every part of the card from the screen width, so it stays a small tidy rectangle. */
    private void layoutWeather() {
        weatherCard.setPadding(wpad(), wpad() * 3 / 5, wpad(), wpad() / 2);
        setPx(weatherCity, 12);
        setPx(weatherAge, 10);
        LinearLayout.LayoutParams cl0 = (LinearLayout.LayoutParams) weatherCity.getLayoutParams();
        cl0.topMargin = cl0.bottomMargin = lead(weatherCity);
        weatherCity.setLayoutParams(cl0);
        LinearLayout.LayoutParams al0 = (LinearLayout.LayoutParams) weatherAge.getLayoutParams();
        al0.topMargin = al0.bottomMargin = lead(weatherAge);
        weatherAge.setLayoutParams(al0);
        LinearLayout.LayoutParams sp = (LinearLayout.LayoutParams) wSep.getLayoutParams();
        sp.height = Math.max(1, wsc(1));
        sp.topMargin = tgt(5);
        sp.bottomMargin = tgt(3);
        wSep.setLayoutParams(sp);
        final boolean cols = prefs.getInt("w_layout", 0) == 1;
        final boolean oneLine = !cols && prefs.getInt("w_lines", 1) == 1;
        final int gap = tgt(prefs.getInt("w_gap", 2));
        wHead = 0;
        if (prefs.getBoolean("w_city", true))
            wHead = Math.max(wHead, (int) Math.ceil(Layout.getDesiredWidth(weatherCity.getText(), weatherCity.getPaint())));
        if (prefs.getBoolean("w_age", true))
            wHead = Math.max(wHead, (int) Math.ceil(Layout.getDesiredWidth(weatherAge.getText(), weatherAge.getPaint())));
        {                                              // measure real text so columns hug their content
            int dW = 0, iW = 0, tW = 0, subW = 0;
            for (int i = 0; i < 3; i++) {
                setPx(wDay[i], 12);
                setPx(wTemp[i], 15);
                setPx(wSub[i], 10);
                setPx(wIcon[i], 26);
                if (i >= Math.max(1, Math.min(3, prefs.getInt("w_days", 3)))) continue;
                dW = Math.max(dW, (int) Math.ceil(Layout.getDesiredWidth(wDay[i].getText(), wDay[i].getPaint())));
                iW = Math.max(iW, (int) Math.ceil(Layout.getDesiredWidth(wIcon[i].getText(), wIcon[i].getPaint())));
                tW = Math.max(tW, (int) Math.ceil(Layout.getDesiredWidth(wTemp[i].getText(), wTemp[i].getPaint())));
                if (wSub[i].getVisibility() != View.GONE) subW = Math.max(subW, Math.min(cols ? wsc(120) : wsc(230),
                        (int) Math.ceil(Layout.getDesiredWidth(wSub[i].getText(), wSub[i].getPaint()))));
            }
            wRowSub = subW;
            wRowDay = dW > 0 ? dW + 3 : wsc(80);
            wRowIcon = iW > 0 ? iW + 3 : wsc(34);
            wRowTemp = tW > 0 ? tW + 3 : wsc(80);
            wColW = Math.max(Math.max(wRowDay, wRowIcon), Math.max(wRowTemp, subW + 3));
        }
        final int nd = Math.max(1, Math.min(3, prefs.getInt("w_days", 3)));
        weatherCity.setVisibility(prefs.getBoolean("w_city", true) ? View.VISIBLE : View.GONE);
        weatherAge.setVisibility(prefs.getBoolean("w_age", true) ? View.VISIBLE : View.GONE);
        wSep.setVisibility(prefs.getBoolean("w_city", true) && prefs.getBoolean("w_line", true)
                ? View.VISIBLE : View.GONE);
        weatherCard.setLayoutDirection(prefs.getInt("w_flip", 0) == 1
                ? View.LAYOUT_DIRECTION_LTR : View.LAYOUT_DIRECTION_RTL);
        daysBox.setOrientation(cols ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        for (int i = 0; i < 3; i++) {
            LinearLayout row = dayView[i];
            LinearLayout main = (LinearLayout) row.getChildAt(0);
            row.setVisibility(i < nd ? View.VISIBLE : View.GONE);
            row.setOrientation(oneLine ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
            row.setBaselineAligned(false);
            row.setGravity(cols ? Gravity.CENTER_HORIZONTAL : (oneLine ? Gravity.CENTER_VERTICAL : Gravity.LEFT));
            int vp = tgt(prefs.getInt("w_rowgap", 1));
            row.setPadding(cols ? tgt(2) : 0, vp, cols ? tgt(2) : 0, vp);
            row.setLayoutParams(cols ? new LinearLayout.LayoutParams(wColW + tgt(4),
                    ViewGroup.LayoutParams.WRAP_CONTENT) : new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            main.setOrientation(cols ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
            main.setBaselineAligned(true);                 // day, icon and temperature share one baseline
            main.setGravity(cols ? Gravity.CENTER_HORIZONTAL : Gravity.NO_GRAVITY);
            setPx(wDay[i], 12);
            setPx(wIcon[i], 26);
            setPx(wTemp[i], 15);
            setPx(wSub[i], 10);
            int al = prefs.getInt("w_align", 0);            // 0 right, 1 centre, 2 left (day text)
            wDay[i].setGravity(cols ? Gravity.CENTER : (al == 1 ? Gravity.CENTER
                    : al == 2 ? Gravity.LEFT : Gravity.RIGHT));
            wTemp[i].setGravity(Gravity.CENTER);
            wDay[i].setIncludeFontPadding(false);
            wIcon[i].setIncludeFontPadding(false);
            wTemp[i].setIncludeFontPadding(false);
            wSub[i].setIncludeFontPadding(false);
            wSub[i].setSingleLine(false);
            wSub[i].setMaxLines(oneLine ? 1 : 3);
            wSub[i].setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams dl = cols ? new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    : mlp(wRowDay, 0, gap);
            LinearLayout.LayoutParams il = cols ? new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    : mlp(wRowIcon, 0, gap);
            LinearLayout.LayoutParams tl = cols ? new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    : new LinearLayout.LayoutParams(wRowTemp, ViewGroup.LayoutParams.WRAP_CONTENT);
            dl.topMargin = dl.bottomMargin = lead(wDay[i]);
            il.topMargin = il.bottomMargin = lead(wIcon[i]);
            tl.topMargin = tl.bottomMargin = lead(wTemp[i]);
            wDay[i].setLayoutParams(dl);
            wIcon[i].setLayoutParams(il);
            wTemp[i].setLayoutParams(tl);
            LinearLayout.LayoutParams sl2 = oneLine ? new LinearLayout.LayoutParams(
                    Math.max(1, wRowSub), ViewGroup.LayoutParams.WRAP_CONTENT) : new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (oneLine) sl2.leftMargin = sl2.rightMargin = gap;
            sl2.topMargin = oneLine ? 0 : tgt(1);
            sl2.bottomMargin = lead(wSub[i]);
            wSub[i].setLayoutParams(sl2);
        }
        FrameLayout.LayoutParams clp = (FrameLayout.LayoutParams) weatherCard.getLayoutParams();
        clp.width = weatherCardWidth();
        clp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        clp.gravity = Gravity.TOP | Gravity.LEFT;
        clp.setMargins(0, 0, 0, 0);
        weatherCard.setLayoutParams(clp);
        applyWeatherStyle();
        positionWeather();
    }

    /** Fixed width = padding + day column + icon + temperature column (never depends on text). */
    private int weatherCardWidth() {
        int nd = Math.max(1, Math.min(3, prefs.getInt("w_days", 3)));
        if (prefs.getInt("w_layout", 0) == 1) return 2 * wpad() + nd * (wColW + tgt(4)) + 2;
        int mainW = (wRowDay > 0 ? wRowDay : wsc(80)) + (wRowIcon > 0 ? wRowIcon : wsc(34))
                + (wRowTemp > 0 ? wRowTemp : wsc(80)) + 2 * tgt(prefs.getInt("w_gap", 2));
        boolean one = prefs.getInt("w_lines", 1) == 1;
        int body = one ? mainW + (wRowSub > 0 ? wRowSub + 2 * tgt(prefs.getInt("w_gap", 2)) + 4 : 0)
                : Math.max(mainW, wRowSub + 4);
        return 2 * wpad() + Math.max(body, wHead + 4) + 2;
    }

    private int wRowDay, wRowIcon, wRowTemp, wRowSub, wHead, wColW;

    private LinearLayout.LayoutParams mlp(int w, int l, int r) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(l, 0, r, 0);
        return p;
    }

    /**
     * Places the card using the saved position (0-100% of the free space on each axis).
     * Uses translation, so the position can never be lost to layout-parameter quirks.
     */
    private void positionWeather() {
        if (sizeW <= 0 || weatherCard == null) return;
        int cw = weatherCardWidth();
        weatherCard.measure(View.MeasureSpec.makeMeasureSpec(cw, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(sizeH, View.MeasureSpec.AT_MOST));
        int ch = weatherCard.getMeasuredHeight();
        int inset = wsc(14);
        int region = topH > 0 ? topH : sizeH;
        int availW = Math.max(0, sizeW - cw - 2 * inset);
        int availH = Math.max(0, region - ch - 2 * inset);
        float tx = inset + availW * prefs.getInt("w_x", 100) / 100f;
        float ty = inset + availH * prefs.getInt("w_y", 0) / 100f;
        weatherCard.setTranslationX(tx);
        weatherCard.setTranslationY(ty);
        AppLog.info("WEATHER", "card " + cw + "x" + ch + " at " + Math.round(tx) + "," + Math.round(ty)
                + " (screen " + sizeW + "x" + sizeH + ")");
    }

    private void applyWeatherStyle() {
        int a = Math.max(0, Math.min(100, prefs.getInt("w_alpha", 55)));
        int c0 = paletteColors()[0], c1 = paletteColors()[1];
        if (prefs.getBoolean("w_tod", true) && weather != null) {
            if (weather.phase == 0) { c0 = 0xFF2B7BB9; c1 = 0xFF6DB6E6; }
            else if (weather.phase == 1) { c0 = 0xFF6E4A8E; c1 = 0xFFE8955A; }
            else { c0 = 0xFF0B1B3F; c1 = 0xFF1B2F63; }
        }
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{((a * 255 / 100) << 24) | (c0 & 0xFFFFFF),
                        ((a * 255 / 100) << 24) | (c1 & 0xFFFFFF)});
        int bw = prefs.getInt("w_border", 1);
        if (bw > 0) bg.setStroke(Math.max(1, dp(bw)), argb(a * 90 / 100, 0xF4, 0xC9, 0x5D));
        bg.setCornerRadius(wsc(prefs.getInt("w_radius", 16)));
        weatherCard.setBackground(bg);
        weatherCard.setVisibility(prefs.getBoolean("w_show", true) ? View.VISIBLE : View.GONE);
    }

    private static int argb(int a, int r, int g, int b) {
        return (Math.max(0, Math.min(255, a)) << 24) | (r << 16) | (g << 8) | b;
    }

    private void updateWeatherHeader() {
        weatherCity.setText("📍 " + (manualLocation() ? "الموقع: " : "مدينتك الحالية: ")
                + currentCity());
    }

    /** "اليوم: 11/19" - the date is lighter and a little smaller. */
    private CharSequence dayLabel(String name, String date) {
        String full = date.isEmpty() ? name : name + ": " + date;
        SpannableString sp = new SpannableString(full);
        if (!date.isEmpty()) {
            int st = name.length() + 1;
            sp.setSpan(new ForegroundColorSpan(0xCCFFFFFF), st, full.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sp.setSpan(new AbsoluteSizeSpan(wsc(11)), st, full.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sp;
    }

    private void updateWeatherUi() {
        if (weather == null) return;
        updateWeatherHeader();
        applyWeatherStyle();
        String[] names = {"اليوم", "غداً", "بعد غد"};
        String sep = prefs.getInt("w_layout", 0) == 1 ? "\n" : "  ";
        boolean fh = prefs.getBoolean("w_hum", true), fr = prefs.getBoolean("w_rain", true),
                fw = prefs.getBoolean("w_wind", false), ff = prefs.getBoolean("w_feels", true),
                fu = prefs.getBoolean("w_uv", false);
        for (int i = 0; i < 3; i++) {
            Day d = weather.days[i];
            wDay[i].setText(dayLabel(names[i], d.date));
            StringBuilder sb = new StringBuilder();
            long hum = i == 0 ? weather.curHum : d.hum, wind = i == 0 && weather.curWind >= 0
                    ? weather.curWind : d.wind;
            if (fh) sb.append("💧 ").append(hum >= 0 ? hum + "%" : "—");
            if (fr && d.rain >= 0) sb.append(sb.length() > 0 ? sep : "").append("🌧 ").append(d.rain).append("%");
            if (fw && wind >= 0) sb.append(sb.length() > 0 ? sep : "").append("💨 ").append(wind).append(" km/h");
            if (fu && d.uv >= 0) sb.append(sb.length() > 0 ? sep : "").append("☀ UV ").append(d.uv);
            if (ff && i == 0 && weather.curFeels > -900)
                sb.append(sb.length() > 0 ? sep : "").append("🌡 يُحسّ ").append(weather.curFeels).append("°");
            wSub[i].setText(sb);
            wSub[i].setVisibility(sb.length() == 0 ? View.GONE : View.VISIBLE);
            if (i == 0) {
                wIcon[i].setText(weather.curIcon);
                wTemp[i].setText(weather.curTemp + "°");
            } else {
                wIcon[i].setText(d.icon);
                wTemp[i].setText(d.max + "° / " + d.min + "°");
            }
        }
        layoutWeather();
        updateWeatherAge();
        positionWeather();
    }

    private void updateWeatherAge() {
        if (weatherAt == 0L) {
            weatherAge.setText(weatherFailed ? "⚠ تعذّر التحميل" : "جارٍ التحميل…");
            return;
        }
        long min = (System.currentTimeMillis() - weatherAt) / 60000L;
        String s = min < 1 ? "آخر تحديث: الآن" : "آخر تحديث: منذ " + min + " د";
        if (weatherFailed) s += "  ⚠";
        weatherAge.setText(s);
    }

    private void startWeatherMove() {
        wMove = true;
        if (railOpen) closeRail(false);
        showBubble("↕↔ حرّك بطاقة الطقس بالأسهم • OK للتثبيت", 0L);
    }

    private void endWeatherMove() {
        wMove = false;
        hideBubble();
        toast("تم حفظ موضع البطاقة");
    }

    private void adjustWeather(int dx, int dy) {
        int x = Math.max(0, Math.min(100, prefs.getInt("w_x", 100) + dx));
        int y = Math.max(0, Math.min(100, prefs.getInt("w_y", 0) + dy));
        prefs.edit().putInt("w_x", x).putInt("w_y", y).apply();
        positionWeather();
    }

    // --------------------------------------------------------- Prayer times

    private void extractPrayerTimes() {
        if (destroyed) return;
        handler.removeCallbacks(prayerTask);
        handler.postDelayed(prayerTask, PRAYER_EVERY_MS);
        webView.evaluateJavascript(JS_TIMES, new ValueCallback<String>() {
            @Override public void onReceiveValue(String v) {
                int[] t = parseTimesFromPage(v);
                if (t != null) setPrayerTimes(t, "صفحة Mawaqit (confData)");
                else {
                    AppLog.info("PRAYER", "page times unavailable (" + v + ") -> fallback");
                    fetchAladhan();
                }
            }
        });
    }

    private static int parseHm(String s) {
        if (s == null) return -1;
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("^\\s*(\\d{1,2}):(\\d{2})").matcher(s);
        if (!m.find()) return -1;
        int h = Integer.parseInt(m.group(1)), mi = Integer.parseInt(m.group(2));
        if (h > 23 || mi > 59) return -1;
        return h * 60 + mi;
    }

    private static int[] parseTimesFromPage(String v) {
        try {
            if (v == null || v.equals("null")) return null;
            Object o = new JSONTokener(v).nextValue();
            if (!(o instanceof String)) return null;
            JSONArray a = new JSONArray((String) o);
            int[] idx;
            if (a.length() == 6) idx = new int[]{0, 2, 3, 4, 5};
            else if (a.length() == 5) idx = new int[]{0, 1, 2, 3, 4};
            else return null;
            int[] t = new int[5];
            for (int i = 0; i < 5; i++) {
                t[i] = parseHm(a.getString(idx[i]));
                if (t[i] < 0) return null;
            }
            for (int i = 1; i < 5; i++) if (t[i] <= t[i - 1]) return null;
            return t;
        } catch (Exception e) {
            return null;
        }
    }

    private void fetchAladhan() {
        new Thread(new Runnable() {
            @Override public void run() {
                int[] t = null;
                try {
                    double[] loc = resolveLocation();
                    String day = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
                    String u = "https://api.aladhan.com/v1/timings/" + day
                            + "?latitude=" + loc[0] + "&longitude=" + loc[1] + "&method=13";
                    JSONObject tm = new JSONObject(httpGet(u))
                            .getJSONObject("data").getJSONObject("timings");
                    String[] keys = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
                    int[] r = new int[5];
                    for (int i = 0; i < 5; i++) {
                        r[i] = parseHm(tm.getString(keys[i]));
                        if (r[i] < 0) throw new IOException("bad time");
                    }
                    t = r;
                } catch (Exception e) {
                    AppLog.error("PRAYER", "Aladhan fallback failed", e);
                }
                final int[] result = t;
                handler.post(new Runnable() {
                    @Override public void run() {
                        if (destroyed) return;
                        if (result != null) setPrayerTimes(result, "Aladhan (Diyanet) — بديل");
                        else {
                            handler.removeCallbacks(prayerTask);
                            handler.postDelayed(prayerTask, PRAYER_RETRY_MS);
                        }
                    }
                });
            }
        }, "aladhan").start();
    }

    private void setPrayerTimes(int[] t, String source) {
        WakeScheduler.save(prefs, t, source);
        prefs.edit().putLong("prayers_ts", System.currentTimeMillis()).apply();
        long next = WakeScheduler.scheduleNext(this);
        AppLog.info("PRAYER", source + ": " + hm(t[0]) + " " + hm(t[1]) + " " + hm(t[2]) + " "
                + hm(t[3]) + " " + hm(t[4]) + " | next wake alarm=" + next);
    }

    private static String hm(int m) {
        return String.format(Locale.US, "%02d:%02d", m / 60, m % 60);
    }

    private void showPrayerDiagnostics() {
        int[] t = WakeScheduler.load(prefs);
        StringBuilder sb = new StringBuilder();
        if (t == null) {
            sb.append("لم تُقرأ أوقات الصلاة بعد.\nانتظر دقيقة بعد فتح الصفحة، أو اضغط «تحديث الصفحة».");
        } else {
            sb.append("المصدر: ").append(prefs.getString("prayers_src", "—")).append("\n");
            long ts = prefs.getLong("prayers_ts", 0L);
            if (ts > 0) {
                sb.append("آخر قراءة: ").append(TimeCfg.dateTime(prefs, ts))
                        .append("  (تتجدد كل ساعة)\n");
            }
            sb.append("\n");
            for (int i = 0; i < 5; i++) {
                sb.append(PRAYER_NAMES[i]).append("  ").append(TimeCfg.hm(prefs, t[i])).append("\n");
            }
            long nw = prefs.getLong("next_wake", 0L);
            sb.append("\nالإيقاظ القادم: ");
            if (nw > 0) {
                sb.append(TimeCfg.dateTime(prefs, nw));
            } else {
                sb.append("غير مجدول");
            }
            sb.append("  (").append(WakeScheduler.LEAD_MIN).append(" د قبل الأذان)");
        }
        dialog("أوقات الصلاة والإيقاظ").setMessage(sb.toString())
                .setPositiveButton("حسناً", null).show();
    }

    // ------------------------------------------------- Sleep / admin / ADB

    private boolean adminActive() {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            return dpm != null && dpm.isAdminActive(new ComponentName(this, AdminReceiver.class));
        } catch (Exception e) {
            return false;
        }
    }

    private void sleepDevice() {
        sleepTried = true;
        if (WakeScheduler.scheduleNext(this) == 0L) {
            AppLog.info("SLEEP", "skipped: prayer times unknown, so no wake-up alarm");
            return;
        }
        if (adminActive()) {
            try {
                AppLog.info("SLEEP", "going to sleep (device admin lockNow)");
                ((DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE)).lockNow();
                return;
            } catch (Exception e) {
                AppLog.error("SLEEP", "lockNow failed", e);
            }
        }
        if (AdbShell.blocked(this)) {
            AppLog.info("SLEEP", "no ADB and no device-admin permission: cannot sleep");
            return;
        }
        AppLog.info("SLEEP", "going to sleep (adb keyevent 223)");
        AdbShell.run(this, "input keyevent 223", new AdbShell.Callback() {
            @Override public void done(boolean ok, String output) {
                if (!ok) AppLog.info("SLEEP", "adb failed: " + output);
            }
        });
    }

    // ---- background pictures / short videos from the internet

    private boolean bgBusy;
    private int videoErrors;
    private long bgRetryAt;

    private File bgFile() {
        return Backgrounds.file(this, prefs.getInt("bg_slot", 0));
    }

    private boolean bgVideo() {
        return prefs.getInt("bg_type", 0) == 1;
    }

    private int bgSources() {
        return prefs.getInt("bg_src", 0);
    }

    private int bgVSrc() {
        return prefs.getInt("bg_vsrc", 0);
    }

    private boolean bgEnabled() {
        return bgVideo() ? bgVSrc() != 0 : bgSources() != 0;
    }

    private final Runnable bgTask = new Runnable() {
        @Override public void run() {
            handler.removeCallbacks(this);
            handler.postDelayed(this, 20_000L);
            if (!bgEnabled() || bgBusy || !online() || System.currentTimeMillis() < bgRetryAt) return;
            if (bgDue() || smartChanged()) bgFetch(false);
        }
    };

    // ---- smart background: theme chosen from prayer window / Friday / weather / time of day / season
    private String bgPendingKey = "";

    private boolean smartOn() {
        return prefs.getInt("bg_sm", 0) != 0;
    }

    /** {key, image query, video query} or null when nothing special applies. */
    private String[] smartTheme() {
        int m = prefs.getInt("bg_sm", 0);
        if (m == 0) return null;
        java.util.Random r = new java.util.Random();
        java.util.Calendar c = java.util.Calendar.getInstance();
        int now = c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE);
        if ((m & 8) != 0) {
            int[] t = WakeScheduler.load(prefs);
            if (t != null) for (int pm : t)
                if (now >= pm - 15 && now <= pm + 3) {
                    String[] iq = {"mosque interior", "mihrab", "mosque prayer hall", "minaret mosque sky"};
                    return new String[]{"prayer", iq[r.nextInt(iq.length)], "mosque"};
                }
        }
        if ((m & 4) != 0 && c.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.FRIDAY) {
            boolean mk = r.nextBoolean();
            return new String[]{"friday", mk ? "Kaaba Mecca" : "Masjid al-Nabawi Medina",
                    mk ? "kaaba mecca" : "medina mosque"};
        }
        WeatherData w = weather;
        if ((m & 2) != 0 && w != null) {
            int cd = w.code;
            if (cd == 71 || cd == 73 || cd == 75 || cd == 77 || cd == 85 || cd == 86)
                return new String[]{"snow", "snow winter landscape", "snow falling"};
            if ((cd >= 51 && cd <= 67) || (cd >= 80 && cd <= 82))
                return new String[]{"rain", "rain drops nature", "rain"};
            if (cd >= 95) return new String[]{"storm", "thunderstorm lightning", "thunderstorm"};
            if (cd == 45 || cd == 48) return new String[]{"fog", "fog mist landscape", "fog"};
            if (w.curTemp >= 32) return new String[]{"hot", "summer sun desert heat", "summer sun"};
            if (w.curTemp <= 0) return new String[]{"frost", "frost winter landscape", "winter landscape"};
        }
        if ((m & 1) != 0) {
            int ph = w != null ? w.phase : (now >= 360 && now < 1080 ? 0 : 2);
            int mo = c.get(java.util.Calendar.MONTH);                    // 0..11
            String season = mo == 11 || mo <= 1 ? "winter" : mo <= 4 ? "spring" : mo <= 7 ? "summer" : "autumn";
            if (ph == 2) {
                String[] iq = {"night sky stars", "city night lights", "moon night sky"};
                return new String[]{"night", iq[r.nextInt(iq.length)], "night sky stars"};
            }
            if (ph == 1) return new String[]{"dusk", "sunset sky", "sunset"};
            String iq = season.equals("winter") ? "winter landscape sunny" : season.equals("spring")
                    ? "spring blossom nature" : season.equals("summer") ? "summer meadow blue sky" : "autumn forest";
            String vq = season.equals("winter") ? "winter landscape" : season.equals("spring") ? "spring flowers"
                    : season.equals("summer") ? "summer beach" : "autumn forest";
            return new String[]{"day-" + season, iq, vq};
        }
        return null;
    }

    private boolean smartChanged() {
        if (!smartOn()) return false;
        long last = prefs.getLong("bg_last", 0);
        if (System.currentTimeMillis() - last < 3 * 60_000L) return false;
        String[] t = smartTheme();
        String key = t == null ? "" : t[0];
        return !key.equals(prefs.getString("bg_smartkey", ""));
    }

    private static final int[] BG_UNIT_MIN = {1, 60, 1440};
    private static final String[] BG_UNIT_NAMES = {"دقائق", "ساعات", "أيام"};

    private int bgMinutes() {
        if (!prefs.contains("bg_every")) return prefs.getInt("bg_min", 360);
        return Math.max(1, prefs.getInt("bg_every", 6)) * BG_UNIT_MIN[Math.max(0, Math.min(2, prefs.getInt("bg_unit", 1)))];
    }

    private String bgEveryText() {
        if (prefs.getInt("bg_mode", 0) == 1) return "يومياً الساعة " + prefs.getInt("bg_hour", 4) + ":00";
        int unit = Math.max(0, Math.min(2, prefs.getInt("bg_unit", 1)));
        if (!prefs.contains("bg_every")) return "كل " + (bgMinutes() / 60) + " ساعة";
        return "كل " + prefs.getInt("bg_every", 6) + " " + BG_UNIT_NAMES[unit];
    }

    private boolean bgDue() {
        long now = System.currentTimeMillis(), last = prefs.getLong("bg_last", 0);
        boolean have = bgVideo() ? !prefs.getString("bg_vurl", "").isEmpty() : bgFile().exists();
        if (!have || last == 0) return true;
        if (prefs.getInt("bg_mode", 0) == 0) return now - last >= bgMinutes() * 60_000L;
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.HOUR_OF_DAY, Math.max(0, Math.min(23, prefs.getInt("bg_hour", 4))));
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        return now >= c.getTimeInMillis() && last < c.getTimeInMillis();
    }

    private void bgDone(boolean manual, boolean ok, String credit, String err) {
        runOnUiThread(() -> {
            bgBusy = false;
            if (destroyed) return;
            if (ok) {
                prefs.edit().putLong("bg_last", System.currentTimeMillis())
                        .putString("bg_smartkey", bgPendingKey)
                        .putString("bg_credit", credit == null ? "" : credit).apply();
                AppLog.info("BG", "new " + (bgVideo() ? "video" : "picture") + ": " + credit);
                applyPageCss();
                if (manual) toast("تم تغيير الخلفية ✓");
            } else {
                bgRetryAt = System.currentTimeMillis() + 2 * 60_000L;
                AppLog.info("BG", "failed: " + err);
                if (manual) toast("تعذّر التحميل: " + err);
            }
        });
    }

    private void bgFetch(final boolean manual) {
        if (bgBusy) return;
        bgBusy = true;
        if (manual) toast("جارٍ تحميل خلفية جديدة…");
        Backgrounds.quality = prefs.getInt("bg_q", 0);
        String[] th = smartOn() ? smartTheme() : null;
        Backgrounds.smartQ = th == null ? null : th[1];
        bgPendingKey = th == null ? "" : th[0];
        if (th != null) AppLog.info("BG", "smart theme " + th[0] + " → " + (bgVideo() ? th[2] : th[1]));
        if (bgVideo()) {
            Backgrounds.fetchVideo(bgVSrc(), prefs.getString("bg_vlinks", ""), prefs.getString("bg_pexels", ""),
                    prefs.getString("bg_pixabay", ""), th != null ? th[2] : prefs.getString("bg_topic", "nature landscape"), (ok, url, credit, err) -> {
                        if (ok) prefs.edit().putString("bg_vurl", url).apply();
                        bgDone(manual, ok, credit, err);
                    });
        } else {
            Backgrounds.fetch(this, bgSources(), prefs.getString("bg_links", ""),
                    prefs.getString("bg_pixabay", ""), prefs.getString("bg_pexels", ""),
                    1 - prefs.getInt("bg_slot", 0), (ok, credit, err) -> {
                        if (ok) prefs.edit().putInt("bg_slot", 1 - prefs.getInt("bg_slot", 0)).apply();
                        bgDone(manual, ok, credit, err);
                    });
        }
    }

    private static final String[] TOPIC_NAMES = {"طبيعة ومناظر", "غيوم وسماء", "بحر وماء", "غابات وجبال",
            "مدن ليلاً", "نجوم وفضاء", "صحراء", "شلالات وأنهار", "🕌 إسلامي متنوع", "🕌 مساجد",
            "🕋 الكعبة ومكة", "🕌 المدينة المنورة", "🌙 رمضان والهلال"};
    private static final String[] TOPIC_Q = {"nature landscape", "clouds sky timelapse", "ocean sea waves",
            "forest mountains", "city night", "stars night sky milky way", "desert dunes", "waterfall river",
            "islamic mix", "mosque", "kaaba mecca", "medina mosque", "ramadan crescent moon"};

    private void showBgSettings() {
        final List<String> l = new ArrayList<>();
        final List<Runnable> a = new ArrayList<>();
        final boolean vid = bgVideo();
        l.add("🎞  نوع الخلفية:  " + (vid ? "فيديو قصير متحرك" : "صور ثابتة"));
        a.add(() -> choice("نوع الخلفية", new String[]{"صور ثابتة (Bing / ويكيميديا / روابطي)",
                "فيديو قصير متحرك (ويكيميديا / Pexels / روابطي)"}, vid ? 1 : 0, i -> {
            prefs.edit().putInt("bg_type", i).apply();
            applyPageCss();
            showBgSettings();
        }, this::showBgSettings));
        if (!vid) {
            final int src = bgSources();
            StringBuilder sn = new StringBuilder();
            if ((src & Backgrounds.BING) != 0) sn.append("Bing ");
            if ((src & Backgrounds.WIKI) != 0) sn.append("ويكيميديا ");
            if ((src & Backgrounds.OWN) != 0) sn.append("روابطي ");
            if ((src & Backgrounds.ISLAMIC) != 0) sn.append("إسلامية(ويكيميديا) ");
            if ((src & Backgrounds.PIX_IS) != 0) sn.append("إسلامية(Pixabay) ");
            if ((src & Backgrounds.PEX_IS) != 0) sn.append("إسلامية(Pexels) ");
            l.add("🌐  المصادر:  " + (src == 0 ? "متوقفة (خلفية Mawaqit الأصلية)" : sn.toString().trim()));
            a.add(() -> {
                final int[] bits = {1, 2, 8, 16, 32, 4};
                final boolean[] on = new boolean[bits.length];
                for (int i = 0; i < bits.length; i++) on[i] = (src & bits[i]) != 0;
                dialog("مصادر الصور (اختر واحداً أو أكثر)")
                        .setMultiChoiceItems(new String[]{"Bing — صورة اليوم (مناظر عالمية)",
                                "Wikimedia Commons — مناظر مميزة", "🕌 إسلامية — Wikimedia (مساجد، كعبة، عمارة، زخارف)",
                                "🕌 إسلامية — Pixabay (يحتاج المفتاح)", "🕌 إسلامية — Pexels (يحتاج المفتاح)",
                                "روابطي الخاصة"}, on, null)
                        .setPositiveButton("حفظ", (d, w) -> {
                            int v = 0;
                            for (int i = 0; i < bits.length; i++) if (on[i]) v |= bits[i];
                            prefs.edit().putInt("bg_src", v).apply();
                            if (v == 0) applyPageCss();
                            else if (!bgFile().exists()) bgFetch(true);
                            showBgSettings();
                        })
                        .setNegativeButton("إلغاء", (d, w) -> showBgSettings()).show();
            });
            final String links = prefs.getString("bg_links", "");
            l.add("🔗  روابطي الخاصة:  " + Backgrounds.links(links).size() + " رابط  — أضفها من الجوال");
            a.add(this::showLinkInput);
        } else {
            final int vs = bgVSrc();
            StringBuilder sn = new StringBuilder();
            if ((vs & Backgrounds.V_WIKI) != 0) sn.append("ويكيميديا ");
            if ((vs & Backgrounds.V_PEXELS) != 0) sn.append("Pexels ");
            if ((vs & Backgrounds.V_PIXABAY) != 0) sn.append("Pixabay ");
            if ((vs & Backgrounds.V_OWN) != 0) sn.append("روابطي ");
            l.add("🌐  مصادر الفيديو:  " + (vs == 0 ? "متوقفة" : sn.toString().trim()));
            a.add(() -> {
                final boolean[] on = {(vs & 1) != 0, (vs & 2) != 0, (vs & 8) != 0, (vs & 4) != 0};
                dialog("مصادر الفيديو (اختر واحداً أو أكثر)")
                        .setMultiChoiceItems(new String[]{"Wikimedia Commons — بلا مفتاح",
                                "Pexels — مجاني (يحتاج مفتاح API مجاني)", "Pixabay — مجاني (يحتاج مفتاح API مجاني)",
                                "روابطي الخاصة (mp4 / webm)"}, on, null)
                        .setPositiveButton("حفظ", (d, w) -> {
                            int v = (on[0] ? 1 : 0) | (on[1] ? 2 : 0) | (on[2] ? 8 : 0) | (on[3] ? 4 : 0);
                            prefs.edit().putInt("bg_vsrc", v).apply();
                            if (v == 0) applyPageCss();
                            else if (prefs.getString("bg_vurl", "").isEmpty()) bgFetch(true);
                            showBgSettings();
                        })
                        .setNegativeButton("إلغاء", (d, w) -> showBgSettings()).show();
            });
            final String key = prefs.getString("bg_pexels", "");
            l.add("🔑  مفتاح Pexels:  " + (key.length() > 8 ? "محفوظ ✓" : "غير مُدخل (من pexels.com/api مجاناً)"));
            a.add(() -> promptText("مفتاح Pexels API", "الصق المفتاح هنا", key, v -> {
                prefs.edit().putString("bg_pexels", v).apply();
                showBgSettings();
            }));
            final String pk = prefs.getString("bg_pixabay", "");
            l.add("🔑  مفتاح Pixabay:  " + (pk.length() > 8 ? "محفوظ ✓" : "غير مُدخل (من pixabay.com/api/docs مجاناً)"));
            a.add(() -> promptText("مفتاح Pixabay API", "الصق المفتاح هنا", pk, v -> {
                prefs.edit().putString("bg_pixabay", v).apply();
                showBgSettings();
            }));
            final String vl = prefs.getString("bg_vlinks", "");
            l.add("🔗  روابط فيديو خاصة:  " + Backgrounds.links(vl).size() + " رابط  — أضفها من الجوال");
            a.add(this::showLinkInput);
            String tq = prefs.getString("bg_topic", "nature landscape");
            int ti = Math.max(0, Arrays.asList(TOPIC_Q).indexOf(tq));
            l.add("🎬  موضوع الفيديو:  " + TOPIC_NAMES[ti]);
            a.add(() -> choice("موضوع الفيديو", TOPIC_NAMES, ti, i -> {
                prefs.edit().putString("bg_topic", TOPIC_Q[i]).apply();
                showBgSettings();
            }, this::showBgSettings));
        }
        final boolean daily = prefs.getInt("bg_mode", 0) == 1;
        l.add("⏱  طريقة التغيير:  " + bgEveryText());
        a.add(() -> choice("متى تتغير الخلفية؟", new String[]{"كل مدة محددة (دقائق / ساعات / أيام)",
                "يومياً في ساعة محددة"}, daily ? 1 : 0, i -> {
            prefs.edit().putInt("bg_mode", i).apply();
            if (i == 0) {
                choice("وحدة المدة", BG_UNIT_NAMES, prefs.getInt("bg_unit", 1), u -> {
                    prefs.edit().putInt("bg_unit", u).apply();
                    numberDialog("عدد " + BG_UNIT_NAMES[u] + " بين كل تغيير وآخر", "bg_every",
                            u == 0 ? 10 : u == 1 ? 6 : 1, 1, u == 0 ? 120 : u == 1 ? 72 : 30, "", null, null,
                            this::showBgSettings, this::showBgSettings);
                }, this::showBgSettings);
            } else {
                numberDialog("ساعة التغيير اليومي (0–23)", "bg_hour", 4, 0, 23, "", null, null,
                        this::showBgSettings, this::showBgSettings);
            }
        }, this::showBgSettings));
        if (vid) {
            l.add("📺  طريقة عرض الفيديو:  " + (prefs.getInt("bg_vmode", 2) == 2 ? "أصلي (مشغّل النظام)" : prefs.getInt("bg_vmode", 2) == 1 ? "لوحة رسم" : "مباشر"));
            a.add(() -> choice("طريقة عرض الفيديو", new String[]{"مباشر داخل الصفحة",
                    "لوحة رسم داخل الصفحة", "أصلي — مشغّل فيديو النظام (الأفضل)"}, prefs.getInt("bg_vmode", 2), i -> {
                prefs.edit().putInt("bg_vmode", i).apply();
                applyPageCss();
                showBgSettings();
            }, this::showBgSettings));
        }
 l.add("✨  جودة الصور/الفيديو:  " + new String[]{"عادية", "عالية (1080p / 1920)", "قصوى (صور 2560)"}[prefs.getInt("bg_q", 0) % 3]);
        a.add(() -> choice("جودة الصور والفيديو", new String[]{"عادية (أخف وأسرع)", "عالية — 1080p / عرض 1920",
                "قصوى — صور 2560 (الفيديو يبقى 1080p)"}, prefs.getInt("bg_q", 0), i -> {
            prefs.edit().putInt("bg_q", i).apply();
            bgFetch(true);
            showBgSettings();
        }, this::showBgSettings));
        final int sm = prefs.getInt("bg_sm", 0);
        l.add("🧠  خلفية ذكية:  " + (sm == 0 ? "متوقفة" : "مفعّلة (" + Integer.bitCount(sm) + " قواعد)"));
        a.add(() -> {
            final int[] bits = {1, 2, 4, 8};
            final boolean[] on = new boolean[4];
            for (int i = 0; i < 4; i++) on[i] = (sm & bits[i]) != 0;
            dialog("الخلفية الذكية — تختار الموضوع تلقائياً")
                    .setMultiChoiceItems(new String[]{"حسب الوقت: نهار / غروب / ليل + الفصل",
                            "حسب الطقس: ثلج / مطر / عاصفة / ضباب / حرّ / صقيع",
                            "يوم الجمعة: مكة والمدينة",
                            "قبل الصلاة: مساجد وأجواء الأذان"}, on, null)
                    .setPositiveButton("حفظ", (d, w) -> {
                        int v = 0;
                        for (int i = 0; i < 4; i++) if (on[i]) v |= bits[i];
                        prefs.edit().putInt("bg_sm", v).putString("bg_smartkey", "").apply();
                        if (v != 0) bgFetch(true);
                        showBgSettings();
                    })
                    .setNegativeButton("إلغاء", (d, w) -> showBgSettings()).show();
        });
        if (vid) {
            l.add("🌊  حركة هادئة مستمرة (تقريب بطيء):  " + (prefs.getBoolean("bg_drift", true) ? "نعم" : "لا"));
            a.add(() -> {
                prefs.edit().putBoolean("bg_drift", !prefs.getBoolean("bg_drift", true)).apply();
                applyNativeVideo();
                showBgSettings();
            });
        }
        l.add("🎞  مدة الانتقال الناعم بين الخلفيات:  " + prefs.getInt("bg_fade", 2) + " ث");
        a.add(() -> numberDialog("مدة الانتقال الناعم (ثوانٍ، 0 = فوري)", "bg_fade", 2, 0, 10, " ث", "فوري", null,
                this::applyPageCss, this::showBgSettings));
        l.add("🌑  تعتيم الخلفية (لتبقى الأوقات واضحة):  " + prefs.getInt("bg_dim", 35) + "%");
        a.add(() -> numberDialog("تعتيم الخلفية", "bg_dim", 35, 0, 85, "%", null, this::applyPageCss,
                this::showBgSettings, this::showBgSettings));
        l.add("🔄  تغيير الخلفية الآن");
        a.add(() -> {
            if (!bgEnabled()) toast("اختر مصدراً أولاً");
            else bgFetch(true);
            showBgSettings();
        });
        String cr = prefs.getString("bg_credit", "");
        l.add("©  المصدر الحالي:  " + (cr.isEmpty() ? "—" : cr));
        a.add(this::showBgSettings);
        menu("الخلفية من الإنترنت (صور / فيديو)", l, a, this::showLauncherSettings);
    }

    private void showAdminDialog() {
        final boolean on = adminActive();
        Intent probe = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        if (!on && probe.resolveActivity(getPackageManager()) == null) {
            dialog("«مدير الجهاز» غير متاح").setMessage("نظام هذا الجهاز (Fire OS) يحجب شاشة تفعيل «مدير الجهاز» "
                    + "بالكامل، فلا يمكن استعمالها للنوم هنا. تبقى الشاشة السوداء مع التعتيم الكامل، "
                    + "ويعمل النوم الحقيقي فقط عند توفر ADB الذاتي.")
                    .setPositiveButton("حسناً", null).show();
            return;
        }
        dialog("صلاحية «مدير الجهاز» (نوم الجهاز)")
                .setMessage("تسمح للانشر بقفل الجهاز/إطفاء الشاشة بعد التعتيم الكامل بدون ADB. "
                        + "الحالة الحالية: " + (on ? "مفعّلة ✓" : "غير مفعّلة")
                        + "\n\nعند التفعيل ستظهر نافذة من أندرويد، وافق عليها. وإن لم تناسبك ألغها من هنا.")
                .setPositiveButton(on ? "إلغاء الصلاحية" : "تفعيل", (d, w) -> {
                    try {
                        ComponentName cn = new ComponentName(this, AdminReceiver.class);
                        if (on) {
                            ((DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE)).removeActiveAdmin(cn);
                            toast("أُلغيت الصلاحية");
                        } else {
                            Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                            i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cn);
                            i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    "لقفل الشاشة بعد التعتيم الكامل");
                            startActivity(i);
                        }
                    } catch (Exception e) {
                        AppLog.error("ADMIN", "request failed", e);
                        toast("هذا الجهاز لا يدعم هذه الصلاحية");
                    }
                })
                .setNegativeButton("إغلاق", null).show();
    }

    // ------------------------------------------------------------- Updates

    private void checkUpdate(final boolean manual) {
        final String repo = prefs.getString("upd_repo", Updater.DEFAULT_REPO);
        Updater.check(this, repo, new Updater.CheckCb() {
            @Override public void done(Updater.Info info, String err) {
                if (destroyed) return;
                prefs.edit().putLong("upd_checked", System.currentTimeMillis()).apply();
                if (info == null) {
                    if (manual) dialog("تعذّر فحص التحديث").setMessage(err)
                            .setPositiveButton("حسناً", null).show();
                    return;
                }
                if (info.code <= Updater.currentCode(MainActivity.this)) {
                    pendingInfo = null;
                    if (manual) toast("أنت على آخر إصدار ✓ (" + Updater.currentName(MainActivity.this) + ")");
                    return;
                }
                pendingInfo = info;
                AppLog.info("UPDATE", "available: " + info.name + " code " + info.code);
                int mode = prefs.getInt("upd_mode", 1);
                if (manual) {
                    askInstall();
                } else {
                    if (mode == 2) downloadUpdate(false);
                    // tell the user: a dialog once per app start (when nothing else is open), else a bubble
                    if (!updShown && openDialogs == 0 && stage == 0 && resumed) {
                        updShown = true;
                        askInstall();
                    } else if (prefs.getInt("upd_notified", 0) != info.code) {
                        prefs.edit().putInt("upd_notified", info.code).apply();
                        showBubble("⬆ تحديث متاح: " + info.name + " — ⚙ ← إعدادات Mawaqit Home ← التحديثات",
                                12000L);
                    }
                }
            }
        });
    }

    private boolean updShown;

    private void askInstall() {
        if (pendingInfo == null) return;
        dialog("تحديث متاح").setMessage("الإصدار الجديد: " + pendingInfo.name
                + "\nإصدارك الحالي: " + Updater.currentName(this)
                + "\n\nسيُنزَّل ويُثبَّت فوق النسخة الحالية وتبقى كل إعداداتك.")
                .setPositiveButton("تحديث الآن", (d, w) -> downloadUpdate(true))
                .setNegativeButton("لاحقاً", null).show();
    }

    /** interactive = the user asked: show progress, then install right away. */
    private void downloadUpdate(final boolean interactive) {
        if (pendingInfo == null || updBusy) return;
        if (readyApk != null && readyApk.exists()) {
            if (interactive) installUpdate(false);
            return;
        }
        updBusy = true;
        if (interactive) showBubble("⬇ جارٍ تنزيل التحديث…", 0L);
        Updater.download(this, pendingInfo, new Updater.DownloadCb() {
            @Override public void progress(int pct) {
                if (interactive && !destroyed) bubble.setText("⬇ جارٍ تنزيل التحديث… " + pct + "%");
            }

            @Override public void done(File apk, String err) {
                updBusy = false;
                if (destroyed) return;
                if (apk == null) {
                    if (interactive) {
                        hideBubble();
                        dialog("فشل التنزيل").setMessage(err).setPositiveButton("حسناً", null).show();
                    }
                    return;
                }
                readyApk = apk;
                AppLog.info("UPDATE", "downloaded " + apk.length() + " bytes");
                if (interactive) installUpdate(false);
                // otherwise the idle task installs it while the screen saver is on
            }
        });
    }

    private void installUpdate(boolean background) {
        if (readyApk == null || updBusy) return;
        updBusy = true;
        long idle = background ? SystemClock.elapsedRealtime() - lastInput : 0L;
        if (!background) showBubble("⚙ جارٍ تثبيت التحديث… سيعاد تشغيل التطبيق", 0L);
        final File apkF = readyApk;
        Updater.install(this, readyApk, idle, new AdbShell.Callback() {
            @Override public void done(boolean ok, String output) {
                // Reached only when the install did not replace the app (failure)
                updBusy = false;
                readyApk = null;
                hideBubble();
                AppLog.info("UPDATE", "install result ok=" + ok + " " + output);
                if (ok) return;
                if (background) return;
                // No ADB on this device (phone / new Google TV): use the system installer instead.
                showBubble("⚙ فتح مثبّت أندرويد… وافق على التحديث", 8000L);
                Updater.installViaSession(MainActivity.this, apkF, () -> dialog("تعذّر تثبيت التحديث")
                        .setMessage("لم ينجح التثبيت. اسمح لـ Mawaqit Home بتثبيت التطبيقات من "
                                + "إعدادات أندرويد (تثبيت تطبيقات غير معروفة) ثم أعد المحاولة.\n\n" + output)
                        .setPositiveButton("حسناً", null).show());
            }
        });
    }

    // ---------------------------------------------------------------- Views

    // ---- fonts, text scale, accent colour

    static final String[] FONT_NAMES = {"الخط الافتراضي للنظام", "Tajawal — عصري واضح", "Almarai — حديث مريح",
            "Amiri — نسخ كلاسيكي", "Scheherazade — خط المصحف", "Lalezar — عناوين عريضة",
            "Cairo — عصري أنيق", "Noto Naskh — نسخ مريح", "Noto Kufi — كوفي حديث", "Reem Kufi — كوفي هندسي",
            "El Messiri — فخم", "Aref Ruqaa — رقعة", "Mada — بسيط نظيف", "Changa — جريء", "Markazi — كتابي"};
    private static final String[][] FONT_FILES = {null, {"Tajawal-Regular", "Tajawal-Bold"},
            {"Almarai-Regular", "Almarai-Bold"}, {"Amiri-Regular", "Amiri-Bold"},
            {"ScheherazadeNew-Regular", "ScheherazadeNew-Bold"}, {"Lalezar-Regular", "Lalezar-Regular"},
            {"Cairo-Regular", "Cairo-Bold"}, {"NotoNaskhArabic-Regular", "NotoNaskhArabic-Bold"},
            {"NotoKufiArabic-Regular", "NotoKufiArabic-Bold"}, {"ReemKufi-Regular", "ReemKufi-Bold"},
            {"ElMessiri-Regular", "ElMessiri-Bold"}, {"ArefRuqaa-Regular", "ArefRuqaa-Bold"},
            {"Mada-Regular", "Mada-Bold"}, {"Changa-Regular", "Changa-Bold"},
            {"MarkaziText-Regular", "MarkaziText-Bold"}};
    static final String[] ACC_NAMES = {"ذهبي", "أبيض", "أخضر", "أزرق", "وردي", "برتقالي", "سماوي"};
    private static final int[] ACC = {0xFFF4C95D, 0xFFFFFFFF, 0xFF7BE0A0, 0xFF6AB7FF, 0xFFFF8FC0,
            0xFFFFA14A, 0xFF5FE3E3};
    private Typeface tfReg = Typeface.DEFAULT, tfBold = Typeface.DEFAULT_BOLD;

    private void loadFonts() {
        int f = prefs.getInt("font", 0);
        if (f <= 0 || f >= FONT_FILES.length) return;
        try {
            tfReg = Typeface.createFromAsset(getAssets(), "fonts/" + FONT_FILES[f][0] + ".ttf");
            tfBold = Typeface.createFromAsset(getAssets(), "fonts/" + FONT_FILES[f][1] + ".ttf");
        } catch (Throwable t) {
            AppLog.error("FONT", "load failed", t);
        }
    }

    private Typeface tf(boolean bold) { return bold ? tfBold : tfReg; }

    /** Text size scaled by the user's font-size setting. */
    private int fs(int v) {
        return Math.round(dp(v) * Math.max(60, Math.min(200, prefs.getInt("font_pct", 100))) / 100f);
    }

    private int acc() { return ACC[Math.max(0, Math.min(ACC.length - 1, prefs.getInt("ui_accent", 0)))]; }

    private static int withAlpha(int c, int a) { return (a << 24) | (c & 0xFFFFFF); }

    /** Applies the chosen font to every text view below v (monospace blocks keep their font). */
    private void applyFont(View v) {
        if (v instanceof TextView) {
            TextView t = (TextView) v;
            Typeface cur = t.getTypeface();
            if (cur != Typeface.MONOSPACE) t.setTypeface(tf(cur != null && cur.isBold()));
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) applyFont(g.getChildAt(i));
        }
    }

    private TextView text(int size, boolean bold) {
        TextView t = new TextView(this);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, fs(size));
        t.setShadowLayer(dp(2), 0f, dp(1), 0xFF000000);
        t.setTextDirection(View.TEXT_DIRECTION_LTR);
        t.setSingleLine(true);
        t.setTypeface(tf(bold));
        return t;
    }

    /** One timing for every slide/fade in the app, adjustable in Settings. */
    private long ms(long base) {
        return Math.round(base * prefs.getInt("anim_pct", 100) / 100f);
    }

    private void buildStrip() {
        strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.VERTICAL);
        strip.setClipChildren(false);

        hair = new View(this);
        hair.setBackgroundColor(0x44F4C95D);
        strip.addView(hair, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1))));

        topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setClipChildren(false);

        badge = new LinearLayout(this);
        badge.setOrientation(LinearLayout.VERTICAL);
        badge.setGravity(Gravity.CENTER);
        badgeIcon = text(22, false);
        badgeIcon.setGravity(Gravity.CENTER);
        TextView chevron = text(18, true);
        chevron.setTextColor(0xFFF4C95D);
        chevron.setText(railRight ? "›" : "‹");
        chevron.setGravity(Gravity.CENTER);
        badge.addView(badgeIcon);
        badge.addView(chevron);
        badge.setClickable(true);
        badge.setOnClickListener(v -> openRail());

        rowScroll = new HorizontalScrollView(this) {
            @Override protected int computeScrollDeltaToGetChildRectOnScreen(android.graphics.Rect r) {
                return 0;                                  // we scroll smoothly ourselves (applyTileFocus)
            }
        };
        rowScroll.setHorizontalScrollBarEnabled(false);
        rowScroll.setHorizontalFadingEdgeEnabled(false);
        rowScroll.setFocusable(false);
        rowScroll.setClipChildren(false);
        rowScroll.setClipToPadding(false);
        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setGravity(Gravity.CENTER_VERTICAL);
        grid.setClipChildren(false);
        rowScroll.addView(grid, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (railRight) {
            topRow.addView(rowScroll, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            topRow.addView(badge);
        } else {
            topRow.addView(badge);
            topRow.addView(rowScroll, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        }
        strip.addView(topRow);

        nameLabel = text(13, false);
        nameLabel.setGravity(Gravity.CENTER);
        nameLabel.setTextDirection(View.TEXT_DIRECTION_RTL);
        nameLabel.setEllipsize(TextUtils.TruncateAt.END);
        strip.addView(nameLabel);

        shiftLayer.addView(strip, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(110), Gravity.BOTTOM));
        applyStripStyle();
    }

    private static final String[] STRIP_STYLES = {"داكن", "شفاف", "متدرج أخضر (مثل الصفحة)",
            "يتبع لون الصفحة تلقائياً"};

    static final String[] PAL_NAMES = {"أخضر زمردي", "أزرق ليلي", "بنفسجي", "ذهبي بنّي", "رمادي فحمي",
            "أحمر عنابي", "تركوازي", "وردي", "برتقالي", "أسود نقي", "أزرق سماوي", "زيتي", "لون مخصص…"};
    static final int[] PAL_TOP = {0xFF14503C, 0xFF16396B, 0xFF4A2A78, 0xFF6B4A14, 0xFF3A4048, 0xFF6E1E2E, 0xFF0F6B6B, 0xFF8A2F5C, 0xFF9A4A10, 0xFF1A1A1A, 0xFF1E6FA8, 0xFF4A5A1C};
    static final int[] PAL_BOT = {0xFF04140E, 0xFF050E22, 0xFF150A26, 0xFF1E1404, 0xFF0C0E10, 0xFF1E050C, 0xFF03181A, 0xFF220A18, 0xFF241002, 0xFF000000, 0xFF06182A, 0xFF121804};

    private int[] paletteColors() {
        int c = prefs.getInt("strip_col", 0);
        if (c >= PAL_TOP.length) {
            try {
                int t = Color.parseColor(prefs.getString("strip_hex", "#103A2C"));
                int r = (t >> 16) & 255, g = (t >> 8) & 255, b = t & 255;
                return new int[]{0xFF000000 | t, 0xFF000000 | ((r * 30 / 100) << 16) | ((g * 30 / 100) << 8)
                        | (b * 30 / 100)};
            } catch (Exception e) {
                c = 0;
            }
        }
        return new int[]{PAL_TOP[c], PAL_BOT[c]};
    }

    private void applyStripStyle() {
        int style = prefs.getInt("strip_style", 2);
        int a = Math.max(0, Math.min(100, prefs.getInt("strip_alpha", 92))) * 255 / 100;
        int top, bot;
        if (style == 0) {
            top = argb(a, 7, 13, 11);
            bot = top;
        } else if (style == 1) {
            top = argb(a * 6 / 10, 0, 0, 0);
            bot = argb(a * 8 / 10, 0, 0, 0);
        } else if (style == 3) {
            int r = (pageColor >> 16) & 255, g = (pageColor >> 8) & 255, b = pageColor & 255;
            top = argb(a, r, g, b);
            bot = argb(a, r * 55 / 100, g * 55 / 100, b * 55 / 100);
        } else {
            int[] pc = paletteColors();
            top = (a << 24) | (pc[0] & 0xFFFFFF);
            bot = (a << 24) | (pc[1] & 0xFFFFFF);
        }
        // 3D: rounded top corners, gold rim, a glossy highlight layer and a raised edge
        GradientDrawable body = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{top, bot});
        float rd = dp(22);
        body.setCornerRadii(new float[]{rd, rd, rd, rd, 0, 0, 0, 0});
        GradientDrawable gloss = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{argb(a * 30 / 255, 255, 255, 255), 0x00FFFFFF});
        gloss.setCornerRadii(new float[]{rd, rd, rd, rd, 0, 0, 0, 0});
        GradientDrawable rim = new GradientDrawable();
        rim.setColor(Color.TRANSPARENT);
        rim.setCornerRadii(new float[]{rd, rd, rd, rd, 0, 0, 0, 0});
        rim.setStroke(Math.max(1, dp(1)), argb(a * 130 / 255, 0xF4, 0xC9, 0x5D));
        LayerDrawable ld = new LayerDrawable(new android.graphics.drawable.Drawable[]{body, gloss, rim});
        ld.setLayerInset(1, 0, 0, 0, dp(70));
        if (style == 1) ld = new LayerDrawable(new android.graphics.drawable.Drawable[]{body, rim});
        strip.setBackground(ld);
        if (Build.VERSION.SDK_INT >= 21) strip.setElevation(dp(10));
    }

    /** Style 3: reads the colour of the page's bottom edge so the strip matches the page. */
    private void samplePageColor() {
        if (prefs.getInt("strip_style", 2) != 3 || webView.getWidth() <= 0
                || webView.getHeight() <= 0) return;
        try {
            Bitmap bmp = Bitmap.createBitmap(64, 36, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(bmp);
            cv.scale(64f / webView.getWidth(), 36f / webView.getHeight());
            webView.draw(cv);
            long r = 0, g = 0, b = 0;
            int n = 0;
            for (int y = 28; y < 36; y++) {
                for (int x = 0; x < 64; x++) {
                    int c = bmp.getPixel(x, y);
                    if (Color.alpha(c) < 200) continue;
                    r += Color.red(c);
                    g += Color.green(c);
                    b += Color.blue(c);
                    n++;
                }
            }
            bmp.recycle();
            if (n > 100) {
                int rr = (int) (r / n), gg = (int) (g / n), bb = (int) (b / n);
                if (rr + gg + bb > 30) {
                    pageColor = 0xFF000000 | (rr << 16) | (gg << 8) | bb;
                    applyStripStyle();
                    AppLog.info("STRIP", "page colour " + Integer.toHexString(pageColor));
                }
            }
        } catch (Throwable t) {
            AppLog.info("STRIP", "colour sampling failed: " + t);
        }
    }

    private void recoverPage(String why) {
        long now = SystemClock.elapsedRealtime();
        if (now - wdRecoverAt < 10L * 60 * 1000) return;
        wdRecoverAt = now;
        wdSame = 0;
        wdLast = "";
        AppLog.info("WATCHDOG", why + " -> reloading the page");
        webView.reload();
    }

    private boolean online() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (Build.VERSION.SDK_INT >= 23) {
                android.net.Network n = cm.getActiveNetwork();
                android.net.NetworkCapabilities nc = n == null ? null : cm.getNetworkCapabilities(n);
                return nc != null && nc.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
            }
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        } catch (Exception e) {
            return true;
        }
    }

    private void buildBubble() {
        bubble = text(18, true);
        bubble.setTextColor(0xFFF4C95D);
        bubble.setTextDirection(View.TEXT_DIRECTION_RTL);
        bubble.setGravity(Gravity.CENTER);
        bubble.setPadding(dp(22), dp(10), dp(22), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE60B3D2E);
        bg.setStroke(dp(1), 0x66F4C95D);
        bg.setCornerRadius(dp(40));
        bubble.setBackground(bg);
        bubble.setAlpha(0f);
        shiftLayer.addView(bubble, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
    }

    private void showBubble(String s, long autoHideMs) {
        bubble.setText(s);
        handler.removeCallbacks(hideBubbleTask);
        bubble.animate().cancel();
        bubble.setTranslationY(dp(14));
        bubble.animate().alpha(1f).translationY(0f).setDuration(ms(240))
                .setInterpolator(new DecelerateInterpolator(1.6f)).start();
        bubbleSticky = autoHideMs <= 0;
        if (autoHideMs > 0) handler.postDelayed(hideBubbleTask, autoHideMs);
    }

    private void hideBubble() {
        bubbleSticky = false;
        bubble.animate().alpha(0f).translationY(dp(10)).setDuration(ms(300)).start();
    }

    // ------------------------------------------------ Floating category menu

    private void buildRail() {
        railScroll = new CapScroll(this);
        railScroll.setVerticalScrollBarEnabled(false);
        railScroll.setFocusable(false);
        railScroll.setClipChildren(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF2071410);
        bg.setStroke(dp(1), 0x44F4C95D);
        bg.setCornerRadius(dp(26));
        railScroll.setBackground(bg);
        railScroll.setElevation(dp(12));

        railList = new LinearLayout(this);
        railList.setOrientation(LinearLayout.VERTICAL);
        railList.setPadding(dp(14), dp(18), dp(14), dp(14));
        railScroll.addView(railList, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        railScroll.setAlpha(0f);
        railScroll.setVisibility(View.INVISIBLE);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(260),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                (railRight ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
        lp.setMargins(dp(16), dp(16), dp(16), dp(120));
        shiftLayer.addView(railScroll, lp);
    }

    private void rebuildRailItems() {
        railList.removeAllViews();
        railItems.clear();
        TextView title = text(15, true);
        title.setTextColor(0xFFF4C95D);
        title.setText("🕌  Mawaqit Home");
        title.setPadding(dp(10), 0, 0, dp(12));
        railList.addView(title);

        for (int i = 0; i < tabs.size(); i++) {
            final int idx = i;
            Tab tb = tabs.get(i);
            TextView t = text(19, false);
            t.setText(tb.icon + "   " + tb.name);
            t.setTextDirection(View.TEXT_DIRECTION_RTL);
            t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            t.setPadding(dp(14), 0, dp(14), 0);
            t.setFocusable(true);
            t.setClickable(true);
            t.setBackground(focusBackground());
            t.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) return;
                tick();
                if (currentTab != idx) {
                    showTab(idx, -1);
                    showBubble(tabs.get(idx).icon + "  " + tabs.get(idx).name, BUBBLE_MS);
                }
            });
            t.setOnClickListener(v -> {
                if (currentTab != idx) showTab(idx, -1);
                closeRail(true);
            });
            railItems.add(t);
            railList.addView(t, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        }
        railW = computeRailW();
        FrameLayout.LayoutParams rlp = (FrameLayout.LayoutParams) railScroll.getLayoutParams();
        if (rlp != null && rlp.width != railW) {
            rlp.width = railW;
            railScroll.setLayoutParams(rlp);
            railScroll.setTranslationX(railOpen ? 0f : railHidden());
        }
    }

    /** Width that fits the longest category name, so nothing is cut off. */
    private int computeRailW() {
        int sw = sizeW > 0 ? sizeW : getResources().getDisplayMetrics().widthPixels;
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(tf(true));
        p.setTextSize(fs(19));
        float mx = p.measureText("🕌  Mawaqit Home");
        for (Tab tb : tabs) mx = Math.max(mx, p.measureText(tb.icon + "   " + tb.name));
        int need = Math.round(mx) + dp(14 * 2 + 14 * 2 + 12);
        return Math.max(dp(200), Math.min(Math.round(sw * 0.5f), need));
    }

    private float railHidden() {
        return (railRight ? 1 : -1) * (railW + dp(30));
    }

    private void openRail() {
        if (railOpen || stage > 0 || moveMode) return;
        railOpen = true;
        railScroll.setVisibility(View.VISIBLE);
        railScroll.animate().translationX(0f).alpha(1f).setDuration(ms(280))
                .setInterpolator(new DecelerateInterpolator(1.6f)).start();
        if (currentTab >= 0 && currentTab < railItems.size()) railItems.get(currentTab).requestFocus();
        scheduleRailClose();
    }

    private void closeRail(boolean refocus) {
        if (!railOpen) return;
        railOpen = false;
        handler.removeCallbacks(railCloseTask);
        railScroll.animate().translationX(railHidden()).alpha(0f).setDuration(ms(240))
                .setInterpolator(new DecelerateInterpolator(1.6f))
                .withEndAction(() -> {
                    if (!railOpen) railScroll.setVisibility(View.INVISIBLE);
                }).start();
        if (refocus) focusFirst();
    }

    private void scheduleRailClose() {
        handler.removeCallbacks(railCloseTask);
        handler.postDelayed(railCloseTask, prefs.getInt("rail_secs", 6) * 1000L);
    }

    // ---------------------------------------------------------- Responsive

    private void onSized(int w, int h) {
        pageRatio = prefs.getInt("top_pct", 82) / 100f;
        fillMode = prefs.getInt("fit", 1) == 1;
        int hp = prefs.getInt("strip_hp", 0);                  // 0 = automatic (from the page area)
        stripH = hp > 0 ? Math.round(h * hp / 100f) : h - Math.round(h * pageRatio);
        topH = h - stripH;
        pageRatio = topH / (float) h;
        layoutWeather();
        applyOverscan();

        FrameLayout.LayoutParams wl = (FrameLayout.LayoutParams) webView.getLayoutParams();
        applyRootBg();
        if (fillMode) {
            wl.height = prefs.getBoolean("strip_over", false) ? ViewGroup.LayoutParams.MATCH_PARENT : topH;
            webView.setScaleX(1f);
            webView.setScaleY(1f);
        } else {
            wl.height = ViewGroup.LayoutParams.MATCH_PARENT;
            webView.setPivotX(w / 2f);
            webView.setPivotY(0f);
        }
        webView.setLayoutParams(wl);

        int labelH = dp(22);
        int avail = stripH - labelH - dp(9);
        float scale = prefs.getInt("tile_pct", 100) / 100f;
        int perRow = avail / rows;
        float base = perRow * 0.96f;
        tilePx = Math.max(dp(34), Math.min(perRow, Math.round(base * scale)));
        iconPx = Math.round(tilePx * Math.max(50, Math.min(100, prefs.getInt("icon_fill", 84))) / 100f);

        FrameLayout.LayoutParams sl = (FrameLayout.LayoutParams) strip.getLayoutParams();
        sl.height = stripH;
        sl.width = Math.round(w * Math.max(30, Math.min(100, prefs.getInt("strip_wp", 100))) / 100f);
        sl.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        strip.setLayoutParams(sl);

        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(dp(40), avail + dp(8))));
        badge.setLayoutParams(new LinearLayout.LayoutParams(
                Math.max(dp(52), Math.round(Math.min(tilePx, dp(100)) * 0.8f)),
                ViewGroup.LayoutParams.MATCH_PARENT));
        nameLabel.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, labelH));

        railW = computeRailW();
        FrameLayout.LayoutParams rl = (FrameLayout.LayoutParams) railScroll.getLayoutParams();
        rl.width = railW;
        rl.gravity = (railRight ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP;
        rl.setMargins(dp(16), dp(16), dp(16), stripH + dp(14));
        railScroll.maxH = Math.max(dp(160), h - stripH - dp(34));
        railScroll.setLayoutParams(rl);
        railScroll.setTranslationX(railOpen ? 0f : railHidden());

        FrameLayout.LayoutParams bu = (FrameLayout.LayoutParams) bubble.getLayoutParams();
        bu.bottomMargin = stripH + dp(14);
        bubble.setLayoutParams(bu);

        AppLog.info("LAYOUT", "screen " + w + "x" + h + " page=" + topH + " strip=" + stripH
                + " tile=" + tilePx + " rows=" + rows + " mode=" + (fillMode ? "fill" : "scale"));

        rebuildRailItems();
        showTab(currentTab, -1);
        applyStage(stage, false);
        if (root.findFocus() == null) focusFirst();
        maybeShowNotice();
    }

    // ------------------------------------------------------------ Strip/tabs

    private void loadCats() {
        cats.clear();
        try {
            JSONArray a = new JSONArray(prefs.getString("cats", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Cat c = new Cat();
                c.id = o.getString("id");
                c.name = o.getString("name");
                c.icon = o.optString("icon", "📁");
                JSONArray p = o.optJSONArray("pkgs");
                if (p != null) for (int k = 0; k < p.length(); k++) c.pkgs.add(p.getString(k));
                cats.add(c);
            }
        } catch (Exception e) {
            AppLog.error("CATS", "load failed", e);
        }
    }

    private void saveCats() {
        try {
            JSONArray a = new JSONArray();
            for (Cat c : cats) {
                JSONObject o = new JSONObject();
                o.put("id", c.id);
                o.put("name", c.name);
                o.put("icon", c.icon);
                o.put("pkgs", new JSONArray(c.pkgs));
                a.put(o);
            }
            prefs.edit().putString("cats", a.toString()).apply();
        } catch (Exception e) {
            AppLog.error("CATS", "save failed", e);
        }
    }

    private Tab mkTab(String id, String name, String icon, int kind, Cat cat) {
        Tab t = new Tab();
        t.id = id;
        t.name = name;
        t.icon = icon;
        t.kind = kind;
        t.cat = cat;
        return t;
    }

    private void buildTabs() {
        tabs.clear();
        tabs.add(mkTab("fav", "المفضلة", "★", K_FAV, null));
        tabs.add(mkTab("tv", "تطبيقات التلفزيون", "📺", K_TV, null));
        tabs.add(mkTab("mobile", "تطبيقات الجوال", "📱", K_MOBILE, null));
        tabs.add(mkTab("all", "كل التطبيقات", "🗂", K_ALL, null));
        for (Cat c : cats) tabs.add(mkTab(c.id, c.name, c.icon, K_CUSTOM, c));
        tabs.add(mkTab("sys", "الإعدادات", "⚙️", K_SYS, null));
    }

    private int tabIndexById(String id) {
        for (int i = 0; i < tabs.size(); i++) if (tabs.get(i).id.equals(id)) return i;
        return -1;
    }

    private int tabIndexByKind(int kind) {
        for (int i = 0; i < tabs.size(); i++) if (tabs.get(i).kind == kind) return i;
        return 0;
    }

    private void showTab(int tabIdx, int focusIndex) {
        if (tilePx <= 0) return;
        if (tabIdx < 0 || tabIdx >= tabs.size()) tabIdx = tabIndexByKind(K_ALL);
        currentTab = tabIdx;
        Tab tab = tabs.get(tabIdx);
        prefs.edit().putString("tab_id", tab.id).apply();
        badgeIcon.setText(tab.icon);

        for (int i = 0; i < railItems.size(); i++) {
            boolean sel = i == tabIdx;
            railItems.get(i).setTextColor(sel ? 0xFFF4C95D : Color.WHITE);
            railItems.get(i).setTypeface(tf(sel));
        }

        grid.removeAllViews();
        tiles.clear();
        rowScroll.scrollTo(0, 0);
        nameLabel.setText("");

        List<LinearLayout> lines = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setClipChildren(false);
            grid.addView(line, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, tilePx));
            lines.add(line);
        }

        int n = 0;
        if (tab.kind == K_SYS) {
            for (Shortcut s : SHORTCUTS) {
                View v = buildShortcutTile(s, s.actions[0].equals("#settings") && pendingInfo != null);
                lines.get(n % rows).addView(v);
                tiles.add(v);
                n++;
            }
        } else {
            List<AppEntry> list = listFor(tab);
            for (AppEntry a : list) {
                View v = buildTile(a, tab.kind != K_FAV && favs.contains(a.pkg));
                lines.get(n % rows).addView(v);
                tiles.add(v);
                n++;
            }
            if (list.isEmpty()) {
                TextView hint = text(15, false);
                hint.setTextDirection(View.TEXT_DIRECTION_RTL);
                hint.setPadding(dp(14), 0, dp(14), 0);
                hint.setGravity(Gravity.CENTER_VERTICAL);
                hint.setText(tab.kind == K_FAV
                        ? "لا توجد مفضلة بعد. اضغط مطولاً على OK فوق أي تطبيق واختر «إضافة إلى المفضلة» ★"
                        : tab.kind == K_CUSTOM
                        ? "التصنيف فارغ. اضغط مطولاً على OK فوق أي تطبيق واختر «التصنيفات»، أو من ⚙ ← الشريط والتصنيفات"
                        : "لا توجد تطبيقات هنا");
                lines.get(0).addView(hint);
            }
        }

        applySounds(grid);
        applyFont(grid);
        if (focusIndex >= 0 && !tiles.isEmpty()) {
            tiles.get(Math.min(focusIndex, tiles.size() - 1)).requestFocus();
        }
    }

    private List<AppEntry> listFor(Tab tab) {
        List<AppEntry> out = new ArrayList<>();
        if (tab.kind == K_FAV) {
            for (String p : favs) {
                AppEntry a = byPkg.get(p);
                if (a != null && !hidden.contains(p)) out.add(a);
            }
            return out;
        }
        if (tab.kind == K_CUSTOM) {
            for (String p : tab.cat.pkgs) {
                AppEntry a = byPkg.get(p);
                if (a != null && !hidden.contains(p)) out.add(a);
            }
            return out;
        }
        for (AppEntry a : entries) {
            if (hidden.contains(a.pkg)) continue;
            if (tab.kind == K_ALL || (tab.kind == K_TV && a.tv) || (tab.kind == K_MOBILE && !a.tv)) {
                out.add(a);
            }
        }
        return out;
    }

    private Tab tab() {
        return tabs.get(Math.max(0, Math.min(tabs.size() - 1, currentTab)));
    }

    private int indexOfPkg(List<AppEntry> list, String pkg) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).pkg.equals(pkg)) return i;
        return -1;
    }

    private void applyTileFocus(View v, boolean has) {
        float s = has ? prefs.getInt("zoom", 114) / 100f : 1f;
        v.animate().cancel();
        v.animate().scaleX(s).scaleY(s).translationY(has ? -dp(5) : 0f)
                .setDuration(has ? ms(260) : ms(200))
                .setInterpolator(has ? new android.view.animation.OvershootInterpolator(1.4f)
                        : new DecelerateInterpolator(1.5f)).start();
        v.setTranslationZ(has ? dp(8) : 0f);
        if (has && rowScroll != null) {                      // glide the strip so the focused tile is centred
            View line = (View) v.getParent();
            int target = line.getLeft() + v.getLeft() + v.getWidth() / 2 - rowScroll.getWidth() / 2;
            smoothScrollTo(Math.max(0, target));
        }
    }

    private android.animation.ValueAnimator scrollAnim;

    private void smoothScrollTo(int x) {
        if (scrollAnim != null) scrollAnim.cancel();
        int from = rowScroll.getScrollX();
        if (from == x) return;
        scrollAnim = android.animation.ValueAnimator.ofInt(from, x);
        scrollAnim.setDuration(ms(300));
        scrollAnim.setInterpolator(new DecelerateInterpolator(1.8f));
        scrollAnim.addUpdateListener(an -> rowScroll.scrollTo((Integer) an.getAnimatedValue(), 0));
        scrollAnim.start();
    }

    private View buildTile(final AppEntry a, boolean showStar) {
        FrameLayout f = new FrameLayout(this);
        f.setFocusable(true);
        f.setClickable(true);
        f.setLongClickable(true);
        boolean moving = moveMode && a.pkg.equals(movePkg);
        f.setBackground(moving ? moveBackground() : focusBackground());

        ImageView iv = new ImageView(this);
        boolean ban = rectIcons() && a.banner != null;
        iv.setImageDrawable(ban ? a.banner : a.icon);
        iv.setScaleType(ban ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
        styleIcon(iv, ban);
        f.addView(iv, new FrameLayout.LayoutParams(rectIcons() ? Math.round(iconPx * 1.6f) : iconPx, iconPx,
                Gravity.CENTER));

        if (showStar) {
            TextView star = text(12, false);
            star.setText("★");
            star.setTextColor(0xFFFFD54F);
            f.addView(star, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.RIGHT));
        }

        f.setOnClickListener(v -> {
            clickSound();
            launch(a);
        });
        f.setOnLongClickListener(v -> {
            showAppMenu(a);
            return true;
        });
        f.setOnFocusChangeListener((v, hasFocus) -> {
            applyTileFocus(v, hasFocus);
            if (hasFocus) {
                nameLabel.setText(moveMode ? MOVE_HINT : a.label);
                tick();
            }
        });
        f.setLayoutParams(new LinearLayout.LayoutParams(tileW(), tilePx));
        return f;
    }

    static final String[] ICON_STYLES = {"الأصلية كما هي", "مربع بزوايا مدورة", "دائرة", "مجسّمة (ظل وتدرج)",
            "مسطحة (لون واحد)", "زجاجية شفافة", "تملأ الإطار (مدورة)", "تملأ الإطار (دائرة)", "مربع حاد",
            "▭ مستطيلة (بانر التطبيق إن وُجد)", "▭ مستطيلة مجسّمة"};

    private boolean rectIcons() {
        return prefs.getInt("icon_style", 0) >= 9;
    }

    private int tileW() {
        return rectIcons() ? Math.round(tilePx * 1.6f) : tilePx;
    }

    /** Draws the app icon on a plate whose shape/finish follows the "icon style" setting. */
    private void styleIcon(ImageView iv, boolean hasBanner) {
        final int st = prefs.getInt("icon_style", 0);
        if (st == 0) return;
        final boolean circle = st == 2 || st == 7;
        final float r = circle ? iconPx / 2f : st == 8 ? dp(2) : iconPx * (st >= 9 ? 0.16f : 0.22f);
        int pad = (st == 6 || st == 7 || (st == 9 && hasBanner)) ? 0
                : Math.round(iconPx * (circle ? 0.10f : st >= 9 ? 0.08f : 0.06f));
        iv.setPadding(pad, pad, pad, pad);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(r);
        int[] pc = paletteColors();
        if (st == 3 || st == 10) {
            g.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
            g.setColors(new int[]{0xFF3A4A44, 0xFF121C18});
            g.setStroke(Math.max(1, dp(1)), withAlpha(acc(), 0x99));
            iv.setElevation(dp(6));
        } else if (st == 4 || st == 8 || st == 9) {
            g.setColor(0xFF22302A);
        } else if (st == 5) {
            g.setColor(0x33FFFFFF);
            g.setStroke(Math.max(1, dp(1)), 0x88FFFFFF);
        } else if (st == 1 || st == 2) {
            g.setColor(0x55000000);
        } else {
            g.setColor(Color.TRANSPARENT);
        }
        iv.setBackground(g);
        final float rr = r;
        iv.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View v, android.graphics.Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), rr);
            }
        });
        iv.setClipToOutline(true);
    }

    private View buildShortcutTile(final Shortcut s, boolean dot) {
        FrameLayout f = new FrameLayout(this);
        f.setFocusable(true);
        f.setClickable(true);
        f.setBackground(focusBackground());

        TextView ic = text(Math.max(18, Math.round(tilePx / (float) dp(1) * 0.42f)), false);
        ic.setText(s.icon);
        ic.setGravity(Gravity.CENTER);
        f.addView(ic, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (dot) {          // an update is waiting
            TextView d = text(12, true);
            d.setText("⬆");
            d.setTextColor(0xFF7CFF9B);
            f.addView(d, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.RIGHT));
        }

        f.setOnClickListener(v -> {
            clickSound();
            runShortcut(s);
        });
        f.setOnFocusChangeListener((v, hasFocus) -> {
            applyTileFocus(v, hasFocus);
            if (hasFocus) {
                nameLabel.setText(s.label);
                tick();
            }
        });
        f.setLayoutParams(new LinearLayout.LayoutParams(tileW(), tilePx));
        return f;
    }

    private Drawable focusBackground() {
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(0x33FFFFFF);
        focused.setStroke(dp(2), 0xFFF4C95D);
        focused.setCornerRadius(dp(12));

        StateListDrawable sl = new StateListDrawable();
        sl.addState(new int[]{android.R.attr.state_focused}, focused);
        if (prefs.getBoolean("tile_plate", true)) {
            GradientDrawable plate = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0x2EFFFFFF, 0x0DFFFFFF});
            plate.setCornerRadius(dp(12));
            plate.setStroke(Math.max(1, dp(1)), 0x22FFFFFF);
            sl.addState(new int[]{}, plate);
        } else {
            sl.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
        }
        return sl;
    }

    private Drawable moveBackground() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(0x55FFD54F);
        d.setStroke(dp(3), 0xFFFFD54F);
        d.setCornerRadius(dp(12));
        return d;
    }

    /** Is the focused tile the first (left) or last (right) one in its row? */
    private boolean atEdge(boolean left) {
        View f = root.findFocus();
        if (tiles.isEmpty() || f == null || !(f.getParent() instanceof LinearLayout)) return true;
        LinearLayout line = (LinearLayout) f.getParent();
        int i = line.indexOfChild(f);
        return left ? i <= 0 : i >= line.getChildCount() - 1;
    }

    private void focusFirst() {
        if (!tiles.isEmpty()) tiles.get(0).requestFocus();
    }

    // --------------------------------------------------------------- Sounds

    private void tick() {
        if (audio != null && stage == 0 && prefs.getBoolean("sounds", true)) {
            audio.playSoundEffect(AudioManager.FX_FOCUS_NAVIGATION_RIGHT, 0.5f);
        }
    }

    private void clickSound() {
        if (audio != null && stage == 0 && prefs.getBoolean("sounds", true)) {
            audio.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.7f);
        }
    }

    // -------------------------------------------------------------- Actions

    private void launch(AppEntry a) {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setClassName(a.pkg, a.cls);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(i);
        } catch (Exception e) {
            AppLog.error("LAUNCH", a.pkg, e);
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    /** Our own dialog: no AlertDialog internals, so width, padding and alignment are fully ours. */
    private class Dlg extends Dialog {
        ListView listView;
        View focusFirst;
        Runnable backAction;

        @Override public void onBackPressed() {
            dismiss();                                  // Back = one step back (the "رجوع" button), not out of the menus
            if (backAction != null) backAction.run();
        }

        Dlg() {
            super(MainActivity.this, R.style.LauncherDialog);
        }

        ListView getListView() { return listView; }

        @Override public boolean dispatchKeyEvent(KeyEvent e) {
            lastInput = SystemClock.elapsedRealtime();
            return super.dispatchKeyEvent(e);
        }

        @Override public boolean dispatchTouchEvent(MotionEvent e) {
            lastInput = SystemClock.elapsedRealtime();
            return super.dispatchTouchEvent(e);
        }

        @Override protected void onStart() {
            super.onStart();
            openDialogs++;
        }

        @Override protected void onStop() {
            super.onStop();
            openDialogs = Math.max(0, openDialogs - 1);
            lastInput = SystemClock.elapsedRealtime();
        }
    }

    private int openDialogs = 0;

    /** Caps its height so long lists/messages scroll instead of leaving the screen. */
    private class CapFrame extends FrameLayout {
        final int maxH;

        CapFrame(int maxH) {
            super(MainActivity.this);
            this.maxH = maxH;
        }

        @Override protected void onMeasure(int w, int h) {
            super.onMeasure(w, MeasureSpec.makeMeasureSpec(maxH, MeasureSpec.AT_MOST));
        }
    }

    private class DlgBuilder {
        final String title;
        CharSequence msg;
        View view;
        BaseAdapter adapter;
        DialogInterface.OnClickListener itemL;
        final String[] btnText = new String[3];
        final DialogInterface.OnClickListener[] btnL = new DialogInterface.OnClickListener[3];
        boolean multi;
        boolean[] checked;
        DialogInterface.OnMultiChoiceClickListener multiL;
        String[] multiNames;

        boolean centerTitle;

        DlgBuilder(String t) { title = t; }

        DlgBuilder centered() { centerTitle = true; return this; }

        DlgBuilder setMessage(CharSequence m) { msg = m; return this; }

        DlgBuilder setView(View v) { view = v; return this; }

        DlgBuilder setAdapter(BaseAdapter a, DialogInterface.OnClickListener l) {
            adapter = a;
            itemL = l;
            return this;
        }

        DlgBuilder setItems(String[] items, DialogInterface.OnClickListener l) {
            return setAdapter(new MenuAdapter(items, -1), l);
        }

        DlgBuilder setMultiChoiceItems(String[] names, boolean[] on,
                                       DialogInterface.OnMultiChoiceClickListener l) {
            multi = true;
            multiNames = names;
            checked = on;
            multiL = l;
            return this;
        }

        DlgBuilder setPositiveButton(CharSequence t, DialogInterface.OnClickListener l) {
            btnText[2] = t.toString();
            btnL[2] = l;
            return this;
        }

        DlgBuilder setNegativeButton(CharSequence t, DialogInterface.OnClickListener l) {
            btnText[0] = t.toString();
            btnL[0] = l;
            return this;
        }

        DlgBuilder setNeutralButton(CharSequence t, DialogInterface.OnClickListener l) {
            btnText[1] = t.toString();
            btnL[1] = l;
            return this;
        }

        Dlg show() {
            Dlg d = create();
            d.show();
            return d;
        }

        Dlg create() {
            final Dlg d = new Dlg();
            d.backAction = () -> {
                if (btnL[0] != null) {
                    btnL[0].onClick(d, DialogInterface.BUTTON_NEGATIVE);
                    handler.post(() -> {
                        if (topDlg == null && pendingReturn != null) returnPending();
                    });
                } else returnPending();
            };
            topDlg = d;
            d.setOnDismissListener(x -> {
                if (topDlg == d) topDlg = null;
            });
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            final android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            int pct = Math.max(30, Math.min(100, prefs.getInt("ui_alpha", 92)));
            LinearLayout root = new LinearLayout(MainActivity.this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(argb(pct * 255 / 100, 0x0B, 0x1E, 0x17));
            bg.setCornerRadius(dp(22));
            bg.setStroke(dp(1), withAlpha(acc(), Math.min(255, pct * 255 / 100)));
            root.setBackground(bg);
            root.setPadding(dp(10), dp(6), dp(10), dp(14));
            root.setClipChildren(false);
            root.setClipToPadding(false);

            TextView tt = new TextView(MainActivity.this);
            tt.setText(title);
            tt.setTextSize(TypedValue.COMPLEX_UNIT_PX, fs(21));
            tt.setTypeface(tf(true));
            tt.setTextColor(acc());
            tt.setTextDirection(View.TEXT_DIRECTION_RTL);
            tt.setGravity((centerTitle ? Gravity.CENTER_HORIZONTAL : Gravity.RIGHT) | Gravity.CENTER_VERTICAL);
            tt.setPadding(dp(16), dp(14), dp(16), dp(10));
            root.addView(tt, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            int maxH = Math.max(dp(160), Math.round(dm.heightPixels * 0.88f) - dp(215));
            CapFrame body = new CapFrame(maxH);
            if (adapter != null || multi) {
                final BaseAdapter ad;
                if (multi) {
                    ad = new BaseAdapter() {
                        @Override public int getCount() { return multiNames.length; }
                        @Override public Object getItem(int i) { return multiNames[i]; }
                        @Override public long getItemId(int i) { return i; }
                        @Override public View getView(int p, View cv, ViewGroup par) {
                            TextView t = cv instanceof TextView ? (TextView) cv : newMenuRow();
                            t.setText((checked[p] ? "☑  " : "☐  ") + multiNames[p]);
                            return t;
                        }
                    };
                } else {
                    ad = adapter;
                }
                final ListView lv = new ListView(MainActivity.this);
                lv.setAdapter(ad);
                lv.setSelector(new ColorDrawable(Color.TRANSPARENT));
                lv.setDivider(new ColorDrawable(Color.TRANSPARENT));
                lv.setDividerHeight(dp(3));
                lv.setPadding(dp(4), dp(4), dp(4), dp(4));
                lv.setClipToPadding(false);
                lv.setVerticalScrollBarEnabled(false);
                lv.setOnItemClickListener((p, v, pos, id) -> {
                    if (multi) {
                        checked[pos] = !checked[pos];
                        ((BaseAdapter) lv.getAdapter()).notifyDataSetChanged();
                        if (multiL != null) multiL.onClick(d, pos, checked[pos]);
                    } else {
                        d.dismiss();
                        if (itemL != null) itemL.onClick(d, pos);
                    }
                });
                d.listView = lv;
                body.addView(lv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            } else if (view != null) {
                body.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            } else if (msg != null) {
                ScrollView sv = new ScrollView(MainActivity.this);
                sv.setFocusable(true);
                TextView mt = new TextView(MainActivity.this);
                mt.setText(msg);
                mt.setTextSize(TypedValue.COMPLEX_UNIT_PX, fs(16));
                mt.setTextColor(Color.WHITE);
                mt.setLineSpacing(0f, 1.15f);
                mt.setTextDirection(View.TEXT_DIRECTION_RTL);
                mt.setGravity(Gravity.RIGHT);
                mt.setPadding(dp(16), dp(4), dp(16), dp(8));
                sv.addView(mt, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                body.addView(sv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            root.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout row = new LinearLayout(MainActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            row.setGravity(Gravity.CENTER);
            boolean any = false;
            View firstBtn = null;
            for (int i = 0; i < 3; i++) {                  // left to right: cancel, neutral, OK
                if (btnText[i] == null) continue;
                any = true;
                final int which = i;
                TextView bt = new TextView(MainActivity.this);
                bt.setText(btnText[i]);
                bt.setTextSize(TypedValue.COMPLEX_UNIT_PX, fs(16));
                bt.setTypeface(tf(true));
                bt.setTextColor(i == 2 ? acc() : Color.WHITE);
                bt.setGravity(Gravity.CENTER);
                bt.setSingleLine(false);
                bt.setMinHeight(dp(46));
                bt.setPadding(dp(10), dp(6), dp(10), dp(6));
                bt.setFocusable(true);
                bt.setClickable(true);
                GradientDrawable on = new GradientDrawable();
                on.setColor(withAlpha(acc(), 0x33));
                on.setStroke(dp(2), acc());
                on.setCornerRadius(dp(12));
                GradientDrawable off = new GradientDrawable();
                off.setColor(0x22FFFFFF);
                off.setCornerRadius(dp(12));
                StateListDrawable sl = new StateListDrawable();
                sl.addState(new int[]{android.R.attr.state_focused}, on);
                sl.addState(new int[]{android.R.attr.state_pressed}, on);
                sl.addState(new int[]{}, off);
                bt.setBackground(sl);
                bt.setOnClickListener(v -> {
                    d.dismiss();
                    if (btnL[which] != null) btnL[which].onClick(d, which == 2 ? DialogInterface.BUTTON_POSITIVE
                            : which == 1 ? DialogInterface.BUTTON_NEUTRAL : DialogInterface.BUTTON_NEGATIVE);
                    else returnPending();
                    if (which == 0 && btnL[0] != null) handler.post(() -> {
                        if (topDlg == null && pendingReturn != null) returnPending();   // cancel that opened nothing: step back
                    });
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(dp(5), dp(8), dp(5), dp(4));
                row.addView(bt, lp);
                if (i == 2 || firstBtn == null) firstBtn = bt;
            }
            if (!any) {                                    // plain close button
                TextView bt = new TextView(MainActivity.this);
                bt.setText("إغلاق");
                bt.setTextColor(Color.WHITE);
                bt.setGravity(Gravity.CENTER);
                bt.setMinHeight(dp(46));
                bt.setFocusable(true);
                bt.setOnClickListener(v -> {
                    d.dismiss();
                    returnPending();
                });
                row.addView(bt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                firstBtn = bt;
            }
            d.focusFirst = firstBtn;
            root.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            int maxW = Math.min(dm.widthPixels - dp(40), Math.round(dm.widthPixels * 0.80f));
            int width = Math.min(maxW, Math.max(dp(380), Math.round(dm.widthPixels * 0.40f)));
            android.graphics.Paint mp = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            mp.setTypeface(tf(true));
            mp.setTextSize(fs(21));
            int need = Math.round(mp.measureText(title)) + dp(60);
            if (adapter instanceof MenuAdapter) {
                mp.setTextSize(fs(17));
                for (String lb : ((MenuAdapter) adapter).labels) {
                    need = Math.max(need, Math.round(mp.measureText(lb)) + dp(18 * 2 + 8 * 2 + 20 + 12));
                }
            } else if (multi) {
                mp.setTextSize(fs(17));
                for (String lb : multiNames) {
                    need = Math.max(need, Math.round(mp.measureText(lb)) + dp(100));
                }
            }
            width = Math.min(maxW, Math.max(width, need));
            d.setContentView(root, new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT));
            final Window w = d.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                if (pct < 90) w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                else w.setDimAmount(0.45f);
                w.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
                WindowManager.LayoutParams lp = w.getAttributes();
                int side = prefs.getInt("ui_side", 0);       // 0 left, 1 centre, 2 right
                lp.gravity = (side == 0 ? Gravity.LEFT : side == 2 ? Gravity.RIGHT
                        : Gravity.CENTER_HORIZONTAL) | Gravity.CENTER_VERTICAL;
                lp.x = side == 1 ? 0 : dp(28);
                lp.y = 0;
                w.setAttributes(lp);
            }
            d.setOnShowListener(x -> {
                if (w != null) { applySounds(w.getDecorView()); applyFont(w.getDecorView()); }
                if (d.listView != null) d.listView.requestFocus();
                else if (d.focusFirst != null) d.focusFirst.requestFocus();
            });
            return d;
        }
    }

    private DlgBuilder dialog(String title) {
        return new DlgBuilder(title);
    }

    /** Recursively applies the "sounds" setting (the framework plays focus sounds from the decor). */
    private void applySounds(View v) {
        boolean on = prefs.getBoolean("sounds", true);
        v.setSoundEffectsEnabled(on);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) applySounds(g.getChildAt(i));
        }
    }

    private class MenuAdapter extends BaseAdapter {
        final String[] labels;
        int sel;

        MenuAdapter(String[] l, int s) {
            labels = l;
            sel = s;
        }

        @Override public int getCount() { return labels.length; }

        @Override public Object getItem(int i) { return labels[i]; }

        @Override public long getItemId(int i) { return i; }

        @Override public View getView(int pos, View cv, ViewGroup parent) {
            TextView t = cv instanceof TextView ? (TextView) cv : newMenuRow();
            t.setText(labels[pos]);
            t.setTypeface(tf(false));
            return t;
        }
    }

    private TextView newMenuRow() {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, fs(17));
        t.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_selected}, {}},
                new int[]{acc(), 0xFFFFFFFF}));
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        t.setPadding(dp(18), dp(11), dp(18), dp(11));
        t.setMinHeight(dp(50));
        GradientDrawable on = new GradientDrawable();
        on.setColor(withAlpha(acc(), 0x33));
        on.setStroke(dp(2), acc());
        on.setCornerRadius(dp(12));
        StateListDrawable sl = new StateListDrawable();
        sl.setEnterFadeDuration(150);
        sl.setExitFadeDuration(220);
        sl.addState(new int[]{android.R.attr.state_selected}, on);
        sl.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
        t.setBackground(sl);
        t.setSoundEffectsEnabled(prefs.getBoolean("sounds", true));
        return t;
    }

    /** Menu/choice list that remembers the last highlighted row and shows it clearly. */
    private Runnable pendingReturn;
    private long pendingAt;
    private Dlg topDlg;

    /** Where "Back / Cancel / Close" of a dialog opened from a menu goes: back to that menu, one step. */
    private void returnPending() {
        Runnable r = pendingReturn;
        pendingReturn = null;
        if (r != null && android.os.SystemClock.elapsedRealtime() - pendingAt < 180_000L) r.run();
    }

    private void listDialog(final String title, final String[] labels, int initial,
                            final Pick pick, final Runnable back) {
        final MenuAdapter ad = new MenuAdapter(labels, initial);
        DlgBuilder b = dialog(title).setAdapter(ad, (d, which) -> {
            lastPos.put(title, which);
            final int w = which;
            pendingReturn = () -> listDialog(title, labels, w, pick, back);
            pendingAt = android.os.SystemClock.elapsedRealtime();
            pick.on(which);
        });
        if (back != null) b.setNegativeButton("رجوع", (d, w) -> {
            pendingReturn = null;
            back.run();
        });
        else b.setNegativeButton("إغلاق", (d, w) -> pendingReturn = null);
        final Dlg d = b.create();
        d.show();
        final ListView lv = d.getListView();
        lv.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                lastPos.put(title, pos);   // no adapter refresh here: it re-laid the list and ate a DOWN press
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        final int start = Math.max(0, Math.min(labels.length - 1, initial));
        lv.requestFocus();
        lv.post(() -> lv.setSelection(start));
    }

    private void menu(String title, List<String> labels, final List<Runnable> acts,
                      final Runnable back) {
        Integer lp = lastPos.get(title);
        listDialog(title, labels.toArray(new String[0]), lp == null ? 0 : lp,
                i -> acts.get(i).run(), back);
    }

    /** Single-choice list; the current value is marked with a check. */
    private void choice(String title, String[] labels, int selected, final Pick p,
                        final Runnable back) {
        String[] l = new String[labels.length];
        for (int i = 0; i < l.length; i++) l[i] = (i == selected ? "✓  " : "") + labels[i];
        Integer lp = lastPos.get(title);
        listDialog(title, l, selected >= 0 ? selected : (lp == null ? 0 : lp), p, back);
    }

    /** Smooth numeric stepper: arrows change by 1 (hold to speed up), changes preview live. */
    private void numberDialog(String title, final String key, final int def, final int min,
                              final int max, final String unit, final String zero,
                              final Runnable live, final Runnable done, final Runnable back) {
        final int orig = prefs.getInt(key, def);
        final int[] val = {Math.max(min, Math.min(max, orig))};
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(20), dp(10), dp(20), dp(6));
        final TextView tv = text(44, true);
        tv.setTextColor(acc());
        tv.setGravity(Gravity.CENTER);
        TextView hint = text(13, false);
        hint.setTextDirection(View.TEXT_DIRECTION_RTL);
        hint.setSingleLine(false);
        hint.setGravity(Gravity.CENTER);
        hint.setText("▲ ▶ زيادة  •  ▼ ◀ نقصان  •  الضغط المطوّل يسرّع  •  OK حفظ");
        final Runnable refresh = () -> {
            tv.setText(val[0] == 0 && zero != null ? zero : val[0] + unit);
            prefs.edit().putInt(key, val[0]).apply();
            if (live != null) live.run();
        };
        refresh.run();
        box.addView(tv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(hint, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setGravity(Gravity.CENTER);
        btns.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        int[] deltas = {-10, -1, 1, 10};
        for (final int dl : deltas) {
            TextView b = text(16, true);
            b.setText(dl > 0 ? "+" + dl : String.valueOf(dl));
            b.setGravity(Gravity.CENTER);
            GradientDrawable bb = new GradientDrawable();
            bb.setColor(0x26FFFFFF);
            bb.setCornerRadius(dp(10));
            b.setBackground(bb);
            b.setClickable(true);                     // touch only; D-pad uses the arrows
            b.setOnClickListener(v -> {
                val[0] = Math.max(min, Math.min(max, val[0] + dl));
                refresh.run();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(56), dp(38));
            lp.setMargins(dp(5), dp(10), dp(5), dp(4));
            btns.addView(b, lp);
        }
        box.addView(btns);

        final Runnable restore = () -> {
            prefs.edit().putInt(key, orig).apply();
            if (live != null) live.run();
            if (back != null) back.run();
        };
        final Dlg d = dialog(title).setView(box)
                .setPositiveButton("حفظ", (dd, w) -> {
                    if (done != null) done.run();
                })
                .setNegativeButton("إلغاء", (dd, w) -> restore.run())
                .create();
        d.setOnCancelListener(x -> restore.run());
        d.setOnKeyListener((dd, code, ev) -> {
            boolean arrow = code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN
                    || code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT;
            if (!arrow) return false;
            if (ev.getAction() != KeyEvent.ACTION_DOWN) return true;
            int rc = ev.getRepeatCount();
            int step = rc > 25 ? 5 : rc > 8 ? 2 : 1;
            boolean up = code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_RIGHT;
            val[0] = Math.max(min, Math.min(max, val[0] + (up ? step : -step)));
            refresh.run();
            tick();
            return true;
        });
        d.show();
    }

    /** Scrollable info dialog: Arabic text (RTL) plus an optional LTR block (paths, logs). */
    /** One unbroken line: shrinks the text until it fits the dialog width. */
    private void fitOneLine(TextView t, String text, float startPx) {
        int avail = Math.min(getResources().getDisplayMetrics().widthPixels * 7 / 10, dp(620)) - dp(60);
        float px = startPx;
        t.setSingleLine(true);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        while (px > dp(8) && t.getPaint().measureText(text) > avail) {
            px -= 1f;
            t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        }
    }

    private Dlg showInfo(String title, String rtl, String ltr, String posLabel,
                                 final Runnable pos, String neuLabel, final Runnable neu,
                                 String negLabel, final Runnable negAct) {
        ScrollView sv = new ScrollView(this);
        sv.setFocusable(true);                        // so the D-pad can scroll long text
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(8), dp(22), dp(8));
        if (rtl != null && !rtl.isEmpty()) {
            TextView a = new TextView(this);
            a.setTextSize(TypedValue.COMPLEX_UNIT_PX, fs(16));
            a.setTextColor(Color.WHITE);
            a.setTextDirection(View.TEXT_DIRECTION_RTL);
            a.setGravity(Gravity.RIGHT);
            a.setLineSpacing(0f, 1.15f);
            a.setText(rtl);
            box.addView(a, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        if (ltr != null && !ltr.isEmpty()) {
            TextView b = new TextView(this);
            b.setTextColor(0xFFCFE3DA);
            b.setTypeface(Typeface.DEFAULT);
            b.setTextDirection(View.TEXT_DIRECTION_LTR);
            b.setGravity(Gravity.CENTER);
            b.setIncludeFontPadding(true);
            b.setPadding(0, 0, 0, dp(8));
            b.setText(ltr);
            fitOneLine(b, ltr, fs(14));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(10);
            box.addView(b, lp);
        }
        sv.addView(box);
        DlgBuilder b = dialog(title).setView(sv);
        b.setPositiveButton(posLabel == null ? "حسناً" : posLabel, (d, w) -> {
            if (pos != null) pos.run();
        });
        if (neuLabel != null) b.setNeutralButton(neuLabel, (d, w) -> {
            if (neu != null) neu.run();
        });
        if (negLabel != null) b.setNegativeButton(negLabel, (d, w) -> {
            if (negAct != null) negAct.run();
        });
        Dlg d = b.create();
        d.show();
        return d;
    }

    private void runShortcut(Shortcut s) {
        String first = s.actions[0];
        if (first.startsWith("#")) {
            if (first.equals("#settings")) showLauncherSettings();
            else if (first.equals("#prayer")) showPrayerDiagnostics();
            else if (first.equals("#hidden")) showHiddenDialog();
            else if (first.equals("#backup")) backupSettings();
            else if (first.equals("#restore")) restoreSettings();
            else if (first.equals("#reload")) {
                webView.reload();
                toast("جارٍ تحديث الصفحة…");
            }
            return;
        }
        launchSetting(s, 0);
    }

    private void launchSetting(final Shortcut s, int from) {
        for (int i = from; i < s.actions.length; i++) {
            try {
                Intent in = new Intent(s.actions[i]);
                in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(in);
                return;
            } catch (Exception e) {
                AppLog.info("SETTINGS", s.actions[i] + " refused: " + e);
            }
        }
        AdbShell.run(this, "am start -a " + s.actions[0], new AdbShell.Callback() {
            @Override public void done(boolean ok, String output) {
                if (!ok) toast("غير متاح (جرّب ⚙ ← إعدادات Mawaqit Home ← اتصال ADB الذاتي)");
            }
        });
    }

    private void showAppMenu(final AppEntry a) {
        final boolean fav = favs.contains(a.pkg);
        final List<String> items = new ArrayList<>();
        final List<Runnable> acts = new ArrayList<>();
        items.add("↔ تحريك");
        acts.add(() -> startMove(a));
        items.add(fav ? "★ حذف من المفضلة" : "☆ إضافة إلى المفضلة");
        acts.add(() -> toggleFavorite(a));
        items.add("📂 التصنيفات…");
        acts.add(() -> {
            if (cats.isEmpty()) {
                toast("لا توجد تصنيفات مخصصة بعد: ⚙ ← الشريط والتصنيفات");
            } else {
                chooseAppCategories(a);
            }
        });
        items.add("🙈 إخفاء من اللانشر");
        acts.add(() -> hideApp(a));
        items.add("ℹ معلومات التطبيق");
        acts.add(() -> openPackageScreen(APP_DETAILS_ACTION, a.pkg));
        items.add("🗑 إلغاء التثبيت");
        acts.add(() -> openPackageScreen(Intent.ACTION_DELETE, a.pkg));
        dialog(a.label).setItems(items.toArray(new String[0]), (d, which) -> acts.get(which).run())
                .show();
    }

    private void chooseAppCategories(final AppEntry a) {
        String[] names = new String[cats.size()];
        final boolean[] on = new boolean[cats.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = cats.get(i).icon + "  " + cats.get(i).name;
            on[i] = cats.get(i).pkgs.contains(a.pkg);
        }
        dialog("تصنيفات: " + a.label)
                .setMultiChoiceItems(names, on, (d, which, checked) -> on[which] = checked)
                .setPositiveButton("حفظ", (d, w) -> {
                    for (int i = 0; i < on.length; i++) {
                        Cat c = cats.get(i);
                        c.pkgs.remove(a.pkg);
                        if (on[i]) c.pkgs.add(a.pkg);
                    }
                    saveCats();
                    showTab(currentTab, -1);
                })
                .setNegativeButton("إلغاء", null).show();
    }

    private void openPackageScreen(String action, String pkg) {
        try {
            Intent i = new Intent(action, Uri.parse("package:" + pkg));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            toast("غير متاح على هذا الجهاز");
        }
    }

    // --------------------------------------------------- Launcher settings

    private int stageMinutes(int i) {
        return prefs.getInt("t" + (i + 1), STAGE_DEFAULTS[i]);
    }

    private void setStageMinutes(int i, int v) {
        int[] t = new int[4];
        for (int j = 0; j < 4; j++) t[j] = stageMinutes(j);
        t[i] = v;
        int prev = 0;
        for (int j = 0; j < 4; j++) {
            if (t[j] > 0) {
                if (t[j] <= prev) t[j] = prev + 1;
                prev = t[j];
            }
        }
        SharedPreferences.Editor ed = prefs.edit();
        for (int j = 0; j < 4; j++) ed.putInt("t" + (j + 1), t[j]);
        ed.apply();
        lastInput = SystemClock.elapsedRealtime();
    }

    private static String minLabel(int m) {
        return m == 0 ? "متوقف" : m + " د";
    }

    private static String onOff(boolean b) {
        return b ? "تشغيل" : "إيقاف";
    }

    private static int indexOf(int[] arr, int v, int def) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == v) return i;
        return def;
    }

    private void showLauncherSettings() {
        final List<String> l = new ArrayList<>();
        final List<Runnable> a = new ArrayList<>();
        l.add("📊  لوحة الحالة");
        a.add(this::showStatus);
        l.add("🔗  رابط الصفحة");
        a.add(this::promptUrl);
        l.add("🌙  حماية الشاشة (الاختفاء والتعتيم والنوم)");
        a.add(this::showSaverSettings);
        l.add("☰  الشريط والتصنيفات والقائمة");
        a.add(this::showStripSettings);
        l.add("🌦  بطاقة الطقس");
        a.add(this::showWeatherSettings);
        l.add("🎯  تحريك عناصر صفحة المواقيت");
        a.add(this::showPageElements);
        l.add("🕒  الوقت والتاريخ والمنطقة الزمنية");
        a.add(this::showTimeSettings);
        l.add("🎛  الصوت والحركة والمظهر");
        a.add(this::showMotionSettings);
        l.add("🔄  التحديثات" + (pendingInfo != null ? "  ⬆ متاح" : ""));
        a.add(this::showUpdateSettings);
        l.add("🚀  التشغيل عند الإقلاع:  " + onOff(prefs.getBoolean("boot", true)));
        a.add(() -> {
            prefs.edit().putBoolean("boot", !prefs.getBoolean("boot", true)).apply();
            showLauncherSettings();
        });
        l.add("💾  النسخ الاحتياطي وملفات التعريف (رمضان وغيره)");
        a.add(this::showBackupMenu);
        l.add("🔒  مدير الجهاز (نوم الجهاز):  " + (adminActive() ? "مفعّل" : "غير مفعّل"));
        a.add(this::showAdminDialog);
        l.add("🖼  صور الخلفية من الإنترنت:  " + (bgEnabled() ? "تعمل" : "متوقفة"));
        a.add(this::showBgSettings);
        l.add("🔌  اتصال ADB الذاتي");
        a.add(this::showAdbDialog);
        l.add("📋  سجل الأخطاء ومشاركته");
        a.add(this::showLogDialog);
        l.add("🔁  إعادة تشغيل التطبيق");
        a.add(this::restartApp);
        l.add("↩  إعادة ضبط اللانشر");
        a.add(this::showReset);
        l.add("ℹ  حول التطبيق — الإصدار " + Updater.currentName(this));
        a.add(this::showAbout);
        menu("إعدادات Mawaqit Home", l, a, null);
    }

    private void restartApp() {
        AppLog.info("APP", "restart requested");
        try {
            Intent i = new Intent(this, MainActivity.class).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            android.app.PendingIntent pi = android.app.PendingIntent.getActivity(this, 77, i,
                    android.app.PendingIntent.FLAG_CANCEL_CURRENT);
            ((android.app.AlarmManager) getSystemService(ALARM_SERVICE)).set(
                    android.app.AlarmManager.RTC, System.currentTimeMillis() + 800, pi);
        } catch (Throwable t) {
            AppLog.error("APP", "restart alarm failed", t);
        }
        finishAffinity();
        handler.postDelayed(() -> android.os.Process.killProcess(android.os.Process.myPid()), 300L);
    }

    private String statusText() {
        long now = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append("🌐 الإنترنت: ").append(online() ? "متصل ✓" : "غير متصل ✗").append("\n");
        sb.append("📄 الصفحة: ").append(pageOk ? "تعمل ✓" : "لم تُحمَّل ✗");
        if (pageLoadedAt > 0) sb.append("\n      آخر تحميل: ").append(TimeCfg.dateTime(prefs, pageLoadedAt));
        sb.append("\n");
        long ts = prefs.getLong("prayers_ts", 0L);
        sb.append("🕌 أوقات الصلاة: ");
        if (ts > 0) {
            sb.append(prefs.getString("prayers_src", "—")).append("\n      آخر قراءة: ")
                    .append(TimeCfg.dateTime(prefs, ts));
        } else sb.append("لم تُقرأ بعد");
        sb.append("\n");
        long nw = prefs.getLong("next_wake", 0L);
        sb.append("⏰ الإيقاظ القادم: ").append(nw > 0 ? TimeCfg.dateTime(prefs, nw) : "غير مجدول").append("\n");
        sb.append("🌦 الطقس: ");
        if (weatherAt > 0) {
            sb.append(currentCity()).append("\n      آخر تحديث: ").append(TimeCfg.dateTime(prefs, weatherAt));
        } else sb.append(weatherFailed ? "فشل التحميل ✗" : "جارٍ التحميل…");
        sb.append("\n");
        int as = prefs.getInt("adb_state", 0);
        sb.append("🔌 ADB الذاتي: ").append(as == 0 ? "لم يُجرَّب بعد" : as == 1 ? "يعمل ✓" : "غير متصل ✗").append("\n");
        sb.append("🔄 الإصدار: ").append(Updater.currentName(this));
        if (pendingInfo != null) sb.append("   ⬆ متاح: ").append(pendingInfo.name);
        sb.append("\n");
        sb.append("🕒 الآن: ").append(TimeCfg.dateTime(prefs, now)).append("\n      ")
                .append(TimeCfg.zoneId()).append(TimeCfg.dst() ? " — توقيت صيفي" : " — توقيت شتوي").append("\n");
        if (Updater.skewAt > 0) {
            double sec = Math.abs(Updater.skewMs) / 1000.0;
            sb.append("⏱ ساعة الجهاز مقابل الإنترنت: ")
                    .append(sec < 2 ? "مضبوطة ✓" : String.format(Locale.US, "%.0f ث %s", sec,
                            Updater.skewMs > 0 ? "متأخرة" : "متقدمة")).append("\n");
        }
        File f = AppLog.file();
        sb.append("📋 السجل: ").append(f != null ? (f.length() / 1024) + " KB" : "—");
        return sb.toString();
    }

    private void showStatus() {
        showInfo("لوحة الحالة", statusText(), null, "حسناً", null, "تحديث", this::showStatus,
                null, null);
    }

    private void showReset() {
        showInfo("إعادة ضبط اللانشر",
                "• إعادة الإعدادات: يعيد المظهر والأوقات وكل الخيارات إلى الأصل، ويحتفظ بالمفضلة "
                        + "والتصنيفات والتطبيقات المخفية والرابط.\n\n"
                        + "• إعادة كاملة: يمسح كل شيء (عدا اتصال ADB).",
                null, "إعادة الإعدادات", () -> doReset(false), "إعادة كاملة", () -> doReset(true),
                "إلغاء", null);
    }

    private void doReset(boolean full) {
        java.util.Set<String> soft = new java.util.HashSet<>(Arrays.asList(
                "favs", "order", "hidden", "cats", "url_v2"));
        java.util.Set<String> always = new java.util.HashSet<>(Arrays.asList(
                "last_code", "adb_state", "adb_ts"));
        Map<String, Object> keep = new HashMap<>();
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            if (always.contains(e.getKey()) || (!full && soft.contains(e.getKey()))) {
                keep.put(e.getKey(), e.getValue());
            }
        }
        SharedPreferences.Editor ed = prefs.edit();
        ed.clear();
        for (Map.Entry<String, Object> e : keep.entrySet()) {
            Object v = e.getValue();
            if (v instanceof String) ed.putString(e.getKey(), (String) v);
            else if (v instanceof Integer) ed.putInt(e.getKey(), (Integer) v);
            else if (v instanceof Long) ed.putLong(e.getKey(), (Long) v);
            else if (v instanceof Boolean) ed.putBoolean(e.getKey(), (Boolean) v);
        }
        ed.apply();
        lastPos.clear();
        AppLog.info("SETTINGS", "reset " + (full ? "full" : "settings"));
        toast("تمت إعادة الضبط");
        recreate();
    }

    // ---- time

    private static final String[] TZ_IDS = {"", "Europe/Berlin", "Europe/London", "Europe/Paris",
            "Europe/Istanbul", "Asia/Riyadh", "Asia/Dubai", "Asia/Amman", "Asia/Damascus",
            "Asia/Baghdad", "Africa/Cairo", "Africa/Casablanca", "America/New_York", "UTC"};
    private static final String[] TZ_NAMES = {"تلقائي (حسب ساعة الجهاز)", "ألمانيا — برلين",
            "بريطانيا — لندن", "فرنسا — باريس", "تركيا — إسطنبول", "السعودية — الرياض",
            "الإمارات — دبي", "الأردن — عمّان", "سوريا — دمشق", "العراق — بغداد", "مصر — القاهرة",
            "المغرب — الدار البيضاء", "أمريكا — نيويورك", "UTC", "كتابة معرّف يدوياً…"};

    private void showTimeSettings() {
        final List<String> l = new ArrayList<>();
        final List<Runnable> a = new ArrayList<>();
        final boolean h24 = prefs.getBoolean("h24", true);
        l.add("🕒  نظام الساعة:  " + (h24 ? "24 ساعة" : "12 ساعة"));
        a.add(() -> choice("نظام الساعة", new String[]{"24 ساعة", "12 ساعة (ص / م)"}, h24 ? 0 : 1,
                i -> {
                    prefs.edit().putBoolean("h24", i == 0).apply();
                    showTimeSettings();
                }, this::showTimeSettings));
        String tz = prefs.getString("tz", "");
        l.add("🌍  المنطقة الزمنية:  " + (tz.isEmpty() ? "تلقائي — " + TimeCfg.zoneId() : tz));
        a.add(() -> {
            int cur = Arrays.asList(TZ_IDS).indexOf(prefs.getString("tz", ""));
            choice("المنطقة الزمنية (تؤثر على كل الأوقات وعلى منبّه الأذان)", TZ_NAMES, cur,
                    i -> {
                        if (i < TZ_IDS.length) applyTz(TZ_IDS[i]);
                        else promptText("معرّف المنطقة", "Europe/Berlin", "", v -> {
                            if (TimeCfg.valid(v)) applyTz(v);
                            else {
                                toast("معرّف غير معروف");
                                showTimeSettings();
                            }
                        });
                    }, this::showTimeSettings);
        });
        l.add("☀  التوقيت الصيفي:  تلقائي — حالياً " + (TimeCfg.dst() ? "صيفي" : "شتوي"));
        a.add(this::showTimeSettings);
        l.add("📅  الآن:  " + TimeCfg.dateTime(prefs, System.currentTimeMillis()));
        a.add(this::showTimeSettings);
        if (Updater.skewAt > 0) {
            double sec = Math.abs(Updater.skewMs) / 1000.0;
            l.add("⏱  ساعة الجهاز مقابل الإنترنت:  " + (sec < 2 ? "مضبوطة ✓"
                    : String.format(Locale.US, "%.0f ث %s", sec, Updater.skewMs > 0 ? "متأخرة" : "متقدمة")));
            a.add(this::showTimeSettings);
        }
        menu("الوقت والتاريخ", l, a, this::showLauncherSettings);
    }

    private void applyTz(String id) {
        prefs.edit().putString("tz", id).apply();
        TimeCfg.apply(this);
        WakeScheduler.scheduleNext(this);
        lastDay = -1;
        AppLog.info("TIME", "time zone -> " + (id.isEmpty() ? "device (" + TimeCfg.zoneId() + ")" : id));
        showTimeSettings();
    }

    private void aboutRow(LinearLayout box, String title, String value, boolean ltr) {
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, fs(13));
        t.setTextColor(acc());
        t.setTypeface(tf(true));
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(12), 0, dp(2));
        box.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(TypedValue.COMPLEX_UNIT_PX, fs(ltr ? 13 : 16));
        v.setTextColor(Color.WHITE);
        v.setTextDirection(ltr ? View.TEXT_DIRECTION_LTR : View.TEXT_DIRECTION_RTL);
        v.setGravity(Gravity.CENTER);
        v.setLineSpacing(0f, 1.1f);
        box.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void showAbout() {
        ScrollView sv = new ScrollView(this);
        sv.setFocusable(true);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(16), dp(4), dp(16), dp(8));
        aboutRow(box, "الإصدار", Updater.currentName(this) + "   (build " + Updater.currentCode(this) + ")", false);
        aboutRow(box, "حقوق النشر", "© Moh-F — جميع الحقوق محفوظة", false);
        aboutRow(box, "بيانات الطقس", "Open-Meteo.com (CC BY 4.0)", false);
        aboutRow(box, "أوقات الصلاة", "Mawaqit / Aladhan", false);
        aboutRow(box, "الخطوط", "Tajawal · Almarai · Amiri · Scheherazade · Lalezar (OFL)", false);
        aboutRow(box, "الصفحة", url, true);
        aboutRow(box, "ملف السجل", AppLog.file() != null ? AppLog.file().getAbsolutePath() : "—", true);
        sv.addView(box);
        dialog("Mawaqit Home").centered().setView(sv).setPositiveButton("حسناً", null).show();
    }

    private TextView addressView(String addr) {
        TextView a = new TextView(this);
        a.setTextColor(acc());
        a.setTypeface(Typeface.DEFAULT_BOLD);
        a.setTextDirection(View.TEXT_DIRECTION_LTR);
        a.setGravity(Gravity.CENTER);
        a.setIncludeFontPadding(true);
        a.setText(addr);
        fitOneLine(a, addr, fs(15));
        a.setPadding(0, dp(6), 0, dp(10));
        return a;
    }

    private ImageView qrView(String data) {
        ImageView qr = new ImageView(this);
        try {
            com.google.zxing.common.BitMatrix m = new com.google.zxing.qrcode.QRCodeWriter().encode(data,
                    com.google.zxing.BarcodeFormat.QR_CODE, 400, 400);
            Bitmap bm = Bitmap.createBitmap(400, 400, Bitmap.Config.RGB_565);
            for (int x = 0; x < 400; x++)
                for (int y = 0; y < 400; y++) bm.setPixel(x, y, m.get(x, y) ? Color.BLACK : Color.WHITE);
            qr.setImageBitmap(bm);
        } catch (Exception e) {
            AppLog.error("QR", "qr failed", e);
        }
        return qr;
    }

    private final PhoneServer phoneSrv = new PhoneServer();

    private static String newToken() {
        return Long.toString(Math.abs(new java.security.SecureRandom().nextLong()), 36).substring(0, 6);
    }

    /** Dialog with a QR code + address (own line) for a phone/laptop on the same network. */
    private Dlg qrDialog(String title, String msg, String addr, String okLabel, final Runnable onClose) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.addView(qrView(addr), new LinearLayout.LayoutParams(dp(210), dp(210)));
        TextView t = text(13, false);
        t.setText(msg);
        t.setGravity(Gravity.CENTER);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        box.addView(t);
        box.addView(addressView(addr));
        DlgBuilder b = dialog(title).setView(box);
        b.setPositiveButton(okLabel, (d, w) -> {
            phoneSrv.stop();
            if (onClose != null) onClose.run();
        });
        b.setNegativeButton("إلغاء", (d, w) -> {
            phoneSrv.stop();
            if (onClose != null) onClose.run();
        });
        Dlg d = b.create();
        d.show();
        return d;
    }

    /** Type text on a phone/laptop instead of the remote: QR code -> small web form -> text arrives here. */
    private void phoneInput(final String title, final String initial, final TextCb cb) {
        final String ip = localIp();
        if (ip == null) {
            toast("لا توجد شبكة محلية — أدخل النص يدوياً");
            return;
        }
        final String token = newToken();
        final String addr = "http://" + ip + ":8767/" + token;
        final Dlg[] dh = new Dlg[1];
        boolean ok = phoneSrv.start(8767, (m, path, body) -> {
            if (!path.startsWith("/" + token)) return new PhoneServer.Resp(404, "text/plain", new byte[0]);
            if (m.equals("POST")) {
                String txt = "";
                try {
                    for (String kv : new String(body, "UTF-8").split("&"))
                        if (kv.startsWith("t=")) txt = urlDecode(kv.substring(2));
                } catch (Exception ignored) {
                }
                final String ft = txt.trim();
                runOnUiThread(() -> {
                    phoneSrv.stop();
                    try {
                        if (dh[0] != null) dh[0].dismiss();
                    } catch (Exception ignored) {
                    }
                    cb.on(ft);
                });
                return PhoneServer.Resp.html("<h2>✓ وصل النص إلى الشاشة</h2>");
            }
            return PhoneServer.Resp.html("<h2>" + esc(title) + "</h2><form method=post action='/" + token + "'>"
                    + "<textarea name=t rows=6 style='width:100%;direction:ltr;font-size:18px'>" + esc(initial)
                    + "</textarea><p><button style='font-size:22px;padding:12px 28px'>💾 إرسال إلى الشاشة</button></p></form>");
        });
        if (!ok) {
            toast("تعذّر تشغيل الخادم");
            return;
        }
        handler.postDelayed(phoneSrv::stop, 10L * 60 * 1000);
        dh[0] = qrDialog(title, "امسح الرمز بالجوال (نفس الواي فاي) أو افتح العنوان، اكتب النص ثم اضغط «إرسال».",
                addr, "إغلاق", null);
    }

    /** Screen capture of the launcher, shared through a QR code to a phone / laptop. */
    private void shareScreenshot() {
        shareScreenshot(900L);
    }

    /** delay: time to let the person open the menu they want captured (menus are included in the picture). */
    private void shareScreenshot(long delayMs) {
        final String ip = localIp();
        if (ip == null) {
            toast("لا توجد شبكة محلية");
            return;
        }
        toast(delayMs > 2000 ? "افتح القائمة المطلوبة — سيُلتقط المشهد بعد " + (delayMs / 1000) + " ثوانٍ"
                : "سيُلتقط المشهد بعد لحظة…");
        handler.postDelayed(() -> {
            try {
                int w = root.getWidth(), h = root.getHeight();
                Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas cv = new Canvas(bm);
                cv.drawColor(Color.BLACK);
                String info = "";
                // 1) our own background underneath (the WebView cannot be drawn with its pictures/videos)
                if (videoBox != null && videoBox.getVisibility() == View.VISIBLE && vcur >= 0
                        && vtv[vcur] != null && vtv[vcur].isAvailable()) {
                    Bitmap vb = vtv[vcur].getBitmap();
                    if (vb != null) {
                        cv.drawBitmap(vb, (w - vb.getWidth()) / 2f, (h - vb.getHeight()) / 2f, null);
                        info += " video";
                    }
                } else if (bgEnabled() && !bgVideo() && bgFile().exists()) {
                    android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
                    o.inSampleSize = 2;
                    Bitmap img = android.graphics.BitmapFactory.decodeFile(bgFile().getAbsolutePath(), o);
                    if (img != null) {
                        float sc = Math.max(w / (float) img.getWidth(), h / (float) img.getHeight());
                        cv.save();
                        cv.translate((w - img.getWidth() * sc) / 2f, (h - img.getHeight() * sc) / 2f);
                        cv.scale(sc, sc);
                        cv.drawBitmap(img, 0, 0, null);
                        cv.restore();
                        cv.drawColor(Color.argb(Math.round(2.55f * Math.max(0, Math.min(85, prefs.getInt("bg_dim", 35)))), 0, 0, 0));
                        img.recycle();
                        info += " image";
                    }
                }
                // 2) the launcher itself (the WebView is drawn separately so a solid page colour can be detected)
                Bitmap wb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas wc = new Canvas(wb);
                android.graphics.drawable.Drawable bgd = root.getBackground();
                root.setBackground(null);
                root.draw(wc);
                root.setBackground(bgd);
                int opaque = 0, n = 0;
                for (int y = 20; y < h; y += Math.max(1, h / 12))
                    for (int x = 20; x < w; x += Math.max(1, w / 16)) {
                        n++;
                        if ((wb.getPixel(x, y) >>> 24) > 250) opaque++;
                    }
                AppLog.info("SHOT", "bg layers:" + info + "; launcher layer opaque samples " + opaque + "/" + n);
                cv.drawBitmap(wb, 0, 0, null);
                wb.recycle();
                // 3) the open menu / dialog, if any
                Dlg td = topDlg;
                if (td != null && td.isShowing() && td.getWindow() != null) {
                    View dv = td.getWindow().getDecorView();
                    int[] loc = new int[2];
                    int[] rl = new int[2];
                    dv.getLocationOnScreen(loc);
                    root.getLocationOnScreen(rl);
                    cv.save();
                    cv.translate(loc[0] - rl[0], loc[1] - rl[1]);
                    dv.draw(cv);
                    cv.restore();
                }
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 92, bo);
                bm.recycle();
                final byte[] jpg = bo.toByteArray();
                final String token = newToken();
                final String addr = "http://" + ip + ":8767/" + token;
                boolean ok = phoneSrv.start(8767, (m, path, body) -> {
                    if (!path.startsWith("/" + token)) return new PhoneServer.Resp(404, "text/plain", new byte[0]);
                    if (path.endsWith(".jpg")) return new PhoneServer.Resp(200, "image/jpeg", jpg);
                    return PhoneServer.Resp.html("<h2>لقطة الشاشة</h2><img src='/" + token
                            + "/screen.jpg' style='width:100%'><p><a download='mawaqit-screen.jpg' href='/" + token
                            + "/screen.jpg' style='font-size:22px'>⬇ تنزيل الصورة</a></p>");
                });
                if (!ok) {
                    toast("تعذّر تشغيل الخادم");
                    return;
                }
                handler.postDelayed(phoneSrv::stop, 10L * 60 * 1000);
                pendingReturn = null;
                qrDialog("لقطة الشاشة", "امسح الرمز بالجوال أو الكمبيوتر (نفس الواي فاي) لعرض الصورة وتنزيلها. "
                        + "ينتهي الرابط بعد 10 دقائق.", addr, "تم", null);
            } catch (Throwable e) {
                AppLog.error("SHOT", "screenshot failed", e);
                toast("تعذّر التقاط الصورة");
            }
        }, delayMs);
    }

    private void promptText(String title, String hint, String initial, final TextCb onOk) {
        final EditText et = new EditText(this);
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        et.setHint(hint);
        if (initial.startsWith("http") || hint.startsWith("http") || hint.contains("/")) {
            et.setTextDirection(View.TEXT_DIRECTION_LTR);
            et.setGravity(Gravity.LEFT);
        }
        et.setText(initial);
        et.setSelection(et.getText().length());
        final String pTitle = title;
        dialog(title).setView(et)
                .setPositiveButton("حفظ", (d, w) -> onOk.on(et.getText().toString().trim()))
                .setNeutralButton("📱 من الجوال", (d, w) -> phoneInput(pTitle, et.getText().toString(), onOk))
                .setNegativeButton("إلغاء", null).show();
    }

    private void promptUrl() {
        promptText("رابط صفحة المواقيت", "https://mawaqit.net/ar/…", url, v -> {
            if (!v.startsWith("https://")) {
                toast("الرابط لازم يبدأ بـ https://");
                return;
            }
            prefs.edit().putString("url_v2", v).apply();
            url = v;
            webView.loadUrl(url);
            AppLog.info("SETTINGS", "url changed: " + v);
            toast("تم تغيير الرابط");
        });
    }

    // ---- screen saver

    private void showSaverSettings() {
        final List<String> l = new ArrayList<>();
        final List<Runnable> a = new ArrayList<>();
        for (int i = 0; i < STAGE_NAMES.length; i++) {
            final int si = i;
            l.add(STAGE_NAMES[i] + ":  " + minLabel(stageMinutes(i)));
            a.add(() -> numberDialog(STAGE_NAMES[si]
                            + (si == 3 ? "\n(يحتاج أوقات الصلاة معروفة ليوقظه المنبّه قبل الأذان)" : ""),
                    "t" + (si + 1), STAGE_DEFAULTS[si], 0, 240, " د", "متوقف", null, () -> {
                        setStageMinutes(si, prefs.getInt("t" + (si + 1), STAGE_DEFAULTS[si]));
                        showSaverSettings();
                    }, this::showSaverSettings));
        }
        l.add("🫧  شفافية كل العناصر أثناء شاشة التوقف:  " + prefs.getInt("sv_clear", 0) + "%  (لتظهر الخلفية أكثر)");
        a.add(() -> numberDialog("شفافية الصفحة والبطاقة أثناء شاشة التوقف", "sv_clear", 0, 0, 90, "%", null,
                null, this::showSaverSettings, this::showSaverSettings));
        l.add("⚡ وضع تجربة سريع (1 / 2 / 3 دقائق)");
        a.add(() -> applyPreset(new int[]{1, 2, 3, 0}));
        l.add("↩ الافتراضي (10 / 15 / 20 / متوقف)");
        a.add(() -> applyPreset(STAGE_DEFAULTS));
        menu("حماية الشاشة — دقائق بدون استخدام الريموت", l, a, this::showLauncherSettings);
    }

    private void applyPreset(int[] v) {
        SharedPreferences.Editor ed = prefs.edit();
        for (int j = 0; j < 4; j++) ed.putInt("t" + (j + 1), v[j]);
        ed.apply();
        lastInput = SystemClock.elapsedRealtime();
        showSaverSettings();
    }

    // ---- strip / categories / menu

    private void showStripSettings() {
        final List<String> l = new ArrayList<>();
        final List<Runnable> a = new ArrayList<>();
        l.add("☰  عدد صفوف الأيقونات:  " + rows);
        a.add(() -> choice("عدد الصفوف", new String[]{"صف واحد", "صفان", "ثلاثة صفوف"}, rows - 1,
                i -> {
                    prefs.edit().putInt("rows", i + 1).apply();
                    recreate();
                }, this::showStripSettings));
        l.add("🎨  شكل الأيقونات:  " + ICON_STYLES[Math.max(0, Math.min(ICON_STYLES.length - 1, prefs.getInt("icon_style", 0)))]);
        a.add(() -> choice("شكل الأيقونات", ICON_STYLES, prefs.getInt("icon_style", 0), i -> {
            prefs.edit().putInt("icon_style", i).apply();
            recreate();
        }, this::showStripSettings));
        l.add("🔍  حجم الأيقونة داخل المربع:  " + prefs.getInt("icon_fill", 84) + "%");
        a.add(() -> numberDialog("حجم الأيقونة داخل المربع (أكبر = أوضح)", "icon_fill", 84, 50, 100, "%",
                null, null, this::recreate, this::showStripSettings));
        l.add("🔲  حجم الأيقونات:  " + prefs.getInt("tile_pct", 100) + "%");
        a.add(() -> numberDialog("حجم الأيقونات (يكبر حتى يملأ ارتفاع الشريط)", "tile_pct", 100, 40, 300, "%",
                null, null, this::recreate, this::showStripSettings));
        l.add("↕  ارتفاع الشريط:  " + (prefs.getInt("strip_hp", 0) == 0 ? "تلقائي" : prefs.getInt("strip_hp", 0) + "%"));
        a.add(() -> numberDialog("ارتفاع الشريط (% من الشاشة، 0 = تلقائي)", "strip_hp", 0, 0, 70, "%", "تلقائي",
                null, this::recreate, this::showStripSettings));
        l.add("↔  عرض الشريط:  " + prefs.getInt("strip_wp", 100) + "%");
        a.add(() -> numberDialog("عرض الشريط (% من الشاشة)", "strip_wp", 100, 30, 100, "%", null, null,
                this::recreate, this::showStripSettings));
        l.add("📐  ارتفاع منطقة الصفحة (يحدد ارتفاع الشريط):  " + prefs.getInt("top_pct", 82) + "%");
        a.add(() -> numberDialog("ارتفاع منطقة الصفحة = 100% − ارتفاع الشريط (قلّله مع صفين أو ثلاثة)",
                "top_pct", 82, 55, 95, "%", null, null, this::recreate, this::showStripSettings));
        l.add("▦  موضع الشريط:  " + (prefs.getBoolean("strip_over", false) ? "فوق صفحة المواقيت (يغطي أسفلها)"
                : "تحت الصفحة (منفصل)"));
        a.add(() -> choice("موضع الشريط", new String[]{"تحت الصفحة (الصفحة تنتهي فوقه)",
                "فوق الصفحة (الصفحة بكامل الشاشة والشريط يغطي أسفلها)"},
                prefs.getBoolean("strip_over", false) ? 1 : 0, i -> {
                    prefs.edit().putBoolean("strip_over", i == 1).apply();
                    recreate();
                }, this::showStripSettings));
        l.add("🖼  وضع عرض الصفحة:  " + (prefs.getInt("fit", 1) == 1 ? "ملء العرض" : "تصغير متناسب"));
        a.add(() -> choice("وضع عرض الصفحة",
                new String[]{"تصغير متناسب: حركة سلسة جداً لكن تظهر أشرطة جانبية",
                        "ملء العرض: الصفحة تملأ العرض بلا أشرطة (انتقال بتلاشي)"},
                prefs.getInt("fit", 1) == 1 ? 1 : 0, i -> {
                    prefs.edit().putInt("fit", i).apply();
                    recreate();
                }, this::showStripSettings));
        l.add("🎨  مظهر الشريط السفلي:  " + STRIP_STYLES[Math.max(0, Math.min(3, prefs.getInt("strip_style", 2)))]);
        a.add(() -> choice("مظهر الشريط السفلي", STRIP_STYLES, prefs.getInt("strip_style", 2), i -> {
            prefs.edit().putInt("strip_style", i).apply();
            applyStripStyle();
            applyWeatherStyle();
            if (i == 3) samplePageColor();
            showTab(currentTab, -1);
            showStripSettings();
        }, this::showStripSettings));
        int pc = prefs.getInt("strip_col", 0);
        l.add("🎨  لون الشريط والبطاقة:  " + PAL_NAMES[Math.min(pc, PAL_NAMES.length - 1)]);
        a.add(() -> choice("لون الشريط والبطاقة", PAL_NAMES, prefs.getInt("strip_col", 0), i -> {
            if (i < PAL_TOP.length) {
                prefs.edit().putInt("strip_col", i).apply();
                applyStripStyle();
                applyWeatherStyle();
                showStripSettings();
            } else {
                promptText("لون مخصص (#RRGGBB)", "#103A2C", prefs.getString("strip_hex", "#103A2C"),
                        v -> {
                            try {
                                Color.parseColor(v);
                                prefs.edit().putInt("strip_col", PAL_TOP.length)
                                        .putString("strip_hex", v).apply();
                                applyStripStyle();
                                applyWeatherStyle();
                            } catch (Exception e) {
                                toast("صيغة اللون غير صحيحة، مثال: #103A2C");
                            }
                            showStripSettings();
                        });
            }
        }, this::showStripSettings));
        l.add("🔆  شفافية الشريط:  " + prefs.getInt("strip_alpha", 92) + "%");
        a.add(() -> numberDialog("شفافية الشريط (100% = معتم، 0% = شفاف تماماً)", "strip_alpha", 92, 0,
                100, "%", null, this::applyStripStyle, this::showStripSettings, this::showStripSettings));
        l.add("🔳  ألواح الأيقونات المجسّمة:  " + onOff(prefs.getBoolean("tile_plate", true)));
        a.add(() -> {
            prefs.edit().putBoolean("tile_plate", !prefs.getBoolean("tile_plate", true)).apply();
            showTab(currentTab, -1);
            showStripSettings();
        });
        l.add("↔  مكان قائمة التصنيفات:  " + (railRight ? "اليمين" : "اليسار"));
        a.add(() -> choice("مكان قائمة التصنيفات", new String[]{"اليسار", "اليمين"},
                railRight ? 1 : 0, i -> {
                    prefs.edit().putInt("rail_side", i).apply();
                    recreate();
                }, this::showStripSettings));
        l.add("⏱  إخفاء القائمة تلقائياً بعد:  " + prefs.getInt("rail_secs", 6) + " ث");
        a.add(() -> numberDialog("إخفاء القائمة بعد", "rail_secs", 6, 2, 30, " ث", null, null,
                this::showStripSettings, this::showStripSettings));
        l.add("🔍  تكبير الأيقونة المحددة:  " + prefs.getInt("zoom", 114) + "%");
        a.add(() -> numberDialog("تكبير الأيقونة عند التركيز", "zoom", 114, 100, 140, "%", null, null,
                this::showStripSettings, this::showStripSettings));
        l.add("🗂  إدارة التصنيفات المخصصة (" + cats.size() + ")");
        a.add(this::showCategories);
        menu("الشريط والتصنيفات", l, a, this::showLauncherSettings);
    }

    private void showCategories() {
        final List<String> l = new ArrayList<>();
        final List<Runnable> a = new ArrayList<>();
        for (final Cat c : cats) {
            l.add(c.icon + "  " + c.name + "  (" + c.pkgs.size() + ")");
            a.add(() -> showCatMenu(c));
        }
        l.add("➕  تصنيف جديد");
        a.add(() -> promptText("اسم التصنيف", "مثال: IPTV أو الصيانة أو المشاهدة", "", name -> {
            if (name.isEmpty()) return;
            Cat c = new Cat();
            c.id = "c" + System.currentTimeMillis();
            c.name = name;
            c.icon = "📁";
            cats.add(c);
            saveCats();
            refreshTabsAfterCatChange();
            chooseCatIcon(c);
        }));
        menu("التصنيفات المخصصة", l, a, this::showStripSettings);
    }

    private void showCatMenu(final Cat c) {
        final List<String> l = new ArrayList<>();
        final List<Runnable> a = new ArrayList<>();
        l.add("📱  اختيار التطبيقات");
        a.add(() -> editCatApps(c));
        l.add("✏  إعادة تسمية");
        a.add(() -> promptText("اسم التصنيف", "", c.name, name -> {
            if (name.isEmpty()) return;
            c.name = name;
            saveCats();
            refreshTabsAfterCatChange();
            showCatMenu(c);
        }));
        l.add("🎨  تغيير الأيقونة");
        a.add(() -> chooseCatIcon(c));
        l.add("↑  تقديم في القائمة");
        a.add(() -> moveCat(c, -1));
        l.add("↓  تأخير في القائمة");
        a.add(() -> moveCat(c, 1));
        l.add("🗑  حذف التصنيف");
        a.add(() -> dialog("حذف «" + c.name + "»؟")
                .setMessage("تُحذف القائمة فقط، والتطبيقات تبقى مثبتة.")
                .setPositiveButton("حذف", (d, w) -> {
                    cats.remove(c);
                    saveCats();
                    refreshTabsAfterCatChange();
                    showCategories();
                }).setNegativeButton("إلغاء", null).show());
        menu(c.icon + " " + c.name, l, a, this::showCategories);
    }

    private void moveCat(Cat c, int d) {
        int i = cats.indexOf(c), j = i + d;
        if (j < 0 || j >= cats.size()) {
            showCatMenu(c);
            return;
        }
        cats.remove(i);
        cats.add(j, c);
        saveCats();
        refreshTabsAfterCatChange();
        showCatMenu(c);
    }

    private void chooseCatIcon(final Cat c) {
        choice("أيقونة التصنيف", CAT_ICONS, Arrays.asList(CAT_ICONS).indexOf(c.icon), i -> {
            c.icon = CAT_ICONS[i];
            saveCats();
            refreshTabsAfterCatChange();
            showCategories();
        }, this::showCategories);
    }

    private void editCatApps(final Cat c) {
        final List<AppEntry> all = new ArrayList<>(entries);
        Collections.sort(all, (x, y) -> x.label.compareToIgnoreCase(y.label));
        String[] names = new String[all.size()];
        final boolean[] on = new boolean[all.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = all.get(i).label;
            on[i] = c.pkgs.contains(all.get(i).pkg);
        }
        dialog("تطبيقات «" + c.name + "»")
                .setMultiChoiceItems(names, on, (d, which, checked) -> on[which] = checked)
                .setPositiveButton("حفظ", (d, w) -> {
                    for (int i = 0; i < on.length; i++) {
                        String p = all.get(i).pkg;
                        if (on[i] && !c.pkgs.contains(p)) c.pkgs.add(p);
                        if (!on[i]) c.pkgs.remove(p);
                    }
                    saveCats();
                    refreshTabsAfterCatChange();
                    showCatMenu(c);
                })
                .setNegativeButton("إلغاء", null).show();
    }

    private void refreshTabsAfterCatChange() {
        String cur = tabs.isEmpty() ? "all" : tab().id;
        buildTabs();
        currentTab = Math.max(0, tabIndexById(cur));
        if (tabIndexById(cur) < 0) currentTab = tabIndexByKind(K_ALL);
        rebuildRailItems();
        showTab(currentTab, -1);
    }

    // ---- weather

    private String weatherPosName() {
        int x = prefs.getInt("w_x", 100), y = prefs.getInt("w_y", 0);
        if (x == 100 && y == 0) return "أعلى اليمين";
        if (x == 0 && y == 0) return "أعلى اليسار";
        if (x == 100 && y == 100) return "أسفل اليمين";
        if (x == 0 && y == 100) return "أسفل اليسار";
        return "مخصص (" + x + "% , " + y + "%)";
    }

    private void showWeatherSettings() {
        final List<String> l = new ArrayList<>();
        final List<Runnable> a = new ArrayList<>();
        final Runnable back = this::showWeatherSettings;
        l.add("👁  إظهار بطاقة الطقس:  " + onOff(prefs.getBoolean("w_show", true)));
        a.add(() -> {
            prefs.edit().putBoolean("w_show", !prefs.getBoolean("w_show", true)).apply();
            applyWeatherStyle();
            showWeatherSettings();
        });
        l.add("📌  موضع البطاقة:  " + weatherPosName());
        a.add(() -> choice("موضع البطاقة",
                new String[]{"أعلى اليمين", "أعلى اليسار", "أسفل اليمين", "أسفل اليسار",
                        "حرّكها بالريموت (أي مكان)"}, -1, i -> {
                    int[][] pos = {{100, 0}, {0, 0}, {100, 100}, {0, 100}};
                    if (i < 4) {
                        prefs.edit().putInt("w_x", pos[i][0]).putInt("w_y", pos[i][1]).apply();
                        positionWeather();
                        showWeatherSettings();
                    } else {
                        startWeatherMove();
                    }
                }, back));
        l.add("↔  الإزاحة الأفقية:  " + prefs.getInt("w_x", 100) + "%");
        a.add(() -> numberDialog("الإزاحة الأفقية (0 = أقصى اليسار، 100 = أقصى اليمين)", "w_x", 100, 0,
                100, "%", null, this::positionWeather, back, back));
        l.add("↕  الإزاحة الرأسية:  " + prefs.getInt("w_y", 0) + "%");
        a.add(() -> numberDialog("الإزاحة الرأسية (0 = أعلى، 100 = أسفل منطقة الصفحة)", "w_y", 0, 0,
                100, "%", null, this::positionWeather, back, back));
        l.add("🔲  حجم البطاقة:  " + prefs.getInt("w_size", 100) + "%");
        a.add(() -> numberDialog("حجم البطاقة", "w_size", 100, 40, 220, "%", null, this::layoutWeather,
                back, back));
        l.add("🔆  شفافية البطاقة:  " + prefs.getInt("w_alpha", 55) + "%  (0 = شفافة تماماً)");
        a.add(() -> numberDialog("شفافية البطاقة (0% = تذوب في الصفحة)", "w_alpha", 55, 0, 100, "%", null,
                this::applyWeatherStyle, back, back));
        l.add("▤  شكل العرض:  " + (prefs.getInt("w_layout", 0) == 1 ? "أعمدة (أفقي)" : "صفوف (عمودي)"));
        a.add(() -> choice("شكل عرض الأيام", new String[]{"صفوف: كل يوم في سطر", "أعمدة: الأيام الثلاثة جنب بعض"},
                prefs.getInt("w_layout", 0), i -> {
                    prefs.edit().putInt("w_layout", i).apply();
                    layoutWeather();
                    showWeatherSettings();
                }, back));
        l.add("☰  أسطر كل يوم:  " + (prefs.getInt("w_lines", 1) == 1 ? "سطر واحد" : "سطران"));
        a.add(() -> choice("عدد أسطر كل يوم", new String[]{"سطران (التفاصيل تحت اليوم)", "سطر واحد (كل شيء بسطر)"},
                prefs.getInt("w_lines", 1), i -> {
                    prefs.edit().putInt("w_lines", i).apply();
                    layoutWeather();
                    showWeatherSettings();
                }, back));
        l.add("⬌  محاذاة نص اليوم:  " + new String[]{"يمين", "وسط", "يسار"}[prefs.getInt("w_align", 0) % 3]);
        a.add(() -> choice("محاذاة نص اليوم في الصفوف", new String[]{"يمين", "وسط", "يسار"},
                prefs.getInt("w_align", 0), i -> {
                    prefs.edit().putInt("w_align", i).apply();
                    layoutWeather();
                    showWeatherSettings();
                }, back));
        l.add("↔  التباعد بين العناصر:  " + prefs.getInt("w_gap", 2) + "  (0 = ملتصقة)");
        a.add(() -> numberDialog("التباعد بين اليوم والأيقونة والحرارة (أقل = أقرب)", "w_gap", 2, 0, 30, "", null,
                this::layoutWeather, back, back));
        l.add("📏  تقليل كل المسافات (المدينة، الأيام، الرموز…):  " + prefs.getInt("w_tight", 40) + "%  (100 = أقرب ما يمكن)");
        a.add(() -> numberDialog("تقليل المسافات بين كل أجزاء البطاقة", "w_tight", 40, 0, 100, "%", null,
                this::layoutWeather, back, back));
        l.add("▭  هامش البطاقة الخارجي:  " + prefs.getInt("w_pad", 5) + "  (0 = بلا هامش)");
        a.add(() -> numberDialog("هامش البطاقة الخارجي (أقل = بطاقة أصغر)", "w_pad", 5, 0, 30, "", null,
                this::layoutWeather, back, back));
        l.add("↕  التباعد بين الأيام:  " + prefs.getInt("w_rowgap", 1) + "  (0 = ملتصقة)");
        a.add(() -> numberDialog("التباعد بين الأيام (أقل = أقرب)", "w_rowgap", 1, 0, 30, "", null,
                this::layoutWeather, back, back));
        l.add("🔠  حجم الخط داخل البطاقة:  " + prefs.getInt("w_font", 100) + "%");
        a.add(() -> numberDialog("حجم الخط داخل البطاقة (تصغير/تكبير)", "w_font", 100, 50, 250, "%", null,
                this::layoutWeather, back, back));
        l.add("📆  عدد الأيام:  " + prefs.getInt("w_days", 3));
        a.add(() -> numberDialog("عدد الأيام المعروضة", "w_days", 3, 1, 3, "", null, this::layoutWeather,
                back, back));
        l.add("🧩  المعلومات المعروضة (رطوبة، مطر، رياح، UV، يُحسّ)");
        a.add(this::showWeatherFields);
        l.add("🎨  الشكل (الزوايا والإطار والخط الفاصل)");
        a.add(this::showWeatherLook);
        l.add("🕌  إخفاء البطاقة وقت الأذان والإقامة:  " + onOff(prefs.getBoolean("w_hide_adhan", true)));
        a.add(() -> {
            prefs.edit().putBoolean("w_hide_adhan", !prefs.getBoolean("w_hide_adhan", true)).apply();
            applyWeatherVis(true);
            showWeatherSettings();
        });
        l.add("⇄  اتجاه الترتيب:  " + (prefs.getInt("w_flip", 0) == 1 ? "من اليسار لليمين" : "من اليمين لليسار"));
        a.add(() -> {
            prefs.edit().putInt("w_flip", prefs.getInt("w_flip", 0) == 1 ? 0 : 1).apply();
            layoutWeather();
            showWeatherSettings();
        });
        l.add("📍  الموقع:  " + (manualLocation() ? currentCity() : "تلقائي (" + currentCity() + ")"));
        a.add(this::showLocationSettings);
        l.add("🔄  تحديث الطقس الآن");
        a.add(() -> {
            fetchWeather();
            toast("جارٍ التحديث…");
        });
        menu("بطاقة الطقس", l, a, this::showLauncherSettings);
    }

    private void showWeatherFields() {
        final String[] keys = {"w_hum", "w_rain", "w_wind", "w_uv", "w_feels", "w_city", "w_age"};
        final boolean[] def = {true, true, false, false, true, true, true};
        final String[] names = {"💧 الرطوبة", "🌧 احتمال المطر", "💨 سرعة الرياح", "☀ مؤشر الأشعة UV",
                "🌡 الحرارة المحسوسة (اليوم)", "📍 اسم المدينة", "🕐 وقت آخر تحديث"};
        final boolean[] on = new boolean[keys.length];
        for (int i = 0; i < keys.length; i++) on[i] = prefs.getBoolean(keys[i], def[i]);
        dialog("المعلومات المعروضة في البطاقة").setMultiChoiceItems(names, on, null)
                .setPositiveButton("حفظ", (d, w) -> {
                    SharedPreferences.Editor ed = prefs.edit();
                    for (int i = 0; i < keys.length; i++) ed.putBoolean(keys[i], on[i]);
                    ed.apply();
                    updateWeatherUi();
                    showWeatherSettings();
                })
                .setNegativeButton("إلغاء", (d, w) -> showWeatherSettings()).show();
    }

    private void showWeatherLook() {
        final List<String> l = new ArrayList<>();
        final List<Runnable> a = new ArrayList<>();
        final Runnable back = this::showWeatherLook;
        l.add("⬭  استدارة الزوايا:  " + prefs.getInt("w_radius", 16));
        a.add(() -> numberDialog("استدارة زوايا البطاقة", "w_radius", 16, 0, 40, "", null,
                this::applyWeatherStyle, back, back));
        l.add("▭  سماكة الإطار:  " + prefs.getInt("w_border", 1));
        a.add(() -> numberDialog("سماكة الإطار (0 = بلا إطار)", "w_border", 1, 0, 4, "", "بلا إطار",
                this::applyWeatherStyle, back, back));
        l.add("🌗  لون البطاقة حسب الوقت (نهار/غروب/ليل):  " + onOff(prefs.getBoolean("w_tod", true)));
        a.add(() -> {
            prefs.edit().putBoolean("w_tod", !prefs.getBoolean("w_tod", true)).apply();
            applyWeatherStyle();
            showWeatherLook();
        });
        l.add("➖  الخط الفاصل تحت المدينة:  " + onOff(prefs.getBoolean("w_line", true)));
        a.add(() -> {
            prefs.edit().putBoolean("w_line", !prefs.getBoolean("w_line", true)).apply();
            layoutWeather();
            showWeatherLook();
        });
        menu("شكل بطاقة الطقس", l, a, this::showWeatherSettings);
    }

    private void showLocationSettings() {
        final List<String> l = new ArrayList<>();
        final List<Runnable> a = new ArrayList<>();
        l.add("🌐 تلقائي حسب الإنترنت (IP)");
        a.add(() -> {
            prefs.edit().putString("loc_mode", "auto").remove("ip_ts").apply();
            toast("جارٍ تحديد الموقع تلقائياً…");
            fetchWeather();
        });
        l.add("🔎 بحث عن مدينة…");
        a.add(() -> promptText("اسم المدينة", "مثال: Saarbrücken", "", this::geocodeCity));
        menu("موقع الطقس (الحالي: " + currentCity() + ")", l, a, this::showWeatherSettings);
    }

    private void geocodeCity(final String name) {
        if (name.isEmpty()) return;
        toast("جارٍ البحث…");
        new Thread(() -> {
            String err = null;
            try {
                String u = "https://geocoding-api.open-meteo.com/v1/search?name="
                        + URLEncoder.encode(name, "UTF-8") + "&count=1&language=de&format=json";
                JSONArray r = new JSONObject(httpGet(u)).optJSONArray("results");
                if (r == null || r.length() == 0) {
                    err = "لم أجد هذه المدينة";
                } else {
                    JSONObject o = r.getJSONObject(0);
                    prefs.edit().putString("loc_mode", "manual")
                            .putString("lat", Double.toString(o.getDouble("latitude")))
                            .putString("lon", Double.toString(o.getDouble("longitude")))
                            .putString("city", o.optString("name", name)).apply();
                    AppLog.info("LOC", "manual city: " + o.optString("name") + " "
                            + o.getDouble("latitude") + "," + o.getDouble("longitude"));
                }
            } catch (Exception e) {
                AppLog.error("LOC", "geocode failed", e);
                err = "تعذّر البحث (تحقق من الإنترنت)";
            }
            final String fe = err;
            handler.post(() -> {
                if (fe != null) toast(fe);
                else {
                    toast("تم: " + currentCity());
                    fetchWeather();
                }
            });
        }, "geocode").start();
    }

    // ---- sound / motion

    private void showMotionSettings() {
        final List<String> l = new ArrayList<>();
        final List<Runnable> a = new ArrayList<>();
        l.add("🔊  أصوات التنقل واللمس:  " + onOff(prefs.getBoolean("sounds", true)));
        a.add(() -> {
            prefs.edit().putBoolean("sounds", !prefs.getBoolean("sounds", true)).apply();
            applySounds(root);
            getWindow().getDecorView().setSoundEffectsEnabled(prefs.getBoolean("sounds", true));
            showMotionSettings();
        });
        l.add("🎬  مدة الحركات:  " + prefs.getInt("anim_pct", 100) + "%  (أقل = أسرع)");
        a.add(() -> numberDialog("مدة الحركات (100% = عادية)", "anim_pct", 100, 30, 250, "%", null, null,
                this::showMotionSettings, this::showMotionSettings));
        l.add("🖥  تحريك الشاشة قليلاً (يحمي من الاحتراق):  " + onOff(prefs.getBoolean("px_shift", true)));
        a.add(() -> {
            prefs.edit().putBoolean("px_shift", !prefs.getBoolean("px_shift", true)).apply();
            showMotionSettings();
        });
        l.add("🔒  مراقب تجمّد الصفحة (يعيد التحميل تلقائياً):  " + onOff(prefs.getBoolean("watchdog", true)));
        a.add(() -> {
            prefs.edit().putBoolean("watchdog", !prefs.getBoolean("watchdog", true)).apply();
            showMotionSettings();
        });
        l.add("🔆  شفافية نوافذ الإعدادات:  " + prefs.getInt("ui_alpha", 92) + "%");
        a.add(() -> numberDialog("شفافية نوافذ الإعدادات (أقل = ترى اللانشر خلفها)", "ui_alpha", 92, 30, 100,
                "%", null, null, this::showMotionSettings, this::showMotionSettings));
        l.add("🔤  نوع الخط:  " + FONT_NAMES[Math.max(0, Math.min(FONT_NAMES.length - 1, prefs.getInt("font", 0)))]);
        a.add(() -> choice("نوع الخط للانشر كله", FONT_NAMES, prefs.getInt("font", 0), i -> {
            prefs.edit().putInt("font", i).apply();
            recreate();
        }, this::showMotionSettings));
        l.add("🔠  حجم الخط:  " + prefs.getInt("font_pct", 100) + "%");
        a.add(() -> numberDialog("حجم الخط في اللانشر كله", "font_pct", 100, 70, 160, "%", null, null,
                this::recreate, this::showMotionSettings));
        l.add("🎨  لون تحديد القوائم:  " + ACC_NAMES[Math.max(0, Math.min(ACC_NAMES.length - 1, prefs.getInt("ui_accent", 0)))]);
        a.add(() -> choice("لون تحديد القوائم", ACC_NAMES, prefs.getInt("ui_accent", 0), i -> {
            prefs.edit().putInt("ui_accent", i).apply();
            showMotionSettings();
        }, this::showMotionSettings));
        final String[] sides = {"اليسار", "الوسط", "اليمين"};
        l.add("📌  مكان نوافذ الإعدادات:  " + sides[Math.max(0, Math.min(2, prefs.getInt("ui_side", 0)))]);
        a.add(() -> choice("مكان نوافذ الإعدادات", sides, prefs.getInt("ui_side", 0), i -> {
            prefs.edit().putInt("ui_side", i).apply();
            showMotionSettings();
        }, this::showMotionSettings));
        menu("الصوت والحركة والمظهر", l, a, this::showLauncherSettings);
    }

    // ---- updates

    private void showUpdateSettings() {
        final List<String> l = new ArrayList<>();
        final List<Runnable> a = new ArrayList<>();
        long chk = prefs.getLong("upd_checked", 0L);
        l.add("ℹ  الإصدار الحالي:  " + Updater.currentName(this));
        a.add(this::showUpdateSettings);
        l.add("🕒  آخر فحص:  " + (chk == 0 ? "لم يُفحص بعد" : TimeCfg.dateTime(prefs, chk)));
        a.add(this::showUpdateSettings);
        if (pendingInfo != null) {
            l.add("🆕  أحدث إصدار:  " + pendingInfo.name);
            a.add(this::showUpdateSettings);
        }
        l.add("🔍  فحص التحديث الآن");
        a.add(() -> {
            toast("جارٍ الفحص…");
            checkUpdate(true);
        });
        if (pendingInfo != null) {
            l.add("⬆  تثبيت " + pendingInfo.name + " الآن");
            a.add(this::askInstall);
        }
        final int mode = prefs.getInt("upd_mode", 1);
        l.add("⚙  وضع التحديث:  " + (mode == 0 ? "يدوي" : mode == 1 ? "تنبيه فقط" : "تلقائي بالخلفية"));
        a.add(() -> choice("وضع التحديث",
                new String[]{"يدوي: لا فحص تلقائي",
                        "تنبيه: أنبّهك عند وجود تحديث",
                        "تلقائي: ينزّل ويثبّت بالخلفية أثناء شاشة الحماية ويخبرك بعدها"},
                prefs.getInt("upd_mode", 1), i -> {
                    prefs.edit().putInt("upd_mode", i).apply();
                    if (i > 0) handler.postDelayed(() -> checkUpdate(false), 3000L);
                    showUpdateSettings();
                }, this::showUpdateSettings));
        l.add("📦  مستودع التحديثات:  " + prefs.getString("upd_repo", Updater.DEFAULT_REPO));
        a.add(() -> promptText("مستودع GitHub (owner/repo)", Updater.DEFAULT_REPO,
                prefs.getString("upd_repo", Updater.DEFAULT_REPO), v -> {
                    if (v.matches("[\\w.-]+/[\\w.-]+")) {
                        prefs.edit().putString("upd_repo", v).apply();
                    } else toast("الصيغة: owner/repo");
                    showUpdateSettings();
                }));
        menu("التحديثات", l, a, this::showLauncherSettings);
    }

    private void showAdbDialog() {
        dialog("اتصال ADB الذاتي")
                .setMessage("يسمح للانشر بتنفيذ أوامر لا يستطيعها تطبيق عادي: فتح شاشات إعدادات محمية، "
                        + "وضع الجهاز في النوم (يُطفئ التلفزيون عبر HDMI-CEC)، وتثبيت التحديثات بالخلفية.\n\n"
                        + "1) فعّل «ADB debugging» من خيارات المطوّر.\n"
                        + "2) اضغط «اختبار الاتصال». أول مرة ستظهر على التلفزيون نافذة "
                        + "«Allow USB debugging?» ← اختر «Always allow».")
                .setPositiveButton("اختبار الاتصال", (d, w) -> {
                    toast("جارٍ الاتصال… وافق على النافذة إن ظهرت");
                    AdbShell.run(MainActivity.this, "echo connected", new AdbShell.Callback() {
                        @Override public void done(boolean ok, String output) {
                            dialog(ok ? "✓ الاتصال يعمل" : "✗ فشل الاتصال")
                                    .setMessage(ok ? output : output + (AdbShell.blocked(MainActivity.this)
                                            ? "\n\nجهازك يُغلق اتصال ADB الذاتي فور فتحه (غالباً حجب من نظام Fire OS). "
                                            + "هذا لا يمنع التحديثات: تُثبَّت عبر مثبّت أندرويد."
                                            : "\n\nتأكد من تفعيل ADB debugging ثم أعد المحاولة."))
                                    .setPositiveButton("حسناً", null).show();
                        }
                    });
                }).setNeutralButton("مفتاح جديد", (d, w) -> {
                    AdbShell.resetKey(MainActivity.this);
                    toast("تم إنشاء مفتاح جديد — اضغط «اختبار الاتصال» ووافق على نافذة التلفزيون");
                }).setNegativeButton("إغلاق", null).show();
    }

    private void showLogDialog() {
        final List<String> l = new ArrayList<>();
        final List<Runnable> a = new ArrayList<>();
        l.add("📧  إرسال التقرير بالبريد (السجل + بنية الصفحة)");
        a.add(() -> emailReport(true));
        l.add("📋  نسخ التقرير كاملاً (سجل + بنية الصفحة)");
        a.add(() -> copyReport(true));
        l.add("📋  نسخ بنية الصفحة فقط");
        a.add(() -> copyReport(false));
        l.add("📧  إرسال السجل فقط بالبريد");
        a.add(() -> emailReport(false));
        l.add("👁  عرض آخر الأحداث");
        a.add(() -> dialog("آخر الأحداث").setMessage(AppLog.tail(60))
                .setPositiveButton("حسناً", null).show());
        l.add("🌐  مشاركة عبر الشبكة المحلية");
        a.add(this::startLogServer);
        l.add("📸  لقطة شاشة (الواجهة فقط) ومشاركتها بالجوال");
        a.add(() -> shareScreenshot(900L));
        l.add("📸  لقطة بعد 12 ثانية — تشمل القائمة المفتوحة (افتحها بعد الضغط)");
        a.add(() -> shareScreenshot(12_000L));
        l.add("🔎  فحص بنية الصفحة ومشاركته عبر الشبكة");
        a.add(this::inspectPage);
        l.add("🗑  مسح السجل");
        a.add(() -> {
            AppLog.clear();
            toast("تم المسح");
        });
        menu("سجل الأخطاء والتقارير", l, a, this::showLauncherSettings);
    }

    /** Opens the mail composer with the report (page structure is refreshed first when asked). */
    private void emailReport(final boolean withDom) {
        if (!withDom) {
            sendMail(reportText(false));
            return;
        }
        toast("جارٍ فحص الصفحة…");
        webView.evaluateJavascript(JS_DOM, v -> {
            String r = v == null ? "" : v;
            try {
                r = new JSONArray("[" + r + "]").getString(0);
            } catch (Exception ignored) {
            }
            domDump = r;
            sendMail(reportText(true));
        });
    }

    /** Copies the report (or only the page structure) to the clipboard. */
    private void copyReport(final boolean full) {
        toast("جارٍ فحص الصفحة…");
        webView.evaluateJavascript(JS_DOM, v -> {
            String r = v == null ? "" : v;
            try {
                r = new JSONArray("[" + r + "]").getString(0);
            } catch (Exception ignored) {
            }
            domDump = r;
            String text = full ? reportText(true) : r;
            try {
                android.content.ClipboardManager cm =
                        (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Mawaqit Home", text));
                toast("تم النسخ ✓ (" + text.length() / 1000 + " ألف حرف)");
            } catch (Exception e) {
                AppLog.error("COPY", "clipboard failed", e);
                toast("تعذّر النسخ");
            }
        });
    }

    private void sendMail(String body) {
        if (body.length() > 150_000) body = body.substring(0, 60_000) + "\n…[cut]…\n"
                + body.substring(body.length() - 85_000);
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, "Mawaqit Home report " + Updater.currentName(this));
            i.putExtra(Intent.EXTRA_TEXT, body);
            startActivity(Intent.createChooser(i, "إرسال التقرير").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            AppLog.error("MAIL", "no mail app", e);
            toast("لا يوجد تطبيق بريد على هذا الجهاز — استخدم المشاركة عبر الشبكة");
        }
    }

    // ---- page element control (CSS injected into the live Mawaqit page)

    private static final String[][] PG = {
            {"head", "اسم المسجد (العنوان)", ".header"},
            {"clock", "الساعة والثواني", ".currentTime"},
            {"hijri", "التاريخ الهجري", ".center-time > span"},
            {"greg", "التاريخ الميلادي", "#gregorianDate"},
            {"next", "الوقت المتبقي للصلاة القادمة", ".next-prayer-bloc"},
            {"shur", "الشروق", ".chourouk"},
            {"jum", "الجمعة", ".joumouaa"},
            {"pall", "الصلوات الخمس (الصف كله)", ".prayers"},
            {"p1", "الفجر", ".prayers > div:nth-child(1)"},
            {"p2", "الظهر", ".prayers > div:nth-child(2)"},
            {"p3", "العصر", ".prayers > div:nth-child(3)"},
            {"p4", "المغرب", ".prayers > div:nth-child(4)"},
            {"p5", "العشاء", ".prayers > div:nth-child(5)"},
            {"logo", "الشعار والإصدار", ".logo, .version"},
    };

    private String pgSummary(String k) {
        if (prefs.getBoolean("pg_" + k + "_h", false)) return "مخفي";
        int x = prefs.getInt("pg_" + k + "_x", 0), y = prefs.getInt("pg_" + k + "_y", 0),
                z = prefs.getInt("pg_" + k + "_s", 100);
        return x == 0 && y == 0 && z == 100 ? "افتراضي" : "↔" + x + "  ↕" + y + "  🔍" + z + "%";
    }

    private void applyPageCss() {
        StringBuilder css = new StringBuilder();
        int pf = prefs.getInt("font", 0);
        if (pf > 0 && pf < FONT_FILES.length) {
            css.append("@font-face{font-family:'MHF';font-weight:400;src:url('/__mhfont/")
                    .append(FONT_FILES[pf][0]).append(".ttf')}")
                    .append("@font-face{font-family:'MHF';font-weight:700;src:url('/__mhfont/")
                    .append(FONT_FILES[pf][1]).append(".ttf')}")
                    .append("html,body,body *{font-family:'MHF',sans-serif!important}");
        }
        for (String[] e : PG) {
            String k = e[0];
            int x = prefs.getInt("pg_" + k + "_x", 0), y = prefs.getInt("pg_" + k + "_y", 0),
                    z = prefs.getInt("pg_" + k + "_s", 100);
            boolean h = prefs.getBoolean("pg_" + k + "_h", false);
            if (!h && x == 0 && y == 0 && z == 100) continue;
            css.append(e[2]).append("{");
            if (h) css.append("visibility:hidden!important;");
            else css.append("transform:translate(").append(x).append("vw,").append(y)
                    .append("vh) scale(").append(z / 100f).append(")!important;");
            css.append("}");
        }
        if (pf > 0) css.append(".currentTime>div:not(#mh-ck){display:none!important}");
        final boolean natOn = nativeVideoWanted();
        final boolean vidOn = bgVideo() && bgVSrc() != 0 && !prefs.getString("bg_vurl", "").isEmpty() && !natOn;
        final boolean imgOn = !bgVideo() && bgSources() != 0 && bgFile().exists();
        if (vidOn || imgOn) css.append("html{background:#000!important}body{background:none!important}");
        if (natOn) css.append("html,body{background:transparent!important}");
        applyNativeVideo();
        applySaverClear();
        String js = "(function(){window.__mhvid=" + JSONObject.quote(vidOn ? prefs.getString("bg_vurl", "") : "")
                + ";window.__mhdim=" + (Math.max(0, Math.min(85, prefs.getInt("bg_dim", 35))) / 100f)
                + ";window.__mhimg=" + JSONObject.quote(imgOn ? "/__mhbg/bg_" + prefs.getInt("bg_slot", 0)
                        + ".jpg?v=" + prefs.getLong("bg_last", 0) : "")
                + ";window.__mhfade=" + Math.max(0, Math.min(10, prefs.getInt("bg_fade", 2)))
                + ";function dim(){var any=window.__mhvid||window.__mhimg||window.__mhnat,d=document.getElementById('mh-dim');"
                + "if(!any){if(d)d.remove();return;}"
                + "if(!d){d=document.createElement('div');d.id='mh-dim';d.style.cssText='position:fixed;left:0;top:0;width:100vw;height:100vh;z-index:-1;pointer-events:none';document.body.appendChild(d);}"
                + "d.style.background='rgba(0,0,0,'+window.__mhdim+')';}"
                + "function img(){var u=window.__mhimg||'';"
                + "if(!u){for(var q=0;q<2;q++){var o=document.getElementById('mh-i'+q);if(o)o.remove();}window.__mhcur='';window.__mhpend='';return;}"
                + "for(var k=0;k<2;k++){if(!document.getElementById('mh-i'+k)){var L=document.createElement('div');L.id='mh-i'+k;"
                + "L.style.cssText='position:fixed;left:0;top:0;width:100vw;height:100vh;background-size:cover;background-position:center;background-repeat:no-repeat;opacity:0;z-index:-4';document.body.appendChild(L);}}"
                + "if(window.__mhcur===u||window.__mhpend===u)return;window.__mhpend=u;"
                + "var im=new Image();im.onload=function(){var nx=window.__mhai===0?1:0,N=document.getElementById('mh-i'+nx),O=document.getElementById('mh-i'+(nx===0?1:0));"
                + "if(!N)return;N.style.transition='none';N.style.opacity=0;N.style.zIndex=-3;N.style.backgroundImage=\"url('\"+u+\"')\";"
                + "void N.offsetWidth;N.style.transition='opacity '+window.__mhfade+'s ease-in-out';N.style.opacity=1;"
                + "window.__mhai=nx;window.__mhcur=u;"
                + "setTimeout(function(){if(O){O.style.transition='none';O.style.opacity=0;O.style.zIndex=-4;}},window.__mhfade*1000+100);};"
                + "im.onerror=function(){window.__mhpend='';};im.src=u;}"
                + ";window.__mhnat=" + (natOn ? "1" : "0")
                + ";window.__mhvm=" + prefs.getInt("bg_vmode", 2)
                + ";function vid(){var u=window.__mhvid||'',v=document.getElementById('mh-vid'),cv=document.getElementById('mh-cv');"
                + "if(!u){if(v)v.remove();if(cv)cv.remove();if(window.__mhcvt){clearInterval(window.__mhcvt);window.__mhcvt=0;}return;}"
                + "if(!document.getElementById('mh-ref')){var mt=document.createElement('meta');mt.id='mh-ref';mt.name='referrer';mt.content='no-referrer';document.head.appendChild(mt);}"
                + "if(!v){v=document.createElement('video');v.id='mh-vid';v.addEventListener('error',function(){console.log('MHVID error code='+(v.error?v.error.code:'?')+' '+v.getAttribute('data-u'));});"
                + "v.addEventListener('playing',function(){if(!window.__mhlogp){window.__mhlogp=1;console.log('MHVID playing '+v.videoWidth+'x'+v.videoHeight);}});"
                + "v.addEventListener('stalled',function(){console.log('MHVID stalled rs='+v.readyState);});"
                + "v.muted=true;v.defaultMuted=true;v.autoplay=true;v.loop=true;v.setAttribute('playsinline','');"
                + "document.body.appendChild(v);}"
                + "v.style.cssText='position:fixed;left:0;top:0;width:100vw;height:100vh;object-fit:cover;background:#000;transform:translateZ(0);z-index:'+(window.__mhvm===1?-6:-2);"
                + "if(window.__mhvm===1){if(!cv){cv=document.createElement('canvas');cv.id='mh-cv';cv.width=960;cv.height=540;"
                + "cv.style.cssText='position:fixed;left:0;top:0;width:100vw;height:100vh;z-index:-2;background:#000';document.body.appendChild(cv);}"
                + "if(!window.__mhcvt){window.__mhcvt=setInterval(function(){var vv=document.getElementById('mh-vid'),c=document.getElementById('mh-cv');"
                + "if(!vv||!c||vv.readyState<2||!vv.videoWidth)return;var x=c.getContext('2d'),sc=Math.max(c.width/vv.videoWidth,c.height/vv.videoHeight),"
                + "w=vv.videoWidth*sc,h=vv.videoHeight*sc;x.drawImage(vv,(c.width-w)/2,(c.height-h)/2,w,h);"
                + "if(!window.__mhlum){window.__mhlum=1;setTimeout(function(){try{var d=x.getImageData(0,0,c.width,c.height).data,t=0,n=0;"
                + "for(var i=0;i<d.length;i+=4096){t+=d[i]+d[i+1]+d[i+2];n+=3;}console.log('MHVID canvas avg-brightness='+Math.round(t/n));}catch(e){console.log('MHVID canvas unreadable '+e);}},3000);}},45);}}"
                + "else{if(cv)cv.remove();if(window.__mhcvt){clearInterval(window.__mhcvt);window.__mhcvt=0;}}"
                + "if(v.getAttribute('data-u')!==u){window.__mhlogp=0;window.__mhlum=0;v.setAttribute('data-u',u);v.src=u;console.log('MHVID load '+u+' mode='+window.__mhvm);var pp;try{pp=v.play();if(pp&&pp.catch)pp.catch(function(e){console.log('MHVID play-rejected '+e);});}catch(e){}"
                + "setTimeout(function(){if(v.readyState<3)console.log('MHVID slow rs='+v.readyState+' ns='+v.networkState+' paused='+v.paused);},12000);}"
                + "else if(v.paused){try{v.play();}catch(e){}}}"
                + "window.__mhcss=" + JSONObject.quote(css.toString()) + ";window.__mhfont="
                + (pf > 0) + ";"
                + "function gw(cs,ch){var t=document.createElement('span');"
                + "t.style.cssText='position:absolute;visibility:hidden;white-space:pre;font-family:'+cs.fontFamily"
                + "+';font-size:'+cs.fontSize+';font-weight:'+cs.fontWeight;t.textContent=ch;document.body.appendChild(t);"
                + "var w=t.getBoundingClientRect().width;document.body.removeChild(t);return w;}"
                + "function dw(cs){var m=0;for(var d=0;d<10;d++)m=Math.max(m,gw(cs,''+d));return m;}"
                + "function cp(g,cs){g.style.cssText='display:inline-block;white-space:pre;direction:ltr;font-family:'+cs.fontFamily"
                + "+';font-size:'+cs.fontSize+';font-weight:'+cs.fontWeight+';color:'+cs.color+';text-shadow:'+cs.textShadow"
                + "+';line-height:'+cs.lineHeight+';letter-spacing:'+cs.letterSpacing;}"
                + "function fill(g,tx,w){if(g.__t===tx&&g.__w===w)return;g.__t=tx;g.__w=w;g.textContent='';"
                + "for(var i=0;i<tx.length;i++){var c=tx.charAt(i),s=document.createElement('span');s.textContent=c;"
                + "if(c>='0'&&c<='9')s.style.cssText='display:inline-block;text-align:center;width:'+w+'px';g.appendChild(s);}}"
                + "function fix(){var ct=document.querySelector('.currentTime');if(!ct)return;"
                + "var o=document.getElementById('mh-ck');"
                + "if(!window.__mhfont){if(o){o.remove();ct.removeAttribute('data-mh');}return;}"
                + "if(document.fonts&&document.fonts.status==='loading')return;"
                + "var kids=[];for(var i=0;i<ct.children.length;i++)if(ct.children[i].id!=='mh-ck')kids.push(ct.children[i]);"
                + "if(kids.length<2)return;var a=kids[0],b=kids[1],ca=getComputedStyle(a),cb=getComputedStyle(b);"
                + "var k=ca.fontFamily+ca.fontSize+cb.fontSize+(document.fonts?document.fonts.size:0);"
                + "if(window.__mhk!==k){window.__mhwa=dw(ca);window.__mhwb=dw(cb);window.__mhk=k;}"
                + "if(!o){o=document.createElement('div');o.id='mh-ck';"
                + "o.style.cssText='display:inline-flex;align-items:baseline;white-space:nowrap';"
                + "o.appendChild(document.createElement('span'));o.appendChild(document.createElement('span'));ct.appendChild(o);}"
                + "if(o.__k!==k){o.style.direction=getComputedStyle(ct).direction;cp(o.children[0],ca);cp(o.children[1],cb);o.__k=k;}"
                + "fill(o.children[0],a.textContent,window.__mhwa);fill(o.children[1],b.textContent,window.__mhwb);"
                + "ct.setAttribute('data-mh','1');}"
                + "function ap(){var s=document.getElementById('mh-style');"
                + "if(!s){s=document.createElement('style');s.id='mh-style';(document.head||document.body).appendChild(s);}"
                + "if(s.textContent!==window.__mhcss)s.textContent=window.__mhcss;fix();vid();img();dim();}"
                + "ap();if(!window.__mhiv)window.__mhiv=setInterval(ap,3000);"
                + "if(!window.__mhiv2)window.__mhiv2=setInterval(fix,500);"
                + "if(!window.__mhmo&&window.MutationObserver){window.__mhmo=new MutationObserver(function(){"
                + "if(window.__mhbusy)return;window.__mhbusy=1;try{fix();}catch(x){}window.__mhmo.takeRecords();window.__mhbusy=0;});"
                + "window.__mhmo.observe(document.body,{childList:true,subtree:true,characterData:true});}})()";
        webView.evaluateJavascript(js, null);
    }

    private void showPageElements() {
        final List<String> l = new ArrayList<>();
        final List<Runnable> a = new ArrayList<>();
        final String[] pfn = FONT_NAMES.clone();
        pfn[0] = "الافتراضي (النظام / الصفحة الأصلية)";
        l.add("🔤  الخط (اللانشر + الصفحة معاً):  " + pfn[Math.max(0, Math.min(pfn.length - 1, prefs.getInt("font", 0)))]);
        a.add(() -> choice("الخط (يطبّق على اللانشر وصفحة المواقيت وبطاقة الطقس)", pfn, prefs.getInt("font", 0), i -> {
            prefs.edit().putInt("font", i).apply();
            recreate();
        }, this::showPageElements));
        for (final String[] e : PG) {
            l.add(e[1] + ":  " + pgSummary(e[0]));
            a.add(() -> showPageElement(e));
        }
        l.add("↩  إعادة كل العناصر لمكانها الأصلي");
        a.add(() -> {
            SharedPreferences.Editor ed = prefs.edit();
            for (String[] e : PG) {
                ed.remove("pg_" + e[0] + "_x").remove("pg_" + e[0] + "_y")
                        .remove("pg_" + e[0] + "_s").remove("pg_" + e[0] + "_h");
            }
            ed.apply();
            applyPageCss();
            showPageElements();
        });
        menu("تحريك عناصر صفحة المواقيت", l, a, this::showLauncherSettings);
    }

    private void showPageElement(final String[] e) {
        final String k = e[0];
        final Runnable back = () -> showPageElement(e);
        final List<String> l = new ArrayList<>();
        final List<Runnable> a = new ArrayList<>();
        l.add("↔  إزاحة أفقية:  " + prefs.getInt("pg_" + k + "_x", 0) + "  (سالب = يسار)");
        a.add(() -> numberDialog(e[1] + " — إزاحة أفقية (% من عرض الصفحة)", "pg_" + k + "_x", 0, -100, 100,
                "", null, this::applyPageCss, back, back));
        l.add("↕  إزاحة رأسية:  " + prefs.getInt("pg_" + k + "_y", 0) + "  (سالب = أعلى)");
        a.add(() -> numberDialog(e[1] + " — إزاحة رأسية (% من ارتفاع الصفحة)", "pg_" + k + "_y", 0, -100, 100,
                "", null, this::applyPageCss, back, back));
        l.add("🔍  الحجم:  " + prefs.getInt("pg_" + k + "_s", 100) + "%");
        a.add(() -> numberDialog(e[1] + " — الحجم", "pg_" + k + "_s", 100, 20, 300, "%", null,
                this::applyPageCss, back, back));
        l.add("👁  الإخفاء:  " + (prefs.getBoolean("pg_" + k + "_h", false) ? "مخفي" : "ظاهر"));
        a.add(() -> {
            prefs.edit().putBoolean("pg_" + k + "_h", !prefs.getBoolean("pg_" + k + "_h", false)).apply();
            applyPageCss();
            showPageElement(e);
        });
        l.add("↩  إعادة هذا العنصر لمكانه");
        a.add(() -> {
            prefs.edit().remove("pg_" + k + "_x").remove("pg_" + k + "_y").remove("pg_" + k + "_s")
                    .remove("pg_" + k + "_h").apply();
            applyPageCss();
            showPageElement(e);
        });
        menu(e[1], l, a, this::showPageElements);
    }

    private String domDump = "";

    private static final String JS_DOM = "(function(){var o=[],n=0;"
            + "function vis(e){var r=e.getBoundingClientRect();return r.width>0&&r.height>0;}"
            + "var all=document.body.getElementsByTagName('*');"
            + "for(var i=0;i<all.length&&n<900;i++){var e=all[i];if(!vis(e))continue;"
            + "var t='';for(var c=e.firstChild;c;c=c.nextSibling){if(c.nodeType==3)t+=c.nodeValue;}"
            + "t=t.replace(/\\s+/g,' ').trim();"
            + "var leaf=t.length>0||e.tagName=='IMG'||e.tagName=='CANVAS'||e.tagName=='SVG';"
            + "var r=e.getBoundingClientRect();var cs=getComputedStyle(e);"
            + "var big=(r.width>innerWidth*0.15&&r.height>innerHeight*0.1)&&e.children.length>0;"
            + "if(!leaf&&!big)continue;n++;"
            + "var d=0,p=e;while(p&&p!==document.body){d++;p=p.parentElement;}"
            + "o.push(d+'|'+e.tagName.toLowerCase()+(e.id?'#'+e.id:'')"
            + "+(e.className&&e.className.baseVal===undefined?'.'+String(e.className).trim().split(/\\s+/).join('.'):'')"
            + "+' ['+Math.round(r.left)+','+Math.round(r.top)+' '+Math.round(r.width)+'x'+Math.round(r.height)+']'"
            + "+' pos='+cs.position+' disp='+cs.display+' fs='+cs.fontSize+(t?' \"'+t.substring(0,40)+'\"':''));}"
            + "var bg=[];['html','body','.main','.sub-main','.top-content','.prayers'].forEach(function(q){"
            + "var e=document.querySelector(q);if(!e)return;var c=getComputedStyle(e);"
            + "bg.push(q+' bg='+String(c.backgroundImage).substring(0,160)+' col='+c.backgroundColor+' size='+c.backgroundSize);});"
            + "var all2=document.querySelectorAll('*');for(var j=0;j<all2.length&&bg.length<60;j++){var e2=all2[j],c2=getComputedStyle(e2);"
            + "if(c2.backgroundImage&&c2.backgroundImage!=='none'){bg.push('* '+e2.tagName.toLowerCase()+(e2.id?'#'+e2.id:'')+(e2.className&&e2.className.baseVal===undefined?'.'+String(e2.className).trim().split(/\\s+/).join('.'):'')+' bg='+String(c2.backgroundImage).substring(0,160));}}"
            + "o.push('BACKGROUNDS\\n'+bg.join('\\n'));"
            + "var im=[];Array.prototype.forEach.call(document.images,function(g){if(im.length<40)im.push(g.src+' '+g.naturalWidth+'x'+g.naturalHeight);});"
            + "o.push('IMAGES\\n'+im.join('\\n'));"
            + "var fo=[];try{document.fonts.forEach(function(f){fo.push(f.family+' '+f.weight+' '+f.status);});}catch(x){}"
            + "o.push('FONTS\\n'+fo.join('\\n'));"
            + "var ss=[];Array.prototype.forEach.call(document.styleSheets,function(x){ss.push(x.href||'inline');});"
            + "o.push('STYLESHEETS\\n'+ss.join('\\n'));"
            + "var sc=[];Array.prototype.forEach.call(document.scripts,function(x){if(x.src)sc.push(x.src);});"
            + "o.push('SCRIPTS\\n'+sc.join('\\n'));"
            + "var ls=[];try{for(var q=0;q<localStorage.length&&q<40;q++)ls.push(localStorage.key(q));}catch(x){}"
            + "o.push('LOCALSTORAGE KEYS\\n'+ls.join(', '));"
            + "try{o.push('confData keys\\n'+Object.keys(window.confData||{}).join(', '));}catch(x){}"
            + "return 'viewport '+innerWidth+'x'+innerHeight+' body.class='+document.body.className"
            + "+' iframes='+document.getElementsByTagName('iframe').length+'\\n'+o.join('\\n');})()";

    /** Dumps the live page's element tree (ids, classes, boxes) so it can be shared and analysed. */
    private void inspectPage() {
        toast("جارٍ فحص الصفحة…");
        webView.evaluateJavascript(JS_DOM, v -> {
            String r = v == null ? "" : v;
            try {
                r = new JSONArray("[" + r + "]").getString(0);
            } catch (Exception ignored) {
            }
            domDump = r;
            AppLog.info("DOM", "dumped " + r.length() + " chars");
            if (r.length() < 20) {
                toast("لم أحصل على بنية الصفحة (هل الصفحة محمّلة؟)");
            } else {
                startLogServer();
            }
        });
    }

    private String localIp() {
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis != null && nis.hasMoreElements()) {
                Enumeration<InetAddress> as = nis.nextElement().getInetAddresses();
                while (as.hasMoreElements()) {
                    InetAddress a = as.nextElement();
                    if (a instanceof Inet4Address && !a.isLoopbackAddress()) return a.getHostAddress();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String reportText() { return reportText(true); }

    private String reportText(boolean withDom) {
        return "Mawaqit Home " + Updater.currentName(this) + " (" + Updater.currentCode(this) + ")\n"
                + "Android " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT
                + ")  " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + "\n"
                + "Screen " + sizeW + "x" + sizeH + "  density " + getResources().getDisplayMetrics().density + "\n\n"
                + statusText() + (withDom ? "\n\n==== PAGE DOM ====\n" + domDump : "") + "\n\n==== LOG ====\n" + AppLog.all();
    }

    /** Serves the full log on the local network for 10 minutes, behind a random link. */
    private void startLogServer() {
        final String ip = localIp();
        if (ip == null) {
            toast("لا يوجد اتصال شبكة محلي");
            return;
        }
        stopLogServer();
        final String token = Long.toString(Math.abs(new java.security.SecureRandom().nextLong()), 36)
                .substring(0, 6);
        try {
            final ServerSocket ss = new ServerSocket(8765);
            logSrv = ss;
            new Thread(() -> {
                while (!ss.isClosed()) {
                    try {
                        Socket c = ss.accept();
                        try {
                            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
                            String line = r.readLine();
                            boolean ok = line != null && line.startsWith("GET /" + token);
                            boolean raw = ok && line.startsWith("GET /" + token + "/raw");
                            String rep = ok ? reportText() : "not found";
                            String page = raw || !ok ? rep : "<!doctype html><html dir=rtl><meta charset=utf-8>"
                                    + "<meta name=viewport content='width=device-width,initial-scale=1'>"
                                    + "<body style='font-family:sans-serif;margin:8px'>"
                                    + "<button id=b style='font-size:20px;padding:10px 18px' onclick='c()'>📋 نسخ التقرير كاملاً</button> "
                                    + "<a href='/" + token + "/raw'>نص خام</a>"
                                    + "<textarea id=t readonly style='width:100%;height:82vh;direction:ltr;margin-top:8px'>"
                                    + rep.replace("&", "&amp;").replace("<", "&lt;") + "</textarea>"
                                    + "<script>function c(){var t=document.getElementById('t');t.focus();t.select();"
                                    + "var ok=false;try{ok=document.execCommand('copy');}catch(e){}"
                                    + "if(!ok&&navigator.clipboard){navigator.clipboard.writeText(t.value);ok=true;}"
                                    + "document.getElementById('b').textContent=ok?'✓ تم النسخ':'حدّد النص وانسخه يدوياً';}</script>";
                            byte[] body = page.getBytes("UTF-8");
                            OutputStream o = c.getOutputStream();
                            o.write(("HTTP/1.1 " + (ok ? "200 OK" : "404 Not Found")
                                    + "\r\nContent-Type: " + (raw || !ok ? "text/plain" : "text/html") + "; charset=utf-8\r\nContent-Length: "
                                    + body.length + "\r\nConnection: close\r\n\r\n").getBytes("UTF-8"));
                            o.write(body);
                            o.flush();
                        } finally {
                            c.close();
                        }
                    } catch (Exception e) {
                        if (ss.isClosed()) break;
                    }
                }
            }, "logsrv").start();
            handler.postDelayed(this::stopLogServer, 10L * 60 * 1000);
            AppLog.info("LOGSRV", "started on " + ip + ":8765");
            showInfo("مشاركة السجل", "افتح هذا الرابط من متصفح الكمبيوتر أو الجوال (على نفس شبكة الواي فاي)، "
                            + "ثم انسخ النص وأرسله لي. ينتهي الرابط تلقائياً بعد 10 دقائق.",
                    "http://" + ip + ":8765/" + token, "حسناً", null, "إيقاف الآن", this::stopLogServer,
                    null, null);
        } catch (Exception e) {
            AppLog.error("LOGSRV", "start failed", e);
            toast("تعذّر تشغيل المشاركة");
        }
    }

    // ---- add own picture / video links from a phone (QR code -> small page on the local network)

    private ServerSocket linkSrv;

    private void stopLinkServer() {
        try {
            if (linkSrv != null) linkSrv.close();
        } catch (Exception ignored) {
        }
        linkSrv = null;
    }

    private static String urlDecode(String v) {
        try {
            return java.net.URLDecoder.decode(v, "UTF-8");
        } catch (Exception e) {
            return v;
        }
    }

    private static String esc(String v) {
        return v.replace("&", "&amp;").replace("<", "&lt;");
    }

    private void showLinkInput() {
        final String ip = localIp();
        if (ip == null) {
            toast("لا يوجد اتصال شبكة محلي — أدخل الروابط يدوياً");
            promptText("روابط صور (https) مفصولة بمسافة", "https://…/image.jpg", prefs.getString("bg_links", ""),
                    v -> {
                        prefs.edit().putString("bg_links", v).apply();
                        showBgSettings();
                    });
            return;
        }
        stopLinkServer();
        final String token = Long.toString(Math.abs(new java.security.SecureRandom().nextLong()), 36).substring(0, 6);
        final String addr = "http://" + ip + ":8766/" + token;
        try {
            final ServerSocket ss = new ServerSocket(8766);
            linkSrv = ss;
            new Thread(() -> {
                while (!ss.isClosed()) {
                    try {
                        Socket c = ss.accept();
                        try {
                            c.setSoTimeout(8000);
                            InputStream in = c.getInputStream();
                            ByteArrayOutputStream hd = new ByteArrayOutputStream();
                            int b, state = 0;
                            while (hd.size() < 16384 && (b = in.read()) >= 0) {      // headers end at \r\n\r\n
                                hd.write(b);
                                state = (b == '\r' && (state == 0 || state == 2)) ? state + 1
                                        : (b == '\n' && (state == 1 || state == 3)) ? state + 1 : 0;
                                if (state == 4) break;
                            }
                            String head = hd.toString("UTF-8");
                            String first = head.split("\r\n")[0];
                            boolean ok = first.contains("/" + token);
                            String page;
                            if (ok && first.startsWith("POST")) {
                                int len = 0;
                                for (String h : head.split("\r\n"))
                                    if (h.toLowerCase().startsWith("content-length:"))
                                        len = Integer.parseInt(h.substring(15).trim());
                                byte[] body = new byte[Math.min(len, 200_000)];
                                int got = 0;
                                while (got < body.length) {
                                    int n = in.read(body, got, body.length - got);
                                    if (n < 0) break;
                                    got += n;
                                }
                                String imgs = "", vids = "";
                                for (String kv : new String(body, 0, got, "UTF-8").split("&")) {
                                    int eq = kv.indexOf('=');
                                    if (eq < 0) continue;
                                    if (kv.startsWith("i=")) imgs = urlDecode(kv.substring(eq + 1));
                                    if (kv.startsWith("v=")) vids = urlDecode(kv.substring(eq + 1));
                                }
                                final String fi = TextUtils.join(" ", Backgrounds.links(imgs));
                                final String fv = TextUtils.join(" ", Backgrounds.links(vids));
                                runOnUiThread(() -> {
                                    SharedPreferences.Editor ed = prefs.edit().putString("bg_links", fi)
                                            .putString("bg_vlinks", fv);
                                    if (!fi.isEmpty()) ed.putInt("bg_src", prefs.getInt("bg_src", 0) | Backgrounds.OWN);
                                    if (!fv.isEmpty()) ed.putInt("bg_vsrc", prefs.getInt("bg_vsrc", 0) | Backgrounds.V_OWN);
                                    ed.apply();
                                    toast("وصلت الروابط من الجوال ✓");
                                    if (bgEnabled()) bgFetch(false);
                                });
                                page = "<h2>✓ تم الحفظ (" + Backgrounds.links(imgs).size() + " صورة، "
                                        + Backgrounds.links(vids).size() + " فيديو)</h2>";
                            } else if (ok) {
                                page = "<h2>روابط الخلفية الخاصة</h2><form method=post action='/" + token + "'>"
                                        + "<p>روابط الصور (رابط مباشر https، سطر لكل رابط):</p>"
                                        + "<textarea name=i rows=6 style='width:100%;direction:ltr'>"
                                        + esc(prefs.getString("bg_links", "").replace(" ", "\n")) + "</textarea>"
                                        + "<p>روابط الفيديو (mp4 / webm):</p>"
                                        + "<textarea name=v rows=6 style='width:100%;direction:ltr'>"
                                        + esc(prefs.getString("bg_vlinks", "").replace(" ", "\n")) + "</textarea>"
                                        + "<p><button style='font-size:22px;padding:12px 28px'>💾 حفظ</button></p></form>";
                            } else {
                                page = "not found";
                            }
                            byte[] out = ("<!doctype html><html dir=rtl><meta charset=utf-8>"
                                    + "<meta name=viewport content='width=device-width,initial-scale=1'>"
                                    + "<body style='font-family:sans-serif;margin:14px'>" + page).getBytes("UTF-8");
                            OutputStream o = c.getOutputStream();
                            o.write(("HTTP/1.1 " + (ok ? "200 OK" : "404 Not Found")
                                    + "\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: " + out.length
                                    + "\r\nConnection: close\r\n\r\n").getBytes("UTF-8"));
                            o.write(out);
                            o.flush();
                        } finally {
                            c.close();
                        }
                    } catch (Exception e) {
                        if (ss.isClosed()) break;
                    }
                }
            }, "linksrv").start();
            handler.postDelayed(this::stopLinkServer, 15L * 60 * 1000);
        } catch (Exception e) {
            AppLog.error("LINKSRV", "start failed", e);
            toast("تعذّر تشغيل خادم الروابط");
            return;
        }
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView qr = new ImageView(this);
        try {
            com.google.zxing.common.BitMatrix m = new com.google.zxing.qrcode.QRCodeWriter().encode(addr,
                    com.google.zxing.BarcodeFormat.QR_CODE, 400, 400);
            Bitmap bm = Bitmap.createBitmap(400, 400, Bitmap.Config.RGB_565);
            for (int x = 0; x < 400; x++)
                for (int y = 0; y < 400; y++) bm.setPixel(x, y, m.get(x, y) ? Color.BLACK : Color.WHITE);
            qr.setImageBitmap(bm);
        } catch (Exception e) {
            AppLog.error("LINKSRV", "qr failed", e);
        }
        box.addView(qr, new LinearLayout.LayoutParams(dp(210), dp(210)));
        TextView t = text(13, false);
        t.setText("على الجوال (نفس الواي فاي) امسح الرمز أو افتح هذا العنوان:");
        t.setGravity(Gravity.CENTER);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        box.addView(t);
        box.addView(addressView(addr));
        TextView t2 = text(13, false);
        t2.setText("ثم الصق الروابط واضغط «حفظ» — تصل هنا فوراً.");
        t2.setGravity(Gravity.CENTER);
        t2.setTextDirection(View.TEXT_DIRECTION_RTL);
        box.addView(t2);
        dialog("إضافة روابط من الجوال").setView(box)
                .setPositiveButton("تم", (d, w) -> {
                    stopLinkServer();
                    showBgSettings();
                })
                .setNeutralButton("إدخال يدوي", (d, w) -> {
                    stopLinkServer();
                    promptText("روابط صور (https) مفصولة بمسافة", "https://…/image.jpg",
                            prefs.getString("bg_links", ""), v -> {
                                prefs.edit().putString("bg_links", v).apply();
                                showBgSettings();
                            });
                }).show();
    }

    // ---- native video background (MediaPlayer on a TextureView behind a transparent WebView)

    private final android.view.TextureView[] vtv = new android.view.TextureView[2];
    private final android.graphics.SurfaceTexture[] vst = new android.graphics.SurfaceTexture[2];
    private final android.media.MediaPlayer[] vmp = new android.media.MediaPlayer[2];
    private int vcur = -1;
    private boolean vxf;
    private FrameLayout videoBox;
    private android.animation.ValueAnimator driftAnim;
    private String videoPlaying = "";
    private static final long VXF_MS = 1800L;

    private boolean nativeVideoWanted() {
        return bgVideo() && bgVSrc() != 0 && !prefs.getString("bg_vurl", "").isEmpty()
                && prefs.getInt("bg_vmode", 2) == 2;
    }

    private void ensureVideoView() {
        if (videoBox != null) return;
        videoBox = new FrameLayout(this);
        for (int i = 0; i < 2; i++) {
            final int idx = i;
            vtv[i] = new android.view.TextureView(this);
            vtv[i].setAlpha(0f);
            vtv[i].setSurfaceTextureListener(new android.view.TextureView.SurfaceTextureListener() {
                @Override public void onSurfaceTextureAvailable(android.graphics.SurfaceTexture st, int w, int h) {
                    vst[idx] = st;
                    applyNativeVideo();
                }

                @Override public void onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture st, int w, int h) {
                }

                @Override public boolean onSurfaceTextureDestroyed(android.graphics.SurfaceTexture st) {
                    releaseSlot(idx);
                    vst[idx] = null;
                    if (vcur == idx) { vcur = -1; videoPlaying = ""; }
                    return true;
                }

                @Override public void onSurfaceTextureUpdated(android.graphics.SurfaceTexture st) {
                }
            });
            videoBox.addView(vtv[i], new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        shiftLayer.addView(videoBox, 0, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        handler.postDelayed(videoTick, 400L);
    }

    private final Runnable videoTick = new Runnable() {
        @Override public void run() {
            try {
                if (nativeVideoWanted() && vcur >= 0 && !vxf && vmp[vcur] != null && !videoPlaying.isEmpty()) {
                    android.media.MediaPlayer m = vmp[vcur];
                    int d = m.getDuration();
                    if (d >= 12000 && m.isPlaying() && m.getCurrentPosition() > d - VXF_MS - 5000) {
                        vxf = true;                       // start the same clip again underneath and cross-fade
                        loadVideo(1 - vcur, videoPlaying);
                    }
                }
            } catch (Exception ignored) {
            }
            handler.postDelayed(this, 300L);
        }
    };

    private void startDrift(boolean on) {
        if (videoBox == null) return;
        if (!on) {
            if (driftAnim != null) driftAnim.cancel();
            driftAnim = null;
            videoBox.setScaleX(1f);
            videoBox.setScaleY(1f);
            videoBox.setTranslationX(0f);
            return;
        }
        if (driftAnim != null) return;
        driftAnim = android.animation.ValueAnimator.ofFloat(0f, 1f);
        driftAnim.setDuration(50_000L);
        driftAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        driftAnim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        driftAnim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        driftAnim.addUpdateListener(an -> {
            float v = (Float) an.getAnimatedValue();
            videoBox.setPivotX(videoBox.getWidth() * 0.5f);
            videoBox.setPivotY(videoBox.getHeight() * 0.5f);
            videoBox.setScaleX(1.0f + 0.07f * v);
            videoBox.setScaleY(1.0f + 0.07f * v);
            videoBox.setTranslationX(-videoBox.getWidth() * 0.012f * v);
        });
        driftAnim.start();
    }

    /** Called whenever the background settings change. */
    private void applyNativeVideo() {
        boolean want = nativeVideoWanted();
        webView.setBackgroundColor(want ? Color.TRANSPARENT : Color.BLACK);
        if (!want) {
            stopNativeVideo();
            if (videoBox != null) videoBox.setVisibility(View.GONE);
            return;
        }
        ensureVideoView();
        videoBox.setVisibility(View.VISIBLE);
        startDrift(prefs.getBoolean("bg_drift", true));
        String u = prefs.getString("bg_vurl", "");
        if (u.isEmpty() || u.equals(videoPlaying)) return;
        int slot = vcur < 0 ? 0 : 1 - vcur;
        if (vst[slot] == null) return;              // surface not ready yet: its listener calls us again
        videoPlaying = u;
        vxf = true;
        loadVideo(slot, u);
    }

    private void releaseSlot(int i) {
        try {
            if (vmp[i] != null) vmp[i].release();
        } catch (Exception ignored) {
        }
        vmp[i] = null;
    }

    private void stopNativeVideo() {
        releaseSlot(0);
        releaseSlot(1);
        vcur = -1;
        vxf = false;
        videoPlaying = "";
        startDrift(false);
        handler.removeCallbacks(videoTick);
        if (videoBox != null) { vtv[0].setAlpha(0f); vtv[1].setAlpha(0f); }
    }

    private void pauseNativeVideo() {
        for (int i = 0; i < 2; i++) {
            try {
                if (vmp[i] != null && vmp[i].isPlaying()) vmp[i].pause();
            } catch (Exception ignored) {
            }
        }
    }

    private void resumeNativeVideo() {
        try {
            if (vcur >= 0 && vmp[vcur] != null && !vmp[vcur].isPlaying()) vmp[vcur].start();
        } catch (Exception ignored) {
        }
        if (videoBox != null) { handler.removeCallbacks(videoTick); handler.postDelayed(videoTick, 400L); }
    }

    private void loadVideo(final int slot, final String url) {
        if (vst[slot] == null || url.isEmpty()) { vxf = false; return; }
        releaseSlot(slot);
        try {
            final android.media.MediaPlayer mp = new android.media.MediaPlayer();
            vmp[slot] = mp;
            mp.setDataSource(url);
            mp.setSurface(new android.view.Surface(vst[slot]));
            mp.setLooping(false);
            mp.setVolume(0f, 0f);
            final boolean[] shown = {false};
            final Runnable reveal = () -> {
                if (shown[0] || vmp[slot] != mp) return;
                shown[0] = true;
                final int old = vcur;
                vcur = slot;
                vxf = false;
                videoErrors = 0;
                long ms = old < 0 ? 900L : VXF_MS;
                vtv[slot].animate().alpha(1f).setDuration(ms).start();
                if (old >= 0 && old != slot) {
                    vtv[old].animate().alpha(0f).setDuration(ms).withEndAction(() -> {
                        if (vcur != old) releaseSlot(old);
                    }).start();
                }
            };
            mp.setOnPreparedListener(m -> {
                try {
                    if (vmp[slot] != m) return;
                    fitVideo(slot, m.getVideoWidth(), m.getVideoHeight());
                    if (m.getDuration() > 0 && m.getDuration() < 12000) m.setLooping(true);   // too short to cross-fade
                    m.start();
                    AppLog.info("VIDEO", "native playing slot " + slot + " " + m.getVideoWidth() + "x"
                            + m.getVideoHeight() + " dur=" + m.getDuration());
                    handler.postDelayed(reveal, 1500L);
                } catch (Exception e) {
                    AppLog.error("VIDEO", "native start failed", e);
                }
            });
            mp.setOnInfoListener((m, what, extra) -> {
                if (what == android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) reveal.run();
                return false;
            });
            mp.setOnVideoSizeChangedListener((m, w, h) -> fitVideo(slot, w, h));
            mp.setOnErrorListener((m, what, extra) -> {
                AppLog.info("VIDEO", "native error what=" + what + " extra=" + extra + " " + url);
                if (vmp[slot] == m) releaseSlot(slot);
                vxf = false;
                if (vcur < 0) videoPlaying = "";
                if (vcur < 0 && ++videoErrors <= 4 && bgVideo()) handler.postDelayed(() -> bgFetch(false), 3000L);
                return true;
            });
            mp.prepareAsync();
            AppLog.info("VIDEO", "native load slot " + slot + " " + url);
        } catch (Exception e) {
            AppLog.error("VIDEO", "native load failed", e);
            vxf = false;
            if (vcur < 0) videoPlaying = "";
        }
    }

    /** Scale-to-fill (crop) like CSS object-fit: cover. */
    private void fitVideo(int slot, int vw, int vh) {
        if (vtv[slot] == null || vw <= 0 || vh <= 0) return;
        int sw = Math.max(1, root.getWidth()), sh = Math.max(1, root.getHeight());
        float sc = Math.max(sw / (float) vw, sh / (float) vh);
        vtv[slot].setLayoutParams(new FrameLayout.LayoutParams(Math.round(vw * sc), Math.round(vh * sc),
                Gravity.CENTER));
    }

    private void stopLogServer() {
        try {
            if (logSrv != null) logSrv.close();
        } catch (Exception ignored) {
        }
        logSrv = null;
    }

    // ------------------------------------------------- Favorites / hidden

    private void loadLists() {
        loadList("favs", favs);
        loadList("order", order);
        loadList("hidden", hidden);
        loadCats();
    }

    private void loadList(String key, List<String> out) {
        out.clear();
        String s = prefs.getString(key, "");
        if (!s.isEmpty()) out.addAll(Arrays.asList(s.split(",")));
    }

    private void saveList(String key, List<String> list) {
        prefs.edit().putString(key, TextUtils.join(",", list)).apply();
    }

    private void toggleFavorite(AppEntry a) {
        int idx = indexOfPkg(listFor(tab()), a.pkg);
        String msg;
        if (favs.contains(a.pkg)) {
            favs.remove(a.pkg);
            msg = "حُذف من المفضلة: ";
        } else {
            favs.add(a.pkg);
            msg = "أُضيف إلى المفضلة ★: ";
        }
        saveList("favs", favs);
        toast(msg + a.label);
        showTab(currentTab, idx);
    }

    private void hideApp(AppEntry a) {
        int idx = indexOfPkg(listFor(tab()), a.pkg);
        if (!hidden.contains(a.pkg)) hidden.add(a.pkg);
        saveList("hidden", hidden);
        toast("أُخفي: " + a.label + " (يُعاد من ⚙ ← التطبيقات المخفية)");
        showTab(currentTab, idx);
    }

    private void showHiddenDialog() {
        if (hidden.isEmpty()) {
            toast("لا توجد تطبيقات مخفية");
            return;
        }
        final List<String> pkgs = new ArrayList<>(hidden);
        String[] items = new String[pkgs.size()];
        for (int i = 0; i < items.length; i++) {
            AppEntry a = byPkg.get(pkgs.get(i));
            items[i] = a != null ? a.label : pkgs.get(i);
        }
        dialog("اضغط على تطبيق لإظهاره").setItems(items, (d, which) -> {
            hidden.remove(pkgs.get(which));
            saveList("hidden", hidden);
            toast("تم إظهاره");
            showTab(currentTab, -1);
        }).show();
    }

    // --------------------------------------------------------------- Backup

    private File backupFile() {
        File dir = getExternalFilesDir(null);
        if (dir == null) dir = getFilesDir();
        return new File(dir, "mawaqit-home-backup.json");
    }

    private static boolean skipKey(String k) {
        return k.equals("next_wake") || k.equals("last_code") || k.equals("upd_notified")
                || k.equals("notice") || k.equals("upd_checked");
    }

    private JSONObject settingsJson(boolean profile) throws Exception {
        JSONObject o = new JSONObject();
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            Object v = e.getValue();
            String k = e.getKey();
            if (skipKey(k)) continue;
            if (profile && (k.equals("profiles") || k.equals("bg_pexels") || k.equals("bg_pixabay")
                    || k.equals("bg_vurl") || k.equals("bg_last") || k.equals("bg_slot") || k.equals("bg_credit"))) continue;
            if (v instanceof String || v instanceof Integer || v instanceof Long || v instanceof Boolean) o.put(k, v);
        }
        return o;
    }

    private void backupSettings() {
        try {
            File f = backupFile();
            FileOutputStream out = new FileOutputStream(f);
            out.write(settingsJson(false).toString(2).getBytes("UTF-8"));
            out.close();
            dialog("تم النسخ الاحتياطي").setMessage("المفضلة والتصنيفات والترتيب والإعدادات حُفظت في:\n\n"
                    + f.getAbsolutePath()
                    + "\n\nلنقلها لجهاز آخر:\nadb pull " + f.getAbsolutePath()
                    + "\n\nأو أرسلها بالبريد من «النسخ الاحتياطي وملفات التعريف».")
                    .setPositiveButton("حسناً", null).show();
        } catch (Exception e) {
            AppLog.error("BACKUP", "backup failed", e);
            toast("فشل النسخ الاحتياطي");
        }
    }

    /** Silent daily backup so the settings can always be restored. */
    private void autoBackup() {
        if (!prefs.getBoolean("auto_backup", true)) return;
        try {
            FileOutputStream out = new FileOutputStream(backupFile());
            out.write(settingsJson(false).toString(2).getBytes("UTF-8"));
            out.close();
        } catch (Exception e) {
            AppLog.error("BACKUP", "auto backup failed", e);
        }
    }

    private static boolean profileExcluded(String k) {
        return k.equals("profiles") || k.equals("bg_pexels") || k.equals("bg_pixabay") || k.equals("bg_vurl")
                || k.equals("bg_last") || k.equals("bg_slot") || k.equals("bg_credit") || skipKey(k);
    }

    /** A saved profile is a full snapshot: settings changed since then (absent from it) go back to default. */
    private void applyFullProfile(JSONObject o) throws Exception {
        SharedPreferences.Editor ed = prefs.edit();
        for (String k : new ArrayList<>(prefs.getAll().keySet())) {
            if (!profileExcluded(k) && !o.has(k)) ed.remove(k);
        }
        ed.apply();
        applyJson(o, true);
    }

    private void applyJson(JSONObject o, boolean profile) throws Exception {
        SharedPreferences.Editor ed = prefs.edit();
        Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String k = it.next();
            if (skipKey(k) || (profile && k.equals("profiles"))) continue;
            Object v = o.get(k);
            if (v instanceof String) ed.putString(k, (String) v);
            else if (v instanceof Boolean) ed.putBoolean(k, (Boolean) v);
            else if (v instanceof Integer) ed.putInt(k, (Integer) v);
            else if (v instanceof Long) ed.putLong(k, (Long) v);
        }
        if (profile) ed.putLong("bg_last", 0);
        ed.apply();
    }

    // ---- profiles ("Ramadan", "Friday"…): built-in presets + the user's saved snapshots

    private static final String[] PROFILE_NAMES = {"🌙 رمضان — خلفيات إسلامية وتغيير كل نصف ساعة",
            "🕌 الجمعة — صور المساجد وتعتيم خفيف", "🌌 ليلي هادئ — تعتيم عالٍ ولون أزرق",
            "🧼 نظيف — خلفية Mawaqit الأصلية بلا صور"};

    private JSONObject builtinProfile(int i) throws Exception {
        JSONObject o = new JSONObject();
        boolean pix = prefs.getString("bg_pixabay", "").length() > 8, pex = prefs.getString("bg_pexels", "").length() > 8;
        int isl = 8 | (pix ? 16 : 0) | (pex ? 32 : 0);
        if (i == 0) {
            o.put("bg_type", 0).put("bg_src", isl).put("bg_mode", 0).put("bg_every", 30).put("bg_unit", 0)
                    .put("bg_fade", 3).put("bg_dim", 40).put("w_tod", true).put("ui_accent", 0);
        } else if (i == 1) {
            o.put("bg_type", 0).put("bg_src", isl).put("bg_mode", 1).put("bg_hour", 11).put("bg_fade", 3)
                    .put("bg_dim", 30).put("ui_accent", 2);
        } else if (i == 2) {
            o.put("bg_type", 0).put("bg_src", 1 | 2).put("bg_mode", 0).put("bg_every", 3).put("bg_unit", 1)
                    .put("bg_fade", 4).put("bg_dim", 60).put("ui_accent", 3);
        } else {
            o.put("bg_src", 0).put("bg_vsrc", 0);
        }
        return o;
    }

    private void showBackupMenu() {
        final List<String> l = new ArrayList<>();
        final List<Runnable> a = new ArrayList<>();
        l.add("🎭  ملفات التعريف (رمضان، الجمعة…)");
        a.add(this::showProfiles);
        l.add("📤  نسخ احتياطي كامل إلى ملف الآن");
        a.add(this::backupSettings);
        l.add("📥  استعادة من الملف");
        a.add(this::restoreSettings);
        l.add("📧  إرسال النسخة الاحتياطية بالبريد");
        a.add(() -> {
            try {
                sendMail(settingsJson(false).toString(2));
            } catch (Exception e) {
                toast("تعذّر");
            }
        });
        l.add("📋  نسخ النسخة الاحتياطية (للصق)");
        a.add(() -> {
            try {
                ((android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(
                        android.content.ClipData.newPlainText("Mawaqit Home backup", settingsJson(false).toString(2)));
                toast("تم النسخ ✓");
            } catch (Exception e) {
                toast("تعذّر النسخ");
            }
        });
        l.add("📝  استعادة من نص ملصوق");
        a.add(() -> promptText("الصق نص النسخة الاحتياطية", "{ … }", "", v -> {
            try {
                applyJson(new JSONObject(v.trim()), false);
                toast("تمت الاستعادة");
                recreate();
            } catch (Exception e) {
                toast("النص غير صالح");
            }
        }));
        l.add("🕒  نسخ احتياطي تلقائي يومي:  " + onOff(prefs.getBoolean("auto_backup", true)));
        a.add(() -> {
            prefs.edit().putBoolean("auto_backup", !prefs.getBoolean("auto_backup", true)).apply();
            showBackupMenu();
        });
        menu("النسخ الاحتياطي وملفات التعريف", l, a, this::showLauncherSettings);
    }

    private void showProfiles() {
        final List<String> l = new ArrayList<>();
        final List<Runnable> a = new ArrayList<>();
        for (int i = 0; i < PROFILE_NAMES.length; i++) {
            final int idx = i;
            l.add(PROFILE_NAMES[i]);
            a.add(() -> dialog(PROFILE_NAMES[idx]).setMessage("يغيّر إعدادات الخلفية والألوان فقط، وبقية إعداداتك تبقى كما هي. "
                    + "يمكنك الرجوع بحفظ إعداداتك الحالية كملف قبل ذلك.")
                    .setPositiveButton("تطبيق", (d, w) -> {
                        try {
                            applyJson(builtinProfile(idx), true);
                            toast("طُبّق الملف");
                            recreate();
                        } catch (Exception e) {
                            toast("تعذّر");
                        }
                    }).setNegativeButton("رجوع", (d, w) -> showProfiles()).show());
        }
        JSONObject saved;
        try {
            saved = new JSONObject(prefs.getString("profiles", "{}"));
        } catch (Exception e) {
            saved = new JSONObject();
        }
        final JSONObject sv = saved;
        Iterator<String> it = sv.keys();
        while (it.hasNext()) {
            final String nm = it.next();
            l.add("💾  " + nm);
            a.add(() -> dialog(nm).setMessage("ملف محفوظ من إعداداتك: كل الإعدادات التي كانت وقت الحفظ.")
                    .setPositiveButton("تطبيق", (d, w) -> {
                        try {
                            applyFullProfile(sv.getJSONObject(nm));
                            toast("طُبّق الملف");
                            recreate();
                        } catch (Exception e) {
                            toast("تعذّر");
                        }
                    })
                    .setNeutralButton("حذف", (d, w) -> {
                        sv.remove(nm);
                        prefs.edit().putString("profiles", sv.toString()).apply();
                        showProfiles();
                    })
                    .setNegativeButton("رجوع", (d, w) -> showProfiles()).show());
        }
        l.add("➕  حفظ إعداداتي الحالية كملف جديد");
        a.add(() -> promptText("اسم الملف", "مثلاً: رمضان بيتي", "", v -> {
            String nm = v.trim();
            if (nm.isEmpty()) return;
            try {
                sv.put(nm, settingsJson(true));
                prefs.edit().putString("profiles", sv.toString()).apply();
                toast("تم الحفظ ✓");
            } catch (Exception e) {
                toast("تعذّر الحفظ");
            }
            showProfiles();
        }));
        menu("ملفات التعريف", l, a, this::showBackupMenu);
    }

    private void restoreSettings() {
        File f = backupFile();
        if (!f.exists()) {
            dialog("لا توجد نسخة احتياطية").setMessage("لم أجد الملف:\n" + f.getAbsolutePath()
                    + "\n\nإذا كانت النسخة على الكمبيوتر:\nadb push mawaqit-home-backup.json "
                    + f.getAbsolutePath())
                    .setPositiveButton("حسناً", null).show();
            return;
        }
        try {
            FileInputStream in = new FileInputStream(f);
            String s = readAll(in);
            in.close();
            applyJson(new JSONObject(s), false);
            toast("تمت الاستعادة");
            recreate();
        } catch (Exception e) {
            AppLog.error("BACKUP", "restore failed", e);
            toast("فشلت الاستعادة: الملف غير صالح");
        }
    }

    // -------------------------------------------------------------- Reorder

    private void startMove(AppEntry a) {
        moveMode = true;
        movePkg = a.pkg;
        showTab(currentTab, indexOfPkg(listFor(tab()), a.pkg));
        nameLabel.setText(MOVE_HINT);
    }

    private void endMove() {
        int idx = movePkg == null ? 0 : indexOfPkg(listFor(tab()), movePkg);
        moveMode = false;
        movePkg = null;
        showTab(currentTab, Math.max(idx, 0));
    }

    private void moveBy(int delta) {
        Tab tb = tab();
        List<AppEntry> list = listFor(tb);
        int from = indexOfPkg(list, movePkg);
        if (from < 0) return;
        int to = Math.max(0, Math.min(list.size() - 1, from + delta));
        if (to == from) return;
        String neighbor = list.get(to).pkg;

        List<String> target;
        if (tb.kind == K_FAV) {
            target = favs;
        } else if (tb.kind == K_CUSTOM) {
            target = tb.cat.pkgs;
        } else {
            order.clear();
            for (AppEntry a : entries) order.add(a.pkg);
            target = order;
        }
        target.remove(movePkg);
        int ni = target.indexOf(neighbor);
        if (ni < 0) ni = target.size();
        target.add(to < from ? ni : ni + 1, movePkg);

        if (tb.kind == K_FAV) saveList("favs", favs);
        else if (tb.kind == K_CUSTOM) saveCats();
        else {
            saveList("order", order);
            sortEntries();
        }
        showTab(currentTab, indexOfPkg(listFor(tab()), movePkg));
        nameLabel.setText(MOVE_HINT);
    }

    private void sortEntries() {
        Collections.sort(entries, new Comparator<AppEntry>() {
            @Override public int compare(AppEntry x, AppEntry y) {
                int ix = order.indexOf(x.pkg), iy = order.indexOf(y.pkg);
                if (ix < 0) ix = Integer.MAX_VALUE;
                if (iy < 0) iy = Integer.MAX_VALUE;
                if (ix != iy) return ix < iy ? -1 : 1;
                return x.label.compareToIgnoreCase(y.label);
            }
        });
    }

    // ------------------------------------------------------------- App list

    private boolean reloadApps() {
        PackageManager pm = getPackageManager();
        Map<String, ResolveInfo> found = new LinkedHashMap<>();
        Map<String, Boolean> isTv = new HashMap<>();

        Intent q = new Intent(Intent.ACTION_MAIN);
        q.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
            String p = ri.activityInfo.packageName;
            if (p.equals(getPackageName()) || found.containsKey(p)) continue;
            found.put(p, ri);
            isTv.put(p, true);
        }
        q = new Intent(Intent.ACTION_MAIN);
        q.addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
            String p = ri.activityInfo.packageName;
            if (p.equals(getPackageName()) || found.containsKey(p)) continue;
            found.put(p, ri);
            isTv.put(p, false);
        }

        TreeSet<String> sigSet = new TreeSet<>();
        for (Map.Entry<String, ResolveInfo> e : found.entrySet()) {
            sigSet.add(e.getKey() + "/" + e.getValue().activityInfo.name
                    + (isTv.get(e.getKey()) ? "T" : "M"));
        }
        String sig = TextUtils.join("|", sigSet);
        if (sig.equals(lastSig) && !entries.isEmpty()) return false;
        lastSig = sig;

        entries.clear();
        byPkg.clear();
        for (Map.Entry<String, ResolveInfo> e : found.entrySet()) {
            ResolveInfo ri = e.getValue();
            AppEntry a = new AppEntry(e.getKey(), ri.activityInfo.name,
                    ri.loadLabel(pm).toString(), hiIcon(pm, ri), isTv.get(e.getKey()));
            if (prefs.getInt("icon_style", 0) >= 9) {
                try {
                    a.banner = ri.activityInfo.loadBanner(pm);
                    if (a.banner == null) a.banner = ri.activityInfo.applicationInfo.loadBanner(pm);
                } catch (Throwable ignored) {
                }
            }
            entries.add(a);
            byPkg.put(a.pkg, a);
        }
        sortEntries();
        return true;
    }

    /** Launcher icon at xhdpi (sharp but memory-safe); trimmed + scaled so every app fills the same area. */
    private Drawable hiIcon(PackageManager pm, ResolveInfo ri) {
        Drawable d = null;
        try {
            android.content.pm.ApplicationInfo ai = ri.activityInfo.applicationInfo;
            int id = ri.activityInfo.icon != 0 ? ri.activityInfo.icon : ai.icon;
            if (id != 0) d = pm.getResourcesForApplication(ai).getDrawableForDensity(id, 320, null);
        } catch (Throwable ignored) {
        }
        try {
            if (d == null) d = ri.loadIcon(pm);
        } catch (Throwable t) {
            return pm.getDefaultActivityIcon();
        }
        if (!prefs.getBoolean("icon_norm", true)) return d;
        try {
            return normalizeIcon(d);
        } catch (Throwable t) {
            return d;
        }
    }

    private Drawable normalizeIcon(Drawable d) {
        final int N = 160;
        Bitmap src = Bitmap.createBitmap(N, N, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas cv = new android.graphics.Canvas(src);
        d.setBounds(0, 0, N, N);
        d.draw(cv);
        int[] px = new int[N * N];
        src.getPixels(px, 0, N, 0, 0, N, N);
        int x0 = N, y0 = N, x1 = -1, y1 = -1;
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                if ((px[y * N + x] >>> 24) > 24) {
                    if (x < x0) x0 = x;
                    if (x > x1) x1 = x;
                    if (y < y0) y0 = y;
                    if (y > y1) y1 = y;
                }
            }
        }
        if (x1 < x0 || y1 < y0) return d;
        int w = x1 - x0 + 1, h = y1 - y0 + 1, m = Math.max(w, h);
        if (m < N / 3) return d;                        // practically empty: leave it
        Bitmap out = Bitmap.createBitmap(N, N, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas oc = new android.graphics.Canvas(out);
        float sc = (N * 0.96f) / m;
        android.graphics.Matrix mx = new android.graphics.Matrix();
        mx.postTranslate(-(x0 + w / 2f), -(y0 + h / 2f));
        mx.postScale(sc, sc);
        mx.postTranslate(N / 2f, N / 2f);
        android.graphics.Paint pt = new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG
                | android.graphics.Paint.ANTI_ALIAS_FLAG);
        oc.drawBitmap(src, mx, pt);
        src.recycle();
        return new android.graphics.drawable.BitmapDrawable(getResources(), out);
    }

    // --------------------------------------------------------- Screen saver

    private void applyStage(int s, boolean animate) {
        if (sizeH <= 0) {
            stage = s;
            return;
        }
        boolean ui = s == 0;
        boolean was = stage == 0;
        stage = s;
        long dur = animate ? ms(800L) : 0L;
        DecelerateInterpolator ip = new DecelerateInterpolator(1.6f);

        if (!ui && railOpen) closeRail(false);
        if (ui) applyRootBg(); else root.setBackgroundColor(Color.BLACK);   // no green edges while pixel-shifting

        strip.animate().translationY(ui ? 0f : stripH + dp(6)).setDuration(dur)
                .setInterpolator(ip).start();
        applyWeatherVis(animate);
        applySaverClear();

        if (!fillMode) {
            webView.animate().scaleX(ui ? pageRatio : 1f).scaleY(ui ? pageRatio : 1f)
                    .setDuration(dur).setInterpolator(ip).start();
        } else if (was != ui) {
            resizeWebFill(!ui, animate);
        }

        float dark = s >= 3 ? 1f : (s == 2 ? DIM_ALPHA : 0f);
        overlay.animate().alpha(dark).setDuration(animate ? (s >= 2 ? 4000L : ms(500L)) : 0L).start();
        AppLog.info("SAVER", "stage " + s);
        if (ui) maybeShowNotice();
    }

    /** During the saver the page content gets more transparent so the picture / video shows more. */
    private void applySaverClear() {
        if (webView == null) return;
        int pc = prefs.getInt("sv_clear", 0);
        boolean on = stage >= 1 && stage <= 2 && pc > 0;
        String css = on ? "body > *:not(#mh-i0):not(#mh-i1):not(#mh-dim){opacity:" + (1 - pc / 100f)
                + " !important;transition:opacity 1.5s}" : "";
        webView.evaluateJavascript("(function(){var s=document.getElementById('mh-sv');if(!s){"
                + "s=document.createElement('style');s.id='mh-sv';(document.head||document.documentElement).appendChild(s);}"
                + "s.textContent='" + css + "';})()", null);
    }

    private void applyRootBg() {
        if (root == null) return;
        root.setBackground(new ColorDrawable(Color.BLACK));   // black behind everything: no coloured edge lines
    }

    private void resizeWebFill(final boolean full, boolean animate) {
        final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) webView.getLayoutParams();
        final int target = full || prefs.getBoolean("strip_over", false)
                ? ViewGroup.LayoutParams.MATCH_PARENT : topH;
        if (!animate) {
            lp.height = target;
            webView.setLayoutParams(lp);
            return;
        }
        webView.animate().alpha(0f).setDuration(ms(220)).withEndAction(() -> {
            lp.height = target;
            webView.setLayoutParams(lp);
            webView.animate().alpha(1f).setDuration(ms(450)).start();
        }).start();
    }

    // ----------------------------------------------------------------- Keys

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        lastInput = SystemClock.elapsedRealtime();
        int k = e.getKeyCode();
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            if (stage > 0) {                       // first key only wakes the screen
                applyStage(0, true);
                return true;
            }
            if (wMove) {
                if (k == KeyEvent.KEYCODE_DPAD_LEFT) adjustWeather(-3, 0);
                else if (k == KeyEvent.KEYCODE_DPAD_RIGHT) adjustWeather(3, 0);
                else if (k == KeyEvent.KEYCODE_DPAD_UP) adjustWeather(0, -3);
                else if (k == KeyEvent.KEYCODE_DPAD_DOWN) adjustWeather(0, 3);
                else if (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER
                        || k == KeyEvent.KEYCODE_BACK) endWeatherMove();
                return true;
            }
            if (moveMode) {
                if (k == KeyEvent.KEYCODE_DPAD_LEFT) moveBy(-1);
                else if (k == KeyEvent.KEYCODE_DPAD_RIGHT) moveBy(1);
                else if (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER
                        || k == KeyEvent.KEYCODE_BACK) endMove();
                return true;
            }
            int toEdge = railRight ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT;
            int away = railRight ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
            if (railOpen) {
                scheduleRailClose();
                if (k == toEdge) return true;
                if (k == away || k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER
                        || k == KeyEvent.KEYCODE_BACK) {
                    closeRail(true);
                    return true;
                }
                return super.dispatchKeyEvent(e);   // up/down moves between categories
            }
            if (k == KeyEvent.KEYCODE_BACK) return true;
            if (k == toEdge && atEdge(!railRight)) {
                openRail();
                return true;
            }
            if ((k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN) && rows == 1) {
                return true;
            }
            if (root.findFocus() == null) focusFirst();
        } else if (moveMode || wMove) {
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    // -------------------------------------------------------------- Helpers

    private void hideSystemUi() {
        root.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
