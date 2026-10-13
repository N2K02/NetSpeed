package com.netspeed.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.SystemClock
import kotlin.math.roundToLong

/**
 * Measures how much internet speed the whole phone is using right now.
 *
 * Every call to [sample] reads the phone's total bytes received / sent
 * (all apps, all networks) and divides the change since the last call by the
 * time that passed. Result is rounded to a whole number.
 */
class SpeedMeter(context: Context) {
    private val cm = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var lastRx = -1L
    private var lastTx = -1L
    private var lastTime = 0L

    data class Reading(val down: Long, val up: Long)

    /** Forget the previous sample, e.g. after the screen was off. */
    fun reset() {
        lastRx = -1L
        lastTx = -1L
    }

    fun hasInternet(): Boolean {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun sample(unit: Int): Reading {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val now = SystemClock.elapsedRealtime()

        if (!hasInternet() || rx == TrafficStats.UNSUPPORTED.toLong() ||
            tx == TrafficStats.UNSUPPORTED.toLong()
        ) {
            reset()
            return Reading(0, 0)
        }

        val first = lastRx < 0 || lastTx < 0
        val seconds = (now - lastTime) / 1000.0
        val dRx = rx - lastRx
        val dTx = tx - lastTx
        lastRx = rx
        lastTx = tx
        lastTime = now

        // First sample, or counters were reset (network switch): nothing to compare yet.
        if (first || seconds <= 0.0 || dRx < 0 || dTx < 0) return Reading(0, 0)

        return Reading(convert(dRx / seconds, unit), convert(dTx / seconds, unit))
    }

    private fun convert(bytesPerSec: Double, unit: Int): Long = when (unit) {
        Prefs.UNIT_KBS -> bytesPerSec / 1024.0
        Prefs.UNIT_MBS -> bytesPerSec / (1024.0 * 1024.0)
        else -> bytesPerSec * 8.0 / 1_000_000.0   // Mbps
    }.roundToLong()
}
