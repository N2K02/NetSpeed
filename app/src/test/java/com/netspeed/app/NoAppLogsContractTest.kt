package com.netspeed.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoAppLogsContractTest {
    private fun src(path: String) = listOf(File(path), File("../$path")).first { it.isFile }.readText()

    @Test fun logsSettingsAreGone() {
        val activity = src("app/src/main/java/com/netspeed/app/MainActivity.kt")
        assertFalse(activity.contains("showingLogs"))
        assertFalse(activity.contains("NetSpeedLogsPage"))
        assertFalse(activity.contains("Text(\"Logs\")"))
    }

    @Test fun noPersistentLogProvider() {
        val log = src("app/src/main/java/com/netspeed/app/AppLog.kt")
        assertTrue(log.contains("Log.d("))
        assertFalse(log.contains("appendText("))
        assertFalse(src("app/src/main/AndroidManifest.xml").contains("CrashJournalProvider"))
    }
}
