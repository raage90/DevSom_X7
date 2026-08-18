package com.galcad.app.ui.folders

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.galcad.app.R
import com.galcad.app.cache.OfflineCache
import com.galcad.app.data.AudioListItem
import com.galcad.app.data.Category
import com.galcad.app.data.VideoListItem
import com.galcad.app.databinding.FragmentFolderBrowserBinding
import com.galcad.app.network.ApiClient
import com.galcad.app.ui.player.VideoPlayerActivity
import kotlinx.coroutines.launch

/**
 * What gets cached for offline viewing of one folder level: the subfolders
 * plus whichever content list applies (video or audio, never both).
 */
data class FolderPageCache(
    val subfolders: List<Category>,
    val videos: List<VideoListItem>,
    val audio: List<AudioListItem>
)

/**
 * Shows one level of your folder structure (e.g. "Taariikhda Soomaliya" ->
 * "Banadir/Muqdisho" -> "Daynille") plus any videos/audio directly inside
 * the current folder. Tapping a sub-folder pushes a new instance of this
 * same fragment one level deeper.
 */
class FolderBrowserFragment : Fragment() {

    private var _binding: FragmentFolderBrowserBinding? = null
    private val binding get() = _binding!!

    private var mediaType: String = "video"
    private var categoryId: Int? = null
    private var titleArg: String = ""

    companion object {
        private const val ARG_MEDIA_TYPE = "media_type"
        private const val ARG_CATEGORY_ID = "category_id"
        private const val ARG_TITLE = "title"

        fun newInstance(mediaType: String, categoryId: Int?, title: String): FolderBrowserFragment {
            val fragment = FolderBrowserFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_MEDIA_TYPE, mediaType)
                categoryId?.let { putInt(ARG_CATEGORY_ID, it) }
                putString(ARG_TITLE, title)
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaType = arguments?.getString(ARG_MEDIA_TYPE) ?: "video"
        categoryId = arguments?.takeIf { it.containsKey(ARG_CATEGORY_ID) }?.getInt(ARG_CATEGORY_ID)
        titleArg = arguments?.getString(ARG_TITLE) ?: "Video"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFolderBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.folderTitleText.text = titleArg
        binding.folderContentRecycler.layoutManager = LinearLayoutManager(requireContext())
        loadContent()
    }

    private fun cacheKey() = "folder_${mediaType}_${categoryId ?: "root"}"

    private fun loadContent() {
        binding.folderLoadingSpinner.visibility = View.VISIBLE
        binding.folderEmptyText.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tree = ApiClient.get().getCategories(mediaType).body() ?: emptyList()
                val currentId = categoryId
                val subfolders: List<Category> = if (currentId == null) {
                    tree
                } else {
                    findNode(tree, currentId)?.children ?: emptyList()
                }

                var videos: List<VideoListItem> = emptyList()
                var audioItems: List<AudioListItem> = emptyList()
                if (currentId != null) {
                    if (mediaType == "video") {
                        videos = ApiClient.get().getVideos(currentId).body() ?: emptyList()
                    } else {
                        audioItems = ApiClient.get().getAudio(currentId).body() ?: emptyList()
                    }
                }

                OfflineCache.save(requireContext(), cacheKey(), FolderPageCache(subfolders, videos, audioItems))
                renderContent(subfolders, videos, audioItems)
            } catch (e: Exception) {
                // No internet or request failed -- show the last cached
                // version of this exact folder level if we have one.
                val cached = OfflineCache.load<FolderPageCache>(requireContext(), cacheKey())
                if (cached != null) {
                    renderContent(cached.subfolders, cached.videos, cached.audio)
                } else {
                    binding.folderLoadingSpinner.visibility = View.GONE
                    binding.folderEmptyText.visibility = View.VISIBLE
                    binding.folderEmptyText.text = "Couldn't load this folder. Check your connection and try again."
                }
            }
        }
    }

    private fun renderContent(subfolders: List<Category>, videos: List<VideoListItem>, audioItems: List<AudioListItem>) {
        val adapters = mutableListOf<androidx.recyclerview.widget.RecyclerView.Adapter<*>>()

        if (subfolders.isNotEmpty()) {
            adapters.add(FolderAdapter(subfolders) { folder ->
                val next = newInstance(mediaType, folder.id, folder.name)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, next)
                    .addToBackStack(null)
                    .commit()
            })
        }

        if (videos.isNotEmpty()) {
            adapters.add(VideoAdapter(videos) { video ->
                val intent = Intent(requireContext(), VideoPlayerActivity::class.java)
                intent.putExtra(VideoPlayerActivity.EXTRA_ITEM_ID, video.id)
                intent.putExtra(VideoPlayerActivity.EXTRA_IS_AUDIO, false)
                startActivity(intent)
            })
        }

        if (audioItems.isNotEmpty()) {
            adapters.add(AudioAdapter(audioItems) { item ->
                val intent = Intent(requireContext(), VideoPlayerActivity::class.java)
                intent.putExtra(VideoPlayerActivity.EXTRA_ITEM_ID, item.id)
                intent.putExtra(VideoPlayerActivity.EXTRA_IS_AUDIO, true)
                startActivity(intent)
            })
        }

        binding.folderLoadingSpinner.visibility = View.GONE
        if (adapters.isEmpty()) {
            binding.folderEmptyText.visibility = View.VISIBLE
            binding.folderEmptyText.text = "Nothing here yet"
        } else {
            binding.folderContentRecycler.adapter = ConcatAdapter(adapters)
        }
    }

    private fun findNode(nodes: List<Category>, id: Int): Category? {
        for (node in nodes) {
            if (node.id == id) return node
            findNode(node.children, id)?.let { return it }
        }
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
