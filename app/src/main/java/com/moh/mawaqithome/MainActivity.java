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
import android.app.AlertDialog;
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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
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
        long max, min, hum = -1, rain = -1;
    }

    private static class WeatherData {
        final Day[] days = new Day[3];
        long curTemp, curHum;
        String curIcon = "☁️";
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
    private ScrollView railScroll;
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
            lastDay = d;
            maybeShowNotice();
            handler.postDelayed(this, 30_000L);
        }
    };

    private final Runnable shiftTask = new Runnable() {
        @Override public void run() {
            boolean on = prefs.getBoolean("px_shift", true);
            shiftIdx = on ? (shiftIdx + 1) % SHIFTS.length : 0;
            shiftLayer.setTranslationX(dp(SHIFTS[shiftIdx][0]));
            shiftLayer.setTranslationY(dp(SHIFTS[shiftIdx][1]));
            handler.postDelayed(this, PIXEL_SHIFT_EVERY_MS);
        }
    };

    private final Runnable idleTask = new Runnable() {
        @Override public void run() {
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
        webView.setFocusable(false);
        webView.setFocusableInTouchMode(false);
        configureWebView();
        shiftLayer.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

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

        setContentView(root);
        applySounds(root);
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

        webView.loadUrl(url);
        handler.postDelayed(reloadTask, RELOAD_EVERY_MS);
        handler.post(weatherTask);
        handler.post(tickTask);
        handler.postDelayed(idleTask, 10_000L);
        handler.postDelayed(shiftTask, PIXEL_SHIFT_EVERY_MS);
        handler.postDelayed(updateTask, 45_000L);
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
        shiftLayer.removeView(webView);
        webView.destroy();
        AppLog.info("APP", "destroyed");
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // Home screen: Back does nothing.
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
            public void onPageStarted(WebView view, String u, Bitmap favicon) {
                errorThisLoad = false;
            }

            @Override
            public void onPageFinished(WebView view, String finishedUrl) {
                if (errorThisLoad) return;          // Android also calls this after a failed load
                pageOk = true;
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
                + "&current=temperature_2m,relative_humidity_2m,weather_code&daily=";
        String tail = "&timezone=auto&forecast_days=3";
        String basic = "weather_code,temperature_2m_max,temperature_2m_min";
        String body;
        try {
            body = httpGet(head + basic + ",relative_humidity_2m_mean,precipitation_probability_max"
                    + tail);
        } catch (IOException e) {
            body = httpGet(head + basic + tail);     // fall back to the basic fields
        }
        JSONObject j = new JSONObject(body);
        WeatherData wd = new WeatherData();

        JSONObject cur = j.getJSONObject("current");
        wd.curTemp = Math.round(cur.getDouble("temperature_2m"));
        wd.curHum = Math.round(cur.getDouble("relative_humidity_2m"));
        wd.curIcon = weatherIcon(cur.getInt("weather_code"));

        JSONObject d = j.getJSONObject("daily");
        JSONArray wc = d.getJSONArray("weather_code");
        JSONArray mx = d.getJSONArray("temperature_2m_max");
        JSONArray mn = d.getJSONArray("temperature_2m_min");
        JSONArray hm = d.optJSONArray("relative_humidity_2m_mean");
        JSONArray rn = d.optJSONArray("precipitation_probability_max");
        JSONArray tm = d.optJSONArray("time");
        for (int i = 0; i < 3; i++) {
            Day day = new Day();
            day.icon = weatherIcon(wc.getInt(i));
            day.max = Math.round(mx.getDouble(i));
            day.min = Math.round(mn.getDouble(i));
            day.hum = (hm != null && !hm.isNull(i)) ? Math.round(hm.getDouble(i)) : -1;
            day.rain = (rn != null && !rn.isNull(i)) ? Math.round(rn.getDouble(i)) : -1;
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

    private float unit() {
        int w = sizeW > 0 ? sizeW : getResources().getDisplayMetrics().widthPixels;
        return Math.max(0.6f, Math.min(2.4f, w / 1280f));
    }

    /** Weather-card pixel size: scales with the screen width and the "card size" setting. */
    private int wsc(float v) {
        return Math.round(v * unit() * prefs.getInt("w_size", 100) / 100f);
    }

    private void setPx(TextView t, float size) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, wsc(size));
    }

    private void buildWeatherCard() {
        weatherCard = new LinearLayout(this);
        weatherCard.setOrientation(LinearLayout.VERTICAL);
        weatherCard.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        weatherCity = text(12, true);
        weatherCity.setTextColor(0xFFF4C95D);
        weatherCity.setTextDirection(View.TEXT_DIRECTION_RTL);
        weatherCity.setGravity(Gravity.CENTER);
        weatherCity.setSingleLine(false);
        weatherCity.setMaxLines(2);
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
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            wDay[i] = text(12, true);
            wDay[i].setTextDirection(View.TEXT_DIRECTION_RTL);
            wDay[i].setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            wIcon[i] = text(26, false);
            wIcon[i].setGravity(Gravity.CENTER);
            wIcon[i].setText("—");

            LinearLayout tc = new LinearLayout(this);
            tc.setOrientation(LinearLayout.VERTICAL);
            wTemp[i] = text(15, true);
            wTemp[i].setGravity(Gravity.CENTER);
            wTemp[i].setText("--°");
            wSub[i] = text(10, false);
            wSub[i].setTextColor(0xE6FFFFFF);
            wSub[i].setGravity(Gravity.CENTER);
            wSub[i].setTextDirection(View.TEXT_DIRECTION_RTL);
            wSub[i].setText("💧 —");
            tc.addView(wTemp[i], new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tc.addView(wSub[i], new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            row.addView(wDay[i]);
            row.addView(wIcon[i]);
            row.addView(tc);
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
        weatherCard.setPadding(wsc(14), wsc(8), wsc(14), wsc(6));
        setPx(weatherCity, 12);
        setPx(weatherAge, 10);
        LinearLayout.LayoutParams sp = (LinearLayout.LayoutParams) wSep.getLayoutParams();
        sp.height = Math.max(1, wsc(1));
        sp.topMargin = wsc(5);
        sp.bottomMargin = wsc(3);
        wSep.setLayoutParams(sp);
        final boolean cols = prefs.getInt("w_layout", 0) == 1;
        weatherCard.setLayoutDirection(prefs.getInt("w_flip", 0) == 1
                ? View.LAYOUT_DIRECTION_LTR : View.LAYOUT_DIRECTION_RTL);
        daysBox.setOrientation(cols ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        for (int i = 0; i < 3; i++) {
            LinearLayout row = dayView[i];
            View tc = (View) wTemp[i].getParent();
            row.setOrientation(cols ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
            row.setGravity(cols ? Gravity.CENTER_HORIZONTAL : Gravity.CENTER_VERTICAL);
            row.setPadding(cols ? wsc(6) : 0, wsc(2), cols ? wsc(6) : 0, wsc(2));
            row.setLayoutParams(cols ? new LinearLayout.LayoutParams(wsc(104),
                    ViewGroup.LayoutParams.WRAP_CONTENT) : new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            setPx(wDay[i], 12);
            wDay[i].setGravity(cols ? Gravity.CENTER : Gravity.START | Gravity.CENTER_VERTICAL);
            wDay[i].setLayoutParams(cols ? new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    : new LinearLayout.LayoutParams(wsc(96), ViewGroup.LayoutParams.WRAP_CONTENT));
            setPx(wIcon[i], 26);
            wIcon[i].setLayoutParams(cols ? new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    : new LinearLayout.LayoutParams(wsc(36), ViewGroup.LayoutParams.WRAP_CONTENT));
            setPx(wTemp[i], 15);
            setPx(wSub[i], 10);
            tc.setLayoutParams(cols ? new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    : new LinearLayout.LayoutParams(wsc(104), ViewGroup.LayoutParams.WRAP_CONTENT));
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
        if (prefs.getInt("w_layout", 0) == 1) return wsc(14 + 14 + 3 * 112);
        return wsc(14 + 14 + 96 + 36 + 104);
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
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{((a * 255 / 100) << 24) | (paletteColors()[0] & 0xFFFFFF),
                        ((a * 255 / 100) << 24) | (paletteColors()[1] & 0xFFFFFF)});
        bg.setStroke(Math.max(1, dp(1)), argb(a * 90 / 100, 0xF4, 0xC9, 0x5D));
        bg.setCornerRadius(wsc(16));
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
        String[] names = {"اليوم", "غداً", "بعد غد"};
        for (int i = 0; i < 3; i++) {
            Day d = weather.days[i];
            wDay[i].setText(dayLabel(names[i], d.date));
            String rain = d.rain >= 0 ? "   🌧 " + d.rain + "%" : "";
            if (i == 0) {
                wIcon[i].setText(weather.curIcon);
                wTemp[i].setText(weather.curTemp + "°");
                wSub[i].setText("💧 " + weather.curHum + "%" + rain);
            } else {
                wIcon[i].setText(d.icon);
                wTemp[i].setText(d.max + "° / " + d.min + "°");
                wSub[i].setText("💧 " + (d.hum >= 0 ? d.hum + "%" : "—") + rain);
            }
        }
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
        AppLog.info("SLEEP", "going to sleep (adb keyevent 223)");
        AdbShell.run(this, "input keyevent 223", new AdbShell.Callback() {
            @Override public void done(boolean ok, String output) {
                if (ok) return;
                AppLog.info("SLEEP", "adb failed, trying device admin lockNow");
                try {
                    if (adminActive()) {
                        ((DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE)).lockNow();
                    }
                } catch (Exception e) {
                    AppLog.error("SLEEP", "lockNow failed", e);
                }
            }
        });
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
                } else if (mode == 2) {
                    downloadUpdate(false);
                } else if (prefs.getInt("upd_notified", 0) != info.code) {
                    prefs.edit().putInt("upd_notified", info.code).apply();
                    showBubble("⬆ تحديث متاح: " + info.name + " — ⚙ ← إعدادات Mawaqit Home ← التحديثات",
                            12000L);
                }
            }
        });
    }

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

    private TextView text(int size, boolean bold) {
        TextView t = new TextView(this);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, dp(size));
        t.setShadowLayer(dp(2), 0f, dp(1), 0xFF000000);
        t.setTextDirection(View.TEXT_DIRECTION_LTR);
        t.setSingleLine(true);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
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

        rowScroll = new HorizontalScrollView(this);
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
        railScroll = new ScrollView(this);
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
                (railRight ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
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
            t.setEllipsize(TextUtils.TruncateAt.END);
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
        topH = Math.round(h * pageRatio);
        stripH = h - topH;
        pageRatio = topH / (float) h;
        layoutWeather();

        FrameLayout.LayoutParams wl = (FrameLayout.LayoutParams) webView.getLayoutParams();
        root.setBackground(fillMode ? new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF05100C, 0xFF0B2A20}) : new ColorDrawable(Color.BLACK));
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
        float base = Math.min(perRow, dp(150)) * 0.88f;
        tilePx = Math.max(dp(34), Math.min(perRow, Math.round(base * scale)));
        iconPx = Math.round(tilePx * 0.62f);

        FrameLayout.LayoutParams sl = (FrameLayout.LayoutParams) strip.getLayoutParams();
        sl.height = stripH;
        strip.setLayoutParams(sl);

        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(dp(40), avail + dp(8))));
        badge.setLayoutParams(new LinearLayout.LayoutParams(
                Math.max(dp(52), Math.round(Math.min(tilePx, dp(100)) * 0.8f)),
                ViewGroup.LayoutParams.MATCH_PARENT));
        nameLabel.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, labelH));

        railW = Math.max(dp(220), Math.min(dp(330), Math.round(w * 0.2f)));
        FrameLayout.LayoutParams rl = (FrameLayout.LayoutParams) railScroll.getLayoutParams();
        rl.width = railW;
        rl.setMargins(dp(16), dp(16), dp(16), stripH + dp(14));
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
            railItems.get(i).setTypeface(sel ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
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
        v.animate().scaleX(s).scaleY(s).setDuration(ms(140)).start();
        v.setTranslationZ(has ? dp(6) : 0f);
    }

    private View buildTile(final AppEntry a, boolean showStar) {
        FrameLayout f = new FrameLayout(this);
        f.setFocusable(true);
        f.setClickable(true);
        f.setLongClickable(true);
        boolean moving = moveMode && a.pkg.equals(movePkg);
        f.setBackground(moving ? moveBackground() : focusBackground());

        ImageView iv = new ImageView(this);
        iv.setImageDrawable(a.icon);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        f.addView(iv, new FrameLayout.LayoutParams(iconPx, iconPx, Gravity.CENTER));

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
        f.setLayoutParams(new LinearLayout.LayoutParams(tilePx, tilePx));
        return f;
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
        f.setLayoutParams(new LinearLayout.LayoutParams(tilePx, tilePx));
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

    private AlertDialog.Builder dialog(String title) {
        // Arabic UI: every dialog is laid out right-to-left and styled like the launcher
        return new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert) {
            @Override public AlertDialog create() {
                AlertDialog d = super.create();
                styleDialog(d);
                return d;
            }
        }.setTitle(title);
    }

    private void styleDialog(final AlertDialog d) {
        final Window w = d.getWindow();
        if (w == null) return;
        View dv = w.getDecorView();
        dv.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        dv.setTextDirection(View.TEXT_DIRECTION_RTL);
        int pct = Math.max(30, Math.min(100, prefs.getInt("ui_alpha", 92)));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(argb(pct * 255 / 100, 0x0B, 0x1E, 0x17));
        bg.setCornerRadius(dp(22));
        bg.setStroke(dp(1), argb(pct * 130 / 100, 0xF4, 0xC9, 0x5D));
        w.setBackgroundDrawable(bg);
        if (pct < 90) w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        else w.setDimAmount(0.45f);
        d.setOnShowListener(x -> {
            applySounds(w.getDecorView());
            WindowManager.LayoutParams lp = w.getAttributes();
            int sw = sizeW > 0 ? sizeW : getResources().getDisplayMetrics().widthPixels;
            int side = prefs.getInt("ui_side", 0);        // 0 left, 1 centre, 2 right
            lp.width = Math.min(sw - dp(40), Math.max(dp(360), Math.round(sw * 0.40f)));
            lp.gravity = (side == 0 ? Gravity.LEFT : side == 2 ? Gravity.RIGHT
                    : Gravity.CENTER_HORIZONTAL) | Gravity.CENTER_VERTICAL;
            lp.x = side == 1 ? 0 : dp(28);
            w.setAttributes(lp);
        });
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
            t.setTypeface(pos == sel ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            return t;
        }
    }

    private TextView newMenuRow() {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, dp(17));
        t.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_selected}, {}},
                new int[]{0xFFF4C95D, 0xFFFFFFFF}));
        t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        t.setPadding(dp(18), dp(11), dp(18), dp(11));
        t.setMinHeight(dp(50));
        GradientDrawable on = new GradientDrawable();
        on.setColor(0x33F4C95D);
        on.setStroke(dp(2), 0xFFF4C95D);
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
    private void listDialog(final String title, final String[] labels, int initial,
                            final Pick pick, final Runnable back) {
        final MenuAdapter ad = new MenuAdapter(labels, initial);
        AlertDialog.Builder b = dialog(title).setAdapter(ad, (d, which) -> {
            lastPos.put(title, which);
            pick.on(which);
        });
        if (back != null) b.setNegativeButton("رجوع", (d, w) -> back.run());
        else b.setNegativeButton("إغلاق", null);
        final AlertDialog d = b.create();
        d.show();
        final ListView lv = d.getListView();
        lv.setSelector(new ColorDrawable(Color.TRANSPARENT));
        lv.setDivider(new ColorDrawable(Color.TRANSPARENT));
        lv.setDividerHeight(dp(3));
        lv.setPadding(dp(8), dp(4), dp(8), dp(4));
        lv.setClipToPadding(false);
        lv.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                lastPos.put(title, pos);
                if (ad.sel != pos) {
                    ad.sel = pos;
                    ad.notifyDataSetChanged();
                }
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
        tv.setTextColor(0xFFF4C95D);
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
        final AlertDialog d = dialog(title).setView(box)
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
    private AlertDialog showInfo(String title, String rtl, String ltr, String posLabel,
                                 final Runnable pos, String neuLabel, final Runnable neu,
                                 String negLabel, final Runnable negAct) {
        ScrollView sv = new ScrollView(this);
        sv.setFocusable(true);                        // so the D-pad can scroll long text
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(8), dp(22), dp(8));
        if (rtl != null && !rtl.isEmpty()) {
            TextView a = new TextView(this);
            a.setTextSize(TypedValue.COMPLEX_UNIT_PX, dp(16));
            a.setTextColor(Color.WHITE);
            a.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            a.setTextDirection(View.TEXT_DIRECTION_RTL);
            a.setGravity(Gravity.RIGHT);
            a.setLineSpacing(0f, 1.15f);
            a.setText(rtl);
            box.addView(a);
        }
        if (ltr != null && !ltr.isEmpty()) {
            TextView b = new TextView(this);
            b.setTextSize(TypedValue.COMPLEX_UNIT_PX, dp(12));
            b.setTextColor(0xFFCFE3DA);
            b.setTypeface(Typeface.MONOSPACE);
            b.setTextDirection(View.TEXT_DIRECTION_LTR);
            b.setGravity(Gravity.LEFT);
            b.setText(ltr);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(10);
            box.addView(b, lp);
        }
        sv.addView(box);
        AlertDialog.Builder b = dialog(title).setView(sv);
        b.setPositiveButton(posLabel == null ? "حسناً" : posLabel, (d, w) -> {
            if (pos != null) pos.run();
        });
        if (neuLabel != null) b.setNeutralButton(neuLabel, (d, w) -> {
            if (neu != null) neu.run();
        });
        if (negLabel != null) b.setNegativeButton(negLabel, (d, w) -> {
            if (negAct != null) negAct.run();
        });
        AlertDialog d = b.create();
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
        l.add("🧩  تحريك عناصر صفحة المواقيت");
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
        l.add("🔌  اتصال ADB الذاتي");
        a.add(this::showAdbDialog);
        l.add("📋  سجل الأخطاء ومشاركته");
        a.add(this::showLogDialog);
        l.add("↩  إعادة ضبط اللانشر");
        a.add(this::showReset);
        l.add("ℹ  حول التطبيق — الإصدار " + Updater.currentName(this));
        a.add(this::showAbout);
        menu("إعدادات Mawaqit Home", l, a, null);
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

    private void showAbout() {
        showInfo("Mawaqit Home",
                "الإصدار: " + Updater.currentName(this) + "  (build " + Updater.currentCode(this) + ")\n"
                        + "© Moh-F — جميع الحقوق محفوظة\n\n"
                        + "بيانات الطقس: Open-Meteo.com (CC BY 4.0)\n"
                        + "أوقات الصلاة: Mawaqit / Aladhan\n"
                        + "اتصال ADB: مكتبة dadb (Apache-2.0)",
                "الصفحة:\n" + url + "\n\nالسجل:\n"
                        + (AppLog.file() != null ? AppLog.file().getAbsolutePath() : "—"),
                "حسناً", null, null, null, null, null);
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
        dialog(title).setView(et)
                .setPositiveButton("حفظ", (d, w) -> onOk.on(et.getText().toString().trim()))
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
        l.add("🔲  حجم الأيقونات:  " + prefs.getInt("tile_pct", 100) + "%");
        a.add(() -> numberDialog("حجم الأيقونات", "tile_pct", 100, 40, 150, "%", null, null,
                this::recreate, this::showStripSettings));
        l.add("📐  ارتفاع منطقة الصفحة (يحدد ارتفاع الشريط):  " + prefs.getInt("top_pct", 82) + "%");
        a.add(() -> numberDialog("ارتفاع منطقة الصفحة = 100% − ارتفاع الشريط (قلّله مع صفين أو ثلاثة)",
                "top_pct", 82, 55, 95, "%", null, null, this::recreate, this::showStripSettings));
        l.add("🪟  موضع الشريط:  " + (prefs.getBoolean("strip_over", false) ? "فوق صفحة المواقيت (يغطي أسفلها)"
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
        l.add("🧱  شكل العرض:  " + (prefs.getInt("w_layout", 0) == 1 ? "أعمدة (أفقي)" : "صفوف (عمودي)"));
        a.add(() -> choice("شكل عرض الأيام", new String[]{"صفوف: كل يوم في سطر", "أعمدة: الأيام الثلاثة جنب بعض"},
                prefs.getInt("w_layout", 0), i -> {
                    prefs.edit().putInt("w_layout", i).apply();
                    layoutWeather();
                    showWeatherSettings();
                }, back));
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
                                    .setMessage(ok ? output : output
                                            + "\n\nتأكد من تفعيل ADB debugging ثم أعد المحاولة.")
                                    .setPositiveButton("حسناً", null).show();
                        }
                    });
                }).setNegativeButton("إغلاق", null).show();
    }

    private void showLogDialog() {
        File f = AppLog.file();
        showInfo("سجل الأخطاء", "آخر الأحداث (للقراءة بالكامل استخدم «مشاركة عبر الشبكة»):",
                (f != null ? f.getAbsolutePath() + "\\n\\n" : "") + AppLog.tail(40),
                "مشاركة عبر الشبكة", this::startLogServer, "فحص بنية الصفحة", this::inspectPage,
                "مسح السجل", () -> {
                    AppLog.clear();
                    toast("تم المسح");
                });
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
        String js = "(function(){window.__mhcss=" + JSONObject.quote(css.toString()) + ";"
                + "function ap(){var s=document.getElementById('mh-style');"
                + "if(!s){s=document.createElement('style');s.id='mh-style';(document.head||document.body).appendChild(s);}"
                + "if(s.textContent!==window.__mhcss)s.textContent=window.__mhcss;}"
                + "ap();if(!window.__mhiv)window.__mhiv=setInterval(ap,3000);})()";
        webView.evaluateJavascript(js, null);
    }

    private void showPageElements() {
        final List<String> l = new ArrayList<>();
        final List<Runnable> a = new ArrayList<>();
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
            + "for(var i=0;i<all.length&&n<260;i++){var e=all[i];if(!vis(e))continue;"
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

    private String reportText() {
        return "Mawaqit Home " + Updater.currentName(this) + " (" + Updater.currentCode(this) + ")\n"
                + "Android " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT
                + ")  " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + "\n"
                + "Screen " + sizeW + "x" + sizeH + "  density " + getResources().getDisplayMetrics().density + "\n\n"
                + statusText() + "\n\n==== PAGE DOM ====\n" + domDump + "\n\n==== LOG ====\n" + AppLog.all();
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
                            byte[] body = (ok ? reportText() : "not found").getBytes("UTF-8");
                            OutputStream o = c.getOutputStream();
                            o.write(("HTTP/1.1 " + (ok ? "200 OK" : "404 Not Found")
                                    + "\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: "
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

    private void backupSettings() {
        try {
            JSONObject o = new JSONObject();
            for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
                Object v = e.getValue();
                if (skipKey(e.getKey())) continue;
                if (v instanceof String || v instanceof Integer || v instanceof Long
                        || v instanceof Boolean) {
                    o.put(e.getKey(), v);
                }
            }
            File f = backupFile();
            FileOutputStream out = new FileOutputStream(f);
            out.write(o.toString(2).getBytes("UTF-8"));
            out.close();
            dialog("تم النسخ الاحتياطي").setMessage("المفضلة والتصنيفات والترتيب والإعدادات حُفظت في:\n\n"
                    + f.getAbsolutePath()
                    + "\n\nلنقلها لجهاز آخر:\nadb pull " + f.getAbsolutePath())
                    .setPositiveButton("حسناً", null).show();
        } catch (Exception e) {
            AppLog.error("BACKUP", "backup failed", e);
            toast("فشل النسخ الاحتياطي");
        }
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
            JSONObject o = new JSONObject(s);
            SharedPreferences.Editor ed = prefs.edit();
            Iterator<String> it = o.keys();
            while (it.hasNext()) {
                String k = it.next();
                if (skipKey(k)) continue;
                Object v = o.get(k);
                if (v instanceof String) ed.putString(k, (String) v);
                else if (v instanceof Boolean) ed.putBoolean(k, (Boolean) v);
                else if (v instanceof Integer) ed.putInt(k, (Integer) v);
                else if (v instanceof Long) ed.putLong(k, (Long) v);
            }
            ed.apply();
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
                    ri.loadLabel(pm).toString(), ri.loadIcon(pm), isTv.get(e.getKey()));
            entries.add(a);
            byPkg.put(a.pkg, a);
        }
        sortEntries();
        return true;
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

        strip.animate().translationY(ui ? 0f : stripH + dp(6)).setDuration(dur)
                .setInterpolator(ip).start();
        if (prefs.getBoolean("w_show", true)) {
            weatherCard.animate().alpha(ui ? 1f : 0f).setDuration(animate ? ms(600L) : 0L).start();
        }

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

    private void resizeWebFill(final boolean full, boolean animate) {
        final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) webView.getLayoutParams();
        final int target = full ? ViewGroup.LayoutParams.MATCH_PARENT : topH;
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
