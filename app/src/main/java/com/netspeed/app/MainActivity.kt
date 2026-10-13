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
import android.widget.Switch
import android.widget.TextView

/** Settings screen. The meter itself runs on its own once started. */
class MainActivity : Activity() {

    private lateinit var preview: TextView
    private lateinit var unitHint: TextView
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

        preview = TextView(this).apply {
            textSize = 34f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            text = SpeedOverlay.lastText
        }
        root.addView(preview)
        unitHint = TextView(this).apply {
            gravity = Gravity.CENTER
            alpha = 0.7f
        }
        root.addView(unitHint)

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
            text = "Show speed meter"
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

        root.addView(header("Refresh every"))
        root.addView(radioRow(
            listOf("1 sec" to 1, "2 sec" to 2, "3 sec" to 3),
            Prefs.intervalSec(this)
        ) { Prefs.setIntervalSec(this, it) })

        root.addView(header("Unit"))
        root.addView(radioRow(
            listOf("Mbps" to Prefs.UNIT_MBPS, "KB/s" to Prefs.UNIT_KBS, "MB/s" to Prefs.UNIT_MBS),
            Prefs.unit(this)
        ) { Prefs.setUnit(this, it) })

        root.addView(header("Text size"))
        root.addView(radioRow(
            listOf("Small" to 11, "Medium" to 13, "Large" to 16),
            Prefs.textSize(this)
        ) { Prefs.setTextSize(this, it) })

        root.addView(Switch(this).apply {
            text = "Lock position (touches pass through)"
            setPadding(0, dp(16), 0, dp(8))
            isChecked = Prefs.locked(context)
            setOnCheckedChangeListener { _, on ->
                Prefs.setLocked(context, on)
                SpeedOverlay.applySettings()
            }
        })

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
            text = "D = download, U = upload, for the whole phone, rounded to a whole " +
                "number. Shows 0 D/ 0 U when there is no internet.\n\n" +
                "Drag the meter to move it. Tap it to open these settings.\n\n" +
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
        SpeedOverlay.listener = { runOnUiThread { preview.text = it } }
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
        unitHint.text = "in ${Prefs.unitLabel(Prefs.unit(this))}"
        preview.text = SpeedOverlay.lastText
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        batteryButton.visibility =
            if (pm.isIgnoringBatteryOptimizations(packageName)) View.GONE else View.VISIBLE
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
