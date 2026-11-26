package com.moh.mawaqithome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Fires at the exact prayer minute: brings the launcher to the front, which then plays the adhan. */
public class AdhanReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        AppLog.init(ctx);
        int idx = intent.getIntExtra("idx", -1);
        AppLog.info("ADHAN", "alarm fired for prayer " + idx);
        WakeUtil.wakeScreen(ctx, 3 * 60_000L);
        try {
            Intent i = new Intent(ctx, MainActivity.class);
            i.putExtra("adhan", idx);
            i.putExtra("adhan_at", System.currentTimeMillis());
            i.putExtra("adhan_test", intent.getBooleanExtra("test", false));
            i.putExtra("adhan_target", intent.getLongExtra("target", 0L));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            WakeUtil.bringToFront(ctx, i, "حان وقت الأذان");
        } catch (Exception e) {
            AppLog.error("ADHAN", "start failed", e);
        } finally {
            WakeScheduler.scheduleAdhan(ctx);
        }
    }
}
