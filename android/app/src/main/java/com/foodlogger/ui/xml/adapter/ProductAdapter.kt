package com.foodlogger.ui.xml.adapter

import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.foodlogger.databinding.ItemProductBinding
import com.foodlogger.domain.model.Product

class ProductAdapter(
    private val onClick: (Product) -> Unit,
    private val onDelete: (Product) -> Unit,
    private val getStoreName: (Int) -> String? = { null },
    private val isInInventory: (Int) -> Boolean = { false },
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(
        private val binding: ItemProductBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            val inInventory = isInInventory(product.id)
            binding.titleText.text = product.name
            binding.subtitleText.text = listOfNotNull(product.brand, product.category).joinToString(" • ").ifBlank { product.barcode ?: "No barcode" }
            binding.barcodeText.text = getStoreName(product.id) ?: "Unknown store"
            binding.inventoryStatusIcon.visibility = if (inInventory) View.VISIBLE else View.INVISIBLE
            binding.inventoryStatusIcon.contentDescription = binding.root.context.getString(
                if (inInventory) com.foodlogger.R.string.status_in_inventory else com.foodlogger.R.string.status_not_in_inventory
            )
            binding.root.setOnClickListener { onClick(product) }
            binding.deleteButton.setOnClickListener { onDelete(product) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean = oldItem == newItem
    }
}