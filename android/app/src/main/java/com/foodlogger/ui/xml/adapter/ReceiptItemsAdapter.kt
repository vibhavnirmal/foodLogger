package com.foodlogger.ui.xml.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.foodlogger.databinding.ItemReceiptProductBinding
import com.foodlogger.domain.model.InventoryItem

class ReceiptItemsAdapter : ListAdapter<InventoryItem, ReceiptItemsAdapter.ItemViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemReceiptProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ItemViewHolder(
        private val binding: ItemReceiptProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InventoryItem) {
            binding.productName.text = item.displayName()
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<InventoryItem>() {
        override fun areItemsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean = oldItem == newItem
    }
}
