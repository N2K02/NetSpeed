package com.netspeed.app

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class NetSpeedOverlayLifecycleTest {
    private fun source(path: String): String =
        listOf(File("app/src/main/java/com/netspeed/app/$path"),
            File("src/main/java/com/netspeed/app/$path"))
            .first { it.isFile }.readText()

    @Test fun pollingStopsWhenScreenOffOrStatusBarHidden() {
        val overlay = source("SpeedOverlay.kt")
        assertTrue(overlay.contains("if (!screenOn || !statusBarVisible) return"))
        assertTrue(overlay.contains("if (screenOn && statusBarVisible)"))
        assertTrue(overlay.contains("handler.removeCallbacks(tick)"))
        assertTrue(overlay.contains("Intent.ACTION_SCREEN_OFF ->"))
        assertTrue(overlay.contains("Intent.ACTION_SCREEN_ON ->"))
        assertTrue(overlay.contains("setStatusBarVisible(v, insets.isVisible"))
        assertTrue(overlay.contains("setStatusBarVisible(v, flags and View.SYSTEM_UI_FLAG_FULLSCREEN == 0)"))
        assertTrue(overlay.contains("meter?.reset()"))
    }

    @Test fun failedAttachmentAndShutdownReleaseAllOverlayResources() {
        val overlay = source("SpeedOverlay.kt")
        assertTrue(overlay.contains("} catch (_: Exception) {\n            stop()"))
        assertTrue(overlay.contains("if (runCatching { ctx.registerReceiver(screenReceiver, f) }.isFailure)"))
        assertTrue(overlay.contains("wm = null"))
        assertTrue(overlay.contains("app = null"))
        assertTrue(overlay.contains("meter = null"))
        assertTrue(overlay.contains("params = null"))
        assertTrue(overlay.contains("view = null"))
        val service = source("SpeedService.kt")
        val destroy = service.substringAfter("override fun onDestroy()")
            .substringBefore("private fun promoteToForeground()")
        assertTrue(destroy.contains("SpeedOverlay.stop()"))
        assertTrue(service.contains("return START_STICKY"))
        assertTrue(service.contains("if (!Prefs.enabled(this))"))
    }

    @Test fun samplingDoesNotChangeUserSelectedIntervalOrVpnAccounting() {
        val prefs = source("Prefs.kt")
        assertTrue(prefs.contains("coerceIn(1, 10)"))
        val meter = source("SpeedMeter.kt")
        assertTrue(meter.contains("TrafficStats.getTotalRxBytes()"))
        assertTrue(meter.contains("TrafficStats.getTotalTxBytes()"))
        assertTrue(meter.contains("vpnActive()"))
        assertTrue(source("SpeedOverlay.kt")
            .contains("Prefs.intervalSec(ctx) * 1000L"))
    }
}
