/**
 * Product Management
 * Handles product list, create, delete operations
 */

const products = {
    allProducts: [],
    filteredProducts: [],

    async onTabActive() {
        await this.loadProducts();
    },

    async loadProducts() {
        try {
            this.allProducts = await ProductAPI.list();
            this.filteredProducts = [...this.allProducts];
            this.renderProducts();
        } catch (error) {
            showNotification('Failed to load products', 'error');
            console.error(error);
        }
    },

    renderProducts() {
        const container = document.getElementById('products-list');
        container.innerHTML = '';

        if (this.filteredProducts.length === 0) {
            container.innerHTML = '<p class="text-gray-500 text-center py-8 col-span-2">No products found.</p>';
            return;
        }

        this.filteredProducts.forEach(product => {
            const nutrientInfo = [];
            if (product.kcal) nutrientInfo.push(`${product.kcal} kcal`);
            if (product.protein) nutrientInfo.push(`${product.protein}g protein`);
            if (product.carbs) nutrientInfo.push(`${product.carbs}g carbs`);
            if (product.fat) nutrientInfo.push(`${product.fat}g fat`);

            const html = `
                <div class="bg-white rounded-lg shadow p-4 space-y-3">
                    <div class="flex flex-col gap-3 sm:flex-row sm:justify-between sm:items-start">
                        <div class="flex-1 min-w-0">
                            <h3 class="font-semibold text-gray-900 break-anywhere">${product.name}</h3>
                            <p class="text-sm text-gray-600 break-anywhere">${product.brand || 'Unknown Brand'}</p>
                            <p class="text-xs text-gray-500 break-anywhere">Category: ${product.category || 'N/A'}</p>
                        </div>
                        <div class="flex flex-wrap gap-3">
                            <button onclick="products.showEditForm('${product.barcode}')" class="text-blue-600 hover:text-blue-700 text-sm">Edit</button>
                            <button onclick="products.deleteProduct('${product.barcode}')" class="text-red-600 hover:text-red-700 text-sm">Delete</button>
                        </div>
                    </div>
                    <div class="bg-gray-50 p-2 rounded text-xs min-w-0">
                        <p class="font-mono text-gray-600 break-all">📦 ${product.barcode}</p>
                        ${product.serving_size ? `<p class="text-gray-600 break-anywhere">Serving: ${product.serving_size}</p>` : ''}
                        ${nutrientInfo.length > 0 ? `<p class="text-gray-600 break-anywhere">${nutrientInfo.join(' • ')}</p>` : ''}
                    </div>
                </div>
            `;
            container.insertAdjacentHTML('beforeend', html);
        });
    },

    filterProducts() {
        const searchTerm = document.getElementById('product-search')?.value.toLowerCase() || '';
        this.filteredProducts = this.allProducts.filter(product => 
            product.name.toLowerCase().includes(searchTerm) ||
            product.brand?.toLowerCase().includes(searchTerm) ||
            product.barcode.includes(searchTerm)
        );
        this.renderProducts();
    },

    showAddForm() {
        document.getElementById('add-product-form').classList.remove('hidden');
        document.getElementById('product-barcode').focus();
    },

    hideAddForm() {
        document.getElementById('add-product-form').classList.add('hidden');
        document.getElementById('product-barcode').value = '';
        document.getElementById('product-name').value = '';
        document.getElementById('product-brand').value = '';
        document.getElementById('product-category').value = '';
        document.getElementById('product-serving').value = '';
        document.getElementById('product-kcal').value = '';
        document.getElementById('product-protein').value = '';
        document.getElementById('product-carbs').value = '';
        document.getElementById('product-fat').value = '';
    },

    showEditForm(barcode) {
        const product = this.allProducts.find(p => p.barcode === barcode);
        if (!product) return;

        document.getElementById('edit-product-barcode').value = barcode;
        document.getElementById('edit-product-name').value = product.name;
        document.getElementById('edit-product-brand').value = product.brand || '';
        document.getElementById('edit-product-category').value = product.category || '';
        document.getElementById('edit-product-serving').value = product.serving_size || '';
        document.getElementById('edit-product-kcal').value = product.kcal || '';
        document.getElementById('edit-product-protein').value = product.protein || '';
        document.getElementById('edit-product-carbs').value = product.carbs || '';
        document.getElementById('edit-product-fat').value = product.fat || '';

        // Hide add form if showing
        document.getElementById('add-product-form').classList.add('hidden');
        document.getElementById('edit-product-form').classList.remove('hidden');
        document.getElementById('edit-product-name').focus();
    },

    hideEditForm() {
        document.getElementById('edit-product-form').classList.add('hidden');
        document.getElementById('edit-product-barcode').value = '';
        document.getElementById('edit-product-name').value = '';
        document.getElementById('edit-product-brand').value = '';
        document.getElementById('edit-product-category').value = '';
        document.getElementById('edit-product-serving').value = '';
        document.getElementById('edit-product-kcal').value = '';
        document.getElementById('edit-product-protein').value = '';
        document.getElementById('edit-product-carbs').value = '';
        document.getElementById('edit-product-fat').value = '';
    },

    async handleEditProduct(event) {
        event.preventDefault();

        const barcode = document.getElementById('edit-product-barcode').value;
        const data = {
            name: document.getElementById('edit-product-name').value,
            brand: document.getElementById('edit-product-brand').value || null,
            category: document.getElementById('edit-product-category').value || null,
            serving_size: document.getElementById('edit-product-serving').value || null,
            kcal: document.getElementById('edit-product-kcal').value ? parseFloat(document.getElementById('edit-product-kcal').value) : null,
            protein: document.getElementById('edit-product-protein').value ? parseFloat(document.getElementById('edit-product-protein').value) : null,
            carbs: document.getElementById('edit-product-carbs').value ? parseFloat(document.getElementById('edit-product-carbs').value) : null,
            fat: document.getElementById('edit-product-fat').value ? parseFloat(document.getElementById('edit-product-fat').value) : null,
        };

        try {
            await ProductAPI.update(barcode, data);
            showNotification('Product updated successfully', 'success');
            this.hideEditForm();
            await this.loadProducts();
        } catch (error) {
            showNotification(`Failed to update product: ${error.message}`, 'error');
        }
    },

    async handleAddProduct(event) {
        event.preventDefault();

        const data = {
            barcode: document.getElementById('product-barcode').value,
            name: document.getElementById('product-name').value,
            brand: document.getElementById('product-brand').value || null,
            category: document.getElementById('product-category').value || null,
            serving_size: document.getElementById('product-serving').value || null,
            kcal: document.getElementById('product-kcal').value ? parseFloat(document.getElementById('product-kcal').value) : null,
            protein: document.getElementById('product-protein').value ? parseFloat(document.getElementById('product-protein').value) : null,
            carbs: document.getElementById('product-carbs').value ? parseFloat(document.getElementById('product-carbs').value) : null,
            fat: document.getElementById('product-fat').value ? parseFloat(document.getElementById('product-fat').value) : null,
        };

        if (!data.barcode || !data.name) {
            showNotification('Barcode and product name are required', 'error');
            return;
        }

        try {
            await ProductAPI.create(
                data.barcode,
                data.name,
                data.brand,
                data.category,
                data.serving_size,
                data.kcal,
                data.protein,
                data.carbs,
                data.fat
            );
            showNotification('Product added successfully', 'success');
            this.hideAddForm();
            await this.loadProducts();
        } catch (error) {
            showNotification(`Failed to add product: ${error.message}`, 'error');
        }
    },

    async deleteProduct(barcode) {
        const confirmed = await showConfirmModal({
            title: 'Delete Product',
            message: 'Are you sure you want to delete this product? This may affect existing inventory items.',
            confirmText: 'Delete Product',
        });

        if (!confirmed) {
            return;
        }

        try {
            await ProductAPI.delete(barcode);
            showNotification('Product deleted', 'success');
            await this.loadProducts();
        } catch (error) {
            showNotification(`Failed to delete product: ${error.message}`, 'error');
        }
    },
};
