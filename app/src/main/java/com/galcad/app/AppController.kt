package com.galcad.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

/**
 * App-wide entry point. Actual network initialization (URL resolution +
 * ApiClient setup) happens in SplashActivity, since it needs to show a
 * loading state while resolving -- kept out of here to avoid blocking
 * app startup with network calls.
 */
class AppController : Application() {
    override fun onCreate() {
        super.onCreate()
        // Light mode by default, regardless of the device's system theme setting.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
