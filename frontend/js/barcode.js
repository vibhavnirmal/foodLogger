/**
 * Barcode Scanning + Product Lookup
 * Uses ZXing for camera decoding and falls back to manual entry when needed.
 */

const barcode = {
    isScanning: false,
    lastDetectedCode: null,
    lastDetectedAt: 0,
    codeReader: null,
    scanControls: null,
    scanVideo: null,

    getVideoConstraints(deviceId = null) {
        const constraints = {
            facingMode: { ideal: 'environment' },
            width: { ideal: 1920 },
            height: { ideal: 1080 },
            aspectRatio: { ideal: 1.7777777778 },
            advanced: [
                { focusMode: 'continuous' },
                { focusMode: 'single-shot' },
            ],
        };

        if (deviceId) {
            constraints.deviceId = { exact: deviceId };
        }

        return constraints;
    },

    async enhanceActiveCameraTrack() {
        const track = this.getActiveVideoTrack();
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
                const targetZoom = Math.min(max, Math.max(min, min + (max - min) * 0.15));
                advanced.push({ zoom: targetZoom });
            }
        }

        const preferredConstraints = {
            width: { ideal: 1920 },
            height: { ideal: 1080 },
            advanced,
        };

        if (advanced.length === 0) {
            return;
        }

        try {
            await track.applyConstraints(preferredConstraints);
        } catch (error) {
            console.debug('Could not apply camera enhancement constraints:', error);
        }
    },

    getScannerVideo() {
        if (!this.scanVideo) {
            this.scanVideo = document.getElementById('barcode-scanner-video');
        }

        return this.scanVideo;
    },

    getActiveVideoTrack() {
        const video = this.getScannerVideo();
        const stream = video?.srcObject;
        return stream?.getVideoTracks?.()[0] || null;
    },

    createCodeReader() {
        if (this.codeReader) {
            return this.codeReader;
        }

        if (typeof ZXingBrowser === 'undefined' || !ZXingBrowser.BrowserMultiFormatReader) {
            return null;
        }

        this.codeReader = new ZXingBrowser.BrowserMultiFormatReader();
        return this.codeReader;
    },

    async chooseVideoDevice() {
        const devices = await ZXingBrowser.BrowserCodeReader.listVideoInputDevices();
        if (!devices || devices.length === 0) {
            throw new Error('No camera found');
        }

        const preferredDevice = devices.find((device) => /back|rear|environment/i.test(device.label || ''));
        return preferredDevice?.deviceId || devices[0].deviceId;
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
        if (!this.createCodeReader()) {
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

        const video = this.getScannerVideo();
        if (!video) {
            throw new Error('Scanner preview video not found');
        }

        const codeReader = this.createCodeReader();
        const selectedDeviceId = await this.chooseVideoDevice();

        this.scanControls = await codeReader.decodeFromConstraints({
            video: this.getVideoConstraints(selectedDeviceId),
            audio: false,
        }, video, (result, error) => {
            if (!result) {
                if (error && error.name && error.name !== 'NotFoundException') {
                    console.debug('ZXing scan error:', error);
                }
                return;
            }

            const code = result.getText();

            if (!this.isLikelyBarcode(code)) {
                return;
            }

            const now = Date.now();
            if (this.lastDetectedCode === code && now - this.lastDetectedAt < 1500) {
                return;
            }

            this.lastDetectedCode = code;
            this.lastDetectedAt = now;
            this.handleDetectedBarcode(code);
        });

        await this.enhanceActiveCameraTrack();
        this.isScanning = true;
        document.body.classList.add('overflow-hidden');
    },

    isLikelyBarcode(code) {
        if (!/^\d+$/.test(code)) {
            return false;
        }

        if (![8, 12, 13, 14].includes(code.length)) {
            return false;
        }

        // UPC-E and EAN-8 can be valid even if checksum behavior differs by encoding details.
        if (code.length === 8) {
            return true;
        }

        return this.hasValidMod10CheckDigit(code);
    },

    hasValidMod10CheckDigit(code) {
        const digits = code.split('').map(Number);
        const checkDigit = digits[digits.length - 1];
        let sum = 0;
        let multiplier = 3;

        for (let i = digits.length - 2; i >= 0; i -= 1) {
            sum += digits[i] * multiplier;
            multiplier = multiplier === 3 ? 1 : 3;
        }

        const expected = (10 - (sum % 10)) % 10;
        return expected === checkDigit;
    },

    stopScanner() {
        if (this.scanControls && typeof this.scanControls.stop === 'function') {
            try {
                this.scanControls.stop();
            } catch (error) {
                // Ignore stop errors when scanner was never fully started.
            }
            this.scanControls = null;
        }

        if (this.codeReader && typeof this.codeReader.reset === 'function') {
            try {
                this.codeReader.reset();
            } catch (error) {
                // Ignore reset errors during teardown.
            }
        }

        const video = this.getScannerVideo();
        const stream = video?.srcObject;
        if (stream) {
            stream.getTracks().forEach((track) => track.stop());
            video.srcObject = null;
        }

        this.isScanning = false;
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
