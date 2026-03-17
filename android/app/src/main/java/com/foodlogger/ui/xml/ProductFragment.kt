package com.foodlogger.ui.xml

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.foodlogger.R
import com.foodlogger.databinding.DialogProductEditBinding
import com.foodlogger.databinding.FragmentProductsBinding
import com.foodlogger.domain.model.Product
import com.foodlogger.ui.viewmodel.ProductViewModel
import com.foodlogger.ui.xml.adapter.ProductAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProductFragment : Fragment(R.layout.fragment_products) {
    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductViewModel by viewModels()
    private lateinit var adapter: ProductAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductsBinding.bind(view)

        adapter = ProductAdapter(
            onClick = ::showEditDialog,
            onDelete = { product -> viewModel.deleteProduct(product.barcode) },
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
                        binding.errorGroup.visibility = if (error != null) View.VISIBLE else View.GONE
                        binding.errorText.text = error?.let { getString(R.string.error_prefix, it) }
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
                                name = name,
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}