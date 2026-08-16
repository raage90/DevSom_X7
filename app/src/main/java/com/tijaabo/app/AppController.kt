package com.tijaabo.app

import android.app.Application

/**
 * App-wide entry point. Actual network initialization (URL resolution +
 * ApiClient setup) happens in SplashActivity, since it needs to show a
 * loading state while resolving -- kept out of here to avoid blocking
 * app startup with network calls.
 */
class AppController : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
