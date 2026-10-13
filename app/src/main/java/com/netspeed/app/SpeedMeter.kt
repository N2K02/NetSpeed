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
 * time that passed. Speeds are in bits per second.
 */
class SpeedMeter(context: Context) {
    private val cm = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var lastRx = -1L
    private var lastTx = -1L
    private var lastTime = 0L

    /** Download / upload in bits per second. */
    data class Reading(val downBps: Double, val upBps: Double)

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

    fun sample(): Reading {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val now = SystemClock.elapsedRealtime()

        if (!hasInternet() || rx == TrafficStats.UNSUPPORTED.toLong() ||
            tx == TrafficStats.UNSUPPORTED.toLong()
        ) {
            reset()
            return Reading(0.0, 0.0)
        }

        val first = lastRx < 0 || lastTx < 0
        val seconds = (now - lastTime) / 1000.0
        val dRx = rx - lastRx
        val dTx = tx - lastTx
        lastRx = rx
        lastTx = tx
        lastTime = now

        // First sample, or counters were reset (network switch): nothing to compare yet.
        if (first || seconds <= 0.0 || dRx < 0 || dTx < 0) return Reading(0.0, 0.0)

        return Reading(dRx * 8.0 / seconds, dTx * 8.0 / seconds)
    }

    companion object {
        /**
         * Whole number with Kb or Mb next to it (kilobits / megabits per second).
         * Below 1000 Kb it shows Kb, from there on Mb.
         */
        fun format(bitsPerSec: Double): String {
            val kb = (bitsPerSec / 1000.0).roundToLong()
            if (kb < 1000) return "$kb Kb"
            return "${(bitsPerSec / 1_000_000.0).roundToLong()} Mb"
        }
    }
}
