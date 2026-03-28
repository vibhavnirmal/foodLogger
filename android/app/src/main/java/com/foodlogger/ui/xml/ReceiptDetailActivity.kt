package com.foodlogger.ui.xml

import android.app.Dialog
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.foodlogger.R
import com.foodlogger.databinding.ActivityReceiptDetailBinding
import com.foodlogger.databinding.DialogReceiptImagePreviewBinding
import com.foodlogger.domain.model.Store
import com.foodlogger.ui.viewmodel.ReceiptDetailViewModel
import com.foodlogger.ui.xml.adapter.ReceiptItemsAdapter
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.TimeZone

@AndroidEntryPoint
class ReceiptDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReceiptDetailBinding
    private val viewModel: ReceiptDetailViewModel by viewModels()
    private lateinit var itemsAdapter: ReceiptItemsAdapter

    private var storeAdapter: ArrayAdapter<String>? = null
    private var stores: List<Store> = emptyList()
    private var selectedDate: LocalDate? = null

    private var originalStoreName: String = ""
    private var originalTotalAmount: String = ""
    private var originalDate: LocalDate? = null
    private var receiptImageUri: Uri? = null

    companion object {
        const val EXTRA_RECEIPT_ID = "extra_receipt_id"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiptDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupSystemUI()
        setupToolbar()
        setupItemsList()
        setupStoreDropdown()
        setupDatePicker()
        setupButtons()
        setupReceiptImagePreview()
        setupAddMissingItem()
        setupObservers()

        val receiptId = intent.getIntExtra(EXTRA_RECEIPT_ID, -1)
        if (receiptId == -1) {
            finish()
            return
        }

        viewModel.loadReceipt(receiptId)
    }

    private fun setupSystemUI() {
        val isDarkMode = (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        window.insetsController?.setSystemBarsAppearance(
            if (isDarkMode) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        )
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupItemsList() {
        itemsAdapter = ReceiptItemsAdapter()
        binding.itemsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.itemsRecyclerView.adapter = itemsAdapter
    }

    private fun setupStoreDropdown() {
        storeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf<String>()
        )
        binding.storeNameInput.setAdapter(storeAdapter)

        binding.storeNameInput.setOnClickListener {
            binding.storeNameInput.showDropDown()
        }
        binding.storeNameInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.storeNameInput.showDropDown()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupDatePicker() {
        binding.dateShoppedInput.setOnClickListener {
            showDatePicker()
        }
        binding.dateShoppedLayout.setEndIconOnClickListener {
            showDatePicker()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePicker() {
        val currentSelection =
            selectedDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
                ?: System.currentTimeMillis()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.label_date_shopped))
            .setSelection(currentSelection)
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            val date = Instant.ofEpochMilli(millis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
            selectedDate = date
            updateDateDisplay(date)
        }

        picker.show(supportFragmentManager, "date_picker")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateDateDisplay(date: LocalDate) {
        val today = LocalDate.now()
        val displayText = if (date == today) {
            "Today"
        } else {
            date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
        binding.dateShoppedInput.setText(displayText)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupButtons() {
        binding.saveButton.setOnClickListener {
            saveReceipt()
        }

        binding.deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }

        binding.storeNameInput.doAfterTextChanged {
            updateSaveButtonState()
        }
        binding.totalAmountInput.doAfterTextChanged {
            updateSaveButtonState()
        }
    }

    private fun setupReceiptImagePreview() {
        binding.receiptImage.setOnClickListener {
            showReceiptImagePreview()
        }
        binding.receiptImage.isEnabled = false
    }

    private fun setupAddMissingItem() {
        binding.addMissingItemButton.setOnClickListener {
            binding.addMissingItemLayout.visibility = View.VISIBLE
            binding.addMissingItemButton.visibility = View.GONE
            binding.missingItemInput.requestFocus()
        }

        binding.confirmMissingItemButton.setOnClickListener {
            val itemName = binding.missingItemInput.text?.toString()?.trim() ?: ""
            if (itemName.isNotEmpty()) {
                viewModel.addMissingItem(itemName)
                binding.missingItemInput.setText("")
                binding.addMissingItemLayout.visibility = View.GONE
                binding.addMissingItemButton.visibility = View.VISIBLE
            }
        }

        binding.missingItemInput.setOnEditorActionListener { _, _, _ ->
            val itemName = binding.missingItemInput.text?.toString()?.trim() ?: ""
            if (itemName.isNotEmpty()) {
                viewModel.addMissingItem(itemName)
                binding.missingItemInput.setText("")
                binding.addMissingItemLayout.visibility = View.GONE
                binding.addMissingItemButton.visibility = View.VISIBLE
            }
            true
        }
    }

    private fun updateSaveButtonState() {
        val currentStoreName = binding.storeNameInput.text?.toString()?.trim() ?: ""
        val currentTotalAmount = binding.totalAmountInput.text?.toString()?.trim() ?: ""
        val currentDate = selectedDate

        val hasChanges = currentStoreName != originalStoreName ||
                currentTotalAmount != originalTotalAmount ||
                currentDate != originalDate

        if (hasChanges) {
            binding.saveButton.setBackgroundColor(getColor(R.color.colorPrimary))
        } else {
            binding.saveButton.setBackgroundColor(getColor(R.color.colorPrimaryContainer))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveReceipt() {
        val storeName = binding.storeNameInput.text?.toString()?.trim() ?: ""
        val totalAmountStr = binding.totalAmountInput.text?.toString()?.trim()
        val totalAmount = totalAmountStr?.toFloatOrNull()

        val dateShopped = selectedDate?.atStartOfDay()

        val storeId = stores.find { it.name.equals(storeName, ignoreCase = true) }?.id

        viewModel.saveReceipt(
            dateShopped = dateShopped,
            storeId = storeId,
            storeName = storeName,
            totalAmount = totalAmount
        )
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.confirm_delete_receipt)
            .setMessage(R.string.confirm_delete_receipt_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteReceipt()
            }
            .show()
    }

    private fun showReceiptImagePreview() {
        val imageUri = receiptImageUri ?: return
        val previewBinding = DialogReceiptImagePreviewBinding.inflate(layoutInflater)
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(previewBinding.root)
        previewBinding.previewImage.setImageURI(imageUri)
        if (previewBinding.previewImage.drawable == null) {
            previewBinding.previewImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        previewBinding.closePreviewButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.receipt.collect { receipt ->
                        receipt?.let { populateReceipt(it) }
                    }
                }
                launch {
                    viewModel.inventoryItems.collect { items ->
                        itemsAdapter.submitList(items)
                        binding.noItemsText.visibility =
                            if (items.isEmpty()) View.VISIBLE else View.GONE
                        binding.itemsRecyclerView.visibility =
                            if (items.isEmpty()) View.GONE else View.VISIBLE
                        binding.itemCount.text = "${items.size} items"
                    }
                }
                launch {
                    viewModel.availableStores.collect { storeList ->
                        stores = storeList
                        val storeNames = storeList.map { it.name }
                        storeAdapter?.clear()
                        storeAdapter?.addAll(storeNames)
                        storeAdapter?.notifyDataSetChanged()
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility =
                            if (isLoading) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.saveSuccess.collect { success ->
                        when (success) {
                            true -> {
                                Snackbar.make(
                                    binding.root,
                                    "Receipt saved",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                                viewModel.clearSaveSuccess()
                                finish()
                            }

                            false -> {
                                viewModel.clearSaveSuccess()
                            }

                            null -> {}
                        }
                    }
                }
                launch {
                    viewModel.deleteSuccess.collect { success ->
                        when (success) {
                            true -> {
                                Snackbar.make(
                                    binding.root,
                                    "Receipt deleted",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                                viewModel.clearDeleteSuccess()
                                finish()
                            }

                            false -> {
                                viewModel.clearDeleteSuccess()
                            }

                            null -> {}
                        }
                    }
                }
                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun populateReceipt(receipt: com.foodlogger.data.db.ReceiptEntity) {
        originalDate = receipt.dateShopped?.toLocalDate()
        originalStoreName = receipt.storeName ?: ""
        originalTotalAmount = receipt.totalAmount?.toString() ?: ""

        receipt.dateShopped?.let { date ->
            selectedDate = date.toLocalDate()
            updateDateDisplay(date.toLocalDate())
        }

        receipt.storeName?.let { name ->
            binding.storeNameInput.setText(name)
        }

        receipt.totalAmount?.let { amount ->
            binding.totalAmountInput.setText(amount.toString())
        }

        updateSaveButtonState()

        val imageFile = File(receipt.imagePath)
        if (imageFile.exists()) {
            try {
                val uri = FileProvider.getUriForFile(this, "${packageName}.provider", imageFile)
                receiptImageUri = uri
                binding.receiptImage.setImageURI(uri)
                binding.receiptImage.isEnabled = true
            } catch (e: Exception) {
                receiptImageUri = null
                binding.receiptImage.setImageResource(android.R.drawable.ic_menu_gallery)
                binding.receiptImage.isEnabled = false
            }
        } else {
            receiptImageUri = null
            binding.receiptImage.setImageResource(android.R.drawable.ic_menu_gallery)
            binding.receiptImage.isEnabled = false
        }
    }
}