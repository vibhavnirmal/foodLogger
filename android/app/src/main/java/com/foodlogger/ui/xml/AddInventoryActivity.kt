package com.foodlogger.ui.xml

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.WindowInsetsController
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.foodlogger.R
import com.foodlogger.databinding.ActivityAddInventoryBinding
import com.foodlogger.domain.model.Product
import com.foodlogger.domain.model.Store
import com.foodlogger.ui.viewmodel.InventoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class AddInventoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddInventoryBinding
    private val viewModel: InventoryViewModel by viewModels()
    
    private var availableProducts: List<Product> = emptyList()
    private var availableStorageLocations: List<String> = emptyList()
    private var availableStores: List<Store> = emptyList()
    
    private var selectedImageUri: Uri? = null
    private var tempImageUri: Uri? = null
    
    private var isCreatingNewProduct = false

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleSelectedImage(it) }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempImageUri?.let { handleSelectedImage(it) }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCamera()
        } else {
            launchImagePicker()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddInventoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupSystemUI()

        setupToolbar()
        setupProductToggle()
        setupImagePicker()
        setupDatePickers()
        setupSaveButton()
        observeData()
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

    private fun setupProductToggle() {
        binding.productToggle.check(R.id.btnSelectProduct)
        
        binding.productToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isCreatingNewProduct = checkedId == R.id.btnNewProduct
                binding.productInputLayout.visibility = if (isCreatingNewProduct) android.view.View.GONE else android.view.View.VISIBLE
                binding.newProductFields.visibility = if (isCreatingNewProduct) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }

    private fun setupImagePicker() {
        binding.imageCard.setOnClickListener {
            showImagePickerOptions()
        }
    }

    private fun showImagePickerOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        android.app.AlertDialog.Builder(this)
            .setTitle("Add Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> launchImagePicker()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        val imageFile = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        tempImageUri = FileProvider.getUriForFile(this, "${packageName}.provider", imageFile)
        tempImageUri?.let { takePictureLauncher.launch(it) }
    }

    private fun launchImagePicker() {
        pickImageLauncher.launch("image/*")
    }

    private fun handleSelectedImage(uri: Uri) {
        selectedImageUri = uri
        binding.itemImageView.setImageURI(uri)
        binding.itemImageView.visibility = android.view.View.VISIBLE
        binding.imagePlaceholder.visibility = android.view.View.GONE
    }

    private fun setupDatePickers() {
        binding.expiryInputEdit.isFocusable = false
        binding.dateBoughtInputEdit.isFocusable = false
        attachDatePicker(binding.expiryInputEdit)
        attachDatePicker(binding.dateBoughtInputEdit)
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
                    viewModel.availableProducts.collect { products ->
                        availableProducts = products
                        val labels = products.map { "${it.name} (${it.barcode ?: "no barcode"})" }
                        binding.productDropdown.setAdapter(
                            ArrayAdapter(this@AddInventoryActivity, android.R.layout.simple_list_item_1, labels)
                        )
                    }
                }
                launch {
                    viewModel.availableStorageLocations.collect { locations ->
                        availableStorageLocations = locations
                        binding.storageInputEdit.setAdapter(
                            ArrayAdapter(this@AddInventoryActivity, android.R.layout.simple_list_item_1, locations)
                        )
                    }
                }
                launch {
                    viewModel.availableStores.collect { stores ->
                        availableStores = stores
                        binding.boughtFromInputEdit.setAdapter(
                            ArrayAdapter(this@AddInventoryActivity, android.R.layout.simple_list_item_1, stores.map { it.name })
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
    }

    private fun saveItem() {
        var hasError = false
        var product: Product? = null
        
        if (isCreatingNewProduct) {
            // Create new product
            val productName = binding.productNameInput.text?.toString()?.trim().orEmpty()
            val barcode = binding.barcodeInput.text?.toString()?.trim().orEmpty().ifEmpty { null }
            
            if (productName.isBlank()) {
                binding.productNameInputLayout.error = getString(R.string.validation_required_name)
                hasError = true
            } else {
                binding.productNameInputLayout.error = null
                product = Product(
                    barcode = barcode,
                    name = productName,
                    brand = null,
                    category = null
                )
            }
        } else {
            // Select existing product
            val products = availableProducts
            val labels = products.map { "${it.name} (${it.barcode ?: "no barcode"})" }
            val selectedIndex = labels.indexOf(binding.productDropdown.text?.toString().orEmpty())
            product = products.getOrNull(selectedIndex)
            
            if (product == null) {
                binding.productInputLayout.error = getString(R.string.validation_required_product)
                hasError = true
            } else {
                binding.productInputLayout.error = null
            }
        }
        
        if (hasError) return

        val selectedStore = availableStores.firstOrNull {
            it.name == binding.boughtFromInputEdit.text?.toString().orEmpty()
        }
        
        val dateBoughtText = binding.dateBoughtInputEdit.text?.toString().orEmpty()
        val isDateBoughtFuture = dateBoughtText.isFutureDateInput()

        if (isDateBoughtFuture) {
            binding.dateBoughtInputLayout.error = getString(R.string.validation_date_bought_not_future)
            hasError = true
        } else {
            binding.dateBoughtInputLayout.error = null
        }

        if (hasError) return

        viewModel.createInventoryItemWithProduct(
            product = product!!,
            expiryDate = binding.expiryInputEdit.text?.toString().orEmpty().parseOptionalDateTime(),
            dateBought = dateBoughtText.parseOptionalDateTime(),
            storageLocation = binding.storageInputEdit.text?.toString()?.trim()?.ifEmpty { null },
            boughtFromStoreId = selectedStore?.id,
            nameOverride = binding.nameOverrideInputEdit.text?.toString()?.trim()?.ifEmpty { null },
            imageUri = selectedImageUri?.toString()
        )

        Toast.makeText(this, "Item added", Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, AddInventoryActivity::class.java)
        }
    }
}