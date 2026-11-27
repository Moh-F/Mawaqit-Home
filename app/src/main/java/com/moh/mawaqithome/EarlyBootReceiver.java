package com.moh.mawaqithome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.UserManager;

/**
 * LOCKED_BOOT_COMPLETED reaches the few direct-boot-aware apps long before the normal BOOT_COMPLETED queue, so the
 * launcher can come up seconds after the system home instead of half a minute later.
 */
public class EarlyBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                UserManager um = (UserManager) ctx.getSystemService(Context.USER_SERVICE);
                if (um != null && !um.isUserUnlocked()) return;           // the normal boot receiver will do it
            }
            AppLog.init(ctx);
            if (!WakeScheduler.prefs(ctx).getBoolean("boot", true)) return;
            AppLog.info("BOOT", "early boot start");
            Intent i = new Intent(ctx, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            i.putExtra("boot_guard", true);
            ctx.startActivity(i);
        } catch (Exception ignored) {
        }
    }
}
