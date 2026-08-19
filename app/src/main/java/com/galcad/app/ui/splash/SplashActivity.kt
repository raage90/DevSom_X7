package com.galcad.app.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.galcad.app.BuildConfig
import com.galcad.app.R
import com.galcad.app.data.TrackRequest
import com.galcad.app.network.ApiClient
import com.galcad.app.network.UrlResolver
import com.galcad.app.ui.main.MainActivity
import com.galcad.app.util.DeviceIdentity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Start parallel initialization for fast startup
        CoroutineScope(Dispatchers.Main).launch {
            // 1. Initialize device ID (fast, local operation)
            val installId = DeviceIdentity.getOrCreateDeviceHash(applicationContext)

            // 2. Resolve backend URL (fast with timeouts)
            val baseUrl = withContext(Dispatchers.IO) {
                UrlResolver.resolveApiBaseUrl(applicationContext)
            }
            ApiClient.init(baseUrl, installId)

            // 3. Check version in background (non-blocking)
            val versionCheckPassed = withContext(Dispatchers.IO) {
                checkAppVersion()
            }

            if (versionCheckPassed) {
                // 4. Fire-and-forget analytics ping - never blocks startup
                trackOpenSilently(installId)
                goToMainApp()
            }
            // if the version check failed, checkAppVersion() already
            // navigated to UpdateRequiredActivity and finished this one
        }
    }

    private suspend fun checkAppVersion(): Boolean {
        return try {
            val response = ApiClient.get().getAppVersion()
            val info = response.body()
            if (response.isSuccessful && info != null) {
                val minimum = info.minimumAppVersion.toIntOrNull() ?: 1
                if (BuildConfig.VERSION_CODE < minimum) {
                    // Switch to main thread for UI operations
                    withContext(Dispatchers.Main) {
                        val intent = Intent(this@SplashActivity, UpdateRequiredActivity::class.java)
                        intent.putExtra("message", info.updateMessage)
                        startActivity(intent)
                        finish()
                    }
                    return false
                }
            }
            true
        } catch (e: Exception) {
            // If the version check itself fails (no internet, server down),
            // never block the app over it -- fail open, let the user in.
            true
        }
    }

    private fun trackOpenSilently(deviceHash: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deviceModel = DeviceIdentity.getDeviceModel()
                ApiClient.get().track(
                    TrackRequest(
                        deviceHash = deviceHash,
                        deviceModel = deviceModel,
                        appVersion = BuildConfig.VERSION_NAME
                    )
                )
            } catch (e: Exception) {
                // analytics ping failing should never affect the user's experience
            }
        }
    }

    private fun goToMainApp() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
