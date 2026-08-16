package com.tijaabo.app.ui.folders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tijaabo.app.data.AudioListItem
import com.tijaabo.app.databinding.ItemAudioRowBinding

class AudioAdapter(
    private val items: List<AudioListItem>,
    private val onClick: (AudioListItem) -> Unit
) : RecyclerView.Adapter<AudioAdapter.AudioViewHolder>() {

    inner class AudioViewHolder(val binding: ItemAudioRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val binding = ItemAudioRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AudioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        val item = items[position]
        holder.binding.audioTitleText.text = item.title
        holder.binding.audioViewsText.text = "${item.views} plays"
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
