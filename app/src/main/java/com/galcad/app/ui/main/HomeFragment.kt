package com.galcad.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.galcad.app.data.NewsItem
import com.galcad.app.databinding.FragmentHomeBinding
import com.galcad.app.network.ApiClient
import com.galcad.app.ui.news.NewsAdapter
import com.galcad.app.ui.player.VideoPlayerActivity
import kotlinx.coroutines.launch

/**
 * Home currently shows the same feed as the News tab -- since every news
 * post can carry an attached video, audio, or photo, this already covers
 * "mixed feed" for now. A dedicated /api/home endpoint that also mixes in
 * standalone folder videos (not just ones linked from a news post) is a
 * natural next upgrade once you have enough content to make that useful --
 * flagging this honestly rather than pretending it's a separate system today.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.newsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.newsSwipeRefresh.setOnRefreshListener { loadFeed() }
        loadFeed()
    }

    private fun loadFeed() {
        binding.newsSwipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val items = ApiClient.get().getNews().body() ?: emptyList()
                binding.newsRecycler.adapter = NewsAdapter(items) { item -> onItemClicked(item) }
            } catch (e: Exception) {
                // keep whatever was already showing rather than crash the tab
            } finally {
                binding.newsSwipeRefresh.isRefreshing = false
            }
        }
    }

    private fun onItemClicked(item: NewsItem) {
        when {
            item.videoId != null -> {
                val intent = Intent(requireContext(), VideoPlayerActivity::class.java)
                intent.putExtra(VideoPlayerActivity.EXTRA_ITEM_ID, item.videoId)
                intent.putExtra(VideoPlayerActivity.EXTRA_IS_AUDIO, false)
                startActivity(intent)
            }
            item.audioId != null -> {
                val intent = Intent(requireContext(), VideoPlayerActivity::class.java)
                intent.putExtra(VideoPlayerActivity.EXTRA_ITEM_ID, item.audioId)
                intent.putExtra(VideoPlayerActivity.EXTRA_IS_AUDIO, true)
                startActivity(intent)
            }
            else -> {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle(item.title)
                    .setMessage(item.body)
                    .setPositiveButton("Close", null)
                    .show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
