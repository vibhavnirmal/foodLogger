package com.foodlogger.ui.xml

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.foodlogger.MainActivity
import com.foodlogger.R
import com.foodlogger.databinding.ActivityReceiptReviewBinding
import com.foodlogger.databinding.DialogReceiptImagePreviewBinding
import com.foodlogger.domain.model.Store
import com.foodlogger.ui.viewmodel.ReceiptScanViewModel
import com.foodlogger.ui.xml.adapter.ReceiptItemAdapter
import com.foodlogger.ui.xml.adapter.ReceiptReviewFooterAdapter
import com.foodlogger.ui.xml.AddCustomItemBottomSheet
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class ReceiptReviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReceiptReviewBinding
    lateinit var viewModel: ReceiptScanViewModel
        private set

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var currentImageUri: Uri? = null
    private lateinit var itemsAdapter: ReceiptItemAdapter
    private lateinit var footerAdapter: ReceiptReviewFooterAdapter
    private var selectedStoreName: String = ""

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_IMAGE_PATH = "extra_image_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiptReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupSystemUI()

        viewModel = androidx.lifecycle.ViewModelProvider(this)[ReceiptScanViewModel::class.java]

        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        
        if (imageUriString == null) {
            finish()
            return
        }
        
        val imageUri = Uri.parse(imageUriString)
        currentImageUri = imageUri
        
        if (imagePath != null) {
            viewModel.setImagePath(imagePath)
        }

        setupToolbar()
        setupPreviewButton()
        setupItemsList()
        setupAddMissingItemButton()
        setupFooterControls()
        setupObservers()
        processImage(imageUri)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupPreviewButton() {
        binding.previewReceiptButton.setOnClickListener {
            showReceiptImagePreview()
        }
    }

    private fun setupItemsList() {
        itemsAdapter = ReceiptItemAdapter { item, isSelected ->
            viewModel.toggleItemSelection(item.id, isSelected)
        }

        footerAdapter = ReceiptReviewFooterAdapter(
            onDateClick = { showDatePicker() },
            onStoreNameChanged = { storeName ->
                // User is typing - don't save yet
            },
            onStoreConfirmed = { storeName ->
                viewModel.setStoreName(storeName)
            },
            onTotalAmountConfirmed = { amount ->
                viewModel.setTotalAmount(amount.toFloatOrNull())
            },
            onAddToInventoryClick = {
                lifecycleScope.launch {
                    val count = viewModel.addSelectedItemsToInventory()
                    if (count > 0) {
                        Snackbar.make(binding.root, "Added $count items to inventory", Snackbar.LENGTH_SHORT).show()
                        navigateToInventory()
                    }
                }
            }
        )

        binding.itemsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.itemsRecyclerView.adapter = ConcatAdapter(itemsAdapter, footerAdapter)
    }

    private fun setupAddMissingItemButton() {
        binding.addMissingItemButton.setOnClickListener {
            val bottomSheet = AddCustomItemBottomSheet.newInstance { name, price ->
                viewModel.addCustomItem(name, price)
            }
            bottomSheet.show(supportFragmentManager, AddCustomItemBottomSheet.TAG)
        }
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

    private fun setupFooterControls() {
        updateDateButton(LocalDate.now())
        footerAdapter.updateStores("")
        updateAddButtonState()
    }

    private fun showDatePicker() {
        val currentDate = viewModel.selectedDateShopped.value.toLocalDate()
        val selection = currentDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date shopped")
            .setSelection(selection)
            .build()
        
        picker.addOnPositiveButtonClickListener { millis ->
            val date = Instant.ofEpochMilli(millis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
            viewModel.setDateShopped(date.atStartOfDay())
            updateDateButton(date)
        }
        
        picker.show(supportFragmentManager, "date_picker")
    }

    private fun updateDateButton(date: LocalDate) {
        val today = LocalDate.now()
        val dateText = if (date == today) {
            "Today"
        } else {
            date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
        footerAdapter.updateDateText(dateText)
    }

    private fun navigateToInventory() {
        startActivity(android.content.Intent(this, MainActivity::class.java).apply {
            putExtra("navigate_to", "inventory")
            flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.detectedItems.collect { items ->
                        itemsAdapter.submitList(items.toList())
                        binding.itemCountText.text = "${items.size} items detected"
                        binding.itemsRecyclerView.post {
                            updateAddButtonState()
                        }
                    }
                }
                launch {
                    viewModel.confirmedStoreName.collect { storeName ->
                        selectedStoreName = storeName
                        footerAdapter.updateStores(storeName)
                    }
                }
                launch {
                    viewModel.selectedTotalAmount.collect { totalAmount ->
                        val amountStr = totalAmount?.toString() ?: ""
                        footerAdapter.updateTotalAmount(amountStr)
                    }
                }
                launch {
                    viewModel.availableStores.collect { storeList ->
                        val storeNames = storeList.map { it.name }
                        footerAdapter.updateAvailableStores(storeNames)
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

    private fun showReceiptImagePreview() {
        val imageUri = currentImageUri ?: return
        val previewBinding = DialogReceiptImagePreviewBinding.inflate(layoutInflater)
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(previewBinding.root)
        previewBinding.previewImage.setImageURI(imageUri)
        if (previewBinding.previewImage.drawable == null) {
            previewBinding.previewImage.setImageResource(R.drawable.ic_nav_products)
        }
        previewBinding.closePreviewButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun updateAddButtonState() {
        val hasSelected = viewModel.detectedItems.value.any { it.isSelected }
        val addText = if (hasSelected) {
            "Add ${viewModel.detectedItems.value.count { it.isSelected }} Items to Inventory"
        } else {
            "Add to Inventory"
        }
        footerAdapter.updateAddButton(addText, hasSelected)
    }

    private fun processImage(imageUri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, imageUri)
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    if (text.isNotBlank() && text.length > 20) {
                        viewModel.processReceiptText(text)
                    } else {
                        Snackbar.make(binding.root, "Could not read text. Try again with better lighting.", Snackbar.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Error loading image: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textRecognizer.close()
    }
}
