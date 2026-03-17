/**
 * Inventory Management
 * Handles inventory list, add, edit, delete operations
 */

const inventory = {
    currentItems: [],

    getMarkedSortPosition() {
        return document.getElementById('marked-sort-position')?.value || 'bottom';
    },

    applyMarkedItemOrdering(items) {
        const position = this.getMarkedSortPosition();
        if (position !== 'top' && position !== 'bottom') {
            return items;
        }

        const marked = [];
        const unmarked = [];
        items.forEach(item => {
            if (item.almost_finished) {
                marked.push(item);
            } else {
                unmarked.push(item);
            }
        });

        return position === 'top' ? [...marked, ...unmarked] : [...unmarked, ...marked];
    },

    toIsoDateTime(dateValue) {
        if (!dateValue) {
            return null;
        }
        return `${dateValue}T00:00:00`;
    },

    async onTabActive() {
        await this.loadItems();
        await this.populateProductSelect();
    },

    async loadItems() {
        try {
            const storageLocation = document.getElementById('storage-filter')?.value;
            const sortBy = document.getElementById('sort-by')?.value || 'expiry_date';

            const items = await InventoryAPI.list(storageLocation, sortBy);
            this.currentItems = this.applyMarkedItemOrdering(items);
            this.renderItems();
        } catch (error) {
            showNotification('Failed to load inventory items', 'error');
            console.error(error);
        }
    },

    renderItems() {
        const container = document.getElementById('inventory-list');
        container.innerHTML = '';

        if (this.currentItems.length === 0) {
            container.innerHTML = '<p class="text-gray-500 text-center py-8">No inventory items. Add one to get started!</p>';
            return;
        }

        this.currentItems.forEach(item => {
            const expiryDate = item.expiry_date ? new Date(item.expiry_date) : null;
            const isExpired = expiryDate && expiryDate < new Date();
            const isExpiringSoon = expiryDate && expiryDate < new Date(Date.now() + 7 * 24 * 60 * 60 * 1000) && !isExpired;
            const almostFinishedBadge = item.almost_finished
                ? '<span class="ml-2 inline-flex items-center rounded-full bg-amber-100 px-2 py-0.5 text-xs text-amber-800">Want to Buy</span>'
                : '';

            let statusClass = '';
            let statusText = '';
            if (isExpired) {
                statusClass = 'border-red-200 bg-red-50';
                statusText = '🔴 EXPIRED';
            } else if (isExpiringSoon) {
                statusClass = 'border-yellow-200 bg-yellow-50';
                statusText = '🟡 Expiring Soon';
            } else {
                statusClass = 'border-green-200 bg-green-50';
                statusText = '✅ Good';
            }

            const displayName = item.name_override || item.product_name;
            const expiryDisplay = expiryDate ? expiryDate.toLocaleDateString() : 'N/A';

            const html = `
                <div class="border-l-4 ${statusClass} bg-white rounded-lg shadow p-4">
                    <div class="flex flex-col gap-3 sm:flex-row sm:justify-between sm:items-start">
                        <div class="flex-1 min-w-0">
                            <h3 class="font-semibold text-gray-900 break-anywhere">${displayName} ${almostFinishedBadge}</h3>
                            <p class="text-sm text-gray-600 flex flex-wrap gap-x-3 gap-y-1">
                                <span>${item.quantity} ${item.unit}</span>
                                <span>📍 ${item.storage_location || 'N/A'}</span>
                                <span>${statusText}</span>
                            </p>
                            <p class="text-xs text-gray-500 mt-1 break-anywhere">
                                Added: ${new Date(item.date_created).toLocaleDateString()} | 
                                Expires: ${expiryDisplay}
                            </p>
                        </div>
                        <div class="flex flex-wrap gap-3">
                            <button onclick="inventory.toggleAlmostFinished(${item.id}, ${item.almost_finished})" class="text-amber-600 hover:text-amber-700 text-sm">${item.almost_finished ? 'Unmark' : 'Almost Finished'}</button>
                            <button onclick="inventory.showEditForm(${item.id})" class="text-blue-600 hover:text-blue-700 text-sm">Edit</button>
                            <button onclick="inventory.deleteItem(${item.id})" class="text-red-600 hover:text-red-700 text-sm">Delete</button>
                        </div>
                    </div>
                </div>
            `;
            container.insertAdjacentHTML('beforeend', html);
        });
    },

    async populateProductSelect() {
        try {
            const products = await ProductAPI.list();
            const select = document.getElementById('product-select');
            if (select) {
                select.innerHTML = '<option value="">Select a product...</option>';
                products.forEach(product => {
                    const option = document.createElement('option');
                    option.value = product.barcode;
                    option.textContent = product.name;
                    select.appendChild(option);
                });
            }
        } catch (error) {
            console.error('Failed to load products for select', error);
        }
    },

    async prefillFromScannedProduct(product) {
        if (!product || !product.barcode) {
            return;
        }

        await this.populateProductSelect();
        this.showAddForm();

        const select = document.getElementById('product-select');
        if (select) {
            const existingOption = Array.from(select.options).find(option => option.value === product.barcode);
            if (!existingOption) {
                const option = document.createElement('option');
                option.value = product.barcode;
                option.textContent = product.name || product.barcode;
                select.appendChild(option);
            }
            select.value = product.barcode;
        }

        const override = document.getElementById('name-override');
        if (override && !override.value && product.name) {
            override.value = product.name;
        }

        const quantity = document.getElementById('quantity');
        if (quantity) {
            quantity.focus();
        }
    },

    showAddForm() {
        document.getElementById('add-inventory-form').classList.remove('hidden');
        document.getElementById('product-select').focus();
    },

    hideAddForm() {
        document.getElementById('add-inventory-form').classList.add('hidden');
        document.getElementById('product-select').value = '';
        document.getElementById('quantity').value = '1';
        document.getElementById('unit').value = 'unit';
        document.getElementById('date-bought').value = '';
        document.getElementById('expiry-date').value = '';
        document.getElementById('storage-location').value = '';
        document.getElementById('name-override').value = '';
    },

    showEditForm(itemId) {
        const item = this.currentItems.find(i => i.id === itemId);
        if (!item) return;

        document.getElementById('edit-item-id').value = itemId;
        document.getElementById('edit-quantity').value = item.quantity;
        document.getElementById('edit-unit').value = item.unit;
        document.getElementById('edit-date-bought').value = item.date_bought ? item.date_bought.split('T')[0] : '';
        document.getElementById('edit-expiry-date').value = item.expiry_date ? item.expiry_date.split('T')[0] : '';
        document.getElementById('edit-storage-location').value = item.storage_location || '';
        document.getElementById('edit-name-override').value = item.name_override || '';

        // Hide add form if showing
        document.getElementById('add-inventory-form').classList.add('hidden');
        document.getElementById('edit-inventory-form').classList.remove('hidden');
        document.getElementById('edit-quantity').focus();
    },

    hideEditForm() {
        document.getElementById('edit-inventory-form').classList.add('hidden');
        document.getElementById('edit-item-id').value = '';
        document.getElementById('edit-quantity').value = '';
        document.getElementById('edit-unit').value = '';
        document.getElementById('edit-date-bought').value = '';
        document.getElementById('edit-expiry-date').value = '';
        document.getElementById('edit-storage-location').value = '';
        document.getElementById('edit-name-override').value = '';
    },

    async handleEditItem(event) {
        event.preventDefault();

        const itemId = parseInt(document.getElementById('edit-item-id').value);
        const quantity = parseFloat(document.getElementById('edit-quantity').value);
        const unit = document.getElementById('edit-unit').value;
        const dateBought = document.getElementById('edit-date-bought').value;
        const expiryDate = document.getElementById('edit-expiry-date').value;
        const storageLocation = document.getElementById('edit-storage-location').value;
        const nameOverride = document.getElementById('edit-name-override').value;

        try {
            await InventoryAPI.update(itemId, {
                quantity,
                unit,
                date_bought: this.toIsoDateTime(dateBought),
                expiry_date: this.toIsoDateTime(expiryDate),
                storage_location: storageLocation || null,
                name_override: nameOverride || null,
            });
            showNotification('Inventory item updated successfully', 'success');
            this.hideEditForm();
            await this.loadItems();
        } catch (error) {
            showNotification(`Failed to update item: ${error.message}`, 'error');
        }
    },

    async handleAddItem(event) {
        event.preventDefault();

        const barcode = document.getElementById('product-select').value;
        const quantity = parseFloat(document.getElementById('quantity').value);
        const unit = document.getElementById('unit').value;
        const dateBought = document.getElementById('date-bought').value;
        const expiryDate = document.getElementById('expiry-date').value;
        const storageLocation = document.getElementById('storage-location').value;
        const nameOverride = document.getElementById('name-override').value;

        if (!barcode) {
            showNotification('Please select a product', 'error');
            return;
        }

        try {
            await InventoryAPI.create(
                barcode,
                quantity,
                unit,
                this.toIsoDateTime(dateBought),
                this.toIsoDateTime(expiryDate),
                storageLocation || null,
                nameOverride || null
            );
            showNotification('Inventory item added successfully', 'success');
            this.hideAddForm();
            await this.loadItems();
        } catch (error) {
            showNotification(`Failed to add item: ${error.message}`, 'error');
        }
    },

    async deleteItem(itemId) {
        const confirmed = await showConfirmModal({
            title: 'Delete Inventory Item',
            message: 'Are you sure you want to delete this item?',
            confirmText: 'Delete Item',
        });

        if (!confirmed) {
            return;
        }

        try {
            await InventoryAPI.delete(itemId);
            showNotification('Inventory item deleted', 'success');
            await this.loadItems();
        } catch (error) {
            showNotification(`Failed to delete item: ${error.message}`, 'error');
        }
    },

    async toggleAlmostFinished(itemId, currentValue) {
        try {
            await InventoryAPI.update(itemId, { almost_finished: !currentValue });
            showNotification(!currentValue ? 'Added to Want to Buy list' : 'Removed from Want to Buy list', 'success');
            await this.loadItems();
            if (typeof wishlist !== 'undefined' && wishlist) {
                await wishlist.loadItems();
            }
        } catch (error) {
            showNotification(`Failed to update item: ${error.message}`, 'error');
        }
    },
};
