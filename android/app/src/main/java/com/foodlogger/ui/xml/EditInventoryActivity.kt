package com.foodlogger.ui.xml

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.foodlogger.R
import com.foodlogger.databinding.ActivityEditInventoryBinding
import com.foodlogger.domain.model.InventoryItem
import com.foodlogger.domain.model.Store
import com.foodlogger.ui.viewmodel.InventoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditInventoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditInventoryBinding
    private val viewModel: InventoryViewModel by viewModels()

    private var availableStorageLocations: List<String> = emptyList()
    private var availableStores: List<Store> = emptyList()
    private var itemId: Int = 0
    private var currentItem: InventoryItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditInventoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        itemId = intent.getIntExtra(EXTRA_ITEM_ID, 0)
        if (itemId == 0) {
            Toast.makeText(this, "Invalid item", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupDatePickers()
        setupSaveButton()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupDatePickers() {
        binding.expiryInputEdit.isFocusable = false
        attachDatePicker(binding.expiryInputEdit)
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            saveItem()
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.inventoryItems.collect { items ->
                        currentItem = items.find { it.id == itemId }
                        currentItem?.let { item ->
                            binding.itemNameText.text = item.displayName()
                            binding.quantityInputEdit.setText(item.quantity.formatQuantity())
                            binding.unitInputEdit.setText(item.unit)
                            binding.expiryInputEdit.setText(item.expiryDate?.toLocalDate()?.toString().orEmpty())
                            binding.storageInputEdit.setText(item.storageLocation.orEmpty())
                            binding.boughtFromInputEdit.setText(item.boughtFromStoreName.orEmpty())
                            binding.nameOverrideInputEdit.setText(item.nameOverride.orEmpty())
                            binding.almostFinishedCheckbox.isChecked = item.almostFinished
                            binding.clearExpiryButton.visibility = if (item.expiryDate != null) {
                                android.view.View.VISIBLE
                            } else {
                                android.view.View.GONE
                            }
                        }
                    }
                }
                launch {
                    viewModel.availableStorageLocations.collect { locations ->
                        availableStorageLocations = locations
                        binding.storageInputEdit.setAdapter(
                            ArrayAdapter(this@EditInventoryActivity, android.R.layout.simple_list_item_1, locations)
                        )
                    }
                }
                launch {
                    viewModel.availableStores.collect { stores ->
                        availableStores = stores
                        binding.boughtFromInputEdit.setAdapter(
                            ArrayAdapter(this@EditInventoryActivity, android.R.layout.simple_list_item_1, stores.map { it.name })
                        )
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.saveButton.isEnabled = !isLoading
                    }
                }
            }
        }

        binding.clearExpiryButton.setOnClickListener {
            binding.expiryInputEdit.setText("")
        }
    }

    private fun saveItem() {
        var hasError = false
        val item = currentItem ?: return

        val quantity = binding.quantityInputEdit.text?.toString().orEmpty().toPositiveFloatOrNull()
        val unit = binding.unitInputEdit.text?.toString().orEmpty().trim()

        if (quantity == null) {
            binding.quantityInputLayout.error = getString(R.string.validation_quantity_positive)
            hasError = true
        } else {
            binding.quantityInputLayout.error = null
        }

        if (unit.isBlank()) {
            binding.unitInputLayout.error = getString(R.string.validation_required_unit)
            hasError = true
        } else {
            binding.unitInputLayout.error = null
        }

        if (hasError) return

        val selectedStore = availableStores.firstOrNull {
            it.name == binding.boughtFromInputEdit.text?.toString().orEmpty()
        }

        viewModel.saveInventoryItem(
            item = item,
            quantity = quantity!!,
            unit = unit,
            expiryDate = binding.expiryInputEdit.text?.toString().orEmpty().parseOptionalDateTime(),
            storageLocation = binding.storageInputEdit.text?.toString()?.trim()?.ifEmpty { null },
            boughtFromStoreId = selectedStore?.id,
            nameOverride = binding.nameOverrideInputEdit.text?.toString()?.trim()?.ifEmpty { null },
            almostFinished = binding.almostFinishedCheckbox.isChecked,
        )

        Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        private const val EXTRA_ITEM_ID = "extra_item_id"

        fun newIntent(context: Context, itemId: Int): Intent {
            return Intent(context, EditInventoryActivity::class.java).apply {
                putExtra(EXTRA_ITEM_ID, itemId)
            }
        }
    }
}
