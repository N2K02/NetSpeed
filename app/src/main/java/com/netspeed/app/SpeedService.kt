package com.netspeed.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/** Foreground host for the enabled speed overlay; sticky restarts re-check the saved setting. */
class SpeedService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLog.init(applicationContext)
        if (!Prefs.enabled(this)) {
            AppLog.add("SERVICE", "METER_START_REJECTED")
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            promoteToForeground()
        } catch (error: Exception) {
            AppLog.add("SERVICE", "FOREGROUND_START_FAILED")
            SpeedOverlay.stop()
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Prefs.enabled(this) || !SpeedOverlay.start(this)) {
            AppLog.add("SERVICE", "METER_START_REJECTED")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        AppLog.add("SERVICE", "METER_RUNNING")
        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (!Prefs.enabled(this)) SpeedOverlay.stop()
        super.onDestroy()
    }

    private fun promoteToForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "NetSpeed", NotificationManager.IMPORTANCE_LOW))
        }
        val pending = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        val notification = builder.setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("NetSpeed is active")
            .setContentText("Showing live network speed")
            .setContentIntent(pending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "netspeed_service"
        private const val NOTIFICATION_ID = 4101

        fun start(context: Context): Boolean {
            val ctx = context.applicationContext
            AppLog.init(ctx)
            if (!SpeedOverlay.start(ctx)) {
                AppLog.add("SERVICE", "OVERLAY_PERMISSION_OR_START_FAILED")
                return false
            }
            val started = runCatching { ctx.startForegroundService(Intent(ctx, SpeedService::class.java)) }
                .onFailure { AppLog.add("SERVICE", "FOREGROUND_START_REJECTED") }.isSuccess
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
