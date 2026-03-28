package com.foodlogger.ui.xml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.foodlogger.R
import com.foodlogger.databinding.BottomSheetAddCustomItemBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddCustomItemBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetAddCustomItemBinding? = null
    private val binding get() = _binding!!

    private var onAddListener: ((String, String?) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddCustomItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        binding.sheetTitle.text = getString(R.string.add_missing_item)
        binding.nameInputLayout.hint = getString(R.string.hint_item_name)
        binding.priceInputLayout.hint = getString(R.string.hint_optional_price)
        binding.addButton.text = getString(R.string.action_add)
    }

    private fun setupListeners() {
        binding.addButton.setOnClickListener {
            val name = binding.nameInput.text?.toString().orEmpty().trim()
            if (name.isBlank()) {
                binding.nameInputLayout.error = getString(R.string.validation_required_name)
                return@setOnClickListener
            }
            val priceText = binding.priceInput.text?.toString().orEmpty().trim()
            val price = if (priceText.isBlank()) null else priceText
            onAddListener?.invoke(name, price)
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

        binding.priceInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.priceInputLayout.error = null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddCustomItemBottomSheet"

        fun newInstance(onAdd: (String, String?) -> Unit): AddCustomItemBottomSheet {
            return AddCustomItemBottomSheet().apply {
                onAddListener = onAdd
            }
        }
    }
}