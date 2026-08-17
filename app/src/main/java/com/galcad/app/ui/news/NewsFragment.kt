package com.galcad.app.ui.news

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.galcad.app.data.NewsItem
import com.galcad.app.databinding.FragmentNewsBinding
import com.galcad.app.network.ApiClient
import com.galcad.app.ui.player.VideoPlayerActivity
import kotlinx.coroutines.launch

class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.newsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.newsSwipeRefresh.setOnRefreshListener { loadNews() }
        loadNews()
    }

    private fun loadNews() {
        binding.newsSwipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val items = ApiClient.get().getNews().body() ?: emptyList()
                binding.newsRecycler.adapter = NewsAdapter(items) { item -> onNewsClicked(item) }
            } catch (e: Exception) {
                // keep whatever was showing before; a silent failure here is
                // better than crashing the whole News tab
            } finally {
                binding.newsSwipeRefresh.isRefreshing = false
            }
        }
    }

    private fun onNewsClicked(item: NewsItem) {
        countNewsViewSilently(item.id)
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
                AlertDialog.Builder(requireContext())
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

    private fun countNewsViewSilently(newsId: Int) {
        // Fire-and-forget: GET /api/news/:id is what increments the view
        // counter server-side. We don't need its response here since the
        // player screens already have everything they need from the list item.
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                ApiClient.get().getNewsDetail(newsId)
            } catch (e: Exception) {
                // a missed view count is not worth surfacing an error for
            }
        }
    }
}
