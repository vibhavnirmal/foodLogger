package com.foodlogger.ui.xml

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentShoppingListBinding.bind(view)

        adapter = WishlistAdapter(
            onMarkFinished = { item -> viewModel.markAsFinished(item.id) },
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        
        setupSwipeToDelete()
        binding.retryButton.setOnClickListener { viewModel.reloadWishlist() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.almostFinishedItems.collect { items ->
                        adapter.submitList(items)
                        binding.headerText.text = "${items.size} items to buy"
                        updateUiState(items.isEmpty())
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.errorMessage.collect { error ->
                        binding.errorGroup.visibility = if (error != null) View.VISIBLE else View.GONE
                        binding.errorText.text = error
                    }
                }
            }
        }
    }

    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val item = adapter.currentList.getOrNull(position) ?: return
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.deleteItem(item.id)
                }
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerView)
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
