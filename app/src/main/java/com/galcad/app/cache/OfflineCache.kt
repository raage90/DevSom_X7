package com.galcad.app.cache

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Simple offline cache: saves the last successful response for a screen as
 * a JSON file, so reopening the app without internet still shows the last
 * content instead of a blank screen. Deliberately NOT using Room here --
 * this is plain file storage with Gson, which avoids the whole class of
 * dependency/version-mismatch risk that Room's annotation processor (KSP)
 * caused earlier in this project.
 *
 * Only text/thumbnail data is cached this way -- video/audio playback URLs
 * are never cached, since they're short-lived signed links tied to your
 * security design and wouldn't work later anyway.
 */
object OfflineCache {
    val gson = Gson()

    fun cacheFile(context: Context, key: String): File {
        // sanitize the key so it's safe as a filename
        val safeKey = key.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(context.cacheDir, "offline_$safeKey.json")
    }

    fun <T> save(context: Context, key: String, data: T) {
        try {
            val json = gson.toJson(data)
            cacheFile(context, key).writeText(json)
        } catch (e: Exception) {
            // caching is a nice-to-have -- never let a failure here affect the app
        }
    }

    inline fun <reified T> load(context: Context, key: String): T? {
        return try {
            val file = cacheFile(context, key)
            if (!file.exists()) return null
            val json = file.readText()
            gson.fromJson(json, object : TypeToken<T>() {}.type)
        } catch (e: Exception) {
            null
        }
    }

    fun hasCache(context: Context, key: String): Boolean = cacheFile(context, key).exists()
}
