package com.netspeed.app

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager

/** A disabled meter never gets relaunched; charging overrides an active battery saver. */
internal object RecoveryPolicy {
    fun shouldRestore(enabled: Boolean, charging: Boolean, powerSaveMode: Boolean): Boolean =
        enabled && (charging || !powerSaveMode)
}

/**
 * Recovers after an OEM's extreme battery mode kills the process.
 * An OS-persisted periodic job is an eventual fallback, not an exact alarm:
 * Android may defer it during device restrictions or after a force-stop.
 */
internal object PowerRecovery {
    private const val JOB_ID = 4102
    private const val PERIOD_MS = 15 * 60 * 1000L

    fun shouldRestore(context: Context): Boolean {
        val battery = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return RecoveryPolicy.shouldRestore(Prefs.enabled(context), battery.isCharging, power.isPowerSaveMode)
    }

    fun schedule(context: Context) {
        if (!Prefs.enabled(context)) return
        val app = context.applicationContext
        val scheduler = app.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        if (scheduler.getPendingJob(JOB_ID) != null) return
        val job = JobInfo.Builder(JOB_ID, ComponentName(app, PowerRecoveryJobService::class.java))
            .setPersisted(true)
            .setPeriodic(PERIOD_MS)
            .build()
        runCatching { scheduler.schedule(job) }
            .onFailure { AppLog.add("RECOVERY", "JOB_SCHEDULE_FAILED") }
    }

    fun cancel(context: Context) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        runCatching { scheduler.cancel(JOB_ID) }
    }

    fun restore(context: Context): Boolean {
        if (!shouldRestore(context)) return false
        if (SpeedOverlay.isRunning) {
            SpeedOverlay.applySettings()
            return true
        }
        return SpeedService.start(context)
    }
}

class PowerRecoveryJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        // The job executes briefly on the main thread; no background work is retained.
        if (PowerRecovery.shouldRestore(this)) {
            if (PowerRecovery.restore(this)) AppLog.add("RECOVERY", "METER_RESTORED")
            else AppLog.add("RECOVERY", "METER_RESTORE_DEFERRED")
        }
        return false
    }

    override fun onStopJob(params: JobParameters): Boolean = false
}
