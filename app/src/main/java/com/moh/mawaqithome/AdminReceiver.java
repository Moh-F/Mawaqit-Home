package com.moh.mawaqithome;

import android.app.admin.DeviceAdminReceiver;

/**
 * Only used for the optional "real sleep" mode (lockNow). Needs to be activated once with:
 * adb shell dpm set-active-admin com.moh.mawaqithome/.AdminReceiver
 */
public class AdminReceiver extends DeviceAdminReceiver {
}
