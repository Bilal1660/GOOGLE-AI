package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.ai.ScannedReceiptResult
import com.example.data.model.ExpenseCategory
import com.example.data.model.ScannedItem
import com.example.ui.theme.CatGroceries
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import java.io.InputStream
import java.util.concurrent.Executor

/**
 * CameraX receipt scanner UI component that captures images and processes them
 * using the Gemini API to extract transaction details.
 */
@Composable
fun CameraXReceiptScanner(
    onExpenseSaved: (
        title: String,
        amount: Double,
        category: ExpenseCategory,
        isScanned: Boolean,
        items: List<ScannedItem>,
        taxAmount: Double,
        merchantName: String
    ) -> Unit,
    onNavigateBack: () -> Unit,
    onScanRequest: (Bitmap, (ScannedReceiptResult) -> Unit, (String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Scanning states
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<ScannedReceiptResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Camera settings
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    val imageCaptureUseCase = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    // Visual photo picker fallback/alternative
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    capturedBitmap = bitmap
                    isAnalyzing = true
                    errorMessage = null
                    onScanRequest(
                        bitmap,
                        { res ->
                            scanResult = res
                            isAnalyzing = false
                        },
                        { err ->
                            errorMessage = err
                            isAnalyzing = false
                        }
                    )
                }
            } catch (e: Exception) {
                errorMessage = "Could not load selected photo: ${e.message}"
            }
        }
    }

    // Scanner beam animation
    val infiniteTransition = rememberInfiniteTransition(label = "camerax_scan_beam")
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beam_offset"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CameraX Receipt Scanner",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Powered by Gemini 3.5 Flash",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("camerax_scanner_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (capturedBitmap == null && hasCameraPermission) {
                        IconButton(
                            onClick = {
                                isTorchEnabled = !isTorchEnabled
                                cameraInstance?.cameraControl?.enableTorch(isTorchEnabled)
                            },
                            modifier = Modifier.testTag("camerax_torch_button")
                        ) {
                            Icon(
                                imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Toggle Flash",
                                tint = if (isTorchEnabled) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            },
                            modifier = Modifier.testTag("camerax_flip_camera_button")
                        ) {
                            Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip Camera")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (scanResult != null && capturedBitmap != null) {
                // Review & Edit extracted transaction details
                ExtractedReceiptDetailsView(
                    bitmap = capturedBitmap!!,
                    result = scanResult!!,
                    onSave = { merchant, amount, category, items, tax ->
                        onExpenseSaved(
                            merchant,
                            amount,
                            category,
                            true,
                            items,
                            tax,
                            merchant
                        )
                    },
                    onRetake = {
                        capturedBitmap = null
                        scanResult = null
                        errorMessage = null
                        isAnalyzing = false
                    }
                )
            } else {
                // Camera Viewfinder or Permission State
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!hasCameraPermission) {
                        // Permission request state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = EmeraldPrimary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = null,
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Camera Access Required",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Camera access is needed to capture physical receipts in real-time with CameraX and process them with Gemini AI.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                        modifier = Modifier.testTag("grant_camerax_permission_button")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Grant Camera Access")
                                    }
                                }
                            }
                        }
                    } else {
                        // Live CameraX Preview Area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color.Black)
                        ) {
                            if (capturedBitmap == null) {
                                AndroidView(
                                    factory = { ctx ->
                                        PreviewView(ctx).apply {
                                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                        }
                                    },
                                    update = { previewView ->
                                        val ctx = previewView.context
                                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                        cameraProviderFuture.addListener({
                                            try {
                                                val cameraProvider = cameraProviderFuture.get()
                                                val preview = Preview.Builder().build().also {
                                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                                }
                                                val cameraSelector = CameraSelector.Builder()
                                                    .requireLensFacing(lensFacing)
                                                    .build()

                                                cameraProvider.unbindAll()
                                                val camera = cameraProvider.bindToLifecycle(
                                                    lifecycleOwner,
                                                    cameraSelector,
                                                    preview,
                                                    imageCaptureUseCase
                                                )
                                                cameraInstance = camera
                                                camera.cameraControl.enableTorch(isTorchEnabled)
                                            } catch (e: Exception) {
                                                errorMessage = "Camera initialization notice: ${e.message}"
                                            }
                                        }, ContextCompat.getMainExecutor(ctx))
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("camerax_preview_view")
                                )

                                // Reticle Framing Guide
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(380.dp)
                                            .border(
                                                BorderStroke(2.dp, EmeraldPrimary.copy(alpha = 0.85f)),
                                                RoundedCornerShape(16.dp)
                                            )
                                    ) {
                                        // Animated Scan Line
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(3.dp)
                                                .offset(y = (380 * scanOffset).dp)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color.Transparent, EmeraldLight, EmeraldPrimary, Color.Transparent)
                                                    )
                                                )
                                        )
                                    }
                                }

                                // Framing hint pill
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CropFree,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Align receipt inside the frame",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else {
                                // Show captured bitmap while analyzing
                                Image(
                                    bitmap = capturedBitmap!!.asImageBitmap(),
                                    contentDescription = "Captured Image",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Analyzing overlay
                            if (isAnalyzing) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.65f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.padding(32.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CircularProgressIndicator(
                                                color = EmeraldPrimary,
                                                modifier = Modifier.size(48.dp),
                                                strokeWidth = 4.dp
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "Analyzing with Gemini AI...",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Extracting merchant, totals, items, and taxes with high precision",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Controls Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Photo library button
                                IconButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .size(52.dp)
                                        .testTag("camerax_gallery_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "Select from Gallery",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Large Shutter Capture Button
                                Button(
                                    onClick = {
                                        if (hasCameraPermission && !isAnalyzing) {
                                            captureFromCameraX(
                                                context = context,
                                                imageCapture = imageCaptureUseCase,
                                                onSuccess = { bitmap ->
                                                    capturedBitmap = bitmap
                                                    isAnalyzing = true
                                                    errorMessage = null
                                                    onScanRequest(
                                                        bitmap,
                                                        { res ->
                                                            scanResult = res
                                                            isAnalyzing = false
                                                        },
                                                        { err ->
                                                            errorMessage = err
                                                            isAnalyzing = false
                                                        }
                                                    )
                                                },
                                                onError = { err ->
                                                    errorMessage = "Capture failed: $err"
                                                }
                                            )
                                        }
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier
                                        .size(72.dp)
                                        .testTag("camerax_capture_button"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Capture Receipt",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                // Presets / Sample receipt button (convenient for testing)
                                IconButton(
                                    onClick = {
                                        // Generate simulated receipt for testing in emulator
                                        val testBitmap = generateCameraXSampleReceipt("Trader Joe's Market", "$24.99")
                                        capturedBitmap = testBitmap
                                        isAnalyzing = true
                                        errorMessage = null
                                        onScanRequest(
                                            testBitmap,
                                            { res ->
                                                scanResult = res
                                                isAnalyzing = false
                                            },
                                            { err ->
                                                errorMessage = err
                                                isAnalyzing = false
                                            }
                                        )
                                    },
                                    modifier = Modifier
                                        .size(52.dp)
                                        .testTag("camerax_sample_receipt_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Use Sample Receipt",
                                        tint = EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Capture frame from CameraX ImageCapture and process into a rotated Bitmap.
 */
private fun captureFromCameraX(
    context: Context,
    imageCapture: ImageCapture,
    onSuccess: (Bitmap) -> Unit,
    onError: (String) -> Unit
) {
    val executor: Executor = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val rotation = image.imageInfo.rotationDegrees
                    val rawBitmap = image.toBitmap()
                    val finalBitmap = if (rotation != 0) {
                        val matrix = Matrix()
                        matrix.postRotate(rotation.toFloat())
                        Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                    } else {
                        rawBitmap
                    }
                    onSuccess(finalBitmap)
                } catch (e: Exception) {
                    onError(e.message ?: "Failed to convert image")
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception.message ?: "Camera capture error")
            }
        }
    )
}

/**
 * Review, verify, and edit transaction details extracted by Gemini API.
 */
@Composable
fun ExtractedReceiptDetailsView(
    bitmap: Bitmap,
    result: ScannedReceiptResult,
    onSave: (
        merchant: String,
        amount: Double,
        category: ExpenseCategory,
        items: List<ScannedItem>,
        tax: Double
    ) -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier
) {
    var merchant by remember { mutableStateOf(result.merchant) }
    var amountStr by remember { mutableStateOf("%.2f".format(result.amount)) }
    var selectedCategory by remember {
        mutableStateOf(ExpenseCategory.fromString(result.category))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Receipt Thumbnail + Gemini AI Badge
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Scanned Receipt Image",
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Gemini AI Verified",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Form Fields
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Extracted Transaction Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Store Name") },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("extracted_merchant_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Total Amount") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("extracted_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category selector
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        ExpenseCategory.GROCERIES,
                        ExpenseCategory.FOOD,
                        ExpenseCategory.SHOPPING,
                        ExpenseCategory.TRANSPORT
                    ).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.displayName, fontSize = 11.sp) },
                            leadingIcon = {
                                CategoryIcon(category = cat, size = 18.dp, iconSize = 12.dp)
                            }
                        )
                    }
                }

                // Itemized Breakdown
                if (result.items.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Text(
                        text = "Itemized Items (${result.items.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldPrimary
                    )

                    result.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$${"%.2f".format(item.price)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (result.tax > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tax",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$${"%.2f".format(result.tax)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("retake_receipt_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retake")
            }

            Button(
                onClick = {
                    val finalAmount = amountStr.toDoubleOrNull() ?: result.amount
                    val scannedItems = result.items.map { ScannedItem(it.name, it.price) }
                    onSave(
                        merchant.ifBlank { "Receipt Expense" },
                        finalAmount,
                        selectedCategory,
                        scannedItems,
                        result.tax
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("save_scanned_expense_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Expense")
            }
        }
    }
}

private fun generateCameraXSampleReceipt(storeName: String, total: String): Bitmap {
    val width = 400
    val height = 550
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // White receipt paper background
    canvas.drawColor(AndroidColor.WHITE)

    val borderPaint = Paint().apply {
        color = AndroidColor.LTGRAY
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawRect(8f, 8f, (width - 8).toFloat(), (height - 8).toFloat(), borderPaint)

    // Header
    val titlePaint = Paint().apply {
        color = AndroidColor.DKGRAY
        textSize = 28f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(storeName, width / 2f, 60f, titlePaint)

    // Subtitle
    val textPaint = Paint().apply {
        color = AndroidColor.GRAY
        textSize = 18f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("TAX INVOICE & RECEIPT", width / 2f, 95f, textPaint)
    canvas.drawText("Date: 2026-09-18", width / 2f, 125f, textPaint)

    // Divider
    val linePaint = Paint().apply {
        color = AndroidColor.LTGRAY
        strokeWidth = 2f
    }
    canvas.drawLine(40f, 150f, (width - 40).toFloat(), 150f, linePaint)

    // Items
    val itemPaint = Paint().apply {
        color = AndroidColor.BLACK
        textSize = 20f
        isAntiAlias = true
    }
    canvas.drawText("Item A - Premium", 40f, 200f, itemPaint)
    canvas.drawText("$12.00", 300f, 200f, itemPaint)

    canvas.drawText("Item B - Standard", 40f, 240f, itemPaint)
    canvas.drawText("$8.50", 300f, 240f, itemPaint)

    canvas.drawText("Item C - Organic", 40f, 280f, itemPaint)
    canvas.drawText("$4.49", 300f, 280f, itemPaint)

    canvas.drawLine(40f, 320f, (width - 40).toFloat(), 320f, linePaint)

    // Total
    val totalPaint = Paint().apply {
        color = AndroidColor.BLACK
        textSize = 26f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    canvas.drawText("TOTAL", 40f, 370f, totalPaint)
    canvas.drawText(total, 280f, 370f, totalPaint)

    return bitmap
}

