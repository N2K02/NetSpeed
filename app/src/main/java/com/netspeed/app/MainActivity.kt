package com.netspeed.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import dev.n2k02.updater.hasPendingUpdate
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Settings screen (Material 3 Expressive, Compose). The meter itself runs on its own. */
class MainActivity : ComponentActivity() {

    // Compose state
    private var down by mutableStateOf(SpeedOverlay.lastDown)
    private var up by mutableStateOf(SpeedOverlay.lastUp)
    private var allowed by mutableStateOf(false)
    private var running by mutableStateOf(false)
    private var batteryOk by mutableStateOf(true)
    private var enabled by mutableStateOf(true)
    private var hPos by mutableFloatStateOf(Prefs.H_DEFAULT.toFloat())
    private var vPos by mutableFloatStateOf(0f)
    private var interval by mutableIntStateOf(1)
    private var textSize by mutableIntStateOf(11)
    private var whiteText by mutableStateOf(true)
    private var showingUpdates by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        LiveUpdater.init(this)
        loadPrefs()
        setContent { NetSpeedTheme { SettingsScreen() } }
    }

    override fun onResume() {
        super.onResume()
        LiveUpdater.onResume(this)
        // Coming back from the permission screen: start automatically if allowed.
        if (Prefs.enabled(this) && Settings.canDrawOverlays(this)) SpeedService.start(this)
        SpeedOverlay.listener = { d, u -> runOnUiThread { down = d; up = u } }
        refresh()
    }

    override fun onPause() {
        SpeedOverlay.listener = null
        super.onPause()
    }

    private fun loadPrefs() {
        enabled = Prefs.enabled(this)
        hPos = Prefs.position(this).toFloat().coerceIn(H_MIN, 1000f)
        vPos = Prefs.verticalDp(this).toFloat()
        interval = Prefs.intervalSec(this)
        textSize = Prefs.textSize(this)
        whiteText = Prefs.whiteText(this)
    }

    private fun refresh() {
        allowed = Settings.canDrawOverlays(this)
        running = SpeedOverlay.isRunning
        down = SpeedOverlay.lastDown
        up = SpeedOverlay.lastUp
        batteryOk = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
    }

    private fun setEnabledPref(on: Boolean) {
        enabled = on
        Prefs.setEnabled(this, on)
        if (on) SpeedService.start(this) else SpeedService.stop(this)
        refresh()
    }

    private fun quitNetSpeed() {
        Prefs.setEnabled(this, false)
        enabled = false
        SpeedService.stop(this)
        finishAndRemoveTask()
    }

    private fun openOverlayPermission() = startActivity(
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
    )

    private fun openBatterySetting() {
        runCatching {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun SettingsScreen() {
        val updateStatus by LiveUpdater.controller.status.collectAsState()
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                    if (showingUpdates) dev.n2k02.updater.N2K02UpdateTitle()
                    else Text("Settings")
                },
                actions = {
                    if (!showingUpdates) {
                        dev.n2k02.updater.N2K02AppIcon(Modifier.size(26.dp))
                        Text("NetSpeed", modifier = Modifier.padding(start = 8.dp, end = 12.dp))
                    }
                },
                    navigationIcon = {},
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.navigationBarsPadding()) {
                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        )
                        NavigationBar(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                        ) {
                            NavigationBarItem(
                                selected = !showingUpdates,
                                onClick = { showingUpdates = false },
                                icon = { dev.n2k02.updater.N2K02UpdateBadgedIcon(updateStatus) { Icon(Icons.Rounded.Home, contentDescription = "Settings") } },
                                label = { Text("Settings") },
                                alwaysShowLabel = true,
                            )
                            NavigationBarItem(
                                selected = showingUpdates,
                                onClick = { showingUpdates = true },
                                icon = { Icon(Icons.Rounded.SystemUpdate, contentDescription = "Updates") },
                                label = { Text("Updates") },
                                alwaysShowLabel = true,
                            )
                        }
                    }
                }
            },
        ) { padding ->
            if (showingUpdates) {
                UpdatesPage(modifier = Modifier.padding(padding))
            } else LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    androidx.compose.material3.ListItem(
                        headlineContent = { Text("About & updates") },
                        supportingContent = { Text(if (updateStatus.hasPendingUpdate()) "A new update is available" else "Version and update checks") },
                        leadingContent = { Icon(Icons.Rounded.SystemUpdate, contentDescription = null) },
                        trailingContent = { dev.n2k02.updater.N2K02UpdateBadge(updateStatus) },
                        modifier = Modifier.clickable { showingUpdates = true },
                    )
                }
                item { SpeedCard() }
                item {
                    AnimatedVisibility(visible = !allowed) {
                        ActionCard(
                            text = "Allow NetSpeed to display over other apps so it can show the speed in the status bar.",
                            onClick = ::openOverlayPermission,
                            error = true,
                        )
                    }
                }
                item { EnableCard() }
                item {
                    SettingCard {
                        Text("Quit NetSpeed", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Stops the floating speed meter and closes NetSpeed. Open the app again and turn the meter back on when you want to resume it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = ::quitNetSpeed, shapes = ButtonDefaults.shapes()) {
                            Text("Quit NetSpeed")
                        }
                    }
                }
                item { PositionCard() }
                item { LookCard() }
                item {
                    AnimatedVisibility(visible = !batteryOk) {
                        ActionCard(
                            title = "Keep running in the background",
                            text = "Stops Android from pausing NetSpeed to save battery.",
                            onClick = ::openBatterySetting,
                        )
                    }
                }
                item {
                    Text(
                        "Speed for the whole phone, rounded to a whole number. Kb = kilobits, Mb = megabits " +
                            "per second. Shows 0 Kb when there is no internet. No notification is used.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }

    @Composable
    private fun SettingCard(
        containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
        content: @Composable () -> Unit,
    ) {
        Card(
            
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp)) { content() }
        }
    }

    @Composable
    private fun SpeedCard() = SettingCard {
        SpeedRow("Download", down, MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        SpeedRow("Upload", up, MaterialTheme.colorScheme.tertiary)
    }

    @Composable
    private fun SpeedRow(label: String, value: String, color: Color) {
        val arrow = if (label == "Download") "↓" else "↑"
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(arrow, style = MaterialTheme.typography.displaySmall, color = color)
            Text(value, style = MaterialTheme.typography.displaySmall, color = color)
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun ActionCard(text: String, onClick: () -> Unit, title: String? = null, error: Boolean = false) {
        val scheme = MaterialTheme.colorScheme
        SettingCard(containerColor = scheme.surfaceContainer) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text,
                style = if (error) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                color = if (error) scheme.onErrorContainer else scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onClick, shapes = ButtonDefaults.shapes()) { Text("Allow") }
        }
    }

    @Composable
    private fun EnableCard() = SettingCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Show in status bar", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        !allowed -> "Needs permission first"
                        running -> "Running in the background"
                        else -> "Off"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled && allowed, onCheckedChange = ::setEnabledPref, enabled = allowed)
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun PositionCard() = SettingCard {
        Text("Position", style = MaterialTheme.typography.titleMedium)
        Text(
            "Line the speed up just left of your clock.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        LabelWithValue("Horizontal", "${(hPos / 10).toInt()}%")
        Slider(
            value = hPos,
            onValueChange = {
                hPos = it
                Prefs.setPosition(this@MainActivity, it.toInt())
                SpeedOverlay.applySettings()
            },
            valueRange = H_MIN..1000f,
        )
        LabelWithValue("Vertical", vPos.toInt().let { if (it > 0) "+$it dp" else "$it dp" })
        Slider(
            value = vPos,
            onValueChange = {
                vPos = it
                Prefs.setVerticalDp(this@MainActivity, it.toInt())
                SpeedOverlay.applySettings()
            },
            valueRange = Prefs.V_MIN.toFloat()..Prefs.V_MAX.toFloat(),
        )
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(
            onClick = {
                Prefs.resetPosition(this@MainActivity)
                SpeedOverlay.applySettings()
                hPos = Prefs.position(this@MainActivity).toFloat()
                vPos = Prefs.verticalDp(this@MainActivity).toFloat()
            },
            shapes = ButtonDefaults.shapes(),
        ) { Text("Reset position") }
    }

    @Composable
    private fun LabelWithValue(label: String, value: String) {
        Row {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }

    @Composable
    private fun LookCard() = SettingCard {
        Text("Refresh every", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Choice(listOf(1 to "1 sec", 2 to "2 sec", 3 to "3 sec"), interval) {
            interval = it; Prefs.setIntervalSec(this@MainActivity, it); SpeedOverlay.applySettings()
        }
        Spacer(Modifier.height(20.dp))
        Text("Text size", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Choice(listOf(10 to "Small", 11 to "Medium", 13 to "Large"), textSize) {
            textSize = it; Prefs.setTextSize(this@MainActivity, it); SpeedOverlay.applySettings()
        }
        Spacer(Modifier.height(20.dp))
        Text("Text colour", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Choice(listOf(1 to "White", 0 to "Black"), if (whiteText) 1 else 0) {
            whiteText = it == 1; Prefs.setWhiteText(this@MainActivity, it == 1); SpeedOverlay.applySettings()
        }
    }

    @Composable
    private fun Choice(options: List<Pair<Int, String>>, current: Int, onPick: (Int) -> Unit) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, (value, label) ->
                SegmentedButton(
                    selected = value == current,
                    onClick = { onPick(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = options.size),
                    label = { Text(label) },
                )
            }
        }
    }

    private companion object {
        const val H_MIN = 50f
    }
}
