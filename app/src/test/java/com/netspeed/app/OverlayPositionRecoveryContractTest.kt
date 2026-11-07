package com.netspeed.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayPositionRecoveryContractTest {
    private fun source(path: String): String = listOf(File(path), File("../$path")).first { it.isFile }.readText()

    @Test fun overlayPositionIsReachableAndPersistsBothAxes() {
        val activity = source("app/src/main/java/com/netspeed/app/MainActivity.kt")
        assertTrue(activity.contains("headlineContent = { Text(\"Overlay position\") }"))
        assertTrue(activity.contains("showingOverlayPosition = true"))
        assertTrue(activity.contains("else if (showingOverlayPosition)"))
        assertTrue(activity.contains("item { PositionCard() }"))
        assertTrue(activity.contains("Prefs.setPosition(this@MainActivity"))
        assertTrue(activity.contains("Prefs.setVerticalDp(this@MainActivity"))
        assertTrue(activity.contains("Prefs.resetPosition(this@MainActivity)"))
    }

    @Test fun recoveryIsPersistedAndDoesNotRestartAfterQuit() {
        val manifest = source("app/src/main/AndroidManifest.xml")
        val service = source("app/src/main/java/com/netspeed/app/SpeedService.kt")
        val recovery = source("app/src/main/java/com/netspeed/app/PowerRecovery.kt")
        assertTrue(manifest.contains("android.permission.BIND_JOB_SERVICE"))
        assertTrue(recovery.contains(".setPersisted(true)"))
        assertTrue(recovery.contains("PowerManager"))
        assertTrue(recovery.contains("battery.isCharging"))
        assertTrue(service.contains("PowerManager.ACTION_POWER_SAVE_MODE_CHANGED"))
        assertTrue(service.contains("Intent.ACTION_POWER_CONNECTED"))
        assertTrue(service.contains("PowerRecovery.cancel(ctx)"))
    }
}
