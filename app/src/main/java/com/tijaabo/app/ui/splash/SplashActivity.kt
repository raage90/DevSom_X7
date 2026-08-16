package com.tijaabo.app.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tijaabo.app.BuildConfig
import com.tijaabo.app.R
import com.tijaabo.app.data.TrackRequest
import com.tijaabo.app.network.ApiClient
import com.tijaabo.app.network.UrlResolver
import com.tijaabo.app.ui.main.MainActivity
import com.tijaabo.app.util.DeviceIdentity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        CoroutineScope(Dispatchers.Main).launch {
            // 1. Find the real backend address (resolver system, falls back
            //    to the built-in Railway URL if nothing responds)
            val baseUrl = UrlResolver.resolveApiBaseUrl(applicationContext)
            ApiClient.init(baseUrl)

            // 2. Check whether this install is too old to keep using
            val versionCheckPassed = checkAppVersion()

            if (versionCheckPassed) {
                // 3. Fire-and-forget analytics ping -- never blocks startup,
                //    and a failure here never stops the app from opening
                trackOpenSilently()
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
                    val intent = Intent(this, UpdateRequiredActivity::class.java)
                    intent.putExtra("message", info.updateMessage)
                    startActivity(intent)
                    finish()
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

    private fun trackOpenSilently() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deviceHash = DeviceIdentity.getOrCreateDeviceHash(applicationContext)
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
