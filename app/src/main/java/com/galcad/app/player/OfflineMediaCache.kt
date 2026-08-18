package com.galcad.app.player

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * A bounded media cache inside Android's private app sandbox. Recently watched
 * HLS chunks can play again when the connection is
 * weak or temporarily unavailable; the cache never exceeds 500 MB.
 */
object OfflineMediaCache {
    private const val MAX_CACHE_BYTES = 500L * 1024L * 1024L
    @Volatile private var cache: SimpleCache? = null

    fun get(context: Context): SimpleCache = cache ?: synchronized(this) {
        cache ?: SimpleCache(
            File(context.applicationContext.cacheDir, "offline_media"),
            LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
            StandaloneDatabaseProvider(context.applicationContext)
        ).also { cache = it }
    }
}
