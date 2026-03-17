package com.foodlogger.ui.xml

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.foodlogger.R
import com.foodlogger.databinding.DialogInventoryEditBinding
import com.foodlogger.databinding.DialogInventoryEntryBinding
import com.foodlogger.databinding.FragmentInventoryBinding
import com.foodlogger.domain.model.ExpiryStatus
import com.foodlogger.domain.model.InventoryItem
import com.foodlogger.domain.model.Product
import com.foodlogger.domain.model.Store
import com.foodlogger.ui.viewmodel.InventoryViewModel
import com.foodlogger.ui.xml.adapter.InventoryAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate

@AndroidEntryPoint
class InventoryFragment : Fragment(R.layout.fragment_inventory) {
    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InventoryViewModel by viewModels()
    private lateinit var adapter: InventoryAdapter
    private var availableProducts: List<Product> = emptyList()
    private var availableStorageLocations: List<String> = emptyList()
    private var availableStores: List<Store> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentInventoryBinding.bind(view)

        adapter = InventoryAdapter(
            onClick = ::showEditDialog,
            onDelete = { item -> viewModel.deleteItem(item.id) },
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        setupFilters()
        binding.addButton.setOnClickListener { showCreateDialog() }
        binding.retryButton.setOnClickListener { viewModel.reloadInventory() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.filteredItems.collect { items ->
                        adapter.submitList(items)
                        binding.recyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                        binding.emptyText.visibility = if (items.isEmpty() && !binding.progressBar.isShown && binding.errorGroup.visibility != View.VISIBLE) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.availableProducts.collect { products ->
                        availableProducts = products
                    }
                }
                launch {
                    viewModel.availableStorageLocations.collect { locations ->
                        availableStorageLocations = locations
                    }
                }
                launch {
                    viewModel.availableStores.collect { stores ->
                        availableStores = stores
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.errorMessage.collect { error ->
                        binding.errorGroup.visibility = if (error != null) View.VISIBLE else View.GONE
                        binding.errorText.text = error?.let { getString(R.string.error_prefix, it) }
                    }
                }
            }
        }
    }

    private fun setupFilters() {
        val sortOptions = InventoryViewModel.SortOption.entries
        val sortLabels = listOf("Expiry", "A-Z", "Bought", "Low stock")
        binding.sortDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sortLabels))
        binding.sortDropdown.setText(sortLabels.first(), false)
        binding.sortDropdown.setOnItemClickListener { _, _, position, _ ->
            viewModel.sortItemsBy(sortOptions[position])
        }

        val statusLabels = listOf("All", "Good", "Soon", "Expired")
        binding.statusDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, statusLabels))
        binding.statusDropdown.setText(statusLabels.first(), false)
        binding.statusDropdown.setOnItemClickListener { _, _, position, _ ->
            viewModel.filterByStatus(ExpiryStatus.entries.getOrNull(position - 1))
        }
    }

    private fun showCreateDialog() {
        val dialogBinding = DialogInventoryEntryBinding.inflate(layoutInflater)
        val products = availableProducts

        dialogBinding.expiryInputEdit.isFocusable = false
        dialogBinding.dateBoughtInputEdit.isFocusable = false
        requireContext().attachDatePicker(dialogBinding.expiryInputEdit)
        requireContext().attachDatePicker(dialogBinding.dateBoughtInputEdit)

        val labels = products.map { "${it.name} (${it.barcode})" }
        val storeLabels = availableStores.map { it.name }
        dialogBinding.productDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels))
        dialogBinding.storageInputEdit.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, availableStorageLocations)
        )
        dialogBinding.boughtFromInputEdit.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, storeLabels)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_add_item)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val selectedIndex = labels.indexOf(dialogBinding.productDropdown.text?.toString().orEmpty())
                        val selectedProduct = products.getOrNull(selectedIndex)
                        val selectedStore = availableStores.firstOrNull {
                            it.name == dialogBinding.boughtFromInputEdit.text?.toString().orEmpty()
                        }
                        val quantity = dialogBinding.quantityInputEdit.text?.toString().orEmpty().toPositiveFloatOrNull()
                        val unit = dialogBinding.unitInputEdit.text?.toString().orEmpty().trim()
                        val dateBoughtText = dialogBinding.dateBoughtInputEdit.text?.toString().orEmpty()
                        val isDateBoughtFuture = dateBoughtText.isFutureDateInput()

                        dialogBinding.productInputLayout.error = if (selectedProduct == null) getString(R.string.validation_required_product) else null
                        dialogBinding.quantityInputLayout.error = if (quantity == null) getString(R.string.validation_quantity_positive) else null
                        dialogBinding.unitInputLayout.error = if (unit.isBlank()) getString(R.string.validation_required_unit) else null
                        dialogBinding.dateBoughtInputEdit.error = if (isDateBoughtFuture) getString(R.string.validation_date_bought_not_future) else null

                        if (selectedProduct == null || quantity == null || unit.isBlank() || isDateBoughtFuture) return@setOnClickListener

                        viewModel.createInventoryItem(
                            barcode = selectedProduct.barcode,
                            quantity = quantity,
                            unit = unit,
                            expiryDate = dialogBinding.expiryInputEdit.text?.toString().orEmpty().parseOptionalDateTime(),
                            dateBought = dateBoughtText.parseOptionalDateTime(),
                            storageLocation = dialogBinding.storageInputEdit.text?.toString()?.trim()?.ifEmpty { null },
                            boughtFromStoreId = selectedStore?.id,
                            nameOverride = dialogBinding.nameOverrideInputEdit.text?.toString()?.trim()?.ifEmpty { null },
                        )
                        dialog.dismiss()
                    }
                }
                dialog.show()
            }
    }

    private fun showEditDialog(item: InventoryItem) {
        val dialogBinding = DialogInventoryEditBinding.inflate(layoutInflater)
        dialogBinding.itemNameText.text = item.displayName()
        dialogBinding.quantityInputEdit.setText(item.quantity.formatQuantity())
        dialogBinding.expiryInputEdit.setText(item.expiryDate?.toLocalDate()?.toString().orEmpty())
        dialogBinding.expiryInputEdit.isFocusable = false
        requireContext().attachDatePicker(dialogBinding.expiryInputEdit)
        dialogBinding.storageInputEdit.setText(item.storageLocation.orEmpty())
        dialogBinding.boughtFromInputEdit.setText(item.boughtFromStoreName.orEmpty())
        dialogBinding.nameOverrideInputEdit.setText(item.nameOverride.orEmpty())
        dialogBinding.almostFinishedCheckbox.isChecked = item.almostFinished
        dialogBinding.storageInputEdit.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, availableStorageLocations)
        )
        dialogBinding.boughtFromInputEdit.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, availableStores.map { it.name })
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_save)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val quantity = dialogBinding.quantityInputEdit.text?.toString().orEmpty().toPositiveFloatOrNull()
                        val selectedStore = availableStores.firstOrNull {
                            it.name == dialogBinding.boughtFromInputEdit.text?.toString().orEmpty()
                        }
                        dialogBinding.quantityInputLayout.error = if (quantity == null) getString(R.string.validation_quantity_positive) else null
                        if (quantity == null) return@setOnClickListener

                        viewModel.saveInventoryItem(
                            item = item,
                            quantity = quantity,
                            expiryDate = dialogBinding.expiryInputEdit.text?.toString().orEmpty().parseOptionalDateTime(),
                            storageLocation = dialogBinding.storageInputEdit.text?.toString()?.trim()?.ifEmpty { null },
                            boughtFromStoreId = selectedStore?.id,
                            nameOverride = dialogBinding.nameOverrideInputEdit.text?.toString()?.trim()?.ifEmpty { null },
                            almostFinished = dialogBinding.almostFinishedCheckbox.isChecked,
                        )
                        dialog.dismiss()
                    }
                }
                dialog.show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}