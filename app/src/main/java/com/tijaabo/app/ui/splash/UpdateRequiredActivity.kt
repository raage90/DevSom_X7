package com.tijaabo.app.ui.splash

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tijaabo.app.R

/**
 * Shown instead of the normal app when the backend says this install's
 * version is below minimum_app_version (Settings tab, admin panel).
 * There's deliberately no "continue anyway" button -- if you raised the
 * minimum, it was for a reason (usually security).
 */
class UpdateRequiredActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_required)

        val message = intent.getStringExtra("message")
            ?: "A new version is available. Please update to continue."
        findViewById<android.widget.TextView>(R.id.updateMessageText).text = message
    }
}
