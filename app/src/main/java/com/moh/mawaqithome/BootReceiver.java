package com.moh.mawaqithome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Starts the launcher after boot (switchable in Settings) and right after the app itself is
 * updated, so a background update ends with the launcher running again.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        AppLog.init(ctx);
        TimeCfg.apply(ctx);
        String action = intent != null ? intent.getAction() : null;
        AppLog.info("BOOT", "received " + action);
        WakeScheduler.scheduleNext(ctx);
        boolean replaced = Intent.ACTION_MY_PACKAGE_REPLACED.equals(action);
        if (!replaced && !WakeScheduler.prefs(ctx).getBoolean("boot", true)) return;
        try {
            Intent i = new Intent(ctx, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (replaced) i.putExtra("postupdate", true);
            else i.putExtra("boot_guard", true);
            ctx.startActivity(i);
        } catch (Exception e) {
            AppLog.error("BOOT", "start failed", e);
        }
    }
}
