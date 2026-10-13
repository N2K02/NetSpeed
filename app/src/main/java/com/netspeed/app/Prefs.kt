package com.netspeed.app

import android.content.Context
import android.content.SharedPreferences

/** All user settings, stored in SharedPreferences. */
object Prefs {
    private const val FILE = "netspeed"

    private fun sp(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Whether the speed meter should be running. On by default once permission is granted. */
    fun enabled(ctx: Context) = sp(ctx).getBoolean("enabled", true)
    fun setEnabled(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("enabled", v).apply()

    /** Refresh interval in seconds: 1 (default), 2 or 3. */
    fun intervalSec(ctx: Context) = sp(ctx).getInt("interval", 1).coerceIn(1, 3)
    fun setIntervalSec(ctx: Context, v: Int) = sp(ctx).edit().putInt("interval", v.coerceIn(1, 3)).apply()

    /** Text size of the status bar meter in sp. */
    fun textSize(ctx: Context) = sp(ctx).getInt("sbTextSize", 11)
    fun setTextSize(ctx: Context, v: Int) = sp(ctx).edit().putInt("sbTextSize", v).apply()

    /** Text colour: true = white (dark status bar), false = black (light status bar). */
    fun whiteText(ctx: Context) = sp(ctx).getBoolean("whiteText", true)
    fun setWhiteText(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("whiteText", v).apply()

    /** Horizontal position in the status bar, 0 = far left … 1000 = far right. */
    fun position(ctx: Context) = sp(ctx).getInt("sbPos", H_DEFAULT).coerceIn(0, 1000)
    fun setPosition(ctx: Context, v: Int) = sp(ctx).edit().putInt("sbPos", v.coerceIn(0, 1000)).apply()

    /**
     * Vertical offset from the top of the screen, in dp (-30 … 120).
     * Default: just below the status bar (its height on this phone).
     */
    fun verticalDp(ctx: Context): Int {
        val s = sp(ctx)
        val v = if (s.contains("sbY")) s.getInt("sbY", 0) else statusBarHeightDp(ctx)
        return v.coerceIn(V_MIN, V_MAX)
    }
    fun setVerticalDp(ctx: Context, v: Int) = sp(ctx).edit().putInt("sbY", v.coerceIn(V_MIN, V_MAX)).apply()

    /** Default: right edge 3% in from the right side of the screen (top right). */
    const val H_DEFAULT = 970
    const val V_MIN = -30
    const val V_MAX = 120

    fun resetPosition(ctx: Context) =
        sp(ctx).edit().putInt("sbPos", H_DEFAULT).remove("sbY").apply()

    private fun statusBarHeightDp(ctx: Context): Int {
        val res = ctx.resources
        val id = res.getIdentifier("status_bar_height", "dimen", "android")
        val px = if (id > 0) res.getDimensionPixelSize(id) else 0
        return if (px > 0) (px / res.displayMetrics.density + 0.5f).toInt() else 24
    }
}
