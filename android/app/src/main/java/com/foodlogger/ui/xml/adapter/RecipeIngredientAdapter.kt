package com.foodlogger.ui.xml.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.foodlogger.databinding.ItemRecipeIngredientBinding
import com.foodlogger.domain.model.RecipeIngredient
import com.foodlogger.ui.xml.formatQuantity

class RecipeIngredientAdapter : ListAdapter<RecipeIngredient, RecipeIngredientAdapter.RecipeIngredientViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeIngredientViewHolder {
        val binding = ItemRecipeIngredientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecipeIngredientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeIngredientViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecipeIngredientViewHolder(
        private val binding: ItemRecipeIngredientBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RecipeIngredient) {
            binding.titleText.text = item.productName
            binding.barcodeText.visibility = View.GONE
            binding.quantityText.text = "${item.quantity.formatQuantity()} ${item.unit}"
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<RecipeIngredient>() {
        override fun areItemsTheSame(oldItem: RecipeIngredient, newItem: RecipeIngredient): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: RecipeIngredient, newItem: RecipeIngredient): Boolean = oldItem == newItem
    }
}