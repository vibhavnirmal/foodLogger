package com.foodlogger.ui.xml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.foodlogger.R
import com.foodlogger.databinding.BottomSheetAddItemBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddItemBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetAddItemBinding? = null
    private val binding get() = _binding!!

    private var itemType: ItemType = ItemType.LOCATION
    private var onAddListener: ((String) -> Unit)? = null

    enum class ItemType {
        LOCATION, STORE, CATEGORY
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        when (itemType) {
            ItemType.LOCATION -> {
                binding.sheetTitle.text = getString(R.string.action_add_location)
                binding.nameInputLayout.hint = getString(R.string.hint_storage_location_name)
                binding.addButton.text = getString(R.string.action_add_location)
            }
            ItemType.STORE -> {
                binding.sheetTitle.text = getString(R.string.action_add_store)
                binding.nameInputLayout.hint = getString(R.string.hint_store_name)
                binding.addButton.text = getString(R.string.action_add_store)
            }
            ItemType.CATEGORY -> {
                binding.sheetTitle.text = getString(R.string.action_add_category)
                binding.nameInputLayout.hint = getString(R.string.hint_category_name)
                binding.addButton.text = getString(R.string.action_add_category)
            }
        }
    }

    private fun setupListeners() {
        binding.addButton.setOnClickListener {
            val name = binding.nameInput.text?.toString().orEmpty().trim()
            if (name.isBlank()) {
                val errorRes = when (itemType) {
                    ItemType.LOCATION -> R.string.validation_required_location_name
                    ItemType.STORE -> R.string.validation_required_store_name
                    ItemType.CATEGORY -> R.string.validation_required_category_name
                }
                binding.nameInputLayout.error = getString(errorRes)
                return@setOnClickListener
            }
            onAddListener?.invoke(name)
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.nameInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.nameInputLayout.error = null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddItemBottomSheet"

        fun newInstance(type: ItemType, onAdd: (String) -> Unit): AddItemBottomSheet {
            return AddItemBottomSheet().apply {
                itemType = type
                onAddListener = onAdd
            }
        }
    }
}
