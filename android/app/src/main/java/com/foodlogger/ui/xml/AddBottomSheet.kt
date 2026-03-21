package com.foodlogger.ui.xml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.foodlogger.R
import com.foodlogger.databinding.BottomSheetAddBinding
import com.foodlogger.domain.model.Product
import com.foodlogger.ui.viewmodel.InventoryViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@AndroidEntryPoint
class AddBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetAddBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InventoryViewModel by viewModels()

    private var availableProducts: List<Product> = emptyList()
    private var availableStorageLocations: List<String> = emptyList()
    private var selectedProduct: Product? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupProductDropdown()
        setupDatePicker()
        setupButtons()
        observeData()
    }

    private fun setupProductDropdown() {
        binding.productDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedProduct = availableProducts.getOrNull(position)
        }
    }

    private fun setupDatePicker() {
        binding.expiryInput.setOnClickListener {
            showDatePicker { date ->
                binding.expiryInput.setText(date.toString())
            }
        }

        binding.clearExpiryButton.setOnClickListener {
            binding.expiryInput.setText("")
        }
    }

    private fun showDatePicker(onDateSelected: (LocalDateTime) -> Unit) {
        val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder
            .datePicker()
            .setTitleText("Select expiry date")
            .build()

        datePicker.addOnPositiveButtonClickListener { millis ->
            val date = java.time.Instant.ofEpochMilli(millis)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            onDateSelected(date.atStartOfDay())
        }

        datePicker.show(parentFragmentManager, "datePicker")
    }

    private fun setupButtons() {
        binding.saveButton.setOnClickListener {
            saveItem()
        }

        binding.fabScan.setOnClickListener {
            Toast.makeText(context, "Scan feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.availableProducts.collect { products ->
                        availableProducts = products
                        val labels = products.map { it.name }
                        binding.productDropdown.setAdapter(
                            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels)
                        )
                    }
                }
                launch {
                    viewModel.availableStorageLocations.collect { locations ->
                        availableStorageLocations = locations
                        binding.storageDropdown.setAdapter(
                            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, locations)
                        )
                    }
                }
            }
        }
    }

    private fun saveItem() {
        var hasError = false

        val productName = binding.productNameInput.text?.toString()?.trim().orEmpty()
        val brand = binding.brandInput.text?.toString()?.trim()?.ifEmpty { null }

        val product: Product? = if (productName.isNotEmpty()) {
            Product(
                barcode = null,
                name = productName,
                brand = brand,
                category = null,
                servingSize = null,
                kcal = null,
                protein = null,
                carbs = null,
                fat = null
            )
        } else {
            selectedProduct
        }

        if (product == null) {
            binding.productInputLayout.error = getString(R.string.validation_required_product)
            hasError = true
        } else {
            binding.productInputLayout.error = null
        }

        val quantityText = binding.quantityInput.text?.toString().orEmpty()
        val quantity = quantityText.toPositiveFloatOrNull()
        if (quantity == null) {
            binding.quantityInputLayout.error = getString(R.string.validation_quantity_positive)
            hasError = true
        } else {
            binding.quantityInputLayout.error = null
        }

        val unit = binding.unitInput.text?.toString()?.trim().orEmpty().orEmpty()
        if (unit.isBlank()) {
            binding.unitInputLayout.error = getString(R.string.validation_required_unit)
            hasError = true
        } else {
            binding.unitInputLayout.error = null
        }

        if (hasError) return

        val expiryText = binding.expiryInput.text?.toString().orEmpty()
        val expiryDate = expiryText.parseOptionalDateTime()
        val storageLocation = binding.storageDropdown.text?.toString()?.trim()?.ifEmpty { null }

        viewModel.createInventoryItemWithProduct(
            product = product!!,
            quantity = quantity!!,
            unit = unit,
            expiryDate = expiryDate,
            dateBought = LocalDateTime.now(),
            storageLocation = storageLocation,
            boughtFromStoreId = null,
            nameOverride = null,
            imageUri = null
        )

        Toast.makeText(context, "Item added", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    private fun String.toPositiveFloatOrNull(): Float? {
        return toFloatOrNull()?.takeIf { it > 0 }
    }

    private fun String.parseOptionalDateTime(): LocalDateTime? {
        return runCatching {
            java.time.LocalDate.parse(trim()).atStartOfDay()
        }.getOrNull()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddBottomSheet"
    }
}
