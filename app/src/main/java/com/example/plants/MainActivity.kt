package com.example.plants

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.plants.ui.theme.PlantsTheme
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var hasCameraPermission by mutableStateOf(false)
    private val plantIdentificationService = PlantIdentificationService()
    private var showPlantIdentification by mutableStateOf(false)
    private var isLoading by mutableStateOf(false)
    private var identificationResult by mutableStateOf<PlantIdentificationResult?>(null)
    private var capturedImage by mutableStateOf<Bitmap?>(null)

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request permissions
        requestCameraPermission()

        // Set up the camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            val coroutineScope = rememberCoroutineScope()
            
            PlantsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(Modifier.padding(bottom = innerPadding.calculateBottomPadding()).fillMaxSize()) {
                        Column {
                            CameraContent(
                                hasCameraPermission = hasCameraPermission,
                                onImageCaptured = { bitmap ->
                                    Log.d("CameraCapture", "Image captured")
                                    coroutineScope.launch {
                                        capturedImage = bitmap
                                        isLoading = true
                                        try {
                                            identificationResult = plantIdentificationService.identifyPlant(bitmap)
                                            showPlantIdentification = true
                                        } catch (e: Exception) {
                                            Log.e("MainActivity", "Error identifying plant", e)
                                            identificationResult = PlantIdentificationResult(
                                                success = false,
                                                name = "Error", 
                                                description = "Failed to identify plant: ${e.message}"
                                            )
                                            showPlantIdentification = true
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                onError = { error ->
                                    Log.e("CameraCapture", "Error capturing image", error)
                                }
                            )
                        }
                        
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(48.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // Show the bottom sheet with plant identification results
                        identificationResult?.let { result ->
                            if (showPlantIdentification) {
                                PlantIdentificationBottomSheet(
                                    plantResult = result,
                                    onDismiss = { showPlantIdentification = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                hasCameraPermission = true
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
fun CameraContent(
    hasCameraPermission: Boolean,
    onImageCaptured: (Bitmap) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreviewScreen(
                onImageCaptured = onImageCaptured,
                onError = onError
            )
        } else {
            Text(
                "Camera permission is required",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun CameraPreviewScreen(
    onImageCaptured: (Bitmap) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    // Preview
                    val preview = CameraPreview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // Image capture
                    imageCapture = ImageCapture.Builder().build()

                    try {
                        // Unbind any previous use cases
                        cameraProvider.unbindAll()

                        // Bind use cases to camera
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Use case binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Capture button
        FloatingActionButton(
            onClick = {
                val imgCapture = imageCapture ?: return@FloatingActionButton

                imgCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            super.onCaptureSuccess(image)
                            val bitmap = image.toBitmap()
                            onImageCaptured(bitmap)
                            image.close()
                        }

                        override fun onError(exception: ImageCaptureException) {
                            super.onError(exception)
                            onError(exception)
                        }
                    }
                )
            },
            modifier = Modifier
                .padding(bottom = 24.dp)
                .align(Alignment.BottomCenter)
                .size(72.dp),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Take Photo",
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

private fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    buffer.rewind()
    val bytes = ByteArray(buffer.capacity())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PlantsTheme {
        Greeting("Android")
    }
}