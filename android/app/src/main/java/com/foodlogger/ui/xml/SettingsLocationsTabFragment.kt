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
import com.foodlogger.databinding.DialogStorageLocationEditBinding
import com.foodlogger.databinding.FragmentSettingsLocationsTabBinding
import com.foodlogger.domain.model.StorageLocation
import com.foodlogger.ui.viewmodel.SettingsViewModel
import com.foodlogger.ui.xml.adapter.StorageLocationAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsLocationsTabFragment : Fragment(R.layout.fragment_settings_locations_tab) {
    private var _binding: FragmentSettingsLocationsTabBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private lateinit var adapter: StorageLocationAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsLocationsTabBinding.bind(view)

        adapter = StorageLocationAdapter(
            onEdit = ::showRenameDialog,
            onDelete = ::confirmDelete,
        )
        binding.locationsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.locationsRecycler.adapter = adapter

        binding.addLocationInput.doAfterTextChanged {
            binding.addLocationInputLayout.error = null
            if (binding.errorText.visibility == View.VISIBLE) {
                binding.errorText.visibility = View.GONE
            }
        }

        binding.addLocationButton.setOnClickListener {
            val name = binding.addLocationInput.text?.toString().orEmpty().trim()
            if (name.isBlank()) {
                binding.addLocationInputLayout.error = getString(R.string.validation_required_location_name)
                return@setOnClickListener
            }
            viewModel.addLocation(name)
            binding.addLocationInput.setText("")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.locations.collect { locations ->
                        adapter.submitList(locations)
                        binding.emptyText.visibility = if (locations.isEmpty() && binding.progressBar.visibility != View.VISIBLE) View.VISIBLE else View.GONE
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

    private fun showRenameDialog(location: StorageLocation) {
        val dialogBinding = DialogStorageLocationEditBinding.inflate(layoutInflater)
        dialogBinding.locationNameInputEdit.setText(location.name)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_rename_location)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newName = dialogBinding.locationNameInputEdit.text?.toString().orEmpty().trim()
                        dialogBinding.locationNameInputLayout.error = if (newName.isBlank()) getString(R.string.validation_required_location_name) else null
                        if (newName.isBlank()) return@setOnClickListener
                        viewModel.renameLocation(location.id, newName)
                        dialog.dismiss()
                    }
                }
                dialog.show()
            }
    }

    private fun confirmDelete(location: StorageLocation) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_delete_location)
            .setMessage(getString(R.string.confirm_delete_location, location.name))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteLocation(location.id)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
