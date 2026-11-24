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
        try {
            Intent i = new Intent(ctx, MainActivity.class);
            i.putExtra("adhan", idx);
            i.putExtra("adhan_at", System.currentTimeMillis());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            ctx.startActivity(i);
        } catch (Exception e) {
            AppLog.error("ADHAN", "start failed", e);
        } finally {
            WakeScheduler.scheduleAdhan(ctx);
        }
    }
}
