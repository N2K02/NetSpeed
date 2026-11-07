package com.netspeed.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerRecoveryPolicyTest {
    @Test fun chargingRestoresDespiteBatterySaver() {
        assertTrue(RecoveryPolicy.shouldRestore(enabled = true, charging = true, powerSaveMode = true))
    }

    @Test fun disablingSaverRestoresWithoutChargingOrBatteryThreshold() {
        assertTrue(RecoveryPolicy.shouldRestore(enabled = true, charging = false, powerSaveMode = false))
    }

    @Test fun saverWithoutChargerDefersRecovery() {
        assertFalse(RecoveryPolicy.shouldRestore(enabled = true, charging = false, powerSaveMode = true))
    }

    @Test fun explicitQuitAlwaysWins() {
        assertFalse(RecoveryPolicy.shouldRestore(enabled = false, charging = true, powerSaveMode = false))
        assertFalse(RecoveryPolicy.shouldRestore(enabled = false, charging = true, powerSaveMode = true))
    }
}
