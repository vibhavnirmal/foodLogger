package com.foodlogger.ui.xml.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.foodlogger.databinding.ItemWishlistBinding
import com.foodlogger.domain.model.InventoryItem
import com.foodlogger.ui.xml.displayDate
import com.foodlogger.ui.xml.formatQuantity

class WishlistAdapter(
    private val onMarkFinished: (InventoryItem) -> Unit,
    private val onDelete: (InventoryItem) -> Unit,
) : ListAdapter<InventoryItem, WishlistAdapter.WishlistViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistViewHolder {
        val binding = ItemWishlistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WishlistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WishlistViewHolder(
        private val binding: ItemWishlistBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InventoryItem) {
            binding.titleText.text = item.displayName()
            binding.subtitleText.text = item.expiryDate.displayDate()?.let { "Expires: $it" } ?: "Expiry not set"
            binding.quantityText.text = "${item.quantity.formatQuantity()} ${item.unit}"
            binding.finishButton.setOnClickListener { onMarkFinished(item) }
            binding.deleteButton.setOnClickListener { onDelete(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<InventoryItem>() {
        override fun areItemsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean = oldItem == newItem
    }
}