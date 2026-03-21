package com.foodlogger.ui.xml

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.foodlogger.databinding.ActivityReceiptCaptureBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class ReceiptCaptureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReceiptCaptureBinding
    private var imageCapture: ImageCapture? = null

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startCamera()
        } else {
            showPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiptCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.grantPermissionButton.setOnClickListener {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.captureButton.setOnClickListener {
            takePhoto()
        }

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        binding.permissionGroup.visibility = View.GONE

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun showPermissionDenied() {
        binding.permissionGroup.visibility = View.VISIBLE
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        binding.captureButton.isEnabled = false

        val receiptsDir = File(filesDir, "receipts")
        if (!receiptsDir.exists()) {
            receiptsDir.mkdirs()
        }
        
        val photoFile = File(
            receiptsDir,
            "receipt_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = android.net.Uri.fromFile(photoFile)
                    val intent = Intent(this@ReceiptCaptureActivity, ReceiptReviewActivity::class.java).apply {
                        putExtra(ReceiptReviewActivity.EXTRA_IMAGE_URI, uri.toString())
                        putExtra(ReceiptReviewActivity.EXTRA_IMAGE_PATH, photoFile.absolutePath)
                    }
                    startActivity(intent)
                    finish()
                }

                override fun onError(exception: ImageCaptureException) {
                    binding.captureButton.isEnabled = true
                    Toast.makeText(
                        this@ReceiptCaptureActivity,
                        "Failed to capture: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}
