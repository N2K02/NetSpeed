package com.netspeed.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetSpeedRefreshIntervalContractTest {
    private fun source(path: String): String =
        listOf(File(path), File("../$path")).first { it.isFile }.readText()

    @Test fun statusBarMeterHasNoUserDisableSwitch() {
        val main = source("app/src/main/java/com/netspeed/app/MainActivity.kt")
        assertFalse(main.contains("Show in status bar"))
        assertFalse(main.contains("EnableCard()"))
        assertTrue(main.contains("Prefs.setEnabled(this, true)"))
    }

    @Test fun refreshIntervalIsClampedToOneThroughTenSeconds() {
        val prefs = source("app/src/main/java/com/netspeed/app/Prefs.kt")
        val main = source("app/src/main/java/com/netspeed/app/MainActivity.kt")
        assertTrue(prefs.contains("getInt(\"interval\", 2)"))
        assertTrue(prefs.contains("coerceIn(1, 10)"))
        assertTrue(main.contains("valueRange = 1f..10f"))
        assertTrue(main.contains("steps = 8"))
        assertTrue(main.contains("item { NetworkRefreshCard() }"))
        assertTrue(main.contains("Refresh interval"))
        assertTrue(main.contains("Choose how often download and upload speeds are sampled"))
        assertTrue(main.contains("Prefs.setIntervalSec(this@MainActivity, interval)"))
        assertTrue(main.contains("SpeedOverlay.applySettings()"))
    }
}
