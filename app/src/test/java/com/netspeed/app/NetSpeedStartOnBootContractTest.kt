package com.netspeed.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NetSpeedStartOnBootContractTest {
    @Test fun startOnBootDefaultsEnabledAndReceiverHonorsIt() {
        val root = File("src/main")
        val prefs = File(root, "java/com/netspeed/app/Prefs.kt").readText()
        val receiver = File(root, "java/com/netspeed/app/BootReceiver.kt").readText()
        val manifest = File(root, "AndroidManifest.xml").readText()
        assertTrue(prefs.contains("getBoolean(\"start_on_boot\", true)"))
        assertTrue(prefs.contains("putBoolean(\"start_on_boot\", value)"))
        assertTrue(receiver.contains("if (Prefs.startOnBoot(context))"))
        assertTrue(receiver.contains("SpeedService.start(context)"))
        assertTrue(manifest.contains("android.permission.RECEIVE_BOOT_COMPLETED"))
    }
}
