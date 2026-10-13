package com.netspeed.app

import android.content.Context
import android.content.SharedPreferences

/** All user settings, stored in SharedPreferences. */
object Prefs {
    private const val FILE = "netspeed"

    const val UNIT_MBPS = 0   // megabits per second (how internet speed is usually quoted)
    const val UNIT_KBS = 1    // kilobytes per second
    const val UNIT_MBS = 2    // megabytes per second

    private fun sp(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Whether the speed meter should be running. On by default once permission is granted. */
    fun enabled(ctx: Context) = sp(ctx).getBoolean("enabled", true)
    fun setEnabled(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("enabled", v).apply()

    /** Refresh interval in seconds: 1 (default), 2 or 3. */
    fun intervalSec(ctx: Context) = sp(ctx).getInt("interval", 1).coerceIn(1, 3)
    fun setIntervalSec(ctx: Context, v: Int) = sp(ctx).edit().putInt("interval", v.coerceIn(1, 3)).apply()

    fun unit(ctx: Context) = sp(ctx).getInt("unit", UNIT_MBPS)
    fun setUnit(ctx: Context, v: Int) = sp(ctx).edit().putInt("unit", v).apply()

    /** Text size of the floating meter in sp. */
    fun textSize(ctx: Context) = sp(ctx).getInt("textSize", 13)
    fun setTextSize(ctx: Context, v: Int) = sp(ctx).edit().putInt("textSize", v).apply()

    /** When locked, the meter can't be dragged and touches pass through it. */
    fun locked(ctx: Context) = sp(ctx).getBoolean("locked", false)
    fun setLocked(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("locked", v).apply()

    fun posX(ctx: Context) = sp(ctx).getInt("x", -1)
    fun posY(ctx: Context) = sp(ctx).getInt("y", -1)
    fun setPos(ctx: Context, x: Int, y: Int) = sp(ctx).edit().putInt("x", x).putInt("y", y).apply()

    fun unitLabel(unit: Int) = when (unit) {
        UNIT_KBS -> "KB/s"
        UNIT_MBS -> "MB/s"
        else -> "Mbps"
    }
}
