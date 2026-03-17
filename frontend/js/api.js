/**
 * API Helper Functions
 * Handles all API calls to the backend
 */

const API_BASE = '/api';

// Generic API call helper
async function apiCall(method, endpoint, data = null) {
    const url = `${API_BASE}${endpoint}`;
    const options = {
        method,
        headers: {
            'Content-Type': 'application/json',
        },
    };

    if (data) {
        options.body = JSON.stringify(data);
    }

    try {
        const response = await fetch(url, options);
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.detail || `API Error: ${response.status}`);
        }
        return await response.json();
    } catch (error) {
        console.error(`API call failed: ${method} ${endpoint}`, error);
        throw error;
    }
}

// Product API
const ProductAPI = {
    async create(barcode, name, brand = null, category = null, serving_size = null, kcal = null, protein = null, carbs = null, fat = null) {
        return apiCall('POST', '/products', {
            barcode, name, brand, category, serving_size, kcal, protein, carbs, fat
        });
    },
    
    async list(search = null) {
        const params = search ? `?search=${encodeURIComponent(search)}` : '';
        return apiCall('GET', `/products${params}`);
    },
    
    async get(barcode) {
        return apiCall('GET', `/products/${barcode}`);
    },

    async lookup(barcode) {
        return apiCall('GET', `/products/${barcode}`);
    },
    
    async update(barcode, updates) {
        return apiCall('PATCH', `/products/${barcode}`, updates);
    },
    
    async delete(barcode) {
        return apiCall('DELETE', `/products/${barcode}`);
    },
};

// Inventory API
const InventoryAPI = {
    async create(barcode, quantity = 1, unit = 'unit', date_bought = null, expiry_date = null, storage_location = null, name_override = null) {
        return apiCall('POST', '/inventory', {
            barcode, quantity, unit, date_bought, expiry_date, storage_location, name_override
        });
    },
    
    async list(storage_location = null, sort_by = 'expiry_date', almost_finished = null) {
        const params = new URLSearchParams();
        if (storage_location) params.append('storage_location', storage_location);
        params.append('sort_by', sort_by);
        if (almost_finished !== null) params.append('almost_finished', almost_finished);
        return apiCall('GET', `/inventory?${params.toString()}`);
    },
    
    async get(item_id) {
        return apiCall('GET', `/inventory/${item_id}`);
    },
    
    async update(item_id, updates) {
        return apiCall('PATCH', `/inventory/${item_id}`, updates);
    },
    
    async delete(item_id) {
        return apiCall('DELETE', `/inventory/${item_id}`);
    },
};

// Recipe API
const RecipeAPI = {
    async create(name, time_type = 'moderate', ingredients = []) {
        return apiCall('POST', '/recipes', {
            name, time_type, ingredients
        });
    },
    
    async list() {
        return apiCall('GET', '/recipes');
    },
    
    async get(recipe_id) {
        return apiCall('GET', `/recipes/${recipe_id}`);
    },
    
    async update(recipe_id, updates) {
        return apiCall('PATCH', `/recipes/${recipe_id}`, updates);
    },
    
    async delete(recipe_id) {
        return apiCall('DELETE', `/recipes/${recipe_id}`);
    },
};

// Notification helper
function showNotification(message, type = 'info') {
    const notifContainer = document.getElementById('notifications');
    const bgColor = type === 'error' ? 'bg-red-500' : type === 'success' ? 'bg-green-500' : 'bg-blue-500';
    const notif = document.createElement('div');
    notif.className = `${bgColor} text-white px-4 py-2 rounded-lg shadow-lg`;
    notif.textContent = message;
    notifContainer.appendChild(notif);

    setTimeout(() => notif.remove(), 3000);
}

let confirmResolver = null;

function showConfirmModal({
    title = 'Confirm Action',
    message = 'Are you sure?',
    confirmText = 'Confirm',
    cancelText = 'Cancel',
} = {}) {
    const modal = document.getElementById('confirm-modal');
    const titleEl = document.getElementById('confirm-modal-title');
    const messageEl = document.getElementById('confirm-modal-message');
    const cancelBtn = document.getElementById('confirm-modal-cancel');
    const confirmBtn = document.getElementById('confirm-modal-confirm');

    if (!modal || !titleEl || !messageEl || !cancelBtn || !confirmBtn) {
        return Promise.resolve(false);
    }

    titleEl.textContent = title;
    messageEl.textContent = message;
    cancelBtn.textContent = cancelText;
    confirmBtn.textContent = confirmText;

    modal.classList.remove('hidden');
    document.body.classList.add('overflow-hidden');
    cancelBtn.focus();

    return new Promise((resolve) => {
        confirmResolver = resolve;
    });
}

function closeConfirmModal(confirmed) {
    const modal = document.getElementById('confirm-modal');
    if (!modal) {
        return;
    }

    modal.classList.add('hidden');
    document.body.classList.remove('overflow-hidden');

    if (confirmResolver) {
        confirmResolver(confirmed);
        confirmResolver = null;
    }
}

document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
        const modal = document.getElementById('confirm-modal');
        if (modal && !modal.classList.contains('hidden')) {
            closeConfirmModal(false);
        }
    }
});
