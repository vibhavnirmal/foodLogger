package com.foodlogger.ui.xml.adapter

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

class InventoryAdapter(
    private val onClick: (InventoryItem) -> Unit,
    private val onAddToShoppingList: (InventoryItem) -> Unit,
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
            val actionIconRes = if (item.almostFinished) {
                R.drawable.action_remove_from_shopping_list
            } else {
                R.drawable.baseline_add_shopping_cart_24
            }
            binding.shoppingCart.setIconResource(actionIconRes)
            binding.shoppingCart.contentDescription = context.getString(
                if (item.almostFinished) R.string.action_remove_from_shopping_list else R.string.action_add_to_shopping_list
            )
            binding.shoppingCart.isEnabled = true
            
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
                val borderColor = when (item.expiryStatus) {
                    ExpiryStatus.GOOD -> context.getString(R.string.status_good) to R.color.expiry_good
                    ExpiryStatus.EXPIRING_SOON -> context.getString(R.string.status_expiring_soon) to R.color.expiry_expiring_soon
                    ExpiryStatus.EXPIRED -> context.getString(R.string.status_expired) to R.color.expiry_expired
                }.second
                binding.expiryLeftBorder.visibility = android.view.View.VISIBLE
                binding.expiryLeftBorder.setBackgroundColor(ContextCompat.getColor(context, borderColor))
            } else {
                binding.expiryLeftBorder.visibility = android.view.View.GONE
            }
            
            // Show item image, otherwise fallback to the first letter of item name.
            bindItemImage(item.imageUri, item.displayName())

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
            binding.shoppingCart.setOnClickListener { onAddToShoppingList(item) }
        }

        private fun bindItemImage(itemImageUri: String?, itemName: String) {
            // Priority 1: Item's uploaded image.
            if (!itemImageUri.isNullOrBlank()) {
                binding.storeImageView.setImageURI(Uri.parse(itemImageUri))
                if (binding.storeImageView.drawable != null) {
                    binding.storeImageView.visibility = android.view.View.VISIBLE
                    binding.storeInitialText.visibility = android.view.View.GONE
                    return
                }
            }

            // Priority 2: Fallback to the first letter of item name.
            val initial = itemName.trim().firstOrNull()?.toString()?.uppercase().orEmpty().ifEmpty { "?" }
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