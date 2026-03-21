package com.foodlogger.ui.xml.adapter

import android.content.res.ColorStateList
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
    private val onReceiptClick: ((InventoryItem) -> Unit)? = null,
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
            binding.quantityText.text = item.quantity.formatQuantity()
            binding.unitText.text = item.unit
            
            // Handle expiry display
            val hasExpiry = item.expiryDate != null
            binding.expiryText.text = if (hasExpiry) {
                item.expiryDate.displayDate()?.let { "Expires $it" }
            } else {
                "No expiry"
            }
            binding.boughtFromText.text = item.boughtFromStoreName ?: context.getString(R.string.store_unknown)

            // Set expiry status chip (only for items with expiry)
            if (hasExpiry) {
                binding.expiryStatusChip.visibility = android.view.View.VISIBLE
                val (statusText, chipColor) = when (item.expiryStatus) {
                    ExpiryStatus.GOOD -> context.getString(R.string.status_good) to R.color.expiry_good
                    ExpiryStatus.EXPIRING_SOON -> context.getString(R.string.status_expiring_soon) to R.color.expiry_expiring_soon
                    ExpiryStatus.EXPIRED -> context.getString(R.string.status_expired) to R.color.expiry_expired
                }
                binding.expiryStatusChip.text = statusText
                binding.expiryStatusChip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, chipColor)))
                binding.expiryStatusChip.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                binding.expiryStatusChip.visibility = android.view.View.GONE
            }
            
            // Show item image first, fall back to store image, then initial
            bindItemImage(item.imageUri, item.boughtFromStoreImageUri, item.boughtFromStoreName)

            // Show receipt icon if item has a receipt
            binding.receiptIcon.visibility = if (item.hasReceipt() && onReceiptClick != null) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            binding.receiptIcon.setOnClickListener {
                if (item.hasReceipt()) {
                    onReceiptClick?.invoke(item)
                }
            }

            binding.root.setOnClickListener { onClick(item) }
            binding.deleteButton.setOnClickListener { onDelete(item) }
        }

        private fun bindItemImage(itemImageUri: String?, storeImageUri: String?, storeName: String?) {
            // Priority 1: Item's uploaded image
            if (!itemImageUri.isNullOrBlank()) {
                binding.storeImageView.setImageURI(Uri.parse(itemImageUri))
                if (binding.storeImageView.drawable != null) {
                    binding.storeImageView.visibility = android.view.View.VISIBLE
                    binding.storeInitialText.visibility = android.view.View.GONE
                    return
                }
            }

            // Priority 2: Store's image
            if (!storeImageUri.isNullOrBlank()) {
                if (storeImageUri.startsWith("android.resource://")) {
                    val resName = storeImageUri.substringAfterLast('/')
                    val resId = binding.root.context.resources.getIdentifier(resName, "drawable", binding.root.context.packageName)
                    if (resId != 0) {
                        binding.storeImageView.setImageResource(resId)
                        binding.storeImageView.visibility = android.view.View.VISIBLE
                        binding.storeInitialText.visibility = android.view.View.GONE
                        return
                    }
                }

                binding.storeImageView.setImageURI(Uri.parse(storeImageUri))
                if (binding.storeImageView.drawable != null) {
                    binding.storeImageView.visibility = android.view.View.VISIBLE
                    binding.storeInitialText.visibility = android.view.View.GONE
                    return
                }
            }

            // Priority 3: Store initial
            val initial = storeName?.trim()?.firstOrNull()?.toString()?.uppercase().orEmpty().ifEmpty { "?" }
            binding.storeInitialText.text = initial
            binding.storeImageView.visibility = android.view.View.GONE
            binding.storeInitialText.visibility = android.view.View.VISIBLE
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<InventoryItem>() {
        override fun areItemsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: InventoryItem, newItem: InventoryItem): Boolean = oldItem == newItem
    }
}