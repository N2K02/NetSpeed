package com.netspeed.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SpeedOverlayEfficiencyTest {
    @Test fun unchangedTrafficDoesNotInvalidateOverlayAndScreenOffStopsSampling() {
        val file = listOf(
            File("src/main/java/com/netspeed/app/SpeedOverlay.kt"),
            File("app/src/main/java/com/netspeed/app/SpeedOverlay.kt")
        ).first { it.exists() }
        val code = file.readText()
        val guard = code.indexOf("if (lastDown == down && lastUp == up) return")
        val change = code.indexOf("view?.text = composed()", guard)
        assertTrue(guard >= 0 && change > guard)
        assertTrue(code.contains("Intent.ACTION_SCREEN_OFF ->"))
        assertTrue(code.contains("handler.removeCallbacks(tick)"))
        assertTrue(code.contains("if (screenOn) handler.postDelayed(this"))
    }
}
