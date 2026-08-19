package com.galcad.app.ui.player

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import coil.load
import com.galcad.app.R
import com.galcad.app.databinding.ActivityVideoPlayerBinding
import com.galcad.app.network.ApiClient
import com.galcad.app.player.OfflineMediaCache
import kotlinx.coroutines.launch

/**
 * Plays one video or audio item via its signed, expiring Bunny URL.
 *
 * The player is NOT fullscreen by default -- it's a fixed 16:9 box at the
 * top (like YouTube/Facebook), with the title and an "Up next" preview
 * visible below it. Tapping the fullscreen button expands the player to
 * landscape fullscreen; tapping it again (or pressing back while
 * fullscreen) returns to the normal inline view.
 *
 * For videos, automatically loads and plays the next video in the same
 * folder when the current one finishes -- matching the YouTube-style
 * autoplay behavior from the spec. No comments/likes, just playback +
 * a views counter (counted server-side, once, when /play is called).
 */
class VideoPlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_IS_AUDIO = "is_audio"
    }

    private var _binding: ActivityVideoPlayerBinding? = null
    private val binding get() = _binding!!
    private var player: ExoPlayer? = null
    private var isAudio: Boolean = false
    private var isFullscreen: Boolean = false
    private var currentItemId: Int = -1
    private var upNextListener: Player.Listener? = null
    private val playbackPrefs by lazy { getSharedPreferences("playback_progress", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isAudio = intent.getBooleanExtra(EXTRA_IS_AUDIO, false)
        val itemId = intent.getIntExtra(EXTRA_ITEM_ID, -1)
        if (itemId == -1) {
            finish()
            return
        }

        // Starts in normal portrait, inline view -- NOT forced fullscreen.
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        // Mirrors the old app's HLS buffering strategy while using Android's
        // native Media3 player: enough forward buffer for unreliable networks,
        // but bounded so it cannot grow without limit.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 60_000, 1_500, 3_000)
            .build()
        val dataSourceFactory = CacheDataSource.Factory()
            .setCache(OfflineMediaCache.get(this))
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(false))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
        binding.playerView.player = player

        // Audio has no meaningful "fullscreen" -- hide the toggle for it
        if (isAudio) {
            binding.fullscreenToggleButton.visibility = View.GONE
        } else {
            binding.fullscreenToggleButton.setOnClickListener { toggleFullscreen() }
        }
        binding.playerErrorText.setOnClickListener {
            if (currentItemId != -1) loadAndPlay(currentItemId)
        }

        // Double-tap left/right half of the video to seek -10s/+10s,
        // YouTube-style. Single taps still toggle the controller as normal.
        if (!isAudio) {
            val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (binding.playerView.isControllerFullyVisible) {
                        binding.playerView.hideController()
                    } else {
                        binding.playerView.showController()
                    }
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    val currentPlayer = player ?: return true
                    val seekMs = 10_000L
                    if (e.x < binding.doubleTapOverlay.width / 2) {
                        currentPlayer.seekTo((currentPlayer.currentPosition - seekMs).coerceAtLeast(0))
                    } else {
                        currentPlayer.seekTo(currentPlayer.currentPosition + seekMs)
                    }
                    return true
                }
            })
            binding.doubleTapOverlay.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }
        }

        loadAndPlay(itemId)
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        binding.fullscreenToggleButton.setImageResource(
            if (isFullscreen) R.drawable.ic_fullscreen_collapse else R.drawable.ic_fullscreen_expand
        )
        if (isFullscreen) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
            binding.playerTitleText.visibility = View.GONE
            binding.upNextLabel.visibility = View.GONE
            binding.upNextRow.visibility = View.GONE
            // Expand the player box to fill the whole screen while fullscreen
            val params = binding.playerContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.height = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT
            params.dimensionRatio = null
            binding.playerContainer.layoutParams = params
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            binding.playerTitleText.visibility = View.VISIBLE
            restoreUpNextVisibility()
            val params = binding.playerContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.height = 0
            params.dimensionRatio = "H,16:9"
            binding.playerContainer.layoutParams = params
        }
    }

    override fun onBackPressed() {
        if (isFullscreen) {
            toggleFullscreen()
        } else {
            super.onBackPressed()
        }
    }

    private fun loadAndPlay(itemId: Int) {
        currentItemId = itemId
        upNextListener?.let { player?.removeListener(it) }
        upNextListener = null
        binding.playerLoadingSpinner.visibility = View.VISIBLE
        binding.playerErrorText.visibility = View.GONE
        binding.upNextRow.visibility = View.GONE
        binding.upNextLabel.visibility = View.GONE

        lifecycleScope.launch {
            try {
                if (isAudio) {
                    val response = ApiClient.get().playAudio(itemId)
                    val data = response.body()
                    if (!response.isSuccessful || data == null) throw Exception("Playback unavailable")
                    binding.playerTitleText.text = data.title
                    playUrl(data.playbackUrl)
                } else {
                    val response = ApiClient.get().playVideo(itemId)
                    val data = response.body()
                    if (!response.isSuccessful || data == null) throw Exception("Playback unavailable")
                    binding.playerTitleText.text = data.title
                    playUrl(data.playbackUrl)

                    // Autoplay-next: when this video ends, load the next one
                    // automatically, YouTube-style. Also show it as "Up next"
                    // right away so the person can tap ahead manually too.
                    val next = data.nextVideo
                    if (next != null) {
                        binding.upNextLabel.visibility = View.VISIBLE
                        binding.upNextRow.visibility = View.VISIBLE
                        binding.upNextTitleText.text = next.title
                        if (!next.thumbnailUrl.isNullOrBlank()) {
                            binding.upNextThumbnail.load(next.thumbnailUrl)
                        }
                        binding.upNextRow.setOnClickListener { loadAndPlay(next.id) }

                        upNextListener = object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_ENDED) {
                                    player?.removeListener(this)
                                    loadAndPlay(next.id)
                                }
                            }
                        }
                        player?.addListener(upNextListener!!)
                    }
                }
            } catch (e: Exception) {
                binding.playerLoadingSpinner.visibility = View.GONE
                binding.playerErrorText.visibility = View.VISIBLE
                binding.playerErrorText.text = "Couldn't load this ${if (isAudio) "audio" else "video"}. Tap here to retry."
            }
        }
    }

    private fun restoreUpNextVisibility() {
        val hasUpNext = binding.upNextTitleText.text.isNotBlank()
        binding.upNextLabel.visibility = if (hasUpNext) View.VISIBLE else View.GONE
        binding.upNextRow.visibility = if (hasUpNext) View.VISIBLE else View.GONE
    }

    private fun playUrl(url: String) {
        binding.playerLoadingSpinner.visibility = View.GONE
        val mediaItem = MediaItem.fromUri(url)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        val savedPosition = playbackPrefs.getLong("video_$currentItemId", 0L)
        if (!isAudio && savedPosition > 5_000) player?.seekTo(savedPosition)
        player?.playWhenReady = true
    }

    override fun onStop() {
        if (!isAudio && currentItemId != -1) {
            playbackPrefs.edit().putLong("video_$currentItemId", player?.currentPosition ?: 0L).apply()
        }
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        upNextListener?.let { player?.removeListener(it) }
        super.onDestroy()
        player?.release()
        player = null
        _binding = null
    }
}
