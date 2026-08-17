package com.galcad.app.ui.folders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.galcad.app.data.VideoListItem
import com.galcad.app.databinding.ItemVideoRowBinding

class VideoAdapter(
    private val videos: List<VideoListItem>,
    private val onClick: (VideoListItem) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    inner class VideoViewHolder(val binding: ItemVideoRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.binding.videoTitleText.text = video.title
        holder.binding.videoViewsText.text = "${video.views} views"
        if (!video.thumbnailUrl.isNullOrBlank()) {
            holder.binding.videoThumbnail.load(video.thumbnailUrl)
        }
        holder.binding.root.setOnClickListener { onClick(video) }
    }

    override fun getItemCount() = videos.size
}
