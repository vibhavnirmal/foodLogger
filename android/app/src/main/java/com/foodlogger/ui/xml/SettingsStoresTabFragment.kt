package com.foodlogger.ui.xml

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.foodlogger.R
import com.foodlogger.databinding.DialogStorageLocationEditBinding
import com.foodlogger.databinding.FragmentSettingsStoresTabBinding
import com.foodlogger.domain.model.Store
import com.foodlogger.ui.viewmodel.SettingsViewModel
import com.foodlogger.ui.xml.adapter.StoreAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsStoresTabFragment : Fragment(R.layout.fragment_settings_stores_tab) {
    private var _binding: FragmentSettingsStoresTabBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private lateinit var storeAdapter: StoreAdapter
    private var pendingStoreImageUri: Uri? = null
    private var imageTargetStoreId: Int? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        val targetStoreId = imageTargetStoreId
        if (targetStoreId != null) {
            viewModel.updateStoreImage(targetStoreId, uri.toString())
            imageTargetStoreId = null
            return@registerForActivityResult
        }

        pendingStoreImageUri = uri
        binding.selectedStoreImageText.text = uri.toString()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsStoresTabBinding.bind(view)

        storeAdapter = StoreAdapter(
            onEdit = ::showStoreRenameDialog,
            onDelete = ::confirmStoreDelete,
            onChangeImage = ::pickStoreImageForRow,
            onClearImage = ::clearStoreImageForRow,
        )
        binding.storesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.storesRecycler.adapter = storeAdapter

        binding.addStoreInput.doAfterTextChanged {
            binding.addStoreInputLayout.error = null
            if (binding.errorText.visibility == View.VISIBLE) {
                binding.errorText.visibility = View.GONE
            }
        }
        binding.selectStoreImageButton.setOnClickListener {
            imageTargetStoreId = null
            pickImageLauncher.launch("image/*")
        }
        binding.clearSelectedStoreImageButton.setOnClickListener {
            pendingStoreImageUri = null
            binding.selectedStoreImageText.text = getString(R.string.no_image_selected)
        }
        binding.addStoreButton.setOnClickListener {
            val name = binding.addStoreInput.text?.toString().orEmpty().trim()
            if (name.isBlank()) {
                binding.addStoreInputLayout.error = getString(R.string.validation_required_store_name)
                return@setOnClickListener
            }
            viewModel.addStore(name, pendingStoreImageUri?.toString())
            pendingStoreImageUri = null
            binding.addStoreInput.setText("")
            binding.selectedStoreImageText.text = getString(R.string.no_image_selected)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.stores.collect { stores ->
                        storeAdapter.submitList(stores)
                        binding.emptyText.visibility = if (stores.isEmpty() && binding.progressBar.visibility != View.VISIBLE) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.errorMessage.collect { error ->
                        binding.errorText.text = error.orEmpty()
                        binding.errorText.visibility = if (error != null) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun showStoreRenameDialog(store: Store) {
        val dialogBinding = DialogStorageLocationEditBinding.inflate(layoutInflater)
        dialogBinding.locationNameInputEdit.setText(store.name)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_rename_store)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newName = dialogBinding.locationNameInputEdit.text?.toString().orEmpty().trim()
                        dialogBinding.locationNameInputLayout.error = if (newName.isBlank()) getString(R.string.validation_required_store_name) else null
                        if (newName.isBlank()) return@setOnClickListener
                        viewModel.renameStore(store.id, newName)
                        dialog.dismiss()
                    }
                }
                dialog.show()
            }
    }

    private fun confirmStoreDelete(store: Store) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_delete_store)
            .setMessage(getString(R.string.confirm_delete_store, store.name))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteStore(store.id)
            }
            .show()
    }

    private fun pickStoreImageForRow(store: Store) {
        imageTargetStoreId = store.id
        pickImageLauncher.launch("image/*")
    }

    private fun clearStoreImageForRow(store: Store) {
        viewModel.updateStoreImage(store.id, null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
