package com.foodlogger.ui.xml

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.foodlogger.MainActivity
import com.foodlogger.R
import com.foodlogger.databinding.ActivityManagementBinding
import com.foodlogger.databinding.DialogStorageLocationEditBinding
import com.foodlogger.domain.model.Category
import com.foodlogger.domain.model.Store
import com.foodlogger.domain.model.StorageLocation
import com.foodlogger.ui.viewmodel.SettingsViewModel
import com.foodlogger.ui.xml.adapter.CategoryAdapter
import com.foodlogger.ui.xml.adapter.StoreAdapter
import com.foodlogger.ui.xml.adapter.StorageLocationAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManagementBinding
    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var locationAdapter: StorageLocationAdapter
    private lateinit var storeAdapter: StoreAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    private var managementType: ManagementType = ManagementType.LOCATIONS
    private var pendingStoreImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        pendingStoreImageUri = uri
    }

    enum class ManagementType {
        LOCATIONS, STORES, CATEGORIES
    }

    companion object {
        const val EXTRA_TYPE = "extra_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemUI()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.updatePadding(top = insets.top)
            binding.fabAdd.updatePadding(bottom = insets.bottom)
            windowInsets
        }

        managementType = when (intent.getStringExtra(EXTRA_TYPE)) {
            "stores" -> ManagementType.STORES
            "categories" -> ManagementType.CATEGORIES
            else -> ManagementType.LOCATIONS
        }

        setupToolbar()
        setupRecyclerView()
        setupFab()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = when (managementType) {
            ManagementType.LOCATIONS -> getString(R.string.settings_storage_locations)
            ManagementType.STORES -> getString(R.string.settings_stores)
            ManagementType.CATEGORIES -> getString(R.string.settings_categories)
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

    private fun setupRecyclerView() {
        when (managementType) {
            ManagementType.LOCATIONS -> {
                locationAdapter = StorageLocationAdapter(
                    onEdit = ::showRenameDialog,
                    onDelete = ::confirmDelete,
                )
                binding.recyclerView.layoutManager = LinearLayoutManager(this)
                binding.recyclerView.adapter = locationAdapter
            }
            ManagementType.STORES -> {
                storeAdapter = StoreAdapter(
                    onEdit = ::showRenameDialog,
                    onDelete = ::confirmDelete,
                    onChangeImage = ::pickStoreImage,
                    onClearImage = ::clearStoreImage,
                )
                binding.recyclerView.layoutManager = LinearLayoutManager(this)
                binding.recyclerView.adapter = storeAdapter
            }
            ManagementType.CATEGORIES -> {
                categoryAdapter = CategoryAdapter(
                    onEdit = ::showRenameDialog,
                    onDelete = ::confirmDelete,
                )
                binding.recyclerView.layoutManager = LinearLayoutManager(this)
                binding.recyclerView.adapter = categoryAdapter
            }
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddBottomSheet()
        }
    }

    private fun showAddBottomSheet() {
        val bottomSheet = AddItemBottomSheet.newInstance(
            type = when (managementType) {
                ManagementType.LOCATIONS -> AddItemBottomSheet.ItemType.LOCATION
                ManagementType.STORES -> AddItemBottomSheet.ItemType.STORE
                ManagementType.CATEGORIES -> AddItemBottomSheet.ItemType.CATEGORY
            }
        ) { name ->
            when (managementType) {
                ManagementType.LOCATIONS -> viewModel.addLocation(name)
                ManagementType.STORES -> viewModel.addStore(name, pendingStoreImageUri?.toString())
                ManagementType.CATEGORIES -> viewModel.addCategory(name)
            }
            pendingStoreImageUri = null
        }
        bottomSheet.show(supportFragmentManager, AddItemBottomSheet.TAG)
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.locations.collect { locations ->
                        if (managementType == ManagementType.LOCATIONS) {
                            locationAdapter.submitList(locations)
                            binding.emptyView.visibility = if (locations.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                }
                launch {
                    viewModel.stores.collect { stores ->
                        if (managementType == ManagementType.STORES) {
                            storeAdapter.submitList(stores)
                            binding.emptyView.visibility = if (stores.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                }
                launch {
                    viewModel.categories.collect { categories ->
                        if (managementType == ManagementType.CATEGORIES) {
                            categoryAdapter.submitList(categories)
                            binding.emptyView.visibility = if (categories.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                }
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun showRenameDialog(item: Any) {
        val dialogBinding = DialogStorageLocationEditBinding.inflate(layoutInflater)
        val (title, currentName, onSave) = when (item) {
            is StorageLocation -> Triple(
                getString(R.string.action_rename_location),
                item.name,
                { newName: String -> viewModel.renameLocation(item.id, newName) }
            )
            is Store -> Triple(
                getString(R.string.action_rename_store),
                item.name,
                { newName: String -> viewModel.renameStore(item.id, newName) }
            )
            is Category -> Triple(
                getString(R.string.action_rename_category),
                item.name,
                { newName: String -> viewModel.renameCategory(item.id, newName) }
            )
            else -> return
        }

        dialogBinding.locationNameInputEdit.setText(currentName)

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newName = dialogBinding.locationNameInputEdit.text?.toString().orEmpty().trim()
                        if (newName.isBlank()) {
                            dialogBinding.locationNameInputLayout.error = getString(R.string.validation_required_location_name)
                            return@setOnClickListener
                        }
                        onSave(newName)
                        dialog.dismiss()
                    }
                }
                dialog.show()
            }
    }

    private fun confirmDelete(item: Any) {
        val (title, message, onConfirm) = when (item) {
            is StorageLocation -> Triple(
                getString(R.string.action_delete_location),
                getString(R.string.confirm_delete_location, item.name),
                { viewModel.deleteLocation(item.id) }
            )
            is Store -> Triple(
                getString(R.string.action_delete_store),
                getString(R.string.confirm_delete_store, item.name),
                { viewModel.deleteStore(item.id) }
            )
            is Category -> Triple(
                getString(R.string.action_delete_category),
                getString(R.string.confirm_delete_category, item.name),
                { viewModel.deleteCategory(item.id) }
            )
            else -> return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ -> onConfirm() }
            .show()
    }

    private fun pickStoreImage(store: Store) {
        pickImageLauncher.launch("image/*")
    }

    private fun clearStoreImage(store: Store) {
        viewModel.updateStoreImage(store.id, null)
    }
}
