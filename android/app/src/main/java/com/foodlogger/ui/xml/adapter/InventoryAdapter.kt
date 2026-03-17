package com.foodlogger.ui.xml.adapter

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.foodlogger.R
import com.foodlogger.databinding.ItemInventoryBinding
import com.foodlogger.domain.model.ExpiryStatus
import com.foodlogger.domain.model.InventoryItem
import com.foodlogger.ui.xml.displayDate
import com.foodlogger.ui.xml.formatQuantity

class InventoryAdapter(
    private val onClick: (InventoryItem) -> Unit,
    private val onDelete: (InventoryItem) -> Unit,
) : ListAdapter<InventoryItem, InventoryAdapter.InventoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val binding = ItemInventoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InventoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class InventoryViewHolder(
        private val binding: ItemInventoryBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InventoryItem) {
            val context = binding.root.context
            binding.titleText.text = item.displayName()
            binding.quantityText.text = "${item.quantity.formatQuantity()} ${item.unit}"
            binding.expiryText.text = item.expiryDate.displayDate()?.let { "Expires: $it" } ?: "No expiry date"
            binding.boughtFromText.text = context.getString(
                R.string.bought_from_label,
                item.boughtFromStoreName ?: context.getString(R.string.store_unknown)
            )

            val statusColor = when (item.expiryStatus) {
                ExpiryStatus.GOOD -> ContextCompat.getColor(context, R.color.expiry_green)
                ExpiryStatus.EXPIRING_SOON -> ContextCompat.getColor(context, R.color.expiry_orange)
                ExpiryStatus.EXPIRED -> ContextCompat.getColor(context, R.color.expiry_red)
            }
            binding.expiryText.setTextColor(statusColor)
            binding.boughtFromText.setTextColor(Color.DKGRAY)
            bindStoreImage(item.boughtFromStoreImageUri, item.boughtFromStoreName)

            binding.root.setOnClickListener { onClick(item) }
            binding.deleteButton.setOnClickListener { onDelete(item) }
        }

        private fun bindStoreImage(imageUri: String?, storeName: String?) {
            val initial = storeName?.trim()?.firstOrNull()?.toString()?.uppercase().orEmpty().ifEmpty { "?" }
            binding.storeInitialText.text = initial

            if (imageUri.isNullOrBlank()) {
                binding.storeImageView.visibility = android.view.View.GONE
                binding.storeInitialText.visibility = android.view.View.VISIBLE
                return
            }

            if (imageUri.startsWith("android.resource://")) {
                val resName = imageUri.substringAfterLast('/')
                val resId = binding.root.context.resources.getIdentifier(resName, "drawable", binding.root.context.packageName)
                if (resId != 0) {
                    binding.storeImageView.setImageResource(resId)
                    binding.storeImageView.visibility = android.view.View.VISIBLE
                    binding.storeInitialText.visibility = android.view.View.GONE
                    return
                }
            }

            binding.storeImageView.setImageURI(Uri.parse(imageUri))
            if (binding.storeImageView.drawable == null) {
                binding.storeImageView.visibility = android.view.View.GONE
                binding.storeInitialText.visibility = android.view.View.VISIBLE
            } else {
                binding.storeImageView.visibility = android.view.View.VISIBLE
                binding.storeInitialText.visibility = android.view.View.GONE
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<InventoryItem>() {
        override fun areItemsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean = oldItem == newItem
    }
}