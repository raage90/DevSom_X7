package com.tijaabo.app.network

import android.content.Context
import android.content.SharedPreferences
import com.tijaabo.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/**
 * Resolves the real backend URL at app startup by checking a small list of
 * resolver addresses (see README-resolver.md for how to set these up).
 * If none respond, falls back to BuildConfig.FALLBACK_API_URL (your Railway
 * URL baked in at build time) so the app never fully breaks.
 *
 * This is what lets you swap backend domains later (e.g. if you buy
 * name.com and want to move off Railway's URL, or need to replace a
 * banned domain) without rebuilding or redistributing the APK.
 */
object UrlResolver {
    private const val PREFS_NAME = "url_resolver_cache"
    private const val KEY_RESOLVED_URL = "resolved_api_url"

    // Add more resolver URLs here over time (different GitHub accounts,
    // a Cloudflare Worker, etc). Checked in order, first success wins.
    private val resolverUrls = listOf(
        // Example: "https://raw.githubusercontent.com/youraccount/resolver1/main/config.json"
    )

    private val client = OkHttpClient.Builder().build()

    suspend fun resolveApiBaseUrl(context: Context): String = withContext(Dispatchers.IO) {
        for (url in resolverUrls) {
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: continue
                        val domain = JSONObject(body).optString("domain")
                        if (domain.isNotBlank()) {
                            val resolved = if (domain.endsWith("/")) domain else "$domain/"
                            cacheUrl(context, resolved)
                            return@withContext resolved
                        }
                    }
                }
            } catch (e: IOException) {
                // this resolver is down/unreachable -- try the next one
                continue
            }
        }

        // All resolvers failed (or none configured yet) -- use last known
        // good URL if we have one cached, otherwise the build-time fallback.
        getCachedUrl(context) ?: BuildConfig.FALLBACK_API_URL
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun cacheUrl(context: Context, url: String) {
        prefs(context).edit().putString(KEY_RESOLVED_URL, url).apply()
    }

    private fun getCachedUrl(context: Context): String? =
        prefs(context).getString(KEY_RESOLVED_URL, null)
}
