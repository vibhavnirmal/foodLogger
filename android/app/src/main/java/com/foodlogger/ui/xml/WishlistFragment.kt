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
import com.foodlogger.databinding.FragmentWishlistBinding
import com.foodlogger.ui.viewmodel.WishlistViewModel
import com.foodlogger.ui.xml.adapter.WishlistAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WishlistFragment : Fragment(R.layout.fragment_wishlist) {
    private var _binding: FragmentWishlistBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WishlistViewModel by viewModels()
    private lateinit var adapter: WishlistAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWishlistBinding.bind(view)

        adapter = WishlistAdapter(
            onMarkFinished = { item -> viewModel.markAsFinished(item.id) },
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.retryButton.setOnClickListener { viewModel.reloadWishlist() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.almostFinishedItems.collect { items ->
                        adapter.submitList(items)
                        binding.recyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                        binding.emptyText.visibility = if (items.isEmpty() && binding.errorGroup.visibility != View.VISIBLE && binding.progressBar.visibility != View.VISIBLE) View.VISIBLE else View.GONE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}