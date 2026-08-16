package com.tijaabo.app.ui.player

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tijaabo.app.databinding.ActivityVideoPlayerBinding
import com.tijaabo.app.network.ApiClient
import kotlinx.coroutines.launch

/**
 * Plays one video or audio item via its signed, expiring Bunny URL.
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

        // Video looks best in landscape; audio doesn't need orientation locking
        if (!isAudio) {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player

        loadAndPlay(itemId)
    }

    private fun loadAndPlay(itemId: Int) {
        binding.playerLoadingSpinner.visibility = View.VISIBLE
        binding.playerErrorText.visibility = View.GONE

        lifecycleScope.launch {
            try {
                if (isAudio) {
                    val response = ApiClient.get().playAudio(itemId)
                    val data = response.body()
                    if (!response.isSuccessful || data == null) throw Exception("Playback unavailable")
                    playUrl(data.playbackUrl)
                } else {
                    val response = ApiClient.get().playVideo(itemId)
                    val data = response.body()
                    if (!response.isSuccessful || data == null) throw Exception("Playback unavailable")
                    playUrl(data.playbackUrl)

                    // Autoplay-next: when this video ends, load the next one
                    // in the same folder automatically, YouTube-style.
                    val nextVideoId = data.nextVideo?.id
                    if (nextVideoId != null) {
                        player?.addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_ENDED) {
                                    player?.removeListener(this)
                                    loadAndPlay(nextVideoId)
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
