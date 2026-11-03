package com.netspeed.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class NetSpeedStartOnBootContractTest {
    private fun source(path: String): String = listOf(File(path), File("../$path")).first { it.isFile }.readText()

    @Test fun startOnBootDefaultsEnabledAndReceiverHonorsIt() {
        val prefs = source("app/src/main/java/com/netspeed/app/Prefs.kt")
        val receiver = source("app/src/main/java/com/netspeed/app/BootReceiver.kt")
        val manifest = source("app/src/main/AndroidManifest.xml")
        assertTrue(prefs.contains("getBoolean(\"start_on_boot\", true)"))
        assertTrue(prefs.contains("putBoolean(\"start_on_boot\", value)"))
        assertTrue(receiver.contains("if (Prefs.startOnBoot(context))"))
        assertTrue(receiver.contains("SpeedService.start(context)"))
        assertTrue(manifest.contains("android.permission.RECEIVE_BOOT_COMPLETED"))
    }
}
