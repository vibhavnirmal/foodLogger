package com.foodlogger.ui.xml.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.foodlogger.R
import com.foodlogger.databinding.ItemReceiptFooterBinding

class ReceiptReviewFooterAdapter(
    private val onDateClick: () -> Unit,
    private val onStoreNameChanged: (String) -> Unit,
    private val onStoreConfirmed: (String) -> Unit,
    private val onTotalAmountConfirmed: (String) -> Unit,
    private val onAddToInventoryClick: () -> Unit,
) : RecyclerView.Adapter<ReceiptReviewFooterAdapter.FooterViewHolder>() {

    private var dateText: String = "Today"
    private var selectedStoreName: String = ""
    private var isStoreConfirmed: Boolean = false
    private var addButtonText: String = "Add to Inventory"
    private var addButtonEnabled: Boolean = false
    private var availableStoreNames: List<String> = emptyList()
    private var totalAmountText: String = ""
    private var isTotalAmountConfirmed: Boolean = false
    private var selectedTotalAmount: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        val binding = ItemReceiptFooterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FooterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FooterViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int = 1

    fun updateDateText(value: String) {
        dateText = value
        notifyItemChanged(0)
    }

    fun updateStores(selectedName: String) {
        selectedStoreName = selectedName
        isStoreConfirmed = selectedName.isNotEmpty()
        notifyItemChanged(0)
    }

    fun updateAvailableStores(names: List<String>) {
        availableStoreNames = names
        notifyItemChanged(0)
    }

    fun updateTotalAmount(amount: String) {
        totalAmountText = amount
        selectedTotalAmount = amount
        isTotalAmountConfirmed = amount.isNotEmpty()
        notifyItemChanged(0)
    }

    fun updateTotalAmountConfirmation(confirmed: Boolean) {
        isTotalAmountConfirmed = confirmed
        notifyItemChanged(0)
    }

    fun updateAddButton(text: String, enabled: Boolean) {
        addButtonText = text
        addButtonEnabled = enabled
        notifyItemChanged(0)
    }

    inner class FooterViewHolder(
        private val binding: ItemReceiptFooterBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            binding.datePickerButton.text = dateText
            binding.datePickerButton.setOnClickListener { onDateClick() }

            val currentText = binding.storeDropdown.text?.toString() ?: ""
            val displayText = if (isStoreConfirmed) selectedStoreName else ""
            
            if (currentText != displayText) {
                binding.storeDropdown.setText(displayText)
            }

            binding.storeDropdown.isEnabled = !isStoreConfirmed

            // Set up autocomplete adapter
            val adapter = ArrayAdapter(
                binding.root.context,
                android.R.layout.simple_dropdown_item_1line,
                availableStoreNames
            )
            binding.storeDropdown.setAdapter(adapter)

            // Show dropdown when field is focused
            binding.storeDropdown.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && !isStoreConfirmed) {
                    binding.storeDropdown.showDropDown()
                }
            }

            // Handle selection from dropdown
            binding.storeDropdown.setOnItemClickListener { parent, _, position, _ ->
                val selectedItem = parent.getItemAtPosition(position) as String
                selectedStoreName = selectedItem
                isStoreConfirmed = true
                binding.storeDropdown.isEnabled = false
                binding.storeDropdown.setText(selectedItem)
                binding.storeDropdown.clearFocus()
                onStoreConfirmed(selectedItem)
                notifyItemChanged(0)
            }

            binding.storeDropdown.doAfterTextChanged { text ->
                binding.confirmStoreButton.isEnabled = !text.isNullOrBlank()
            }

            binding.confirmStoreButton.setIconResource(
                if (isStoreConfirmed) R.drawable.baseline_mode_edit_24 else R.drawable.baseline_check_24
            )

            binding.confirmStoreButton.setOnClickListener {
                val storeInputText = binding.storeDropdown.text?.toString()?.trim() ?: ""
                
                if (isStoreConfirmed) {
                    isStoreConfirmed = false
                    binding.storeDropdown.isEnabled = true
                    binding.storeDropdown.requestFocus()
                } else {
                    if (storeInputText.isNotEmpty()) {
                        selectedStoreName = storeInputText
                        isStoreConfirmed = true
                        binding.storeDropdown.isEnabled = false
                        binding.storeDropdown.clearFocus()
                        onStoreConfirmed(storeInputText)
                    }
                }
                notifyItemChanged(0)
            }

            // Total amount input
            val displayTotalText = if (isTotalAmountConfirmed) selectedTotalAmount else totalAmountText
            val currentTotalText = binding.totalAmountInput.text?.toString() ?: ""
            if (currentTotalText != displayTotalText) {
                binding.totalAmountInput.setText(displayTotalText)
            }
            binding.totalAmountInput.isEnabled = !isTotalAmountConfirmed

            binding.totalAmountInput.doAfterTextChanged { text ->
                if (!isTotalAmountConfirmed) {
                    totalAmountText = text?.toString()?.trim() ?: ""
                }
            }

            binding.confirmTotalAmountButton.setIconResource(
                if (isTotalAmountConfirmed) R.drawable.baseline_mode_edit_24 else R.drawable.baseline_check_24
            )

            binding.confirmTotalAmountButton.setOnClickListener {
                val currentInput = binding.totalAmountInput.text?.toString()?.trim() ?: ""
                if (isTotalAmountConfirmed) {
                    // Edit mode
                    isTotalAmountConfirmed = false
                    totalAmountText = currentInput
                    binding.totalAmountInput.isEnabled = true
                    binding.totalAmountInput.requestFocus()
                } else {
                    // Confirm mode
                    selectedTotalAmount = currentInput
                    isTotalAmountConfirmed = true
                    binding.totalAmountInput.isEnabled = false
                    binding.totalAmountInput.clearFocus()
                    onTotalAmountConfirmed(currentInput)
                }
                notifyItemChanged(0)
            }

            binding.addToInventoryButton.text = addButtonText
            binding.addToInventoryButton.isEnabled = addButtonEnabled
            binding.addToInventoryButton.setOnClickListener { onAddToInventoryClick() }
        }
    }
}
