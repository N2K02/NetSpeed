package com.netspeed.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Keeps the bottom navigation pages separate, without duplicated Home controls. */
class NetSpeedNavigationContractTest {
    private fun screen(): String {
        val relative = "com/netspeed/app/MainActivity.kt"
        return listOf(File("src/main/java", relative), File("app/src/main/java", relative))
            .firstOrNull { it.isFile }?.readText()
            ?: error("Missing app UI source: $relative")
    }

    private fun pages(): Pair<String, String> {
        val content = screen().substringAfter("private fun SettingsScreen()")
            .substringBefore("private fun SettingCard(")
        val settings = content.substringAfter("} else if (showingSettings) {")
            .substringBefore("} else {\n                LazyColumn(")
        val home = content.substringAfter("} else {\n                LazyColumn(")
        return home to settings
    }

    @Test fun homeHasLiveControlsButNoDuplicateSettingsShortcut() {
        val (home, _) = pages()
        assertTrue(home.contains("item { SpeedCard() }"))
        assertTrue(home.contains("item { EnableCard() }"))
        assertTrue(home.contains("openOverlayPermission"))
        assertFalse(home.contains("headlineContent = { Text(\"Settings\") }"))
        assertFalse(home.contains("About & updates"))
    }

    @Test fun settingsContainsOnlyTheUpdatesSubmenu() {
        val (_, settings) = pages()
        assertTrue(settings.contains("headlineContent = { Text(\"About & updates\") }"))
        assertTrue(settings.contains("showingUpdates = true"))
        assertFalse(settings.contains("SpeedCard()"))
        assertFalse(settings.contains("EnableCard()"))
        assertFalse(settings.contains("PositionCard()"))
        assertFalse(settings.contains("LookCard()"))
        assertFalse(settings.contains("Quit NetSpeed"))
    }

    @Test fun bothBottomTabsRetainTheSharedPageHeader() {
        val content = screen().substringAfter("private fun SettingsScreen()")
            .substringBefore("private fun SettingCard(")
        assertTrue(content.contains("Icons.Rounded.Home"))
        assertTrue(content.contains("Icons.Rounded.SettingsIcon"))
        assertTrue(content.contains("Icons.Rounded.SystemUpdate"))
        assertTrue(content.contains("N2K02AppIcon(Modifier.size(28.dp))"))
        assertTrue(content.contains("Text(\"NetSpeed\""))
    }
}
