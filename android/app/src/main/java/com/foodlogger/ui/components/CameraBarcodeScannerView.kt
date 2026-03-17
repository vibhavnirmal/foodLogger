package com.foodlogger.ui.components

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun CameraBarcodeScannerView(
    onBarcodeDetected: (String) -> Unit,
    onCameraError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val flashEnabled = remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context) }
    val barcodeDetectedRef = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            try {
                                if (!barcodeDetectedRef.value) {
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val inputImage = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )

                                        val scanner = BarcodeScanning.getClient()
                                        scanner.process(inputImage)
                                            .addOnSuccessListener { barcodes ->
                                                if (barcodes.isNotEmpty() && !barcodeDetectedRef.value) {
                                                    val barcode = barcodes[0].rawValue
                                                    if (barcode != null) {
                                                        Log.d("BarcodeScanner", "Barcode detected: $barcode")
                                                        barcodeDetectedRef.value = true
                                                        onBarcodeDetected(barcode)
                                                    }
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("BarcodeScanner", "Barcode detection failed", e)
                                            }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("BarcodeScanner", "Error processing image", e)
                            } finally {
                                imageProxy.close()
                            }
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("CameraSetup", "Error binding camera", e)
                    onCameraError(e.message ?: "Error binding camera")
                }
            } catch (e: Exception) {
                Log.e("CameraSetup", "Error setting up camera", e)
                onCameraError(e.message ?: "Unknown camera error")
            }
        }, context.mainExecutor)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Flash toggle button
        IconButton(
            onClick = { flashEnabled.value = !flashEnabled.value },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                if (flashEnabled.value) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                contentDescription = "Toggle Flash",
                tint = Color.White
            )
        }

        // Scanning guide box
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.75f)
                .fillMaxHeight(0.4f)
                .border(2.dp, Color.Green)
        ) {
            Text(
                text = "Align barcode within box",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(4.dp)
            )
        }
    }
}
