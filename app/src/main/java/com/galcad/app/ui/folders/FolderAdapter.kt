package com.galcad.app.ui.folders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.galcad.app.data.Category
import com.galcad.app.databinding.ItemFolderRowBinding

class FolderAdapter(
    private val folders: List<Category>,
    private val onClick: (Category) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    inner class FolderViewHolder(val binding: ItemFolderRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folders[position]
        holder.binding.folderNameText.text = folder.name
        holder.binding.root.setOnClickListener { onClick(folder) }
    }

    override fun getItemCount() = folders.size
}
