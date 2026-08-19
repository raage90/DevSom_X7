package com.galcad.app.ui.video

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.galcad.app.R
import com.galcad.app.cache.OfflineCache
import com.galcad.app.data.VideoListItem
import com.galcad.app.databinding.FragmentAllVideosBinding
import com.galcad.app.network.ApiClient
import com.galcad.app.ui.folders.FolderBrowserFragment
import com.galcad.app.ui.folders.VideoAdapter
import com.galcad.app.ui.player.VideoPlayerActivity
import kotlinx.coroutines.launch

/**
 * The default view when opening the Video tab: a single flat feed of every
 * published video, newest first -- YouTube-style, no folder navigation
 * required. "Browse by folder" at the top still leads into the existing
 * folder structure for anyone organizing/finding things by category.
 */
class AllVideosFragment : Fragment() {

    companion object {
        private const val CACHE_KEY = "all_videos_feed"
    }

    private var _binding: FragmentAllVideosBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAllVideosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.allVideosRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.allVideosSwipeRefresh.setOnRefreshListener { loadVideos() }

        binding.browseFoldersRow.setOnClickListener {
            val fragment = FolderBrowserFragment.newInstance(mediaType = "video", categoryId = null, title = "Video Folders")
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        loadVideos()
    }

    private fun loadVideos() {
        binding.allVideosLoadingSpinner.visibility = View.VISIBLE
        binding.allVideosEmptyText.visibility = View.GONE
        binding.allVideosSwipeRefresh.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val videos = ApiClient.get().getAllVideos().body() ?: emptyList()
                OfflineCache.save(requireContext(), CACHE_KEY, videos)
                render(videos)
            } catch (e: Exception) {
                val cached = OfflineCache.load<List<VideoListItem>>(requireContext(), CACHE_KEY)
                if (cached != null) render(cached) else showEmpty()
            } finally {
                binding.allVideosLoadingSpinner.visibility = View.GONE
                binding.allVideosSwipeRefresh.isRefreshing = false
            }
        }
    }

    private fun render(videos: List<VideoListItem>) {
        if (videos.isEmpty()) {
            showEmpty()
            return
        }
        binding.allVideosEmptyText.visibility = View.GONE
        binding.allVideosRecycler.adapter = VideoAdapter(videos) { video ->
            val intent = Intent(requireContext(), VideoPlayerActivity::class.java)
            intent.putExtra(VideoPlayerActivity.EXTRA_ITEM_ID, video.id)
            intent.putExtra(VideoPlayerActivity.EXTRA_IS_AUDIO, false)
            startActivity(intent)
        }
    }

    private fun showEmpty() {
        binding.allVideosEmptyText.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
