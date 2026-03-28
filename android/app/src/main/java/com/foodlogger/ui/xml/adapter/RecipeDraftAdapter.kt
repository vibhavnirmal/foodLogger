package com.foodlogger.ui.xml.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.foodlogger.databinding.ItemRecipeDraftBinding
import com.foodlogger.domain.model.RecipeIngredientDraft

class RecipeDraftAdapter(
    private val productsById: () -> Map<Int, com.foodlogger.domain.model.Product>,
    private val onChanged: (List<RecipeIngredientDraft>) -> Unit,
) : RecyclerView.Adapter<RecipeDraftAdapter.RecipeDraftViewHolder>() {

    private val items = mutableListOf<RecipeIngredientDraft>()

    fun submitList(newItems: List<RecipeIngredientDraft>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeDraftViewHolder {
        val binding = ItemRecipeDraftBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecipeDraftViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeDraftViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = items.size

    inner class RecipeDraftViewHolder(
        private val binding: ItemRecipeDraftBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val item = items[position]
            val product = productsById()[item.productId]
            binding.titleText.text = product?.name ?: "Unknown Product"
            binding.barcodeText.text = product?.barcode ?: ""

            binding.removeButton.setOnClickListener {
                val index = bindingAdapterPosition
                if (index != RecyclerView.NO_POSITION) {
                    items.removeAt(index)
                    notifyDataSetChanged()
                    onChanged(items.toList())
                }
            }
        }
    }
}
