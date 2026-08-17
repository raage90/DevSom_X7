package com.galcad.app.ui.news

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.galcad.app.data.NewsItem
import com.galcad.app.databinding.ItemNewsRowBinding

class NewsAdapter(
    private val items: List<NewsItem>,
    private val onClick: (NewsItem) -> Unit
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    inner class NewsViewHolder(val binding: ItemNewsRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val item = items[position]
        holder.binding.newsTitleText.text = item.title
        holder.binding.newsBodyPreview.text = item.body

        if (!item.photoUrl.isNullOrBlank()) {
            holder.binding.newsPhoto.visibility = View.VISIBLE
            holder.binding.newsPhoto.load(item.photoUrl)
        } else {
            holder.binding.newsPhoto.visibility = View.GONE
        }

        val mediaLabel = when {
            item.videoTitle != null -> "🎥 Watch: ${item.videoTitle}"
            item.audioTitle != null -> "🎧 Listen: ${item.audioTitle}"
            else -> null
        }
        if (mediaLabel != null) {
            holder.binding.newsMediaBadge.visibility = View.VISIBLE
            holder.binding.newsMediaBadge.text = mediaLabel
        } else {
            holder.binding.newsMediaBadge.visibility = View.GONE
        }

        holder.binding.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
