package com.netspeed.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

/** Settings screen. The meter itself runs on its own once started. */
class MainActivity : Activity() {

    private lateinit var downText: TextView
    private lateinit var upText: TextView
    private lateinit var status: TextView
    private lateinit var permButton: Button
    private lateinit var batteryButton: Button
    private lateinit var enableSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "NetSpeed"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(20)
            setPadding(pad, pad, pad, pad)
        }

        downText = speedRow(root, "Download")
        upText = speedRow(root, "Upload")

        status = TextView(this).apply { setPadding(0, dp(16), 0, dp(4)) }
        root.addView(status)

        permButton = Button(this).apply {
            text = "Allow display over other apps"
            setOnClickListener {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }
        root.addView(permButton)

        enableSwitch = Switch(this).apply {
            text = "Show speed in status bar"
            textSize = 17f
            setPadding(0, dp(12), 0, dp(12))
            isChecked = Prefs.enabled(context)
            setOnCheckedChangeListener { _, on ->
                Prefs.setEnabled(context, on)
                if (on) SpeedService.start(context) else SpeedService.stop(context)
                refresh()
            }
        }
        root.addView(enableSwitch)

        root.addView(header("Position in status bar"))
        root.addView(TextView(this).apply {
            alpha = 0.7f
            text = "Slide until the speed sits just left of your clock."
        })
        root.addView(SeekBar(this).apply {
            min = 50
            max = 1000
            progress = Prefs.position(context)
            setPadding(dp(8), dp(12), dp(8), dp(12))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, v: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    Prefs.setPosition(context, v)
                    SpeedOverlay.applySettings()
                }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) {}
            })
        })

        root.addView(header("Refresh every"))
        root.addView(radioRow(
            listOf("1 sec" to 1, "2 sec" to 2, "3 sec" to 3),
            Prefs.intervalSec(this)
        ) { Prefs.setIntervalSec(this, it) })

        root.addView(header("Text size"))
        root.addView(radioRow(
            listOf("Small" to 10, "Medium" to 11, "Large" to 13),
            Prefs.textSize(this)
        ) { Prefs.setTextSize(this, it) })

        root.addView(header("Text colour"))
        root.addView(radioRow(
            listOf("White" to 1, "Black" to 0),
            if (Prefs.whiteText(this)) 1 else 0
        ) { Prefs.setWhiteText(this, it == 1) })

        batteryButton = Button(this).apply {
            text = "Let it run in the background (battery)"
            setOnClickListener {
                runCatching {
                    startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
            }
        }
        root.addView(batteryButton)

        root.addView(TextView(this).apply {
            alpha = 0.7f
            setPadding(0, dp(16), 0, 0)
            text = "Status bar shows download / upload for the whole phone, e.g. " +
                "\"20 Mb / 5 Kb\". Kb = kilobits, Mb = megabits per second, rounded to " +
                "a whole number. Shows 0 Kb / 0 Kb when there is no internet.\n\n" +
                "No notification is used. If it stops after a while, set NetSpeed's " +
                "battery usage to \"Unrestricted\" in Android settings (needed on " +
                "Samsung, Xiaomi, Huawei and some others)."
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        // Coming back from the permission screen: start automatically if allowed.
        if (Prefs.enabled(this) && Settings.canDrawOverlays(this)) SpeedService.start(this)
        SpeedOverlay.listener = { d, u -> runOnUiThread { downText.text = d; upText.text = u } }
        refresh()
    }

    override fun onPause() {
        SpeedOverlay.listener = null
        super.onPause()
    }

    private fun refresh() {
        val allowed = Settings.canDrawOverlays(this)
        permButton.visibility = if (allowed) View.GONE else View.VISIBLE
        enableSwitch.isEnabled = allowed
        status.text = when {
            !allowed -> "Step 1: allow NetSpeed to display over other apps."
            SpeedOverlay.isRunning -> "Running in the background."
            else -> "Stopped."
        }
        downText.text = SpeedOverlay.lastDown
        upText.text = SpeedOverlay.lastUp
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        batteryButton.visibility =
            if (pm.isIgnoringBatteryOptimizations(packageName)) View.GONE else View.VISIBLE
    }

    /** "Download   20 Mb" row; returns the speed TextView. */
    private fun speedRow(parent: LinearLayout, label: String): TextView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(4))
        }
        row.addView(TextView(this).apply {
            text = label
            textSize = 20f
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val value = TextView(this).apply {
            textSize = 30f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            text = "0 Kb"
        }
        row.addView(value)
        parent.addView(row)
        return value
    }

    private fun header(t: String) = TextView(this).apply {
        text = t
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(14), 0, dp(2))
    }

    private fun radioRow(options: List<Pair<String, Int>>, current: Int, onPick: (Int) -> Unit) =
        RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            options.forEach { (label, value) ->
                val rb = RadioButton(context).apply {
                    id = View.generateViewId()
                    text = label
                    tag = value
                    setPadding(0, 0, dp(16), 0)
                }
                addView(rb)
                if (value == current) check(rb.id)
            }
            setOnCheckedChangeListener { group, id ->
                val v = group.findViewById<RadioButton>(id)?.tag as? Int ?: return@setOnCheckedChangeListener
                onPick(v)
                SpeedOverlay.applySettings()
                refresh()
            }
        }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()
}
