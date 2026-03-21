package com.foodlogger.ui.xml.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.foodlogger.databinding.ItemReceiptBinding
import com.foodlogger.domain.model.ReceiptItem

class ReceiptItemAdapter(
    private val onSelectionChanged: (ReceiptItem, Boolean) -> Unit
) : ListAdapter<ReceiptItem, ReceiptItemAdapter.ReceiptViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptViewHolder {
        val binding = ItemReceiptBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReceiptViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReceiptViewHolder(
        private val binding: ItemReceiptBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentItem: ReceiptItem? = null

        init {
            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                currentItem?.let { item ->
                    onSelectionChanged(item, isChecked)
                }
            }
            binding.root.setOnClickListener {
                binding.checkbox.isChecked = !binding.checkbox.isChecked
            }
        }

        fun bind(item: ReceiptItem) {
            currentItem = item
            binding.itemName.text = item.name
            binding.itemPrice.text = item.price?.let { String.format("$%.2f", it) } ?: ""
            
            if (binding.checkbox.isChecked != item.isSelected) {
                binding.checkbox.isChecked = item.isSelected
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ReceiptItem>() {
        override fun areItemsTheSame(oldItem: ReceiptItem, newItem: ReceiptItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ReceiptItem, newItem: ReceiptItem): Boolean = oldItem == newItem
    }
}
