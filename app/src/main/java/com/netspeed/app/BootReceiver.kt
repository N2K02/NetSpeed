package com.netspeed.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Brings the meter back after a reboot or an app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (Prefs.enabled(context)) SpeedService.start(context)
            }
        }
    }
}
