package com.foodlogger.ui.xml

import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.foodlogger.R
import com.foodlogger.databinding.FragmentInventoryBinding
import com.foodlogger.domain.model.ExpiryStatus
import com.foodlogger.domain.model.InventoryItem
import com.foodlogger.ui.viewmodel.InventoryViewModel
import com.foodlogger.ui.xml.adapter.InventoryAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InventoryFragment : Fragment(R.layout.fragment_inventory) {
    private enum class RemovalConfirmationMode {
        ASK_EACH_TIME,
        ALLOW_SESSION,
    }

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InventoryViewModel by viewModels()
    private lateinit var adapter: InventoryAdapter

    private var latestItems: List<InventoryItem> = emptyList()
    private var currentSearchQuery: String = ""
    private var isSortedByExpiry = false
    private var scrollToTopOnNextRender = false
    private var removalConfirmationMode: RemovalConfirmationMode = RemovalConfirmationMode.ASK_EACH_TIME

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentInventoryBinding.bind(view)

        adapter = InventoryAdapter(
            onClick = ::showEditDialog,
            onAddToShoppingList = ::addItemToShoppingList,
            onReceiptClick = ::showReceiptImage,
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        setupSwipeActions()
        setupSearch()
        setupSortButton()
        binding.errorState.retryButton.setOnClickListener { viewModel.reloadInventory() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.inventoryItems.collect { items ->
                        latestItems = items
                        renderItems()
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        updateLoadingState(isLoading)
                    }
                }
                launch {
                    viewModel.errorMessage.collect { error ->
                        updateErrorState(error)
                    }
                }
            }
        }
        viewModel.forceRefresh()
    }

    private fun setupSearch() {
        binding.searchInput.doAfterTextChanged { editable ->
            currentSearchQuery = editable?.toString().orEmpty()
            renderItems()
        }
    }

    private fun setupSortButton() {
        binding.sortButton.setOnClickListener {
            isSortedByExpiry = !isSortedByExpiry
            scrollToTopOnNextRender = true
            renderItems()
            Toast.makeText(
                context,
                if (isSortedByExpiry) "Sorted by expiry" else "Sort removed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun renderItems() {
        val query = currentSearchQuery.trim().lowercase()
        val filteredItems = if (query.isBlank()) {
            latestItems
        } else {
            latestItems.filter { item ->
                item.displayName().lowercase().contains(query) ||
                    item.productName.lowercase().contains(query) ||
                    (item.barcode?.lowercase()?.contains(query) == true) ||
                    (item.storageLocation?.lowercase()?.contains(query) == true) ||
                    (item.boughtFromStoreName?.lowercase()?.contains(query) == true)
            }
        }

        val displayedItems = if (isSortedByExpiry) {
            filteredItems.sortedWith(compareBy<InventoryItem> { item ->
                when {
                    item.expiryDate == null -> 3
                    item.expiryStatus == ExpiryStatus.EXPIRED -> 0
                    item.expiryStatus == ExpiryStatus.EXPIRING_SOON -> 1
                    else -> 2
                }
            }
                .thenBy { it.expiryDate ?: java.time.LocalDateTime.MAX }
                .thenBy { it.displayName().lowercase() }
                .thenBy { it.id })
        } else {
            filteredItems
        }

        val shouldScrollToTop = scrollToTopOnNextRender
        scrollToTopOnNextRender = false
        adapter.submitList(displayedItems) {
            if (shouldScrollToTop && displayedItems.isNotEmpty()) {
                binding.recyclerView.scrollToPosition(0)
            }
        }
        updateUiState(displayedItems.isEmpty())
    }

    private fun setupSwipeActions() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val item = adapter.currentList.getOrNull(position) ?: return
                adapter.notifyItemChanged(position)

                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        useItem(item)
                    }
                    ItemTouchHelper.RIGHT -> {
                        showEditDialog(item)
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val context = requireContext()

                if (dX > 0) {
                    val background = ColorDrawable(ContextCompat.getColor(context, R.color.md_theme_light_primary))
                    background.setBounds(
                        itemView.left,
                        itemView.top,
                        itemView.left + dX.toInt(),
                        itemView.bottom
                    )
                    background.draw(c)

                    val editIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_edit)
                    editIcon?.let {
                        val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + it.intrinsicHeight
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + it.intrinsicWidth

                        if (dX.toInt() > iconRight + iconMargin) {
                            it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            it.setTint(ContextCompat.getColor(context, R.color.white))
                            it.draw(c)
                        }
                    }
                } else if (dX < 0) {
                    val background = ColorDrawable(ContextCompat.getColor(context, R.color.expiry_expired))
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background.draw(c)

                    val deleteIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete)
                    deleteIcon?.let {
                        val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + it.intrinsicHeight
                        val iconRight = itemView.right - iconMargin
                        val iconLeft = iconRight - it.intrinsicWidth

                        if (-dX.toInt() > iconRight - iconLeft + iconMargin) {
                            it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            it.setTint(ContextCompat.getColor(context, R.color.white))
                            it.draw(c)
                        }
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun useItem(item: InventoryItem) {
        val newQuantity = item.quantity - 1f
        if (newQuantity <= 0) {
            requestItemRemoval(item)
        } else {
            viewModel.saveInventoryItem(
                item = item,
                quantity = newQuantity,
                unit = item.unit,
                expiryDate = item.expiryDate,
                storageLocation = item.storageLocation,
                boughtFromStoreId = item.boughtFromStoreId,
                nameOverride = item.nameOverride,
                almostFinished = item.almostFinished
            )
            Toast.makeText(context, "Used 1 ${item.unit}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestItemRemoval(item: InventoryItem) {
        if (removalConfirmationMode == RemovalConfirmationMode.ALLOW_SESSION) {
            removeItemWithUndo(item)
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_remove_inventory_item)
            .setMessage(getString(R.string.confirm_remove_inventory_item_message, item.displayName()))
            .setNegativeButton(R.string.action_cancel, null)
            .setNeutralButton(R.string.action_allow_once) { _, _ ->
                removeItemWithUndo(item)
            }
            .setPositiveButton(R.string.action_allow_session) { _, _ ->
                removalConfirmationMode = RemovalConfirmationMode.ALLOW_SESSION
                removeItemWithUndo(item)
            }
            .show()
    }

    private fun addItemToShoppingList(item: InventoryItem) {
        val shouldAddToShoppingList = !item.almostFinished

        viewModel.saveInventoryItem(
            item = item,
            quantity = item.quantity,
            unit = item.unit,
            expiryDate = item.expiryDate,
            storageLocation = item.storageLocation,
            boughtFromStoreId = item.boughtFromStoreId,
            nameOverride = item.nameOverride,
            almostFinished = shouldAddToShoppingList
        )
        val message = if (shouldAddToShoppingList) {
            getString(R.string.shopping_list_added)
        } else {
            getString(R.string.shopping_list_removed)
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun removeItemWithUndo(item: InventoryItem) {
        val originalIndex = adapter.currentList.indexOfFirst { it.id == item.id }.takeIf { it >= 0 }
        viewModel.deleteItem(item, originalIndex)
        Snackbar.make(binding.root, getString(R.string.inventory_item_removed), Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.action_undo)) {
                viewModel.restoreDeletedItem(item, originalIndex)
            }
            .show()
    }

    private fun updateUiState(isEmpty: Boolean) {
        val hasError = binding.errorState.root.visibility == View.VISIBLE
        val isLoading = binding.progressBar.visibility == View.VISIBLE

        binding.recyclerView.visibility = if (isEmpty || hasError || isLoading) View.GONE else View.VISIBLE
        binding.emptyState.root.visibility = if (isEmpty && !hasError && !isLoading) View.VISIBLE else View.GONE
    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyState.root.visibility = View.GONE
        } else {
            updateUiState(adapter.currentList.isEmpty())
        }
    }

    private fun updateErrorState(error: String?) {
        if (error != null) {
            binding.errorState.root.visibility = View.VISIBLE
            binding.errorState.errorText.text = getString(R.string.error_prefix, error)
            binding.recyclerView.visibility = View.GONE
            binding.emptyState.root.visibility = View.GONE
        } else {
            binding.errorState.root.visibility = View.GONE
        }
    }

    private fun showEditDialog(item: InventoryItem) {
        startActivity(EditInventoryActivity.newIntent(requireContext(), item.id))
    }

    private fun showReceiptImage(item: InventoryItem) {
        item.receiptId?.let { receiptId ->
            viewLifecycleOwner.lifecycleScope.launch {
                val receipt = viewModel.getReceiptById(receiptId)
                receipt?.let {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse("file://${it.imagePath}"), "image/*")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open receipt image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.forceRefresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
