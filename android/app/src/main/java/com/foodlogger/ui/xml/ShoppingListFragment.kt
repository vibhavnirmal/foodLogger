package com.foodlogger.ui.xml

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.foodlogger.R
import com.foodlogger.databinding.FragmentShoppingListBinding
import com.foodlogger.ui.viewmodel.WishlistViewModel
import com.foodlogger.ui.xml.adapter.WishlistAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShoppingListFragment : Fragment(R.layout.fragment_shopping_list) {
    private var _binding: FragmentShoppingListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WishlistViewModel by viewModels()
    private lateinit var adapter: WishlistAdapter
    private var latestItemsEmpty: Boolean = true
    private var checkedItemIds: Set<Int> = emptySet()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentShoppingListBinding.bind(view)

        adapter = WishlistAdapter(
            onToggleChecked = { item -> viewModel.toggleItemChecked(item.id) },
            isChecked = { id -> checkedItemIds.contains(id) },
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.clearShoppedButton.setOnClickListener {
            viewModel.clearShoppedItems()
        }
        binding.retryButton.setOnClickListener { viewModel.reloadWishlist() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.almostFinishedItems.collect { items ->
                        latestItemsEmpty = items.isEmpty()
                        adapter.submitList(items)
                        binding.headerText.text = "${items.size} items to buy"
                        updateUiState(latestItemsEmpty)
                    }
                }
                launch {
                    viewModel.checkedItemIds.collect { checkedIds ->
                        checkedItemIds = checkedIds
                        binding.clearShoppedButton.isEnabled = checkedIds.isNotEmpty()
                        adapter.notifyDataSetChanged()
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        updateUiState(latestItemsEmpty)
                    }
                }
                launch {
                    viewModel.errorMessage.collect { error ->
                        binding.errorGroup.visibility = if (error != null) View.VISIBLE else View.GONE
                        binding.errorText.text = error
                        updateUiState(latestItemsEmpty)
                    }
                }
            }
        }
    }

    private fun updateUiState(isEmpty: Boolean) {
        val hasError = binding.errorGroup.visibility == View.VISIBLE
        val isLoading = binding.progressBar.visibility == View.VISIBLE

        binding.recyclerView.visibility = if (isEmpty || hasError || isLoading) View.GONE else View.VISIBLE
        binding.emptyState.visibility = if (isEmpty && !hasError && !isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
