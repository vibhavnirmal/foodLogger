package com.foodlogger.ui.xml

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.foodlogger.MainActivity
import com.foodlogger.R
import com.foodlogger.databinding.FragmentSettingsBinding
import com.foodlogger.ui.viewmodel.SettingsViewModel
import com.foodlogger.util.ThemeManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        setupTheme()
        setupCards()
        setupFab()
        observeCounts()
    }

    private fun setupTheme() {
        val currentMode = ThemeManager.getThemeMode(requireContext())
        val checkedId = when (currentMode) {
            ThemeManager.THEME_LIGHT -> R.id.btnLight
            ThemeManager.THEME_DARK -> R.id.btnDark
            else -> R.id.btnSystem
        }
        binding.themeToggleGroup.check(checkedId)

        binding.themeToggleGroup.addOnButtonCheckedListener { _, buttonId, isChecked ->
            if (isChecked) {
                val mode = when (buttonId) {
                    R.id.btnLight -> ThemeManager.THEME_LIGHT
                    R.id.btnDark -> ThemeManager.THEME_DARK
                    else -> ThemeManager.THEME_SYSTEM
                }
                ThemeManager.setThemeMode(requireContext(), mode)
                requireActivity().recreate()
            }
        }
    }

    private fun setupCards() {
        binding.cardLocations.setOnClickListener {
            openManagement(ManagementActivity.ManagementType.LOCATIONS)
        }

        binding.cardStores.setOnClickListener {
            openManagement(ManagementActivity.ManagementType.STORES)
        }

        binding.cardCategories.setOnClickListener {
            openManagement(ManagementActivity.ManagementType.CATEGORIES)
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddTypeDialog()
        }
    }

    private fun showAddTypeDialog() {
        val options = arrayOf(
            getString(R.string.settings_storage_locations),
            getString(R.string.settings_stores),
            getString(R.string.settings_categories)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_add_item)
            .setItems(options) { _, which ->
                val type = when (which) {
                    0 -> AddItemBottomSheet.ItemType.LOCATION
                    1 -> AddItemBottomSheet.ItemType.STORE
                    else -> AddItemBottomSheet.ItemType.CATEGORY
                }
                showAddBottomSheet(type)
            }
            .show()
    }

    private fun showAddBottomSheet(type: AddItemBottomSheet.ItemType) {
        val bottomSheet = AddItemBottomSheet.newInstance(type) { name ->
            when (type) {
                AddItemBottomSheet.ItemType.LOCATION -> viewModel.addLocation(name)
                AddItemBottomSheet.ItemType.STORE -> viewModel.addStore(name, null)
                AddItemBottomSheet.ItemType.CATEGORY -> viewModel.addCategory(name)
            }
        }
        bottomSheet.show(parentFragmentManager, AddItemBottomSheet.TAG)
    }

    private fun openManagement(type: ManagementActivity.ManagementType) {
        val intent = Intent(requireContext(), ManagementActivity::class.java).apply {
            putExtra(
                ManagementActivity.EXTRA_TYPE,
                when (type) {
                    ManagementActivity.ManagementType.LOCATIONS -> "locations"
                    ManagementActivity.ManagementType.STORES -> "stores"
                    ManagementActivity.ManagementType.CATEGORIES -> "categories"
                }
            )
        }
        startActivity(intent)
    }

    private fun observeCounts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.locations.collect { locations ->
                        val count = locations.size
                        binding.locationsCount.text = resources.getQuantityString(
                            R.plurals.count_locations, count, count
                        )
                    }
                }
                launch {
                    viewModel.stores.collect { stores ->
                        val count = stores.size
                        binding.storesCount.text = resources.getQuantityString(
                            R.plurals.count_stores, count, count
                        )
                    }
                }
                launch {
                    viewModel.categories.collect { categories ->
                        val count = categories.size
                        binding.categoriesCount.text = resources.getQuantityString(
                            R.plurals.count_categories, count, count
                        )
                    }
                }
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
