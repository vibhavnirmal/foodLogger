package com.foodlogger.ui.xml

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.foodlogger.R
import com.foodlogger.databinding.FragmentReceiptsBinding
import com.foodlogger.ui.viewmodel.ReceiptSortOption
import com.foodlogger.ui.viewmodel.ReceiptsViewModel
import com.foodlogger.ui.xml.adapter.ReceiptHistoryAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReceiptsFragment : Fragment(R.layout.fragment_receipts) {
    private var _binding: FragmentReceiptsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReceiptsViewModel by viewModels()
    private lateinit var adapter: ReceiptHistoryAdapter
    private var storeNameById: Map<Int, String> = emptyMap()
    private var stores: List<com.foodlogger.domain.model.Store> = emptyList()
    private var itemCounts: Map<Int, Int> = emptyMap()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReceiptsBinding.bind(view)

        adapter = ReceiptHistoryAdapter(
            onReceiptClick = { receipt ->
                openReceiptImage(receipt.id)
            },
            getStoreName = { storeId ->
                storeId?.let { storeNameById[it] }
            },
            getItemCount = { receiptId ->
                itemCounts[receiptId] ?: 0
            }
        )
        binding.receiptsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.receiptsRecyclerView.adapter = adapter

        setupSortDropdown()
        setupStoreFilter()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.receipts.collect { receipts ->
                adapter.submitList(receipts)
                updateEmptyState(receipts.isEmpty())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.stores.collect { storeList ->
                stores = storeList
                storeNameById = storeList.associate { it.id to it.name }
                updateStoreFilterDropdown()
                adapter.notifyDataSetChanged()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.itemCounts.collect { counts ->
                itemCounts = counts
                adapter.notifyDataSetChanged()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalAmount.collect { total ->
                binding.totalAmountText.text = if (total > 0) "Total: $${String.format("%.2f", total)}" else ""
            }
        }
    }

    private fun setupSortDropdown() {
        val sortOptions = listOf(
            "Date (Newest)" to ReceiptSortOption.DATE_SHOPPED_DESC,
            "Date (Oldest)" to ReceiptSortOption.DATE_SHOPPED_ASC,
            "Amount (Desc)" to ReceiptSortOption.AMOUNT_DESC,
            "Amount (Asc)" to ReceiptSortOption.AMOUNT_ASC
        )
        
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown_small,
            sortOptions.map { it.first }
        )
        binding.sortDropdown.setAdapter(adapter)
        binding.sortDropdown.setText(sortOptions.first().first, false)
        
        binding.sortDropdown.setOnItemClickListener { _, _, position, _ ->
            viewModel.setSortOption(sortOptions[position].second)
        }
    }

    private fun setupStoreFilter() {
        updateStoreFilterDropdown()
        
        binding.storeFilterDropdown.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                viewModel.setFilterStoreId(null)
            } else {
                val store = stores.getOrNull(position - 1)
                viewModel.setFilterStoreId(store?.id)
            }
        }
    }

    private fun updateStoreFilterDropdown() {
        val storeOptions = mutableListOf("All")
        storeOptions.addAll(stores.map { it.name })
        
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown_small,
            storeOptions
        )
        binding.storeFilterDropdown.setAdapter(adapter)
        binding.storeFilterDropdown.setText(storeOptions.first(), false)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.receiptsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun openReceiptImage(receiptId: Int) {
        val intent = Intent(requireContext(), ReceiptDetailActivity::class.java).apply {
            putExtra(ReceiptDetailActivity.EXTRA_RECEIPT_ID, receiptId)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
