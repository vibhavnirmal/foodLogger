package com.foodlogger.ui.xml

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.foodlogger.R
import com.foodlogger.databinding.FragmentReceiptsBinding
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReceiptsBinding.bind(view)

        adapter = ReceiptHistoryAdapter(
            onReceiptClick = { receipt ->
                openReceiptImage(receipt.id)
            },
            getStoreName = { storeId ->
                storeId?.let { storeNameById[it] }
            }
        )
        binding.receiptsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.receiptsRecyclerView.adapter = adapter

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
            viewModel.stores.collect { stores ->
                storeNameById = stores.associate { it.id to it.name }
                adapter.notifyDataSetChanged()
            }
        }
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
