package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.ScannedReceiptResult
import com.example.data.model.ExpenseCategory
import com.example.data.model.ScannedItem
import com.example.data.model.TransactionType
import com.example.ui.components.CameraXReceiptScanner
import com.example.ui.components.CategoryIcon
import com.example.ui.theme.CatGroceries
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import java.io.InputStream

@Composable
fun ReceiptScannerScreen(
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
    startWithCameraX: Boolean = true
) {
    val context = LocalContext.current

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<ScannedReceiptResult?>(null) }
    var statusText by remember { mutableStateOf("Take a picture or select an image to scan receipt") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showLiveCameraX by remember { mutableStateOf(startWithCameraX) }

    if (showLiveCameraX) {
        CameraXReceiptScanner(
            onExpenseSaved = { title, amount, category, isScanned, items, tax, merchant ->
                onExpenseSaved(title, amount, category, isScanned, items, tax, merchant)
                showLiveCameraX = false
            },
            onNavigateBack = {
                showLiveCameraX = false
                onNavigateBack()
            },
            onScanRequest = onScanRequest
        )
        return
    }

    // Editable fields after extraction
    var editMerchant by remember { mutableStateOf("") }
    var editAmount by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf(ExpenseCategory.GROCERIES) }

    // Photo picker launcher
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
                    scanReceiptBitmap(bitmap, onScanRequest, { res ->
                        scanResult = res
                        editMerchant = res.merchant
                        editAmount = "%.2f".format(res.amount)
                        editCategory = ExpenseCategory.fromString(res.category)
                        isAnalyzing = false
                    }, { err ->
                        errorMessage = err
                        isAnalyzing = false
                    })
                    isAnalyzing = true
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load image: ${e.message}"
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            scanReceiptBitmap(bitmap, onScanRequest, { res ->
                scanResult = res
                editMerchant = res.merchant
                editAmount = "%.2f".format(res.amount)
                editCategory = ExpenseCategory.fromString(res.category)
                isAnalyzing = false
            }, { err ->
                errorMessage = err
                isAnalyzing = false
            })
            isAnalyzing = true
        }
    }

    var showCameraPermissionDeniedDialog by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            showCameraPermissionDeniedDialog = true
        }
    }

    val onTakePhotoClick: () -> Unit = {
        showLiveCameraX = true
    }

    // Animation for scanning beam
    val infiniteTransition = rememberInfiniteTransition(label = "scan_beam")
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beam_offset"
    )

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Smart Receipt Scanner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scanner Viewport Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .testTag("receipt_scanner_viewport"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (capturedBitmap != null) {
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Captured Receipt",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                        )
                    } else {
                        // Viewfinder placeholder
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Scan Paper or Digital Receipt",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "AI automatically extracts merchant, date, total, and line items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Scanning beam animation when analyzing
                    if (isAnalyzing) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.04f)
                                .offset(y = (260 * scanOffset).dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            EmeraldLight.copy(alpha = 0.8f),
                                            EmeraldLight,
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    // Status pill at top
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = EmeraldPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Analyzing receipt with AI...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EmeraldPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (scanResult != null) "Analysis Complete" else "Ready to Scan",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons (Live CameraX Viewfinder & Photo Picker)
            Button(
                onClick = { showLiveCameraX = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("launch_camerax_scanner_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(Icons.Default.Videocam, contentDescription = "CameraX", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Live CameraX Viewfinder", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onTakePhotoClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("camera_scan_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Take Photo")
                }

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("gallery_picker_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Photo")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fast emulator testing preset buttons
            Text(
                text = "Quick Test Receipts (For Instant Emulator Testing):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    onClick = {
                        val sample = generateSampleReceiptBitmap("Trader Joe's", "$24.99", "Groceries")
                        capturedBitmap = sample
                        scanReceiptBitmap(sample, onScanRequest, { res ->
                            scanResult = res
                            editMerchant = res.merchant
                            editAmount = "%.2f".format(res.amount)
                            editCategory = ExpenseCategory.fromString(res.category)
                            isAnalyzing = false
                        }, { err ->
                            errorMessage = err
                            isAnalyzing = false
                        })
                        isAnalyzing = true
                    },
                    label = { Text("Trader Joe's") },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("sample_trader_joes_button")
                )

                SuggestionChip(
                    onClick = {
                        val sample = generateSampleReceiptBitmap("Starbucks Coffee", "$14.50", "Coffee")
                        capturedBitmap = sample
                        scanReceiptBitmap(sample, onScanRequest, { res ->
                            scanResult = res
                            editMerchant = res.merchant
                            editAmount = "%.2f".format(res.amount)
                            editCategory = ExpenseCategory.fromString(res.category)
                            isAnalyzing = false
                        }, { err ->
                            errorMessage = err
                            isAnalyzing = false
                        })
                        isAnalyzing = true
                    },
                    label = { Text("Cafe Receipt") },
                    icon = { Icon(Icons.Default.Coffee, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("sample_cafe_button")
                )

                SuggestionChip(
                    onClick = {
                        val sample = generateSampleReceiptBitmap("Shell Fuel Station", "$45.00", "Gasoline")
                        capturedBitmap = sample
                        scanReceiptBitmap(sample, onScanRequest, { res ->
                            scanResult = res
                            editMerchant = res.merchant
                            editAmount = "%.2f".format(res.amount)
                            editCategory = ExpenseCategory.fromString(res.category)
                            isAnalyzing = false
                        }, { err ->
                            errorMessage = err
                            isAnalyzing = false
                        })
                        isAnalyzing = true
                    },
                    label = { Text("Fuel Station") },
                    icon = { Icon(Icons.Default.LocalGasStation, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("sample_gas_button")
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Extracted Review Card
            AnimatedVisibility(visible = scanResult != null) {
                val result = scanResult
                if (result != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp)
                            .testTag("scanned_result_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Extracted Details",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldPrimary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "AI Verified",
                                        color = EmeraldPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = editMerchant,
                                onValueChange = { editMerchant = it },
                                label = { Text("Merchant / Store Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("scanned_merchant_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = editAmount,
                                onValueChange = { editAmount = it },
                                label = { Text("Total Amount") },
                                prefix = { Text("$ ", fontWeight = FontWeight.Bold) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("scanned_amount_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Category",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ExpenseCategory.entries.filter { it != ExpenseCategory.INCOME }.forEach { cat ->
                                    FilterChip(
                                        selected = editCategory == cat,
                                        onClick = { editCategory = cat },
                                        label = { Text(cat.displayName) },
                                        leadingIcon = { CategoryIcon(category = cat, size = 20.dp, iconSize = 12.dp) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }

                            if (result.items.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Line Items Detected (${result.items.size}):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                result.items.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "• ${item.name}", fontSize = 12.sp)
                                        Text(text = "$${"%.2f".format(item.price)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    val amount = editAmount.toDoubleOrNull() ?: result.amount
                                    val finalItems = result.items.map { ScannedItem(it.name, it.price) }
                                    onExpenseSaved(
                                        editMerchant.ifBlank { "Receipt Purchase" },
                                        amount,
                                        editCategory,
                                        true,
                                        finalItems,
                                        result.tax,
                                        editMerchant
                                    )
                                    onNavigateBack()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("confirm_scanned_expense_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Extracted Expense", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCameraPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionDeniedDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Camera permission is needed to take a live photo of a receipt. You can grant access now, or use the photo library and test presets below to scan receipts.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCameraPermissionDeniedDialog = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.testTag("dialog_grant_camera_permission_button")
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCameraPermissionDeniedDialog = false },
                    modifier = Modifier.testTag("dialog_dismiss_camera_permission_button")
                ) {
                    Text("Dismiss")
                }
            },
            modifier = Modifier.testTag("scanner_camera_permission_dialog")
        )
    }
}

private fun scanReceiptBitmap(
    bitmap: Bitmap,
    onScanRequest: (Bitmap, (ScannedReceiptResult) -> Unit, (String) -> Unit) -> Unit,
    onSuccess: (ScannedReceiptResult) -> Unit,
    onError: (String) -> Unit
) {
    onScanRequest(bitmap, onSuccess, onError)
}

// Generate realistic synthetic receipt bitmap for testing in emulator without camera
private fun generateSampleReceiptBitmap(storeName: String, total: String, category: String): Bitmap {
    val width = 400
    val height = 550
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // White receipt paper background
    val bgPaint = Paint().apply { color = AndroidColor.WHITE }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Store Title
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
