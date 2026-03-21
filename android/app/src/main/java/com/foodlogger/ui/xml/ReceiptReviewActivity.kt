package com.foodlogger.ui.xml

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.foodlogger.MainActivity
import com.foodlogger.R
import com.foodlogger.databinding.ActivityReceiptReviewBinding
import com.foodlogger.databinding.PageReceiptImageBinding
import com.foodlogger.databinding.PageReceiptItemsBinding
import com.foodlogger.domain.model.Store
import com.foodlogger.ui.viewmodel.ReceiptScanViewModel
import com.foodlogger.ui.xml.adapter.ReceiptItemAdapter
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class ReceiptReviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReceiptReviewBinding
    lateinit var viewModel: ReceiptScanViewModel
        private set

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var storeAdapter: ArrayAdapter<String>? = null
    private var stores: List<Store> = emptyList()

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_IMAGE_PATH = "extra_image_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiptReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = androidx.lifecycle.ViewModelProvider(this)[ReceiptScanViewModel::class.java]

        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        
        if (imageUriString == null) {
            finish()
            return
        }
        
        val imageUri = Uri.parse(imageUriString)
        
        if (imagePath != null) {
            viewModel.setImagePath(imagePath)
        }

        setupToolbar()
        setupDatePicker()
        setupStoreSpinner()
        setupViewPager(imageUri)
        setupButtons()
        setupObservers()
        processImage(imageUri)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupDatePicker() {
        updateDateButton(LocalDate.now())
        binding.datePickerButton.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val currentDate = viewModel.selectedDateShopped.value.toLocalDate()
        val selection = currentDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date shopped")
            .setSelection(selection)
            .build()
        
        picker.addOnPositiveButtonClickListener { millis ->
            val date = Instant.ofEpochMilli(millis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
            viewModel.setDateShopped(date.atStartOfDay())
            updateDateButton(date)
        }
        
        picker.show(supportFragmentManager, "date_picker")
    }

    private fun updateDateButton(date: LocalDate) {
        val today = LocalDate.now()
        val dateText = if (date == today) {
            "Today"
        } else {
            date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
        binding.datePickerButton.text = dateText
    }

    private fun setupStoreSpinner() {
        storeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf("None"))
        storeAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.storeSpinner.adapter = storeAdapter

        binding.storeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    viewModel.setStoreId(null)
                } else {
                    val selectedStore = stores.getOrNull(position - 1)
                    viewModel.setStoreId(selectedStore?.id)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupViewPager(imageUri: Uri) {
        val pagerAdapter = ReceiptPagerAdapter(this, imageUri.toString())
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Image"
                1 -> "Items"
                else -> ""
            }
        }.attach()
    }

    private fun setupButtons() {
        binding.addToInventoryButton.setOnClickListener {
            binding.addToInventoryButton.isEnabled = false
            lifecycleScope.launch {
                val count = viewModel.addSelectedItemsToInventory()
                if (count > 0) {
                    Snackbar.make(binding.root, "Added $count items to inventory", Snackbar.LENGTH_SHORT).show()
                    navigateToInventory()
                }
            }
        }

        binding.scanAnotherButton.setOnClickListener {
            startActivity(android.content.Intent(this, ReceiptCaptureActivity::class.java))
            finish()
        }
    }

    private fun navigateToInventory() {
        startActivity(android.content.Intent(this, MainActivity::class.java).apply {
            putExtra("navigate_to", "inventory")
            flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.detectedItems.collect {
                        updateAddButtonState()
                    }
                }
                launch {
                    viewModel.availableStores.collect { storeList ->
                        stores = storeList
                        val storeNames = listOf("None") + storeList.map { it.name }
                        storeAdapter?.clear()
                        storeAdapter?.addAll(storeNames)
                        storeAdapter?.notifyDataSetChanged()
                    }
                }
                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun updateAddButtonState() {
        val hasItems = viewModel.detectedItems.value.isNotEmpty()
        val hasSelected = viewModel.detectedItems.value.any { it.isSelected }
        binding.addToInventoryButton.isEnabled = hasItems && hasSelected
        binding.addToInventoryButton.text = if (hasSelected) {
            "Add ${viewModel.detectedItems.value.count { it.isSelected }} Items to Inventory"
        } else {
            "Add to Inventory"
        }
    }

    private fun processImage(imageUri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, imageUri)
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    if (text.isNotBlank() && text.length > 20) {
                        viewModel.processReceiptText(text)
                    } else {
                        Snackbar.make(binding.root, "Could not read text. Try again with better lighting.", Snackbar.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Error loading image: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textRecognizer.close()
    }
}

class ReceiptPagerAdapter(
    activity: AppCompatActivity,
    private val imageUri: String
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ImagePageFragment.newInstance(imageUri)
            1 -> ItemsPageFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}

class ImagePageFragment : Fragment() {
    private var _binding: PageReceiptImageBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_IMAGE_URI = "arg_image_uri"
        fun newInstance(imageUri: String): ImagePageFragment {
            return ImagePageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_IMAGE_URI, imageUri)
                }
            }
        }
    }

    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = PageReceiptImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uri = Uri.parse(arguments?.getString(ARG_IMAGE_URI))
        binding.receiptImage.setImageURI(uri)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ItemsPageFragment : Fragment() {
    private var _binding: PageReceiptItemsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = PageReceiptItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as? ReceiptReviewActivity
        
        val adapter = ReceiptItemAdapter { item, isSelected ->
            activity?.viewModel?.toggleItemSelection(item.id, isSelected)
        }
        binding.itemsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.itemsRecyclerView.adapter = adapter

        activity?.let { act ->
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    act.viewModel.detectedItems.collect { items ->
                        adapter.submitList(items.toList())
                        binding.itemCountText.text = "${items.size} items detected"
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
