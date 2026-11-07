package com.netspeed.app

/** The same pure recovery gate is used by NetSpeed and PBar. */
internal object BatteryRecoveryPolicy {
    const val FALLBACK_PERIOD_MS: Long = 15 * 60 * 1000L

    fun canRecover(
        userEnabled: Boolean,
        permissionsGranted: Boolean,
        charging: Boolean,
        saverEnabled: Boolean,
    ): Boolean = userEnabled && permissionsGranted && (charging || !saverEnabled)
}
