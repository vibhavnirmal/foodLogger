/**
 * Barcode Scanning + Product Lookup
 * Uses QuaggaJS for camera decoding and falls back to manual entry when needed.
 */

const barcode = {
    isScanning: false,
    detectedHandler: null,
    lastDetectedCode: null,
    lastDetectedAt: 0,
    pendingCode: null,
    pendingCount: 0,

    getVideoConstraints() {
        return {
            facingMode: { ideal: 'environment' },
            width: { ideal: 1920 },
            height: { ideal: 1080 },
            advanced: [
                { focusMode: 'continuous' },
                { focusMode: 'single-shot' },
            ],
        };
    },

    async enhanceActiveCameraTrack() {
        if (typeof Quagga === 'undefined' || !Quagga.CameraAccess || !Quagga.CameraAccess.getActiveTrack) {
            return;
        }

        const track = Quagga.CameraAccess.getActiveTrack();
        if (!track || typeof track.getCapabilities !== 'function' || typeof track.applyConstraints !== 'function') {
            return;
        }

        const capabilities = track.getCapabilities();
        const advanced = [];

        if (Array.isArray(capabilities.focusMode) && capabilities.focusMode.length > 0) {
            if (capabilities.focusMode.includes('continuous')) {
                advanced.push({ focusMode: 'continuous' });
            } else if (capabilities.focusMode.includes('single-shot')) {
                advanced.push({ focusMode: 'single-shot' });
            }
        }

        if (typeof capabilities.zoom === 'object') {
            const min = Number.isFinite(capabilities.zoom.min) ? capabilities.zoom.min : 1;
            const max = Number.isFinite(capabilities.zoom.max) ? capabilities.zoom.max : min;
            if (max > min) {
                const targetZoom = Math.min(max, Math.max(min, min + (max - min) * 0.25));
                advanced.push({ zoom: targetZoom });
            }
        }

        if (advanced.length === 0) {
            return;
        }

        try {
            await track.applyConstraints({ advanced });
        } catch (error) {
            console.debug('Could not apply camera enhancement constraints:', error);
        }
    },

    async onTabActive() {
        const manualBarcode = document.getElementById('manual-barcode');
        if (manualBarcode) {
            manualBarcode.focus();
        }
    },

    async openScanner() {
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            showNotification('Camera is not available in this browser', 'error');
            console.error('navigator.mediaDevices not available. Browser may not support camera access.');
            this.showManualFallback('Camera access is unavailable. Use manual barcode entry below.');
            return;
        }
        if (typeof Quagga === 'undefined') {
            showNotification('Barcode scanner library failed to load', 'error');
            this.showManualFallback('Scanner library unavailable. Use manual barcode entry below.');
            return;
        }

        const modal = document.getElementById('barcode-scan-modal');
        if (modal) {
            modal.classList.remove('hidden');
        }

        try {
            // Request camera permission explicitly first
            const stream = await navigator.mediaDevices.getUserMedia({ video: this.getVideoConstraints() });
            // Stop the stream immediately—we just wanted to check permission
            stream.getTracks().forEach(track => track.stop());
            
            await this.startScanner();
        } catch (error) {
            console.error('Camera error:', error);
            showNotification(`Camera error: ${error.name}`, 'error');
            this.closeScanner();
            
            if (error.name === 'NotAllowedError') {
                this.showManualFallback('Camera permission denied. Please allow camera access and try again, or use manual barcode entry.');
            } else if (error.name === 'NotFoundError') {
                this.showManualFallback('No camera found on this device. Use manual barcode entry below.');
            } else {
                this.showManualFallback(`Camera unavailable: ${error.message}. Use manual barcode entry below.`);
            }
        }
    },

    closeScanner() {
        const modal = document.getElementById('barcode-scan-modal');
        if (modal) {
            modal.classList.add('hidden');
        }
        document.body.classList.remove('overflow-hidden');
        this.stopScanner();
    },

    async startScanner() {
        if (this.isScanning) {
            return;
        }

        const target = document.getElementById('barcode-scanner-preview');
        if (!target) {
            throw new Error('Scanner preview container not found');
        }
        target.innerHTML = '';

        const config = {
            inputStream: {
                type: 'LiveStream',
                target,
                constraints: this.getVideoConstraints(),
                area: {
                    top: '20%',
                    right: '10%',
                    left: '10%',
                    bottom: '20%',
                },
            },
            locator: {
                patchSize: 'medium',
                halfSample: false,
            },
            decoder: {
                readers: ['ean_reader', 'ean_8_reader', 'upc_reader', 'upc_e_reader'],
                multiple: false,
            },
            locate: true,
            numOfWorkers: 2,
            frequency: 10,
        };

        await new Promise((resolve, reject) => {
            Quagga.init(config, (err) => {
                if (err) {
                    reject(err);
                    return;
                }
                Quagga.start();
                resolve();
            });
        });

        this.detectedHandler = (result) => {
            const code = result?.codeResult?.code;
            if (!code) {
                return;
            }

            // Require two consecutive matching reads to reduce false positives.
            if (this.pendingCode === code) {
                this.pendingCount += 1;
            } else {
                this.pendingCode = code;
                this.pendingCount = 1;
            }

            if (this.pendingCount < 2) {
                return;
            }

            if (!this.isLikelyBarcode(code)) {
                this.pendingCode = null;
                this.pendingCount = 0;
                return;
            }

            const now = Date.now();
            if (this.lastDetectedCode === code && now - this.lastDetectedAt < 1500) {
                return;
            }

            this.lastDetectedCode = code;
            this.lastDetectedAt = now;
            this.pendingCode = null;
            this.pendingCount = 0;
            this.handleDetectedBarcode(code);
        };

        Quagga.onDetected(this.detectedHandler);
        await this.enhanceActiveCameraTrack();
        this.isScanning = true;
        document.body.classList.add('overflow-hidden');
    },

    isLikelyBarcode(code) {
        if (!/^\d+$/.test(code)) {
            return false;
        }

        // Keep common retail barcode sizes and reject noisy partial reads.
        return [8, 12, 13, 14].includes(code.length);
    },

    stopScanner() {
        if (typeof Quagga !== 'undefined') {
            if (this.detectedHandler) {
                Quagga.offDetected(this.detectedHandler);
                this.detectedHandler = null;
            }
            try {
                Quagga.stop();
            } catch (error) {
                // Ignore stop errors when scanner was never fully started.
            }
        }
        this.isScanning = false;
        this.pendingCode = null;
        this.pendingCount = 0;
    },

    async handleDetectedBarcode(barcodeValue) {
        this.closeScanner();

        const manualBarcode = document.getElementById('manual-barcode');
        if (manualBarcode) {
            manualBarcode.value = barcodeValue;
        }

        showNotification(`Scanned barcode: ${barcodeValue}`, 'success');
        await this.lookupBarcode();
    },

    async lookupBarcode() {
        const input = document.getElementById('manual-barcode');
        const barcodeValue = input?.value?.trim();

        if (!barcodeValue) {
            showNotification('Enter a barcode to look up', 'error');
            return;
        }

        this.setLookupStatus('Looking up product...');
        this.hideManualFallback();

        try {
            const lookup = await ProductAPI.lookup(barcodeValue);
            if (lookup?.product) {
                this.renderLookupResult(lookup.product, lookup.status);
                return;
            }

            if (lookup?.status === 'off_unavailable') {
                this.setLookupStatus('Open Food Facts unavailable. Add product manually.');
                this.showManualFallback('Open Food Facts is unavailable. You can still add the product manually.');
                return;
            }

            this.setLookupStatus('Barcode not found. Add product manually.');
            this.showManualFallback('Barcode not found in local DB or Open Food Facts.');
        } catch (error) {
            console.error(error);
            this.setLookupStatus('Lookup failed. Add product manually.');
            this.showManualFallback('Lookup failed. You can still add product details manually.');
        }
    },

    renderLookupResult(product, source) {
        const container = document.getElementById('barcode-lookup-result');
        if (!container) {
            return;
        }

        const sourceLabel = source === 'off' ? 'Open Food Facts' : 'Local';
        container.innerHTML = `
            <div class="rounded-lg border border-green-200 bg-green-50 p-4">
                <p class="text-sm text-green-700">Product found (${sourceLabel})</p>
                <h3 class="mt-1 text-lg font-semibold text-gray-900 break-anywhere">${product.name}</h3>
                <p class="text-sm text-gray-600 break-anywhere">${product.brand || 'Unknown Brand'}</p>
                <p class="mt-1 text-xs text-gray-500 font-mono break-all">${product.barcode}</p>
                <div class="mt-3 flex gap-2">
                    <button onclick="barcode.useScannedProduct('${product.barcode}')" class="rounded-lg bg-indigo-600 px-3 py-2 text-sm text-white hover:bg-indigo-700">Use For Inventory</button>
                </div>
            </div>
        `;

        this.hideManualFallback();
    },

    setLookupStatus(message) {
        const container = document.getElementById('barcode-lookup-result');
        if (!container) {
            return;
        }

        container.innerHTML = `<p class="text-sm text-gray-600">${message}</p>`;
    },

    showManualFallback(contextMessage) {
        const form = document.getElementById('manual-product-form');
        if (form) {
            form.classList.remove('hidden');
        }

        const barcodeValue = document.getElementById('manual-barcode')?.value?.trim() || '';
        const barcodeInput = document.getElementById('manual-product-barcode');
        if (barcodeInput) {
            barcodeInput.value = barcodeValue;
        }

        const hint = document.getElementById('manual-fallback-hint');
        if (hint) {
            hint.textContent = contextMessage;
        }
    },

    hideManualFallback() {
        const form = document.getElementById('manual-product-form');
        if (form) {
            form.classList.add('hidden');
        }
    },

    async createManualProductAndUse(event) {
        event.preventDefault();

        const barcodeValue = document.getElementById('manual-product-barcode')?.value?.trim();
        const name = document.getElementById('manual-product-name')?.value?.trim();
        const brand = document.getElementById('manual-product-brand')?.value?.trim() || null;
        const category = document.getElementById('manual-product-category')?.value?.trim() || null;

        if (!barcodeValue || !name) {
            showNotification('Barcode and product name are required', 'error');
            return;
        }

        try {
            const product = await ProductAPI.create(barcodeValue, name, brand, category);
            showNotification('Product saved. Prefilling inventory form.', 'success');
            this.hideManualFallback();
            this.renderLookupResult(product, 'local');
            await this.useScannedProduct(product.barcode);
        } catch (error) {
            showNotification(`Failed to add product: ${error.message}`, 'error');
        }
    },

    async useScannedProduct(barcodeValue) {
        try {
            const lookup = await ProductAPI.lookup(barcodeValue);
            if (!lookup?.product) {
                showNotification('Product unavailable for inventory prefill', 'error');
                this.showManualFallback('Could not load product for prefill. Add details manually.');
                return;
            }

            app.switchTab('inventory');
            await inventory.prefillFromScannedProduct(lookup.product);
            showNotification('Inventory form prefilled from scanned barcode', 'success');
        } catch (error) {
            showNotification(`Failed to prefill inventory: ${error.message}`, 'error');
        }
    },
};
