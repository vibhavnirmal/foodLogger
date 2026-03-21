package com.foodlogger.ui.xml.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.foodlogger.databinding.ItemRecipeDraftBinding
import com.foodlogger.domain.model.Product
import com.foodlogger.domain.model.RecipeIngredientDraft
import com.foodlogger.ui.xml.formatQuantity
import com.foodlogger.ui.xml.toPositiveFloatOrNull

class RecipeDraftAdapter(
    private val productsById: () -> Map<Int, Product>,
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
            binding.quantityInput.setText(item.quantity.formatQuantity())
            binding.unitInput.setText(item.unit)

            binding.decreaseButton.setOnClickListener {
                val current = items[bindingAdapterPosition].quantity
                updateItem(bindingAdapterPosition, items[bindingAdapterPosition].copy(quantity = (current - 1f).coerceAtLeast(0.5f)))
            }
            binding.increaseButton.setOnClickListener {
                val current = items[bindingAdapterPosition].quantity
                updateItem(bindingAdapterPosition, items[bindingAdapterPosition].copy(quantity = current + 1f))
            }
            binding.removeButton.setOnClickListener {
                val index = bindingAdapterPosition
                if (index != RecyclerView.NO_POSITION) {
                    items.removeAt(index)
                    notifyDataSetChanged()
                    onChanged(items.toList())
                }
            }
            binding.quantityInput.doAfterTextChanged { editable ->
                val index = bindingAdapterPosition
                if (index == RecyclerView.NO_POSITION) return@doAfterTextChanged
                val parsed = editable?.toString().orEmpty().toPositiveFloatOrNull() ?: return@doAfterTextChanged
                updateItem(index, items[index].copy(quantity = parsed))
            }
            binding.unitInput.doAfterTextChanged { editable ->
                val index = bindingAdapterPosition
                if (index == RecyclerView.NO_POSITION) return@doAfterTextChanged
                val unit = editable?.toString().orEmpty()
                updateItem(index, items[index].copy(unit = unit))
            }
        }
    }

    private fun updateItem(index: Int, item: RecipeIngredientDraft) {
        if (index !in items.indices) return
        items[index] = item
        onChanged(items.toList())
    }
}