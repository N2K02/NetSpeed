package com.netspeed.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

/** Settings screen (Material 3 Expressive). The meter itself runs on its own. */
class MainActivity : AppCompatActivity() {

    private lateinit var downText: TextView
    private lateinit var upText: TextView
    private lateinit var status: TextView
    private lateinit var permCard: View
    private lateinit var batteryCard: View
    private lateinit var enableSwitch: MaterialSwitch
    private lateinit var hSlider: Slider
    private lateinit var vSlider: Slider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        downText = findViewById(R.id.downText)
        upText = findViewById(R.id.upText)
        status = findViewById(R.id.status)
        permCard = findViewById(R.id.permCard)
        batteryCard = findViewById(R.id.batteryCard)

        findViewById<Button>(R.id.permButton).setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
        findViewById<Button>(R.id.batteryButton).setOnClickListener {
            runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }

        enableSwitch = findViewById(R.id.enableSwitch)
        enableSwitch.isChecked = Prefs.enabled(this)
        enableSwitch.setOnCheckedChangeListener { _, on ->
            Prefs.setEnabled(this, on)
            if (on) SpeedService.start(this) else SpeedService.stop(this)
            refresh()
        }

        // Position sliders
        hSlider = findViewById(R.id.hSlider)
        hSlider.setLabelFormatter { "${(it / 10).toInt()}%" }
        hSlider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            Prefs.setPosition(this, value.toInt())
            SpeedOverlay.applySettings()
        }
        vSlider = findViewById(R.id.vSlider)
        vSlider.setLabelFormatter { v -> if (v > 0) "+${v.toInt()} dp" else "${v.toInt()} dp" }
        vSlider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            Prefs.setVerticalDp(this, value.toInt())
            SpeedOverlay.applySettings()
        }
        findViewById<Button>(R.id.resetButton).setOnClickListener {
            Prefs.resetPosition(this)
            SpeedOverlay.applySettings()
            syncSliders()
        }
        syncSliders()

        // Choice groups
        bindGroup(
            R.id.intervalGroup,
            mapOf(R.id.int1 to 1, R.id.int2 to 2, R.id.int3 to 3),
            Prefs.intervalSec(this)
        ) { Prefs.setIntervalSec(this, it) }
        bindGroup(
            R.id.sizeGroup,
            mapOf(R.id.sizeS to 10, R.id.sizeM to 11, R.id.sizeL to 13),
            Prefs.textSize(this)
        ) { Prefs.setTextSize(this, it) }
        bindGroup(
            R.id.colorGroup,
            mapOf(R.id.colorWhite to 1, R.id.colorBlack to 0),
            if (Prefs.whiteText(this)) 1 else 0
        ) { Prefs.setWhiteText(this, it == 1) }
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
        permCard.visibility = if (allowed) View.GONE else View.VISIBLE
        enableSwitch.isEnabled = allowed
        status.text = when {
            !allowed -> "Needs permission first"
            SpeedOverlay.isRunning -> "Running in the background"
            else -> "Off"
        }
        downText.text = SpeedOverlay.lastDown
        upText.text = SpeedOverlay.lastUp
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        batteryCard.visibility =
            if (pm.isIgnoringBatteryOptimizations(packageName)) View.GONE else View.VISIBLE
    }

    private fun syncSliders() {
        hSlider.value = Prefs.position(this).toFloat().coerceIn(hSlider.valueFrom, hSlider.valueTo)
        vSlider.value = Prefs.verticalDp(this).toFloat().coerceIn(vSlider.valueFrom, vSlider.valueTo)
    }

    private fun bindGroup(
        groupId: Int,
        buttons: Map<Int, Int>,
        current: Int,
        onPick: (Int) -> Unit
    ) {
        val group = findViewById<MaterialButtonToggleGroup>(groupId)
        buttons.entries.firstOrNull { it.value == current }?.let { group.check(it.key) }
        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val v = buttons[checkedId] ?: return@addOnButtonCheckedListener
            onPick(v)
            SpeedOverlay.applySettings()
        }
    }
}
