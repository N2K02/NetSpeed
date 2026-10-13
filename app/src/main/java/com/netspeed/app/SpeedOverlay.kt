package com.netspeed.app

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.abs

/**
 * The small floating "20 D/ 5 U" label shown on top of every app.
 *
 * A visible overlay window keeps the app alive in the background on its own,
 * so no foreground service and no notification are needed.
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

    /** Latest text shown, for the settings screen preview. */
    var lastText: String = "0 D/ 0 U"
        private set
    var listener: ((String) -> Unit)? = null

    val isRunning get() = view != null

    private val tick = object : Runnable {
        override fun run() {
            val ctx = app ?: return
            val m = meter ?: return
            val r = m.sample(Prefs.unit(ctx))
            show("${r.down} D/ ${r.up} U")
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
            setTextColor(0xFFFFFFFF.toInt())
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            val padH = dp(ctx, 7); val padV = dp(ctx, 3)
            setPadding(padH, padV, padH, padV)
            background = GradientDrawable().apply {
                setColor(0xB3000000.toInt())
                cornerRadius = dp(ctx, 8).toFloat()
            }
            text = lastText
        }
        view = tv

        val dm = ctx.resources.displayMetrics
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            baseFlags(),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = Prefs.posX(ctx).takeIf { it >= 0 } ?: (dm.widthPixels / 2 - dp(ctx, 45))
            y = Prefs.posY(ctx).takeIf { it >= 0 } ?: dp(ctx, 30)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        params = p
        applyStyle()
        attachDrag(tv)

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

    /** Re-applies text size / lock after a settings change, and refreshes the reading now. */
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

    private fun applyStyle() {
        val ctx = app ?: return
        val v = view ?: return
        val p = params ?: return
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, Prefs.textSize(ctx).toFloat())
        if (Prefs.locked(ctx)) {
            // Touches go straight through to the app underneath. Android 12+ only
            // lets touches pass an overlay whose opacity is at most 0.8.
            p.flags = baseFlags() or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            p.alpha = 0.8f
        } else {
            p.flags = baseFlags()
            p.alpha = 1f
        }
    }

    private fun baseFlags() =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

    private fun show(text: String) {
        lastText = text
        view?.text = text
        listener?.invoke(text)
    }

    /** Drag to move; a tap opens the settings screen. */
    @SuppressLint("ClickableViewAccessibility")
    private fun attachDrag(tv: TextView) {
        val slop = ViewConfiguration.get(tv.context).scaledTouchSlop
        var downX = 0f; var downY = 0f
        var startX = 0; var startY = 0
        var dragging = false
        tv.setOnTouchListener { _, e ->
            val p = params ?: return@setOnTouchListener false
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = p.x; startY = p.y
                    dragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX; val dy = e.rawY - downY
                    if (!dragging && (abs(dx) > slop || abs(dy) > slop)) dragging = true
                    if (dragging) {
                        p.x = (startX + dx).toInt().coerceAtLeast(0)
                        p.y = (startY + dy).toInt().coerceAtLeast(0)
                        runCatching { wm?.updateViewLayout(tv, p) }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val ctx = app
                    if (dragging) {
                        if (ctx != null) Prefs.setPos(ctx, p.x, p.y)
                    } else if (ctx != null) {
                        ctx.startActivity(
                            Intent(ctx, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            }
            true
        }
    }

    private fun dp(ctx: Context, v: Int) =
        (v * ctx.resources.displayMetrics.density + 0.5f).toInt()
}
