package com.foodlogger.ui.xml

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.widget.ArrayAdapter
import com.foodlogger.R
import com.foodlogger.databinding.FragmentBarcodeBinding
import com.foodlogger.ui.viewmodel.BarcodeViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@AndroidEntryPoint
class BarcodeFragment : Fragment(R.layout.fragment_barcode) {
    private var _binding: FragmentBarcodeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BarcodeViewModel by viewModels()
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var hasDetectedBarcode = false

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        updateCameraUi()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBarcodeBinding.bind(view)

        binding.expiryInputEdit.isFocusable = false
        binding.dateBoughtInputEdit.isFocusable = false
        requireContext().attachDatePicker(binding.expiryInputEdit)
        requireContext().attachDatePicker(binding.dateBoughtInputEdit)

        binding.modeToggleGroup.setOnCheckedChangeListener { _, checkedId ->
            renderMode(isManual = checkedId == R.id.modeManual)
        }
        binding.grantPermissionButton.setOnClickListener {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        binding.manualEntryButton.setOnClickListener {
            binding.modeManual.isChecked = true
        }
        binding.searchButton.setOnClickListener {
            val barcode = binding.barcodeInputEdit.text?.toString().orEmpty()
            binding.barcodeInputLayout.error = if (barcode.isBlank()) getString(R.string.validation_required_barcode) else null
            if (barcode.isNotBlank()) {
                viewModel.setManualBarcode(barcode)
            }
        }
        binding.addInventoryButton.setOnClickListener {
            val quantity = binding.quantityInputEdit.text?.toString().orEmpty().toPositiveFloatOrNull()
            val dateBoughtText = binding.dateBoughtInputEdit.text?.toString().orEmpty()
            val isDateBoughtFuture = dateBoughtText.isFutureDateInput()
            binding.quantityInputLayout.error = if (quantity == null) getString(R.string.validation_quantity_positive) else null
            binding.dateBoughtInputEdit.error = if (isDateBoughtFuture) getString(R.string.validation_date_bought_not_future) else null
            if (quantity != null && !isDateBoughtFuture) {
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.setQuantity(quantity)
                    viewModel.addScannedItemToInventory()
                    clearForm()
                }
            }
        }
        binding.addManualButton.setOnClickListener {
            val quantity = binding.quantityInputEdit.text?.toString().orEmpty().toPositiveFloatOrNull()
            val name = binding.manualNameInputEdit.text?.toString().orEmpty().trim()
            val barcode = viewModel.scannedBarcode.value
            val dateBoughtText = binding.dateBoughtInputEdit.text?.toString().orEmpty()
            val isDateBoughtFuture = dateBoughtText.isFutureDateInput()
            binding.quantityInputLayout.error = if (quantity == null) getString(R.string.validation_quantity_positive) else null
            binding.manualNameInputLayout.error = if (name.isBlank()) getString(R.string.validation_required_name) else null
            binding.dateBoughtInputEdit.error = if (isDateBoughtFuture) getString(R.string.validation_date_bought_not_future) else null
            if (quantity != null && name.isNotBlank() && barcode != null && !isDateBoughtFuture) {
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.setQuantity(quantity)
                    viewModel.addManualProductAndInventory(barcode, name, binding.manualBrandInputEdit.text?.toString()?.trim()?.ifEmpty { null })
                    clearForm()
                }
            }
        }

        binding.boughtFromInputEdit.setOnItemClickListener { _, _, position, _ ->
            val store = viewModel.availableStores.value.getOrNull(position)
            viewModel.setBoughtFromStoreId(store?.id)
        }

        binding.modeCamera.isChecked = true
        updateCameraUi()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.product.collect { product ->
                        binding.foundCard.visibility = if (product != null) View.VISIBLE else View.GONE
                        binding.manualCard.visibility = if (viewModel.scannedBarcode.value != null && product == null) View.VISIBLE else View.GONE
                        if (product != null) {
                            binding.productFoundText.text = getString(R.string.product_found_prefix, product.name)
                            binding.productMetaText.text = listOfNotNull(product.brand, product.category).joinToString(" • ")
                        }
                    }
                }
                launch {
                    viewModel.errorMessage.collect { error ->
                        binding.errorCard.visibility = if (error != null) View.VISIBLE else View.GONE
                        binding.errorText.text = error.orEmpty()
                    }
                }
                launch {
                    viewModel.isLoading.collect { binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE }
                }
                launch {
                    viewModel.expiryDateInput.collect { if (binding.expiryInputEdit.text?.toString() != it) binding.expiryInputEdit.setText(it) }
                }
                launch {
                    viewModel.dateBoughtInput.collect { if (binding.dateBoughtInputEdit.text?.toString() != it) binding.dateBoughtInputEdit.setText(it) }
                }
                launch {
                    viewModel.storageLocation.collect { if (binding.storageInputEdit.text?.toString() != it) binding.storageInputEdit.setText(it) }
                }
                launch {
                    viewModel.availableStorageLocations.collect { locations ->
                        binding.storageInputEdit.setAdapter(
                            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, locations)
                        )
                    }
                }
                launch {
                    viewModel.availableStores.collect { stores ->
                        binding.boughtFromInputEdit.setAdapter(
                            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, stores.map { it.name })
                        )
                        val currentId = viewModel.boughtFromStoreId.value
                        val selectedName = stores.firstOrNull { it.id == currentId }?.name.orEmpty()
                        if (binding.boughtFromInputEdit.text?.toString() != selectedName) {
                            binding.boughtFromInputEdit.setText(selectedName, false)
                        }
                    }
                }
                launch {
                    viewModel.nameOverride.collect { if (binding.nameOverrideInputEdit.text?.toString() != it) binding.nameOverrideInputEdit.setText(it) }
                }
            }
        }

        binding.expiryInputEdit.doAfterTextChanged { viewModel.setExpiryDateInput(it?.toString().orEmpty()) }
        binding.dateBoughtInputEdit.doAfterTextChanged { viewModel.setDateBoughtInput(it?.toString().orEmpty()) }
        binding.storageInputEdit.doAfterTextChanged { viewModel.setStorageLocation(it?.toString().orEmpty()) }
        binding.boughtFromInputEdit.doAfterTextChanged { value ->
            val selected = viewModel.availableStores.value.firstOrNull { it.name == value?.toString().orEmpty() }
            viewModel.setBoughtFromStoreId(selected?.id)
        }
        binding.nameOverrideInputEdit.doAfterTextChanged { viewModel.setNameOverride(it?.toString().orEmpty()) }
    }

    private fun renderMode(isManual: Boolean) {
        binding.cameraContainer.visibility = if (isManual) View.GONE else View.VISIBLE
        binding.manualContainer.visibility = if (isManual) View.VISIBLE else View.GONE
        if (!isManual) updateCameraUi()
    }

    private fun updateCameraUi() {
        val granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        binding.permissionGroup.visibility = if (granted) View.GONE else View.VISIBLE
        binding.previewView.visibility = if (granted) View.VISIBLE else View.GONE
        if (granted) startCamera() else binding.cameraHintText.text = getString(R.string.camera_required_initial)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                val image = imageProxy.image
                if (image == null || hasDetectedBarcode) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                val input = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
                BarcodeScanning.getClient().process(input)
                    .addOnSuccessListener { barcodes ->
                        val barcode = barcodes.firstOrNull()?.rawValue
                        if (!barcode.isNullOrBlank() && !hasDetectedBarcode) {
                            hasDetectedBarcode = true
                            requireActivity().runOnUiThread {
                                binding.modeManual.isChecked = true
                                binding.barcodeInputEdit.setText(barcode)
                                viewModel.setManualBarcode(barcode)
                            }
                        }
                    }
                    .addOnCompleteListener { imageProxy.close() }
            }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun clearForm() {
        binding.barcodeInputEdit.setText("")
        binding.quantityInputEdit.setText("1")
        binding.manualNameInputEdit.setText("")
        binding.manualBrandInputEdit.setText("")
        binding.expiryInputEdit.setText("")
        binding.dateBoughtInputEdit.setText("")
        binding.storageInputEdit.setText("")
        binding.boughtFromInputEdit.setText("")
        binding.nameOverrideInputEdit.setText("")
        hasDetectedBarcode = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        analysisExecutor.shutdown()
        _binding = null
    }
}