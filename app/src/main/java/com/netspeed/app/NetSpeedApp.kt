package com.netspeed.app

import android.app.Application
import com.google.android.material.color.DynamicColors

class NetSpeedApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Use the phone's wallpaper colours (Pixel "Material You") on Android 12+.
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
