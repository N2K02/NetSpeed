package com.netspeed.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetSpeedForegroundServiceContractTest {
    private fun source(path: String): String =
        listOf(File(path), File("../$path")).first { it.isFile }.readText()

    @Test fun serviceIsDeclaredAsStickyForegroundWithoutNotificationPermission() {
        val manifest = source("app/src/main/AndroidManifest.xml")
        val service = source("app/src/main/java/com/netspeed/app/SpeedService.kt")
        assertTrue(manifest.contains("android.permission.FOREGROUND_SERVICE"))
        assertTrue(manifest.contains("android.permission.FOREGROUND_SERVICE_SPECIAL_USE"))
        assertTrue(manifest.contains("android:foregroundServiceType=\"specialUse\""))
        assertFalse(manifest.contains("POST_NOTIFICATIONS"))
        assertTrue(service.contains("startForegroundService("))
        assertTrue(service.contains("startForeground(NOTIFICATION_ID"))
        assertTrue(service.contains("return START_STICKY"))
        assertTrue(service.contains("stopForeground(STOP_FOREGROUND_REMOVE)"))
        assertTrue(service.contains("if (!Prefs.enabled(this))"))
    }
}
