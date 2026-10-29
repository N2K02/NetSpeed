package com.netspeed.app
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Uses the main app's n2k02theme and single Settings header. */
@Composable
internal fun NetSpeedLogsPage(modifier: Modifier = Modifier, onBack: () -> Unit) {
    N2K02UnifiedLogsPage(modifier = modifier, appName = "NetSpeed")
}
