package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.ReceiptScannerService
import com.example.data.ai.ScannedReceiptResult
import com.example.data.local.CategoryEntity
import com.example.data.model.*
import com.example.data.repository.ExpenseRepository
import com.example.data.sync.SyncStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class MonthlyFinanceSummary(
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netSavings: Double = 0.0,
    val overallBudget: Double = 0.0,
    val overallBudgetSpentPercent: Float = 0f,
    val overallRemaining: Double = 0.0,
    val daysRemaining: Int = 1,
    val dailySafeSpend: Double = 0.0,
    val alerts: List<String> = emptyList()
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ExpenseRepository(application, viewModelScope)
    private val scannerService = ReceiptScannerService()

    val syncStatus: StateFlow<SyncStatus> = repository.syncManager.syncStatus

    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allBudgets: StateFlow<List<Budget>> = repository.allBudgets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allRecurringExpenses: StateFlow<List<RecurringExpense>> = repository.allRecurringExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeRecurringExpenses: StateFlow<List<RecurringExpense>> = repository.activeRecurringExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ExpenseCategory?>(null)
    val selectedCategory: StateFlow<ExpenseCategory?> = _selectedCategory.asStateFlow()

    private val _selectedType = MutableStateFlow<TransactionType?>(null)
    val selectedType: StateFlow<TransactionType?> = _selectedType.asStateFlow()

    private val _filterOnlyRecurring = MutableStateFlow(false)
    val filterOnlyRecurring: StateFlow<Boolean> = _filterOnlyRecurring.asStateFlow()

    private val _isScanningReceipt = MutableStateFlow(false)
    val isScanningReceipt: StateFlow<Boolean> = _isScanningReceipt.asStateFlow()

    val currentMonthYear: String
        get() = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    // Filtered Expenses
    val filteredExpenses: StateFlow<List<Expense>> = combine(
        allExpenses,
        searchQuery,
        selectedCategory,
        selectedType,
        filterOnlyRecurring
    ) { expenses, query, cat, type, onlyRecurring ->
        expenses.filter { expense ->
            val matchesQuery = query.isBlank() ||
                    expense.title.contains(query, ignoreCase = true) ||
                    (expense.merchantName?.contains(query, ignoreCase = true) == true) ||
                    expense.note.contains(query, ignoreCase = true)

            val matchesCategory = cat == null || expense.category == cat
            val matchesType = type == null || expense.type == type
            val matchesRecurring = !onlyRecurring || expense.isRecurring

            matchesQuery && matchesCategory && matchesType && matchesRecurring
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Category Budget Progresses
    val categoryBudgetProgresses: StateFlow<List<CategoryBudgetProgress>> = combine(
        allExpenses,
        allBudgets
    ) { expenses, budgets ->
        val currentMonth = currentMonthYear
        val currentMonthExpenses = expenses.filter {
            val dateStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.date))
            dateStr == currentMonth && it.type == TransactionType.EXPENSE
        }

        val monthBudgets = budgets.filter { it.monthYear == currentMonth }

        val list = mutableListOf<CategoryBudgetProgress>()

        // Specific category budgets
        ExpenseCategory.entries.filter { it != ExpenseCategory.INCOME }.forEach { category ->
            val budget = monthBudgets.firstOrNull { it.category == category.name }
            val spent = currentMonthExpenses.filter { it.category == category }.sumOf { it.amount }
            if (budget != null || spent > 0) {
                val limit = budget?.monthlyLimit ?: 0.0
                val remaining = limit - spent
                val percent = if (limit > 0) (spent / limit).toFloat() else if (spent > 0) 1.5f else 0f
                list.add(
                    CategoryBudgetProgress(
                        category = category,
                        isOverall = false,
                        budgetLimit = limit,
                        spentAmount = spent,
                        remainingAmount = remaining,
                        percentSpent = percent,
                        isExceeded = limit > 0 && spent > limit,
                        isWarning = limit > 0 && percent >= 0.8f && spent <= limit
                    )
                )
            }
        }
        list.sortedByDescending { it.percentSpent }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Monthly Financial Summary
    val monthlySummary: StateFlow<MonthlyFinanceSummary> = combine(
        allExpenses,
        allBudgets
    ) { expenses, budgets ->
        val currentMonth = currentMonthYear
        val cal = Calendar.getInstance()
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val daysRemaining = maxOf(1, daysInMonth - currentDay)

        val monthExpenses = expenses.filter {
            SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.date)) == currentMonth
        }

        val totalExpense = monthExpenses.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val totalIncome = monthExpenses.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val netSavings = totalIncome - totalExpense

        val overallBudgetObj = budgets.firstOrNull { it.monthYear == currentMonth && it.category == "ALL" }
        val overallLimit = overallBudgetObj?.monthlyLimit ?: 0.0
        val remaining = overallLimit - totalExpense
        val percent = if (overallLimit > 0) (totalExpense / overallLimit).toFloat() else 0f
        val dailySafe = if (remaining > 0) remaining / daysRemaining else 0.0

        val alerts = mutableListOf<String>()
        if (overallLimit > 0) {
            if (totalExpense >= overallLimit) {
                alerts.add("Overall monthly budget exceeded by $${"%.2f".format(totalExpense - overallLimit)}")
            } else if (percent >= 0.85f) {
                alerts.add("Overall budget alert: ${(percent * 100).toInt()}% of budget spent with $daysRemaining days left")
            }
        }

        // Check category alerts
        budgets.filter { it.monthYear == currentMonth && it.category != "ALL" }.forEach { b ->
            val cat = ExpenseCategory.fromString(b.category)
            val catSpent = monthExpenses.filter { it.type == TransactionType.EXPENSE && it.category == cat }.sumOf { it.amount }
            if (catSpent > b.monthlyLimit) {
                alerts.add("${cat.displayName} exceeded by $${"%.2f".format(catSpent - b.monthlyLimit)}")
            } else if (b.monthlyLimit > 0 && (catSpent / b.monthlyLimit) >= 0.85f) {
                alerts.add("${cat.displayName} has used ${((catSpent / b.monthlyLimit) * 100).toInt()}% of limit")
            }
        }

        MonthlyFinanceSummary(
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            netSavings = netSavings,
            overallBudget = overallLimit,
            overallBudgetSpentPercent = percent,
            overallRemaining = remaining,
            daysRemaining = daysRemaining,
            dailySafeSpend = dailySafe,
            alerts = alerts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlyFinanceSummary()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: ExpenseCategory?) {
        _selectedCategory.value = category
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedType.value = type
    }

    fun setFilterOnlyRecurring(onlyRecurring: Boolean) {
        _filterOnlyRecurring.value = onlyRecurring
    }

    fun addExpense(
        title: String,
        amount: Double,
        category: ExpenseCategory,
        date: Long = System.currentTimeMillis(),
        type: TransactionType = TransactionType.EXPENSE,
        note: String = "",
        merchantName: String? = null,
        isReceiptScanned: Boolean = false,
        items: List<ScannedItem> = emptyList(),
        taxAmount: Double = 0.0,
        receiptUri: String? = null,
        isRecurring: Boolean = false,
        recurringFrequency: String? = null
    ) {
        viewModelScope.launch {
            val expense = Expense(
                id = UUID.randomUUID().toString(),
                title = title.ifBlank { merchantName ?: category.displayName },
                amount = amount,
                category = category,
                date = date,
                type = type,
                note = note,
                receiptUri = receiptUri,
                merchantName = merchantName,
                isReceiptScanned = isReceiptScanned,
                items = items,
                taxAmount = taxAmount,
                isRecurring = isRecurring,
                recurringFrequency = recurringFrequency,
                deviceId = repository.syncManager.currentDeviceId
            )
            repository.addOrUpdateExpense(expense)
        }
    }

    fun scheduleRecurringExpense(
        title: String,
        amount: Double,
        category: ExpenseCategory,
        type: TransactionType = TransactionType.EXPENSE,
        frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
        startDate: Long = System.currentTimeMillis(),
        note: String = "",
        createFirstOccurrenceImmediately: Boolean = true
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val recurringId = UUID.randomUUID().toString()

            // If user wants first occurrence immediately, record it today as a transaction
            if (createFirstOccurrenceImmediately) {
                val immediateExpense = Expense(
                    id = UUID.randomUUID().toString(),
                    title = title.ifBlank { category.displayName },
                    amount = amount,
                    category = category,
                    date = startDate,
                    type = type,
                    note = if (note.isNotBlank()) "$note • [${frequency.displayName} recurring]" else "[${frequency.displayName} recurring]",
                    isRecurring = true,
                    recurringFrequency = frequency.displayName,
                    deviceId = repository.syncManager.currentDeviceId
                )
                repository.addOrUpdateExpense(immediateExpense)
            }

            // Next due date advances by 1 recurrence interval from start date
            val nextDue = com.example.util.RecurrenceCalculator.computeNextDueDate(startDate, frequency)
            val recurringRule = RecurringExpense(
                id = recurringId,
                title = title.ifBlank { category.displayName },
                amount = amount,
                category = category,
                type = type,
                frequency = frequency,
                startDate = startDate,
                nextDueDate = nextDue,
                lastGeneratedDate = if (createFirstOccurrenceImmediately) startDate else null,
                isActive = true,
                note = note,
                updatedAt = now
            )
            repository.addOrUpdateRecurringExpense(recurringRule)
        }
    }

    fun toggleRecurringExpenseActive(id: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.setRecurringExpenseActive(id, isActive)
        }
    }

    fun deleteRecurringExpense(id: String) {
        viewModelScope.launch {
            repository.deleteRecurringExpense(id)
        }
    }

    fun processDueRecurringExpenses() {
        viewModelScope.launch {
            repository.processDueRecurringExpenses()
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            repository.deleteExpense(id)
        }
    }

    fun setBudgetLimit(categoryName: String, limit: Double) {
        viewModelScope.launch {
            val month = currentMonthYear
            val budgetId = "${categoryName}_$month"
            val budget = Budget(
                id = budgetId,
                category = categoryName,
                monthlyLimit = limit,
                monthYear = month
            )
            repository.setBudget(budget)
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            repository.deleteBudget(id)
        }
    }

    fun addOrUpdateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.addOrUpdateCategory(category)
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    fun scanReceipt(
        bitmap: Bitmap,
        onSuccess: (ScannedReceiptResult) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isScanningReceipt.value = true
            try {
                val result = scannerService.analyzeReceipt(bitmap)
                onSuccess(result)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to scan receipt")
            } finally {
                _isScanningReceipt.value = false
            }
        }
    }

    fun updateSyncGroup(newGroupId: String) {
        repository.syncManager.updateSyncGroupId(newGroupId)
    }

    fun simulateRemoteDeviceSync(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val msg = repository.syncManager.simulateRemoteDeviceEvent()
            onComplete(msg)
        }
    }
}
