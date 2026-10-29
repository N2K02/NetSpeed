package com.netspeed.app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

/**
 * Plain background service (NOT a foreground service, so no notification).
 * It only keeps the meter going and lets Android restart it if the app is killed.
 */
class SpeedService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLog.init(applicationContext)
        if (!Prefs.enabled(this) || !SpeedOverlay.start(this)) {
            AppLog.add("SERVICE", "METER_START_REJECTED")
            stopSelf()
            return START_NOT_STICKY
        }
        AppLog.add("SERVICE", "METER_RUNNING")
        return START_STICKY
    }

    override fun onDestroy() {
        // Removed only when the user turns the meter off.
        if (!Prefs.enabled(this)) SpeedOverlay.stop()
        super.onDestroy()
    }

    companion object {
        /** Starts the meter. Returns false when overlay permission is missing. */
        fun start(context: Context): Boolean {
            val ctx = context.applicationContext
            // Show the overlay first: a visible overlay counts as "in use", which
            // is what allows a background service to be started at all.
            AppLog.init(ctx)
            if (!SpeedOverlay.start(ctx)) {
                AppLog.add("SERVICE", "OVERLAY_PERMISSION_OR_START_FAILED")
                return false
            }
            val started = runCatching { ctx.startService(Intent(ctx, SpeedService::class.java)) }
                .onFailure { AppLog.add("SERVICE", "BACKGROUND_START_REJECTED") }.isSuccess
            if (!started) SpeedOverlay.stop()
            return started
        }

        fun stop(context: Context) {
            val ctx = context.applicationContext
            AppLog.add("SERVICE", "METER_STOP_REQUESTED")
            SpeedOverlay.stop()
            runCatching { ctx.stopService(Intent(ctx, SpeedService::class.java)) }
                .onFailure { AppLog.add("SERVICE", "METER_STOP_FAILED") }
        }
    }
}
