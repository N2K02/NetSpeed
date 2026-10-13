package com.netspeed.app

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

/**
 * The speed text drawn in the status bar row, e.g. "20 Mb / 5 Kb".
 *
 * Android doesn't let normal apps add text to the real status bar, so this is a
 * transparent overlay window lined up exactly with it. The status bar's own
 * icons and touches stay on top. A visible overlay also keeps the app alive in
 * the background on its own, so no foreground service / notification is needed.
 */
@SuppressLint("StaticFieldLeak") // only ever holds the application context
object SpeedOverlay {
    private val handler = Handler(Looper.getMainLooper())
    private var app: Context? = null
    private var wm: WindowManager? = null
    private var view: TextView? = null
    private var params: WindowManager.LayoutParams? = null
    private var meter: SpeedMeter? = null
    private var screenOn = true

    /** Latest reading, for the settings screen. */
    var lastDown: String = "0 Kb"
        private set
    var lastUp: String = "0 Kb"
        private set
    var listener: ((down: String, up: String) -> Unit)? = null

    val isRunning get() = view != null

    private val tick = object : Runnable {
        override fun run() {
            val ctx = app ?: return
            val m = meter ?: return
            val r = m.sample()
            show(SpeedMeter.format(r.downBps), SpeedMeter.format(r.upBps))
            placeIfScreenChanged()
            if (screenOn) handler.postDelayed(this, Prefs.intervalSec(ctx) * 1000L)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                // Pause while the screen is off to save battery.
                Intent.ACTION_SCREEN_OFF -> {
                    screenOn = false
                    handler.removeCallbacks(tick)
                }
                Intent.ACTION_SCREEN_ON -> {
                    screenOn = true
                    meter?.reset()
                    handler.removeCallbacks(tick)
                    handler.post(tick)
                }
            }
        }
    }

    /** Shows the meter. Returns false if "display over other apps" isn't allowed yet. */
    fun start(context: Context): Boolean {
        val ctx = context.applicationContext
        if (!Settings.canDrawOverlays(ctx)) return false
        if (view != null) return true

        app = ctx
        meter = SpeedMeter(ctx)
        val windowManager = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm = windowManager

        val tv = TextView(ctx).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            includeFontPadding = false
            maxLines = 1
            text = composed()
        }
        view = tv

        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            statusBarHeight(ctx),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Anchored by its RIGHT edge, so as the number grows it grows to the
            // left and never runs into the clock on its right.
            gravity = Gravity.TOP or Gravity.END
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        params = p
        applyStyle()

        try {
            windowManager.addView(tv, p)
        } catch (e: Exception) {
            view = null; params = null
            return false
        }

        val f = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ctx.registerReceiver(screenReceiver, f)
        screenOn = (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive

        handler.removeCallbacks(tick)
        handler.post(tick)
        return true
    }

    fun stop() {
        handler.removeCallbacks(tick)
        val ctx = app
        if (ctx != null) runCatching { ctx.unregisterReceiver(screenReceiver) }
        view?.let { v -> runCatching { wm?.removeView(v) } }
        view = null
        params = null
        meter = null
    }

    /** Re-applies position / size / colour after a settings change, and refreshes now. */
    fun applySettings() {
        applyStyle()
        val v = view ?: return
        val p = params ?: return
        runCatching { wm?.updateViewLayout(v, p) }
        if (screenOn) {
            handler.removeCallbacks(tick)
            handler.post(tick)
        }
    }

    private var placedForWidth = 0

    private fun applyStyle() {
        val ctx = app ?: return
        val v = view ?: return
        val p = params ?: return
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, Prefs.textSize(ctx).toFloat())
        val white = Prefs.whiteText(ctx)
        v.setTextColor(if (white) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
        // Faint outline so it stays readable on any background.
        v.setShadowLayer(2f, 0f, 0f, if (white) 0x99000000.toInt() else 0x99FFFFFF.toInt())
        p.height = statusBarHeight(ctx)
        val w = ctx.resources.displayMetrics.widthPixels
        // Slider value = where the meter's right edge sits, from left (0) to right (1000).
        p.x = (w * (1000 - Prefs.position(ctx)) / 1000f).toInt()
        p.y = (Prefs.verticalDp(ctx) * ctx.resources.displayMetrics.density).toInt()
        placedForWidth = w
    }

    /** Keeps the position right after the screen rotates. */
    private fun placeIfScreenChanged() {
        val ctx = app ?: return
        if (ctx.resources.displayMetrics.widthPixels == placedForWidth) return
        applyStyle()
        val v = view ?: return
        val p = params ?: return
        runCatching { wm?.updateViewLayout(v, p) }
    }

    private fun composed() = "$lastDown / $lastUp"

    private fun show(down: String, up: String) {
        lastDown = down
        lastUp = up
        view?.text = composed()
        listener?.invoke(down, up)
    }

    private fun statusBarHeight(ctx: Context): Int {
        val id = ctx.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) ctx.resources.getDimensionPixelSize(id)
        else (24 * ctx.resources.displayMetrics.density).toInt()
    }
}
