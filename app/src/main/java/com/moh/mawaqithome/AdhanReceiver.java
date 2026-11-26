package com.moh.mawaqithome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

/**
 * Fires a few seconds before the prayer minute: wakes the screen, pauses what is playing, and (a little later) brings
 * the launcher to the front, which announces and then plays the adhan at the exact minute.
 */
public class AdhanReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context ctx, Intent intent) {
        AppLog.init(ctx);
        final int idx = intent.getIntExtra("idx", -1);
        final boolean test = intent.getBooleanExtra("test", false);
        SharedPreferences p = WakeScheduler.prefs(ctx);
        int pause = Math.max(0, Math.min(9, p.getInt("adhan_pause_secs", 6)));
        int ann = Math.max(0, Math.min(10, p.getInt("adhan_pre_secs", 3)));
        long now = System.currentTimeMillis();
        long target = intent.getLongExtra("target", 0L);
        if (target == 0L) target = now + WakeScheduler.leadMs(p);
        final long fTarget = target;
        AppLog.info("ADHAN", "alarm fired for prayer " + idx + ", adhan in " + (target - now) + " ms");
        WakeScheduler.scheduleAdhan(ctx);                              // arm the next one right away

        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        boolean off = pm != null && !pm.isInteractive();
        WakeUtil.wakeScreen(ctx, 3 * 60_000L);
        if (off) WakeUtil.adbWake(ctx);
        if (pause > 0 || test) WakeUtil.pauseMedia(ctx);

        final BroadcastReceiver.PendingResult pr = goAsync();
        Runnable go = new Runnable() {
            @Override public void run() {
                try {
                    Intent i = new Intent(ctx, MainActivity.class);
                    i.putExtra("adhan", idx);
                    i.putExtra("adhan_at", System.currentTimeMillis());
                    i.putExtra("adhan_test", test);
                    i.putExtra("adhan_target", fTarget);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                            | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    WakeUtil.bringToFront(ctx, i, "حان وقت الأذان");
                } catch (Exception e) {
                    AppLog.error("ADHAN", "start failed", e);
                } finally {
                    if (pr != null) pr.finish();
                }
            }
        };
        long delay = Math.max(0L, Math.min(9000L, target - ann * 1000L - now));   // media is already paused: wait, then show
        if (delay > 0) new Handler(Looper.getMainLooper()).postDelayed(go, delay);
        else go.run();
    }
}
