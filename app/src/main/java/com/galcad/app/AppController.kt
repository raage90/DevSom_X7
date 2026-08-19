package com.galcad.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

/**
 * App-wide entry point. Optimized for fast startup with minimal initialization.
 * Network initialization happens in SplashActivity to avoid blocking app startup.
 */
class AppController : Application() {
    override fun onCreate() {
        super.onCreate()
        // Dark mode for premium modern look
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        // Preload critical resources for faster startup
        preloadResources()
    }

    private fun preloadResources() {
        // Initialize any heavy resources here in background
        // This ensures smoother first navigation
    }
}
