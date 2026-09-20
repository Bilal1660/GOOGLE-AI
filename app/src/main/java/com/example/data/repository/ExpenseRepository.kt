package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.RecurringExpenseEntity
import com.example.data.model.*
import com.example.data.sync.SyncManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ExpenseRepository(
    private val context: Context,
    val scope: CoroutineScope
) {
    private val database = AppDatabase.getDatabase(context)
    private val expenseDao = database.expenseDao()
    private val budgetDao = database.budgetDao()
    private val categoryDao = database.categoryDao()
    private val recurringExpenseDao = database.recurringExpenseDao()
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    val syncManager = SyncManager(context, expenseDao, budgetDao, scope)

    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses().map { list ->
        list.map { it.toExpense(moshi) }
    }

    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets().map { list ->
        list.map { it.toBudget() }
    }

    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    val allRecurringExpenses: Flow<List<RecurringExpense>> = recurringExpenseDao.getAllRecurringExpenses().map { list ->
        list.map { it.toRecurringExpense() }
    }

    val activeRecurringExpenses: Flow<List<RecurringExpense>> = recurringExpenseDao.getActiveRecurringExpenses().map { list ->
        list.map { it.toRecurringExpense() }
    }

    init {
        scope.launch(Dispatchers.IO) {
            checkAndSeedInitialData()
            processDueRecurringExpenses()
        }
    }

    private suspend fun checkAndSeedInitialData() {
        val prefs = context.getSharedPreferences("expense_seed_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("is_seeded_v1", false)) {
            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val now = System.currentTimeMillis()
            val dayMillis = 24 * 60 * 60 * 1000L

            // Initial Categories
            val defaultCategories = ExpenseCategory.entries.map { cat ->
                val defaultLimit = when (cat) {
                    ExpenseCategory.FOOD -> 450.0
                    ExpenseCategory.GROCERIES -> 400.0
                    ExpenseCategory.SHOPPING -> 300.0
                    ExpenseCategory.TRANSPORT -> 200.0
                    ExpenseCategory.ENTERTAINMENT -> 150.0
                    ExpenseCategory.BILLS -> 600.0
                    else -> 0.0
                }
                CategoryEntity.fromExpenseCategory(cat, defaultLimit)
            }
            categoryDao.insertOrUpdateAll(defaultCategories)

            // Initial Budgets
            val initialBudgets = listOf(
                BudgetEntity("OVERALL_$currentMonth", "ALL", 2500.0, currentMonth, now),
                BudgetEntity("FOOD_$currentMonth", ExpenseCategory.FOOD.name, 450.0, currentMonth, now),
                BudgetEntity("GROCERIES_$currentMonth", ExpenseCategory.GROCERIES.name, 400.0, currentMonth, now),
                BudgetEntity("SHOPPING_$currentMonth", ExpenseCategory.SHOPPING.name, 300.0, currentMonth, now),
                BudgetEntity("TRANSPORT_$currentMonth", ExpenseCategory.TRANSPORT.name, 200.0, currentMonth, now),
                BudgetEntity("ENTERTAINMENT_$currentMonth", ExpenseCategory.ENTERTAINMENT.name, 150.0, currentMonth, now),
                BudgetEntity("BILLS_$currentMonth", ExpenseCategory.BILLS.name, 600.0, currentMonth, now)
            )
            budgetDao.insertOrUpdateAll(initialBudgets)

            // Initial sample expenses
            val initialExpenses = listOf(
                Expense(
                    id = "seed-1",
                    title = "Monthly Salary Deposit",
                    amount = 4200.00,
                    category = ExpenseCategory.INCOME,
                    date = now - (dayMillis * 14),
                    type = TransactionType.INCOME,
                    note = "Bi-weekly paycheck direct deposit",
                    deviceId = syncManager.currentDeviceId
                ),
                Expense(
                    id = "seed-2",
                    title = "Whole Foods Market",
                    amount = 112.45,
                    category = ExpenseCategory.GROCERIES,
                    date = now - (dayMillis * 1),
                    type = TransactionType.EXPENSE,
                    note = "Weekly organic groceries & produce",
                    merchantName = "Whole Foods Market",
                    isReceiptScanned = true,
                    items = listOf(
                        ScannedItem("Avocados Bag", 4.99),
                        ScannedItem("Organic Almond Milk", 3.49),
                        ScannedItem("Atlantic Salmon Fillet", 22.50),
                        ScannedItem("Greek Yogurt", 5.99),
                        ScannedItem("Fresh Berries", 8.49)
                    ),
                    taxAmount = 7.85,
                    deviceId = syncManager.currentDeviceId
                ),
                Expense(
                    id = "seed-3",
                    title = "Artisan Italian Trattoria",
                    amount = 78.50,
                    category = ExpenseCategory.FOOD,
                    date = now - (dayMillis * 3),
                    type = TransactionType.EXPENSE,
                    note = "Dinner with friends",
                    merchantName = "Artisan Trattoria",
                    isReceiptScanned = true,
                    items = listOf(
                        ScannedItem("Truffle Tagliatelle", 28.00),
                        ScannedItem("Margherita Pizza", 18.00),
                        ScannedItem("Tiramisu", 12.00)
                    ),
                    taxAmount = 6.20,
                    deviceId = syncManager.currentDeviceId
                ),
                Expense(
                    id = "seed-4",
                    title = "Metro Transit Pass",
                    amount = 45.00,
                    category = ExpenseCategory.TRANSPORT,
                    date = now - (dayMillis * 5),
                    type = TransactionType.EXPENSE,
                    note = "Monthly subway & bus card recharge",
                    deviceId = syncManager.currentDeviceId
                ),
                Expense(
                    id = "seed-5",
                    title = "High-Speed Fiber Internet",
                    amount = 70.00,
                    category = ExpenseCategory.BILLS,
                    date = now - (dayMillis * 7),
                    type = TransactionType.EXPENSE,
                    note = "Home fiber internet bill",
                    deviceId = syncManager.currentDeviceId
                ),
                Expense(
                    id = "seed-6",
                    title = "Cinema & Popcorn",
                    amount = 32.00,
                    category = ExpenseCategory.ENTERTAINMENT,
                    date = now - (dayMillis * 9),
                    type = TransactionType.EXPENSE,
                    note = "Weekend movie premiere tickets",
                    deviceId = syncManager.currentDeviceId
                )
            )

            val entities = initialExpenses.map { ExpenseEntity.fromExpense(it, moshi) }
            expenseDao.insertOrUpdateAll(entities)

            // Seed sample recurring expenses
            val initialRecurring = listOf(
                RecurringExpenseEntity(
                    id = "rec-seed-1",
                    title = "Apartment Rent",
                    amount = 1200.00,
                    category = ExpenseCategory.BILLS.name,
                    type = TransactionType.EXPENSE.name,
                    frequency = RecurrenceFrequency.MONTHLY.name,
                    startDate = now - (dayMillis * 30),
                    nextDueDate = now + (dayMillis * 1),
                    lastGeneratedDate = now - (dayMillis * 30),
                    isActive = true,
                    note = "Monthly apartment rent payment",
                    updatedAt = now
                ),
                RecurringExpenseEntity(
                    id = "rec-seed-2",
                    title = "Netflix & Streaming Subscription",
                    amount = 19.99,
                    category = ExpenseCategory.ENTERTAINMENT.name,
                    type = TransactionType.EXPENSE.name,
                    frequency = RecurrenceFrequency.MONTHLY.name,
                    startDate = now - (dayMillis * 45),
                    nextDueDate = now + (dayMillis * 5),
                    lastGeneratedDate = now - (dayMillis * 15),
                    isActive = true,
                    note = "Monthly 4K Ultra HD plan",
                    updatedAt = now
                ),
                RecurringExpenseEntity(
                    id = "rec-seed-3",
                    title = "Gym & Fitness Club",
                    amount = 45.00,
                    category = ExpenseCategory.HEALTH.name,
                    type = TransactionType.EXPENSE.name,
                    frequency = RecurrenceFrequency.MONTHLY.name,
                    startDate = now - (dayMillis * 20),
                    nextDueDate = now + (dayMillis * 10),
                    lastGeneratedDate = now - (dayMillis * 20),
                    isActive = true,
                    note = "All-access monthly gym membership",
                    updatedAt = now
                )
            )
            recurringExpenseDao.insertOrUpdateAll(initialRecurring)

            prefs.edit().putBoolean("is_seeded_v1", true).apply()
        }
    }

    /**
     * Checks for any recurring expenses that have reached or passed their nextDueDate.
     * For each due rule, creates an Expense transaction and advances nextDueDate.
     */
    suspend fun processDueRecurringExpenses() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val dueList = recurringExpenseDao.getDueRecurringExpenses(now)
        for (recEntity in dueList) {
            val rec = recEntity.toRecurringExpense()
            val generatedExpense = Expense(
                id = UUID.randomUUID().toString(),
                title = rec.title,
                amount = rec.amount,
                category = rec.category,
                date = rec.nextDueDate,
                type = rec.type,
                note = if (rec.note.isNotBlank()) "${rec.note} • [${rec.frequency.displayName} recurring]" else "[${rec.frequency.displayName} recurring]",
                isRecurring = true,
                recurringFrequency = rec.frequency.displayName,
                deviceId = syncManager.currentDeviceId
            )
            val expenseEntity = ExpenseEntity.fromExpense(generatedExpense, moshi)
            expenseDao.insertOrUpdate(expenseEntity)

            val updatedNextDue = com.example.util.RecurrenceCalculator.computeNextDueDate(rec.nextDueDate, rec.frequency)
            val updatedRecEntity = recEntity.copy(
                nextDueDate = updatedNextDue,
                lastGeneratedDate = rec.nextDueDate,
                updatedAt = now
            )
            recurringExpenseDao.insertOrUpdate(updatedRecEntity)
        }
    }

    suspend fun addOrUpdateRecurringExpense(recurring: RecurringExpense) = withContext(Dispatchers.IO) {
        val entity = RecurringExpenseEntity.fromRecurringExpense(recurring)
        recurringExpenseDao.insertOrUpdate(entity)
        // Also check if immediately due
        if (recurring.isActive && recurring.nextDueDate <= System.currentTimeMillis()) {
            processDueRecurringExpenses()
        }
    }

    suspend fun setRecurringExpenseActive(id: String, isActive: Boolean) = withContext(Dispatchers.IO) {
        recurringExpenseDao.setRecurringExpenseActive(id, isActive)
        if (isActive) {
            processDueRecurringExpenses()
        }
    }

    suspend fun deleteRecurringExpense(id: String) = withContext(Dispatchers.IO) {
        recurringExpenseDao.deleteRecurringExpenseById(id)
    }

    suspend fun addOrUpdateExpense(expense: Expense) = withContext(Dispatchers.IO) {
        val entity = ExpenseEntity.fromExpense(expense, moshi)
        expenseDao.insertOrUpdate(entity)
        syncManager.syncExpenseToCloud(expense)
    }

    suspend fun deleteExpense(id: String) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpenseById(id)
        syncManager.deleteExpenseFromCloud(id)
    }

    suspend fun setBudget(budget: Budget) = withContext(Dispatchers.IO) {
        val entity = BudgetEntity.fromBudget(budget)
        budgetDao.insertOrUpdate(entity)
        syncManager.syncBudgetToCloud(budget)
    }

    suspend fun deleteBudget(id: String) = withContext(Dispatchers.IO) {
        budgetDao.deleteBudgetById(id)
    }

    suspend fun addOrUpdateCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.insertOrUpdate(category)
    }

    suspend fun deleteCategory(id: String) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategoryById(id)
    }

    fun getExpensesByCategory(category: String): Flow<List<Expense>> {
        return expenseDao.getExpensesByCategory(category).map { list ->
            list.map { it.toExpense(moshi) }
        }
    }

    fun getCategorySpending(category: String, startTime: Long, endTime: Long): Flow<Double?> {
        return expenseDao.getCategorySpendingBetween(category, startTime, endTime)
    }
}
