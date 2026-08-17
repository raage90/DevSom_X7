package com.galcad.app.ui.player

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.load
import com.galcad.app.databinding.ActivityVideoPlayerBinding
import com.galcad.app.network.ApiClient
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

        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player

        // Audio has no meaningful "fullscreen" -- hide the toggle for it
        if (isAudio) {
            binding.fullscreenToggleButton.visibility = View.GONE
        } else {
            binding.fullscreenToggleButton.setOnClickListener { toggleFullscreen() }
        }

        loadAndPlay(itemId)
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
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

                        player?.addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_ENDED) {
                                    player?.removeListener(this)
                                    loadAndPlay(next.id)
                                }
                            }
                        })
                    }
                }
            } catch (e: Exception) {
                binding.playerLoadingSpinner.visibility = View.GONE
                binding.playerErrorText.visibility = View.VISIBLE
                binding.playerErrorText.text = "Couldn't load this ${if (isAudio) "audio" else "video"}. Check your connection and try again."
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
        player?.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
        _binding = null
    }
}
