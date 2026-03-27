package com.foodlogger.ui.xml

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.foodlogger.R
import com.foodlogger.databinding.DialogProductBarcodeScanBinding
import com.foodlogger.databinding.DialogProductEditBinding
import com.foodlogger.databinding.FragmentProductsBinding
import com.foodlogger.domain.model.Product
import com.foodlogger.ui.viewmodel.ProductViewModel
import com.foodlogger.ui.xml.adapter.ProductAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

@AndroidEntryPoint
class ProductFragment : Fragment(R.layout.fragment_products) {
    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductViewModel by viewModels()
    private lateinit var adapter: ProductAdapter
    private var storeNameByProductId: Map<Int, String> = emptyMap()
    private var inInventoryProductIds: Set<Int> = emptySet()
    private val scanExecutor = Executors.newSingleThreadExecutor()
    private var pendingScanAction: (() -> Unit)? = null
    private var pendingImageResult: ((Uri?) -> Unit)? = null

    private val productImagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pendingImageResult?.invoke(uri)
        pendingImageResult = null
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pendingScanAction?.invoke()
        } else {
            Toast.makeText(requireContext(), getString(R.string.camera_required_denied), Toast.LENGTH_SHORT).show()
        }
        pendingScanAction = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductsBinding.bind(view)

        adapter = ProductAdapter(
            onClick = ::showEditDialog,
            onDelete = ::showDeleteConfirmation,
            getStoreName = { productId -> storeNameByProductId[productId] },
            isInInventory = { productId -> inInventoryProductIds.contains(productId) },
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.searchInput.doAfterTextChanged { editable ->
            viewModel.setSearchQuery(editable?.toString().orEmpty())
        }
        binding.retryButton.setOnClickListener { viewModel.reloadProducts() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.filteredProducts.collect { products ->
                        adapter.submitList(products)
                        binding.recyclerView.visibility = if (products.isEmpty()) View.GONE else View.VISIBLE
                        binding.emptyText.visibility = if (products.isEmpty() && binding.errorGroup.visibility != View.VISIBLE && binding.progressBar.visibility != View.VISIBLE) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.isLoading.collect { binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE }
                }
                launch {
                    viewModel.errorMessage.collect { error ->
                        if (!error.isNullOrBlank()) {
                            binding.errorGroup.visibility = View.GONE
                            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                            viewModel.clearError()
                        }
                    }
                }
                launch {
                    viewModel.storeNameByProductId.collect { map ->
                        storeNameByProductId = map
                        adapter.notifyDataSetChanged()
                    }
                }
                launch {
                    viewModel.inInventoryProductIds.collect { ids ->
                        inInventoryProductIds = ids
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun showEditDialog(product: Product) {
        val dialogBinding = DialogProductEditBinding.inflate(layoutInflater)
        dialogBinding.nameInputEdit.setText(product.name)
        dialogBinding.brandInputEdit.setText(product.brand.orEmpty())
        dialogBinding.categoryInputEdit.setText(product.category.orEmpty())
        dialogBinding.barcodeInputEdit.setText(product.barcode.orEmpty())
        var selectedImagePath: String? = product.imagePath
        updateProductImagePreview(dialogBinding, selectedImagePath)

        dialogBinding.selectProductImageButton.setOnClickListener {
            pendingImageResult = imageResult@{ uri ->
                if (uri == null) {
                    return@imageResult
                }
                val persistedPath = copyProductImageToInternalStorage(uri)
                if (persistedPath == null) {
                    Toast.makeText(requireContext(), getString(R.string.no_image_selected), Toast.LENGTH_SHORT).show()
                } else {
                    selectedImagePath = persistedPath
                    updateProductImagePreview(dialogBinding, selectedImagePath)
                }
            }
            productImagePickerLauncher.launch("image/*")
        }

        dialogBinding.clearProductImageButton.setOnClickListener {
            selectedImagePath = null
            updateProductImagePreview(dialogBinding, selectedImagePath)
        }

        dialogBinding.barcodeInputLayout.setEndIconOnClickListener {
            requestBarcodeScan { scanned ->
                dialogBinding.barcodeInputEdit.setText(scanned)
                dialogBinding.barcodeInputEdit.setSelection(scanned.length)
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_products)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val name = dialogBinding.nameInputEdit.text?.toString().orEmpty().trim()
                        dialogBinding.nameInputLayout.error = if (name.isBlank()) getString(R.string.validation_required_name) else null
                        if (name.isBlank()) return@setOnClickListener

                        viewModel.saveProduct(
                            product.copy(
                                barcode = normalizeBarcode(dialogBinding.barcodeInputEdit.text?.toString()),
                                name = name,
                                imagePath = selectedImagePath,
                                brand = dialogBinding.brandInputEdit.text?.toString()?.trim()?.ifEmpty { null },
                                category = dialogBinding.categoryInputEdit.text?.toString()?.trim()?.ifEmpty { null },
                            )
                        )
                        dialog.dismiss()
                    }
                }
                dialog.show()
            }
    }

    private fun updateProductImagePreview(binding: DialogProductEditBinding, imagePath: String?) {
        val file = imagePath?.let { File(it) }
        if (file != null && file.exists()) {
            binding.productImagePreview.setImageURI(Uri.fromFile(file))
        } else {
            binding.productImagePreview.setImageResource(R.drawable.store_placeholder)
        }
    }

    private fun copyProductImageToInternalStorage(sourceUri: Uri): String? {
        return runCatching {
            val productImagesDir = File(requireContext().filesDir, "product_images")
            if (!productImagesDir.exists()) {
                productImagesDir.mkdirs()
            }
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val targetFile = File(productImagesDir, "product_${timestamp}_${System.currentTimeMillis()}.jpg")

            requireContext().contentResolver.openInputStream(sourceUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            targetFile.absolutePath
        }.getOrNull()
    }

    private fun requestBarcodeScan(onScanned: (String) -> Unit) {
        val action = { showBarcodeScannerDialog(onScanned) }
        if (hasCameraPermission()) {
            action()
        } else {
            pendingScanAction = action
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun showBarcodeScannerDialog(onScanned: (String) -> Unit) {
        val scanBinding = DialogProductBarcodeScanBinding.inflate(layoutInflater)
        val scanner = BarcodeScanning.getClient()
        var cameraProvider: ProcessCameraProvider? = null
        var hasDetectedBarcode = false

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_scan_barcode)
            .setView(scanBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        dialog.setOnDismissListener {
            cameraProvider?.unbindAll()
        }

        dialog.show()

        val providerFuture = ProcessCameraProvider.getInstance(requireContext())
        providerFuture.addListener({
            cameraProvider = providerFuture.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(scanBinding.previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(scanExecutor) { imageProxy ->
                val image = imageProxy.image
                if (image == null || hasDetectedBarcode) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val scannedValue = barcodes.firstOrNull()?.rawValue.orEmpty().trim()
                        if (scannedValue.isNotBlank() && !hasDetectedBarcode) {
                            hasDetectedBarcode = true
                            val normalized = normalizeBarcode(scannedValue) ?: scannedValue
                            requireActivity().runOnUiThread {
                                onScanned(normalized)
                                dialog.dismiss()
                            }
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }

            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun normalizeBarcode(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) {
            return null
        }
        val digitsOnly = raw.filter { it.isDigit() }
        return if (digitsOnly.isNotEmpty()) digitsOnly else raw
    }

    private fun showDeleteConfirmation(product: Product) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_product)
            .setMessage(getString(R.string.confirm_delete_product_message, product.name))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteProductById(product.id)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        scanExecutor.shutdown()
    }
}