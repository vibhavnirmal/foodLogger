/**
 * Main App Logic
 * Handles tab navigation and initialization
 */

const app = {
    currentTab: 'inventory',

    init() {
        console.log('Initializing Food Logger...');
        this.attachEventListeners();
        this.loadTab('inventory');
    },

    attachEventListeners() {
        // Tab buttons
        document.querySelectorAll('.tab-button').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.switchTab(btn.dataset.tab);
            });
        });
    },

    switchTab(tabName) {
        // Hide all tabs
        document.querySelectorAll('.tab-content').forEach(tab => {
            tab.classList.add('hidden');
        });

        // Remove active class from all buttons
        document.querySelectorAll('.tab-button').forEach(btn => {
            btn.classList.remove('border-indigo-500', 'text-indigo-600', 'bg-indigo-50');
            btn.classList.add('border-transparent', 'text-gray-500');
        });

        // Show selected tab
        const tabEl = document.getElementById(`${tabName}-tab`);
        if (tabEl) {
            tabEl.classList.remove('hidden');
        }

        // Activate matching desktop and mobile buttons
        const activeButtons = document.querySelectorAll(`[data-tab="${tabName}"]`);
        activeButtons.forEach((btn) => {
            btn.classList.add('border-indigo-500', 'text-indigo-600', 'bg-indigo-50');
            btn.classList.remove('border-transparent', 'text-gray-500');
        });

        this.currentTab = tabName;

        // Load tab data
        this.loadTab(tabName);
    },

    loadTab(tabName) {
        if (tabName === 'inventory') {
            inventory.onTabActive();
        } else if (tabName === 'barcode') {
            barcode.onTabActive();
        } else if (tabName === 'wishlist') {
            wishlist.onTabActive();
        } else if (tabName === 'recipes') {
            recipes.onTabActive();
        } else if (tabName === 'products') {
            products.onTabActive();
        }
    },
};

// Initialize app when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    app.init();
});
