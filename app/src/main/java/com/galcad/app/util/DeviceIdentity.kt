package com.galcad.app.util

import android.content.Context
import android.os.Build
import java.util.UUID

/**
 * Generates and stores a random, anonymous ID for this install -- not tied
 * to any real identity, phone number, or account. Used only so the backend
 * can count "how many different installs used the app today" for the
 * Analytics dashboard. Uninstalling and reinstalling produces a new ID.
 */
object DeviceIdentity {
    private const val PREFS_NAME = "device_identity"
    private const val KEY_DEVICE_HASH = "device_hash"

    fun getOrCreateDeviceHash(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_HASH, null)
        if (existing != null) return existing

        val fresh = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_HASH, fresh).apply()
        return fresh
    }

    /** e.g. "Samsung SM-G991B" -- this is what powers the Devices breakdown in Analytics */
    fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    }
}
