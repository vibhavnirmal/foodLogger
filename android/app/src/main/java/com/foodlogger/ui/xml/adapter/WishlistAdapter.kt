package com.foodlogger.ui.xml.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.foodlogger.R
import com.foodlogger.databinding.ItemWishlistBinding
import com.foodlogger.domain.model.InventoryItem

class WishlistAdapter(
    private val onToggleChecked: (InventoryItem) -> Unit,
    private val isChecked: (Int) -> Boolean,
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
            val checked = isChecked(item.id)
            binding.checkIcon.visibility = if (checked) View.VISIBLE else View.INVISIBLE
            binding.titleText.alpha = if (checked) 0.6f else 1f
            binding.titleText.paintFlags = if (checked) {
                binding.titleText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.titleText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            binding.root.setOnClickListener { onToggleChecked(item) }
            binding.checkIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(binding.root.context, R.color.expiry_good)
            )
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<InventoryItem>() {
        override fun areItemsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean = oldItem == newItem
    }
}
