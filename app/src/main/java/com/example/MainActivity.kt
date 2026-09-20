package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionType
import com.example.ui.ExpenseViewModel
import com.example.ui.screens.*
import com.example.ui.theme.ExpenseManagerTheme
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    TRANSACTIONS("Transactions", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    BUDGETS("Budgets", Icons.Filled.PieChart, Icons.Outlined.PieChart),
    SYNC("Sync Hub", Icons.Filled.CloudSync, Icons.Outlined.CloudSync)
}

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ExpenseManagerTheme {
                ExpenseApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseApp(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showReceiptScanner by remember { mutableStateOf(false) }
    var showCameraRationaleDialog by remember { mutableStateOf(false) }

    // Runtime Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showReceiptScanner = true
        } else {
            showCameraRationaleDialog = true
        }
    }

    // Function to check permission before opening scanner
    val requestCameraPermissionAndOpenScanner: () -> Unit = {
        val permissionCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        )
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            showReceiptScanner = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val filteredExpenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()
    val summary by viewModel.monthlySummary.collectAsStateWithLifecycle()
    val categoryProgresses by viewModel.categoryBudgetProgresses.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val allRecurringExpenses by viewModel.allRecurringExpenses.collectAsStateWithLifecycle()
    val filterOnlyRecurring by viewModel.filterOnlyRecurring.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedType.collectAsStateWithLifecycle()

    if (showReceiptScanner) {
        ReceiptScannerScreen(
            onExpenseSaved = { title, amount, category, isScanned, items, tax, merchant ->
                viewModel.addExpense(
                    title = title,
                    amount = amount,
                    category = category,
                    isReceiptScanned = isScanned,
                    items = items,
                    taxAmount = tax,
                    merchantName = merchant
                )
            },
            onNavigateBack = { showReceiptScanner = false },
            onScanRequest = { bitmap, onSuccess, onError ->
                viewModel.scanReceipt(bitmap, onSuccess, onError)
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Expense Manager",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { requestCameraPermissionAndOpenScanner() },
                            modifier = Modifier.testTag("appbar_scan_receipt_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = "Scan Receipt",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    AppScreen.entries.forEach { screen ->
                        val selected = currentScreen == screen
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                        )
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddExpenseDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("main_add_expense_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    AppScreen.DASHBOARD -> {
                        DashboardScreen(
                            summary = summary,
                            recentExpenses = allExpenses,
                            categoryProgresses = categoryProgresses,
                            syncStatus = syncStatus,
                            onScanReceiptClick = { requestCameraPermissionAndOpenScanner() },
                            onAddExpenseClick = { showAddExpenseDialog = true },
                            onViewBudgetsClick = { currentScreen = AppScreen.BUDGETS },
                            onViewSyncClick = { currentScreen = AppScreen.SYNC },
                            onViewAllTransactionsClick = { currentScreen = AppScreen.TRANSACTIONS },
                            onDeleteExpense = { id -> viewModel.deleteExpense(id) }
                        )
                    }

                    AppScreen.TRANSACTIONS -> {
                        TransactionsScreen(
                            expenses = filteredExpenses,
                            searchQuery = searchQuery,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            selectedCategory = selectedCategoryFilter,
                            onCategoryChange = { viewModel.setCategoryFilter(it) },
                            selectedType = selectedTypeFilter,
                            onTypeChange = { viewModel.setTypeFilter(it) },
                            onDeleteExpense = { id -> viewModel.deleteExpense(id) },
                            recurringExpenses = allRecurringExpenses,
                            filterOnlyRecurring = filterOnlyRecurring,
                            onToggleRecurringFilter = { viewModel.setFilterOnlyRecurring(it) },
                            onDeleteRecurring = { id -> viewModel.deleteRecurringExpense(id) },
                            onToggleRecurringActive = { id, active -> viewModel.toggleRecurringExpenseActive(id, active) }
                        )
                    }

                    AppScreen.BUDGETS -> {
                        BudgetScreen(
                            summary = summary,
                            categoryProgresses = categoryProgresses,
                            onSetBudget = { cat, limit -> viewModel.setBudgetLimit(cat, limit) }
                        )
                    }

                    AppScreen.SYNC -> {
                        MultiDeviceSyncScreen(
                            syncStatus = syncStatus,
                            onJoinSyncGroup = { newGroup -> viewModel.updateSyncGroup(newGroup) },
                            onSimulateRemoteSync = { onDone -> viewModel.simulateRemoteDeviceSync(onDone) }
                        )
                    }
                }
            }
        }
    }

    if (showCameraRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showCameraRationaleDialog = false },
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
                    text = "Camera access is needed to capture and scan physical receipts in real time. You can grant camera permission to use the viewfinder, or proceed to the scanner to import receipt photos from your gallery.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCameraRationaleDialog = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.testTag("grant_camera_permission_button")
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCameraRationaleDialog = false
                        showReceiptScanner = true
                    },
                    modifier = Modifier.testTag("continue_without_camera_button")
                ) {
                    Text("Continue with Photos")
                }
            },
            modifier = Modifier.testTag("camera_permission_rationale_dialog")
        )
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onSave = { title, amount, category, type, note ->
                viewModel.addExpense(
                    title = title,
                    amount = amount,
                    category = category,
                    type = type,
                    note = note
                )
            },
            onSaveRecurring = { title, amount, category, type, frequency, note ->
                viewModel.scheduleRecurringExpense(
                    title = title,
                    amount = amount,
                    category = category,
                    type = type,
                    frequency = frequency,
                    note = note,
                    createFirstOccurrenceImmediately = true
                )
            }
        )
    }
}

// Retained for tests and backward compatibility
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
