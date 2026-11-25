package com.moh.mawaqithome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Fires at the scheduled Quran time: brings the launcher to the front, which then starts reading. */
public class QuranReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        AppLog.init(ctx);
        long id = intent.getLongExtra("id", -1L);
        AppLog.info("QURAN", "alarm fired for item " + id);
        try {
            Intent i = new Intent(ctx, MainActivity.class);
            i.putExtra("quran", id);
            i.putExtra("quran_at", System.currentTimeMillis());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            ctx.startActivity(i);
        } catch (Exception e) {
            AppLog.error("QURAN", "start failed", e);
        } finally {
            QuranSched.schedule(ctx);
        }
    }
}
