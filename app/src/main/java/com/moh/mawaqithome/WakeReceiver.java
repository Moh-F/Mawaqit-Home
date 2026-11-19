package com.moh.mawaqithome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

/**
 * Fires shortly before each adhan. If the device is asleep (screen off) it brings the
 * launcher up so the prayer page is visible. If someone is already watching something,
 * it does nothing (it never interrupts a movie), and just schedules the next alarm.
 */
public class WakeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        AppLog.init(ctx);
        TimeCfg.apply(ctx);
        AppLog.info("WAKE", "alarm fired");
        try {
            PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isInteractive()) {
                Intent i = new Intent(ctx, MainActivity.class);
                i.putExtra("wake", true);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                ctx.startActivity(i);
            }
        } finally {
            WakeScheduler.scheduleNext(ctx);
        }
    }
}
