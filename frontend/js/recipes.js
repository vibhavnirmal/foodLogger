/**
 * Recipe Management
 * Handles recipe list, create, edit, delete operations
 */

const recipes = {
    currentRecipes: [],
    ingredientRowCount: 0,
    productOptions: [],

    async onTabActive() {
        await this.loadRecipes();
    },

    async loadRecipes() {
        try {
            this.currentRecipes = await RecipeAPI.list();
            this.renderRecipes();
        } catch (error) {
            showNotification('Failed to load recipes', 'error');
            console.error(error);
        }
    },

    renderRecipes() {
        const container = document.getElementById('recipes-list');
        container.innerHTML = '';

        if (this.currentRecipes.length === 0) {
            container.innerHTML = '<p class="text-gray-500 text-center py-8 col-span-2">No recipes yet. Create one to get started!</p>';
            return;
        }

        this.currentRecipes.forEach(recipe => {
            const timeTypeEmoji = {
                'very_fast': '⚡',
                'moderate': '🔥',
                'slow': '🍳'
            }[recipe.time_type] || '⏱️';

            const timeTypeText = {
                'very_fast': 'Very Fast',
                'moderate': 'Moderate',
                'slow': 'Slow'
            }[recipe.time_type] || recipe.time_type;

            const ingredientsList = recipe.ingredients.map(ing => 
                `<li class="text-sm text-gray-600 break-anywhere">• ${ing.product_name} - ${ing.quantity} ${ing.unit}</li>`
            ).join('');

            const html = `
                <div class="bg-white rounded-lg shadow p-4 space-y-3">
                    <div class="flex flex-col gap-3 sm:flex-row sm:justify-between sm:items-start">
                        <div class="min-w-0">
                            <h3 class="font-semibold text-lg text-gray-900 break-anywhere">${recipe.name}</h3>
                            <p class="text-sm text-gray-600">${timeTypeEmoji} ${timeTypeText}</p>
                        </div>
                        <div class="flex flex-wrap gap-3">
                            <button onclick="recipes.showEditForm(${recipe.id})" class="text-blue-600 hover:text-blue-700 text-sm">Edit</button>
                            <button onclick="recipes.deleteRecipe(${recipe.id})" class="text-red-600 hover:text-red-700 text-sm">Delete</button>
                        </div>
                    </div>
                    <div>
                        <p class="text-xs font-medium text-gray-700 mb-1">Ingredients:</p>
                        <ul class="space-y-1">
                            ${ingredientsList}
                        </ul>
                    </div>
                </div>
            `;
            container.insertAdjacentHTML('beforeend', html);
        });
    },

    showAddForm() {
        this.ingredientRowCount = 0;
        document.getElementById('add-recipe-form').classList.remove('hidden');
        document.getElementById('recipe-name').focus();
        document.getElementById('ingredients-list').innerHTML = '';
    },

    hideAddForm() {
        document.getElementById('add-recipe-form').classList.add('hidden');
        document.getElementById('recipe-name').value = '';
        document.getElementById('ingredients-list').innerHTML = '';
        document.querySelector('input[name="time_type"]').checked = true;
    },

    showEditForm(recipeId) {
        const recipe = this.currentRecipes.find(r => r.id === recipeId);
        if (!recipe) return;

        document.getElementById('edit-recipe-id').value = recipeId;
        document.getElementById('edit-recipe-name').value = recipe.name;

        // Set time type radio button
        document.querySelector(`input[name="edit-time_type"][value="${recipe.time_type}"]`).checked = true;

        // Build ingredients list
        const ingredientsList = document.getElementById('edit-ingredients-list');
        ingredientsList.innerHTML = '';
        recipe.ingredients.forEach(ingredient => {
            const row = document.createElement('div');
            row.className = 'flex flex-col gap-2 sm:flex-row ingredient-row edit-ingredient-row';
            row.innerHTML = `
                <div class="flex-1">
                    <input type="text" placeholder="Search ingredient..." value="${ingredient.product_name || ''}" class="ingredient-search mb-2 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border px-3 py-2">
                    <select class="ingredient-select block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border px-3 py-2" value="${ingredient.barcode}" required>
                        <option value="">Select ingredient...</option>
                    </select>
                </div>
                <input type="number" placeholder="Qty" step="0.1" value="${ingredient.quantity}" class="ingredient-quantity w-full sm:w-20 rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border px-3 py-2" required>
                <input type="text" placeholder="Unit" value="${ingredient.unit}" class="ingredient-unit w-full sm:w-20 rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border px-3 py-2">
                <button type="button" onclick="this.parentElement.remove()" class="text-red-600 hover:text-red-700">Remove</button>
            `;
            ingredientsList.appendChild(row);
            const select = row.querySelector('.ingredient-select');
            this.attachIngredientSearch(row);
            this.populateIngredientSelect(select, ingredient.barcode, ingredient.product_name || '');
        });

        // Hide add form if showing
        document.getElementById('add-recipe-form').classList.add('hidden');
        document.getElementById('edit-recipe-form').classList.remove('hidden');
        document.getElementById('edit-recipe-name').focus();
    },

    hideEditForm() {
        document.getElementById('edit-recipe-form').classList.add('hidden');
        document.getElementById('edit-recipe-id').value = '';
        document.getElementById('edit-recipe-name').value = '';
        document.getElementById('edit-ingredients-list').innerHTML = '';
    },

    addEditIngredientRow() {
        const ingredientsList = document.getElementById('edit-ingredients-list');
        const row = document.createElement('div');
        row.className = 'flex flex-col gap-2 sm:flex-row ingredient-row edit-ingredient-row';
        row.innerHTML = `
            <div class="flex-1">
                <input type="text" placeholder="Search ingredient..." class="ingredient-search mb-2 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border px-3 py-2">
                <select class="ingredient-select block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border px-3 py-2" required>
                    <option value="">Select ingredient...</option>
                </select>
            </div>
            <input type="number" placeholder="Qty" step="0.1" value="1" class="ingredient-quantity w-full sm:w-20 rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border px-3 py-2" required>
            <input type="text" placeholder="Unit" value="unit" class="ingredient-unit w-full sm:w-20 rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border px-3 py-2">
            <button type="button" onclick="this.parentElement.remove()" class="text-red-600 hover:text-red-700">Remove</button>
        `;
        ingredientsList.appendChild(row);
        this.attachIngredientSearch(row);
        this.populateIngredientSelect(row.querySelector('.ingredient-select'));
    },

    async handleEditRecipe(event) {
        event.preventDefault();

        const recipeId = parseInt(document.getElementById('edit-recipe-id').value);
        const name = document.getElementById('edit-recipe-name').value;
        const timeType = document.querySelector('input[name="edit-time_type"]:checked').value;

        // Collect ingredients
        const ingredientRows = document.querySelectorAll('.edit-ingredient-row');
        const ingredients = [];

        for (const row of ingredientRows) {
            const barcode = row.querySelector('.ingredient-select').value;
            const quantity = parseFloat(row.querySelector('.ingredient-quantity').value);
            const unit = row.querySelector('.ingredient-unit').value;

            if (!barcode || !quantity) {
                showNotification('Please fill in all ingredient fields', 'error');
                return;
            }

            ingredients.push({ barcode, quantity, unit });
        }

        if (ingredients.length === 0) {
            showNotification('Please add at least one ingredient', 'error');
            return;
        }

        try {
            await RecipeAPI.update(recipeId, {
                name,
                time_type: timeType,
                ingredients
            });
            showNotification('Recipe updated successfully', 'success');
            this.hideEditForm();
            await this.loadRecipes();
        } catch (error) {
            showNotification(`Failed to update recipe: ${error.message}`, 'error');
        }
    },

    addIngredientRow() {
        this.ingredientRowCount++;
        const ingredientsList = document.getElementById('ingredients-list');
        
        const row = document.createElement('div');
        row.className = 'flex flex-col gap-2 sm:flex-row ingredient-row';
        row.innerHTML = `
            <div class="flex-1">
                <input type="text" placeholder="Search ingredient..." class="ingredient-search mb-2 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border px-3 py-2">
                <select class="ingredient-select block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border px-3 py-2" required>
                    <option value="">Select ingredient...</option>
                </select>
            </div>
            <input type="number" placeholder="Qty" step="0.1" value="1" class="ingredient-quantity w-full sm:w-20 rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border px-3 py-2" required>
            <input type="text" placeholder="Unit" value="unit" class="ingredient-unit w-full sm:w-20 rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border px-3 py-2">
            <button type="button" onclick="this.parentElement.remove()" class="text-red-600 hover:text-red-700">Remove</button>
        `;
        
        ingredientsList.appendChild(row);
        this.attachIngredientSearch(row);
        this.populateIngredientSelect(row.querySelector('.ingredient-select'));
    },

    async ensureProductOptions() {
        if (this.productOptions.length > 0) {
            return;
        }
        this.productOptions = await ProductAPI.list();
    },

    filterIngredientOptions(select, query = '', selectedValue = null) {
        const normalizedQuery = query.trim().toLowerCase();
        const currentValue = selectedValue ?? select.value;

        let filtered = this.productOptions;
        if (normalizedQuery) {
            filtered = this.productOptions.filter(product => {
                const name = (product.name || '').toLowerCase();
                const brand = (product.brand || '').toLowerCase();
                const barcode = (product.barcode || '').toLowerCase();
                return name.includes(normalizedQuery) || brand.includes(normalizedQuery) || barcode.includes(normalizedQuery);
            });
        }

        select.innerHTML = '<option value="">Select ingredient...</option>';

        filtered.forEach(product => {
            const option = document.createElement('option');
            option.value = product.barcode;
            option.textContent = product.name;
            select.appendChild(option);
        });

        if (currentValue) {
            const hasCurrent = filtered.some(product => product.barcode === currentValue);
            if (!hasCurrent) {
                const currentProduct = this.productOptions.find(product => product.barcode === currentValue);
                if (currentProduct) {
                    const option = document.createElement('option');
                    option.value = currentProduct.barcode;
                    option.textContent = currentProduct.name;
                    select.appendChild(option);
                }
            }
            select.value = currentValue;
        }
    },

    attachIngredientSearch(row) {
        const searchInput = row.querySelector('.ingredient-search');
        const select = row.querySelector('.ingredient-select');

        if (!searchInput || !select) {
            return;
        }

        searchInput.addEventListener('input', () => {
            this.filterIngredientOptions(select, searchInput.value);
        });

        select.addEventListener('change', () => {
            const selectedProduct = this.productOptions.find(product => product.barcode === select.value);
            if (selectedProduct) {
                searchInput.value = selectedProduct.name;
            }
        });
    },

    async populateIngredientSelect(select, selectedValue = null, query = '') {
        try {
            await this.ensureProductOptions();
            this.filterIngredientOptions(select, query, selectedValue);
        } catch (error) {
            console.error('Failed to load products', error);
        }
    },

    async handleAddRecipe(event) {
        event.preventDefault();

        const name = document.getElementById('recipe-name').value;
        const timeType = document.querySelector('input[name="time_type"]:checked').value;

        // Collect ingredients
        const ingredientRows = document.querySelectorAll('.ingredient-row');
        const ingredients = [];

        for (const row of ingredientRows) {
            const barcode = row.querySelector('.ingredient-select').value;
            const quantity = parseFloat(row.querySelector('.ingredient-quantity').value);
            const unit = row.querySelector('.ingredient-unit').value;

            if (!barcode || !quantity) {
                showNotification('Please fill in all ingredient fields', 'error');
                return;
            }

            ingredients.push({ barcode, quantity, unit });
        }

        if (ingredients.length === 0) {
            showNotification('Please add at least one ingredient', 'error');
            return;
        }

        try {
            await RecipeAPI.create(name, timeType, ingredients);
            showNotification('Recipe created successfully', 'success');
            this.hideAddForm();
            await this.loadRecipes();
        } catch (error) {
            showNotification(`Failed to create recipe: ${error.message}`, 'error');
        }
    },

    async deleteRecipe(recipeId) {
        const confirmed = await showConfirmModal({
            title: 'Delete Recipe',
            message: 'Are you sure you want to delete this recipe?',
            confirmText: 'Delete Recipe',
        });

        if (!confirmed) {
            return;
        }

        try {
            await RecipeAPI.delete(recipeId);
            showNotification('Recipe deleted', 'success');
            await this.loadRecipes();
        } catch (error) {
            showNotification(`Failed to delete recipe: ${error.message}`, 'error');
        }
    },
};
