package com.foodlogger.ui.xml.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.foodlogger.data.db.ReceiptEntity
import com.foodlogger.databinding.ItemReceiptHistoryBinding
import java.io.File
import java.time.format.DateTimeFormatter

class ReceiptHistoryAdapter(
    private val onReceiptClick: (ReceiptEntity) -> Unit,
    private val getStoreName: ((Int?) -> String?) = { null }
) : ListAdapter<ReceiptEntity, ReceiptHistoryAdapter.ReceiptViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptViewHolder {
        val binding = ItemReceiptHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReceiptViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReceiptViewHolder(
        private val binding: ItemReceiptHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

        fun bind(receipt: ReceiptEntity) {
            binding.root.setOnClickListener { onReceiptClick(receipt) }

            val displayDate = receipt.dateShopped ?: receipt.dateScanned
            binding.receiptDate.text = displayDate.format(dateFormatter)

            val storeName = getStoreName(receipt.storeId)
            binding.receiptStore.text = storeName ?: "Unknown Store"

            binding.receiptItemCount.text = "Scanned: ${receipt.dateScanned.format(dateFormatter)}"

            val imageFile = File(receipt.imagePath)
            if (imageFile.exists()) {
                try {
                    val uri = FileProvider.getUriForFile(
                        binding.root.context,
                        "${binding.root.context.packageName}.provider",
                        imageFile
                    )
                    binding.receiptThumbnail.setImageURI(uri)
                    binding.receiptThumbnail.visibility = View.VISIBLE
                    binding.receiptPlaceholder.visibility = View.GONE
                } catch (e: Exception) {
                    binding.receiptThumbnail.visibility = View.GONE
                    binding.receiptPlaceholder.visibility = View.VISIBLE
                }
            } else {
                binding.receiptThumbnail.visibility = View.GONE
                binding.receiptPlaceholder.visibility = View.VISIBLE
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ReceiptEntity>() {
        override fun areItemsTheSame(oldItem: ReceiptEntity, newItem: ReceiptEntity): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ReceiptEntity, newItem: ReceiptEntity): Boolean = oldItem == newItem
    }
}
