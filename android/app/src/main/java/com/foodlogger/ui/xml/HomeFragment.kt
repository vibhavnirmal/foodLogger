package com.foodlogger.ui.xml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.foodlogger.MainActivity
import com.foodlogger.databinding.FragmentHomeBinding
import com.foodlogger.domain.model.ExpiryStatus
import com.foodlogger.ui.viewmodel.InventoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InventoryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeData()
    }

    private fun setupClickListeners() {
        binding.cardBrowseProducts.setOnClickListener {
            (activity as? MainActivity)?.navigateToProducts()
        }

        binding.cardShoppingList.setOnClickListener {
            (activity as? MainActivity)?.navigateToShoppingList()
        }

        binding.cardReceiptScan.setOnClickListener {
            (activity as? MainActivity)?.navigateToReceiptScan()
        }

        binding.cardAddInventory.setOnClickListener {
            (activity as? MainActivity)?.navigateToAddInventory()
        }

        binding.cardAddRecipe.setOnClickListener {
            (activity as? MainActivity)?.navigateToAddRecipe()
        }

        binding.btnViewExpiringSoon.setOnClickListener {
            (activity as? MainActivity)?.navigateToInventory()
        }

        binding.btnViewLowStock.setOnClickListener {
            (activity as? MainActivity)?.navigateToShoppingList()
        }

        binding.cardExpiringSoon.setOnClickListener {
            (activity as? MainActivity)?.navigateToInventory()
        }

        binding.cardLowStock.setOnClickListener {
            (activity as? MainActivity)?.navigateToShoppingList()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.inventoryItems.collect { items ->
                    val expiringCount = items.count { 
                        it.expiryStatus == ExpiryStatus.EXPIRING_SOON ||
                        it.expiryStatus == ExpiryStatus.EXPIRED 
                    }
                    val lowStockCount = items.count { it.almostFinished }

                    binding.textExpiringSoonCount.text = "$expiringCount items expiring soon"
                    binding.textLowStockCount.text = "$lowStockCount items low stock"

                    binding.cardExpiringSoon.visibility = if (expiringCount > 0) View.VISIBLE else View.GONE
                    binding.cardLowStock.visibility = if (lowStockCount > 0) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
