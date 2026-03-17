package com.foodlogger.ui.xml.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.foodlogger.databinding.ItemStorageLocationBinding
import com.foodlogger.domain.model.StorageLocation

class StorageLocationAdapter(
    private val onEdit: (StorageLocation) -> Unit,
    private val onDelete: (StorageLocation) -> Unit,
) : ListAdapter<StorageLocation, StorageLocationAdapter.StorageLocationViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StorageLocationViewHolder {
        val binding = ItemStorageLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StorageLocationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StorageLocationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StorageLocationViewHolder(
        private val binding: ItemStorageLocationBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(location: StorageLocation) {
            binding.locationNameText.text = location.name
            binding.editButton.setOnClickListener { onEdit(location) }
            binding.deleteButton.setOnClickListener { onDelete(location) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<StorageLocation>() {
        override fun areItemsTheSame(oldItem: StorageLocation, newItem: StorageLocation): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: StorageLocation, newItem: StorageLocation): Boolean = oldItem == newItem
    }
}
