/**
 * Wishlist / Want to Buy Management
 * Shows items marked as almost finished
 */

const wishlist = {
    currentItems: [],

    async onTabActive() {
        await this.loadItems();
    },

    async loadItems() {
        try {
            this.currentItems = await InventoryAPI.list(null, 'date_created', true);
            this.renderItems();
        } catch (error) {
            showNotification('Failed to load wishlist items', 'error');
            console.error(error);
        }
    },

    renderItems() {
        const container = document.getElementById('wishlist-list');
        if (!container) {
            return;
        }

        container.innerHTML = '';

        if (this.currentItems.length === 0) {
            container.innerHTML = '<p class="text-gray-500 text-center py-8">No items yet. Mark inventory items as Almost Finished to add them here.</p>';
            return;
        }

        this.currentItems.forEach(item => {
            const displayName = item.name_override || item.product_name;
            const html = `
                <div class="border-l-4 border-amber-200 bg-white rounded-lg shadow p-4">
                    <div class="flex flex-col gap-3 sm:flex-row sm:justify-between sm:items-start">
                        <div class="flex-1 min-w-0">
                            <h3 class="font-semibold text-gray-900 break-anywhere">${displayName}</h3>
                            <p class="text-sm text-gray-600 flex flex-wrap gap-x-3 gap-y-1">
                                <span>${item.quantity} ${item.unit}</span>
                                <span>📍 ${item.storage_location || 'N/A'}</span>
                            </p>
                            <p class="text-xs text-gray-500 mt-1">Want to Buy</p>
                        </div>
                        <div class="flex flex-wrap gap-2">
                            <button onclick="wishlist.unmark(${item.id})" class="text-indigo-600 hover:text-indigo-700 text-sm">Back to Inventory</button>
                        </div>
                    </div>
                </div>
            `;
            container.insertAdjacentHTML('beforeend', html);
        });
    },

    async unmark(itemId) {
        try {
            await InventoryAPI.update(itemId, { almost_finished: false });
            showNotification('Moved back to inventory', 'success');
            await this.loadItems();
            if (typeof inventory !== 'undefined' && inventory) {
                await inventory.loadItems();
            }
        } catch (error) {
            showNotification(`Failed to update item: ${error.message}`, 'error');
        }
    },
};
