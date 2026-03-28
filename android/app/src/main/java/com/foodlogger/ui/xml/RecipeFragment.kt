package com.foodlogger.ui.xml

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.foodlogger.R
import com.foodlogger.databinding.DialogRecipeCreateBinding
import com.foodlogger.databinding.DialogRecipeDetailsBinding
import com.foodlogger.databinding.FragmentRecipesBinding
import com.google.android.material.snackbar.Snackbar
import com.foodlogger.domain.model.Product
import com.foodlogger.domain.model.Recipe
import com.foodlogger.domain.model.RecipeIngredientDraft
import com.foodlogger.domain.model.TimeType
import com.foodlogger.ui.viewmodel.RecipeViewModel
import com.foodlogger.ui.xml.adapter.RecipeAdapter
import com.foodlogger.ui.xml.adapter.RecipeDraftAdapter
import com.foodlogger.ui.xml.adapter.RecipeIngredientAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RecipeFragment : Fragment(R.layout.fragment_recipes) {
    private var _binding: FragmentRecipesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecipeViewModel by viewModels()
    private lateinit var adapter: RecipeAdapter
    private var availableProducts: List<Product> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRecipesBinding.bind(view)

        adapter = RecipeAdapter(onClick = ::showDetailsDialog)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        setupFilter()
        binding.addButton.setOnClickListener { showCreateDialog() }
        binding.retryButton.setOnClickListener { viewModel.reloadRecipes() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.recipes.collect { recipes ->
                        val filtered = viewModel.getFilteredRecipes()
                        adapter.submitList(filtered)
                        updateUiState(filtered.isEmpty())
                    }
                }
                launch {
                    viewModel.timeTypeFilter.collect {
                        val filtered = viewModel.getFilteredRecipes()
                        adapter.submitList(filtered)
                        updateUiState(filtered.isEmpty())
                    }
                }
                launch {
                    viewModel.availableProducts.collect { products -> availableProducts = products }
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

    private fun setupFilter() {
        val labels = listOf("All") + TimeType.entries.map { it.displayLabel() }
        val filterAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.timeDropdown.adapter = filterAdapter
        binding.timeDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setTimeTypeFilter(TimeType.entries.getOrNull(position - 1))
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun updateUiState(isEmpty: Boolean) {
        val hasError = binding.errorGroup.visibility == View.VISIBLE
        val isLoading = binding.progressBar.visibility == View.VISIBLE
        binding.recyclerView.visibility = if (isEmpty || hasError || isLoading) View.GONE else View.VISIBLE
        binding.emptyState.visibility = if (isEmpty && !hasError && !isLoading) View.VISIBLE else View.GONE
    }

    fun showCreateDialog() {
        val dialogBinding = DialogRecipeCreateBinding.inflate(layoutInflater)
        val draftAdapter = RecipeDraftAdapter(
            productsById = { availableProducts.associateBy { it.id } },
            onChanged = { },
        )
        val drafts = mutableListOf<RecipeIngredientDraft>()
        dialogBinding.ingredientsRecycler.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.ingredientsRecycler.adapter = draftAdapter

        val timeLabels = TimeType.entries.map { it.displayLabel() }
        val timeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timeLabels).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        dialogBinding.timeDropdown.adapter = timeAdapter
        dialogBinding.timeDropdown.setSelection(TimeType.entries.indexOf(TimeType.MODERATE))

        val productLabels = availableProducts.map { it.name }
        dialogBinding.productDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, productLabels))
        dialogBinding.addIngredientButton.setOnClickListener {
            val selection = dialogBinding.productDropdown.text?.toString().orEmpty()
            val product = availableProducts.getOrNull(productLabels.indexOf(selection)) ?: run {
                dialogBinding.productInputLayout.error = getString(R.string.validation_required_product)
                return@setOnClickListener
            }
            dialogBinding.productInputLayout.error = null
            if (drafts.any { it.productId == product.id }) return@setOnClickListener
            drafts += RecipeIngredientDraft(productId = product.id)
            draftAdapter.submitList(drafts.toList())
            dialogBinding.productDropdown.setText("", false)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_add_recipe)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val name = dialogBinding.recipeNameInputEdit.text?.toString().orEmpty().trim()
                        val selectedTimeIndex = dialogBinding.timeDropdown.selectedItemPosition
                        val timeType = TimeType.entries.getOrElse(selectedTimeIndex) { TimeType.MODERATE }
                        val normalizedDrafts = drafts.toList()
                        dialogBinding.recipeNameInputLayout.error = if (name.isBlank()) getString(R.string.validation_required_recipe_name) else null
                        dialogBinding.ingredientsError.text = if (normalizedDrafts.isEmpty()) getString(R.string.validation_required_ingredient) else ""
                        if (name.isBlank() || normalizedDrafts.isEmpty()) return@setOnClickListener

                        viewModel.createRecipe(name, timeType, normalizedDrafts)
                        dialog.dismiss()
                    }
                }
                dialog.show()
            }
    }

    private fun showDetailsDialog(recipe: Recipe) {
        val dialogBinding = DialogRecipeDetailsBinding.inflate(layoutInflater)
        dialogBinding.recipeTitleText.text = recipe.name
        dialogBinding.recipeMetaText.text = getString(R.string.recipe_cook_time, recipe.timeType.displayLabel())
        dialogBinding.overviewText.text = "${recipe.ingredients.size} ingredients"
        val adapter = RecipeIngredientAdapter()
        dialogBinding.ingredientsRecycler.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.ingredientsRecycler.adapter = adapter
        adapter.submitList(recipe.ingredients)

        var dialog: androidx.appcompat.app.AlertDialog? = null
        dialogBinding.cookButton.setOnClickListener {
            dialog?.dismiss()
            viewModel.cookRecipeAsync(recipe) { result ->
                result.onSuccess { count ->
                    val message = when {
                        count == recipe.ingredients.size -> getString(R.string.action_cooked_success, count)
                        count > 0 -> getString(R.string.action_cooked_partial)
                        else -> getString(R.string.action_cooked_none)
                    }
                    view?.let { Snackbar.make(it, message, Snackbar.LENGTH_LONG).show() }
                }.onFailure { error ->
                    view?.let { Snackbar.make(it, error.message ?: "Error", Snackbar.LENGTH_LONG).show() }
                }
            }
        }

        dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_delete) { _, _ -> viewModel.deleteRecipeItem(recipe.id) }
            .setPositiveButton(R.string.action_close, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
