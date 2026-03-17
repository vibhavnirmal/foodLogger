package com.foodlogger.ui.xml.adapter

import android.net.Uri
import android.widget.PopupMenu
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.foodlogger.R
import com.foodlogger.databinding.ItemStoreBinding
import com.foodlogger.domain.model.Store

class StoreAdapter(
    private val onEdit: (Store) -> Unit,
    private val onDelete: (Store) -> Unit,
    private val onChangeImage: (Store) -> Unit,
    private val onClearImage: (Store) -> Unit,
) : ListAdapter<Store, StoreAdapter.StoreViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreViewHolder {
        val binding = ItemStoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StoreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoreViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StoreViewHolder(
        private val binding: ItemStoreBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        private val menuChangeImageId = 1
        private val menuClearImageId = 2
        private val menuDeleteId = 3

        fun bind(store: Store) {
            binding.storeNameText.text = store.name
            bindStoreImage(store.imageUri, store.name)
            binding.editButton.setOnClickListener { onEdit(store) }
            binding.menuButton.setOnClickListener { showOverflowMenu(store) }
        }

        private fun showOverflowMenu(store: Store) {
            val popup = PopupMenu(binding.root.context, binding.menuButton)
            popup.menu.add(0, menuChangeImageId, 0, R.string.action_select_store_image)
            popup.menu.add(0, menuClearImageId, 1, R.string.action_clear_image)
            popup.menu.add(0, menuDeleteId, 2, R.string.action_delete)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    menuChangeImageId -> {
                        onChangeImage(store)
                        true
                    }
                    menuClearImageId -> {
                        onClearImage(store)
                        true
                    }
                    menuDeleteId -> {
                        onDelete(store)
                        true
                    }
                    else -> false
                }
            }

            popup.show()
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

    private object DiffCallback : DiffUtil.ItemCallback<Store>() {
        override fun areItemsTheSame(oldItem: Store, newItem: Store): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Store, newItem: Store): Boolean = oldItem == newItem
    }
}
