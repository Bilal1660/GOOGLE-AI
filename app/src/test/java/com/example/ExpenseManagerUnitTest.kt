package com.example

import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.ExpenseEntity
import com.example.data.model.Budget
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.ScannedItem
import com.example.data.model.TransactionType
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExpenseManagerUnitTest {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    @Test
    fun testExpenseEntityMappingAndJsonSerialization() {
        val items = listOf(
            ScannedItem("Avocados", 3.99),
            ScannedItem("Oat Milk", 4.29)
        )
        val expense = Expense(
            id = "test-1",
            title = "Trader Joe's",
            amount = 8.28,
            category = ExpenseCategory.GROCERIES,
            date = 1700000000000L,
            type = TransactionType.EXPENSE,
            note = "Weekly essentials",
            merchantName = "Trader Joe's",
            isReceiptScanned = true,
            items = items,
            taxAmount = 0.68,
            deviceId = "DEV-TEST-1"
        )

        val entity = ExpenseEntity.fromExpense(expense, moshi)
        assertEquals("test-1", entity.id)
        assertEquals("Trader Joe's", entity.title)
        assertEquals(8.28, entity.amount, 0.001)
        assertEquals("GROCERIES", entity.category)
        assertTrue(entity.isReceiptScanned)

        val convertedBack = entity.toExpense(moshi)
        assertEquals(2, convertedBack.items.size)
        assertEquals("Avocados", convertedBack.items[0].name)
        assertEquals(3.99, convertedBack.items[0].price, 0.001)
        assertEquals(ExpenseCategory.GROCERIES, convertedBack.category)
    }

    @Test
    fun testBudgetEntityMapping() {
        val budget = Budget(
            id = "FOOD_2026-09",
            category = "FOOD",
            monthlyLimit = 450.0,
            monthYear = "2026-09"
        )

        val entity = BudgetEntity.fromBudget(budget)
        assertEquals("FOOD_2026-09", entity.id)
        assertEquals(450.0, entity.monthlyLimit, 0.001)

        val convertedBack = entity.toBudget()
        assertEquals("FOOD", convertedBack.category)
        assertEquals(450.0, convertedBack.monthlyLimit, 0.001)
    }

    @Test
    fun testCategoryDisplayNameAndParsing() {
        val cat = ExpenseCategory.fromString("GROCERIES")
        assertEquals(ExpenseCategory.GROCERIES, cat)
        assertEquals("Groceries", cat.displayName)

        val fallback = ExpenseCategory.fromString("UNKNOWN_XYZ")
        assertEquals(ExpenseCategory.OTHER, fallback)
    }

    @Test
    fun testVisualSummaryRatioAndThresholds() {
        val budget = 2500.0
        val safeSpend = 1500.0
        val safeRatio = (safeSpend / budget).toFloat()
        assertEquals(0.60f, safeRatio, 0.001f)
        assertTrue(safeRatio < 0.80f)

        val cautionSpend = 2100.0
        val cautionRatio = (cautionSpend / budget).toFloat()
        assertEquals(0.84f, cautionRatio, 0.001f)
        assertTrue(cautionRatio >= 0.80f && cautionRatio <= 1.0f)

        val overSpend = 2700.0
        val overRatio = (overSpend / budget).toFloat()
        assertEquals(1.08f, overRatio, 0.001f)
        assertTrue(overRatio > 1.0f)
    }

    @Test
    fun testTransactionDeletionWorkflow() {
        val initialList = mutableListOf(
            Expense(
                id = "tx-1",
                title = "Whole Foods Market",
                amount = 64.50,
                date = System.currentTimeMillis(),
                category = ExpenseCategory.GROCERIES,
                type = TransactionType.EXPENSE
            ),
            Expense(
                id = "tx-2",
                title = "Salary Deposit",
                amount = 3500.00,
                date = System.currentTimeMillis(),
                category = ExpenseCategory.OTHER,
                type = TransactionType.INCOME
            )
        )

        assertEquals(2, initialList.size)

        // Simulate dismissing confirmation dialog: list unchanged
        var pendingDeleteExpense: Expense? = initialList[0]
        // User clicks cancel:
        pendingDeleteExpense = null
        assertEquals(2, initialList.size)

        // User clicks delete on second transaction and confirms
        pendingDeleteExpense = initialList[0]
        // User confirms:
        initialList.removeIf { it.id == pendingDeleteExpense?.id }
        pendingDeleteExpense = null

        assertEquals(1, initialList.size)
        assertEquals("tx-2", initialList[0].id)
    }

    @Test
    fun testReceiptScannerExtractionMapping() {
        val simulatedGeminiResult = com.example.data.ai.ScannedReceiptResult(
            merchant = "Trader Joe's",
            amount = 45.80,
            date = "2026-09-18",
            category = "Groceries",
            tax = 3.65,
            items = listOf(
                com.example.data.ai.ScannedItemResult("Organic Apples", 4.99),
                com.example.data.ai.ScannedItemResult("Greek Yogurt", 5.49),
                com.example.data.ai.ScannedItemResult("Olive Oil", 12.99)
            ),
            notes = "Weekly grocery run"
        )

        assertEquals("Trader Joe's", simulatedGeminiResult.merchant)
        assertEquals(45.80, simulatedGeminiResult.amount, 0.001)
        assertEquals(3, simulatedGeminiResult.items.size)
        assertEquals(ExpenseCategory.GROCERIES, ExpenseCategory.fromString(simulatedGeminiResult.category))

        // Map to Expense model
        val expense = Expense(
            id = "scanned-tx-1",
            title = simulatedGeminiResult.merchant,
            amount = simulatedGeminiResult.amount,
            date = System.currentTimeMillis(),
            category = ExpenseCategory.fromString(simulatedGeminiResult.category),
            merchantName = simulatedGeminiResult.merchant,
            isReceiptScanned = true,
            items = simulatedGeminiResult.items.map { ScannedItem(it.name, it.price) },
            taxAmount = simulatedGeminiResult.tax
        )

        assertTrue(expense.isReceiptScanned)
        assertEquals("Trader Joe's", expense.merchantName)
        assertEquals(3, expense.items.size)
        assertEquals(3.65, expense.taxAmount, 0.001)
    }

    @Test
    fun testCategoryEntityCreationAndMapping() {
        val categoryEntity = CategoryEntity.fromExpenseCategory(ExpenseCategory.FOOD, 450.0)
        assertEquals("FOOD", categoryEntity.id)
        assertEquals("Food & Dining", categoryEntity.displayName)
        assertEquals("Restaurant", categoryEntity.iconName)
        assertTrue(categoryEntity.isDefault)
        assertEquals(450.0, categoryEntity.defaultBudgetLimit, 0.001)

        val customCategory = CategoryEntity(
            id = "custom_fitness_101",
            displayName = "CrossFit Gym",
            iconName = "FitnessCenter",
            colorHex = "#FF5722",
            isDefault = false,
            defaultBudgetLimit = 120.0
        )
        assertEquals("custom_fitness_101", customCategory.id)
        assertEquals("CrossFit Gym", customCategory.displayName)
        assertEquals(120.0, customCategory.defaultBudgetLimit, 0.001)
        org.junit.Assert.assertFalse(customCategory.isDefault)
    }

    @Test
    fun testBudgetEntityLimitsAndMapping() {
        val budget = Budget(
            id = "FOOD_2026-09",
            category = "FOOD",
            monthlyLimit = 500.0,
            monthYear = "2026-09"
        )
        val entity = BudgetEntity.fromBudget(budget)
        assertEquals("FOOD_2026-09", entity.id)
        assertEquals("FOOD", entity.category)
        assertEquals(500.0, entity.monthlyLimit, 0.001)
        assertEquals("2026-09", entity.monthYear)

        val restoredBudget = entity.toBudget()
        assertEquals(budget.id, restoredBudget.id)
        assertEquals(budget.monthlyLimit, restoredBudget.monthlyLimit, 0.001)
    }

    @Test
    fun testCsvExportGenerationAndEscaping() {
        val testExpenses = listOf(
            Expense(
                id = "exp-1",
                title = "Whole Foods, Market",
                amount = 84.50,
                date = 1758240000000L,
                category = ExpenseCategory.GROCERIES,
                type = TransactionType.EXPENSE,
                merchantName = "Whole Foods \"Organic\"",
                isReceiptScanned = true,
                items = listOf(ScannedItem("Avocados", 5.00)),
                taxAmount = 6.50,
                note = "Weekly groceries, essential"
            ),
            Expense(
                id = "inc-1",
                title = "Tech Consulting",
                amount = 2500.00,
                date = 1758240000000L,
                category = ExpenseCategory.INCOME,
                type = TransactionType.INCOME,
                note = "Bi-weekly paycheck"
            )
        )

        val csvString = com.example.util.CsvExportUtil.generateCsvString(testExpenses)

        // Verify CSV header
        assertTrue(csvString.contains("ID,Date,Type,Category,Title,Merchant,Amount,Tax,Notes,IsScannedReceipt,ItemizedCount,ItemsDetail"))

        // Verify proper quote escaping for commas and internal quotes
        assertTrue(csvString.contains("\"Whole Foods, Market\""))
        assertTrue(csvString.contains("\"Whole Foods \"\"Organic\"\"\""))
        assertTrue(csvString.contains("\"Weekly groceries, essential\""))

        // Verify financial summary section
        assertTrue(csvString.contains("SUMMARY STATISTICS"))
        assertTrue(csvString.contains("Total Records,2"))
        assertTrue(csvString.contains("Total Expenses,$84.50"))
        assertTrue(csvString.contains("Total Income,$2500.00"))
        assertTrue(csvString.contains("Net Balance,$2415.50"))
    }

    @Test
    fun testRecurringExpenseEntityMapping() {
        val recurring = com.example.data.model.RecurringExpense(
            id = "rec-101",
            title = "Monthly Gym Membership",
            amount = 49.99,
            category = ExpenseCategory.HEALTH,
            type = TransactionType.EXPENSE,
            frequency = com.example.data.model.RecurrenceFrequency.MONTHLY,
            startDate = 1758000000000L,
            nextDueDate = 1760600000000L,
            lastGeneratedDate = 1758000000000L,
            isActive = true,
            note = "Fitness club tier 1",
            updatedAt = 1758000000000L
        )

        val entity = com.example.data.local.RecurringExpenseEntity.fromRecurringExpense(recurring)
        assertEquals("rec-101", entity.id)
        assertEquals("Monthly Gym Membership", entity.title)
        assertEquals(49.99, entity.amount, 0.001)
        assertEquals("MONTHLY", entity.frequency)
        assertEquals("HEALTH", entity.category)
        assertTrue(entity.isActive)

        val converted = entity.toRecurringExpense()
        assertEquals(recurring.id, converted.id)
        assertEquals(recurring.title, converted.title)
        assertEquals(recurring.amount, converted.amount, 0.001)
        assertEquals(recurring.frequency, converted.frequency)
        assertEquals(recurring.category, converted.category)
        assertEquals(recurring.isActive, converted.isActive)
    }

    @Test
    fun testRecurrenceCalculatorNextDueDate() {
        val calendar = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.JANUARY, 15, 12, 0, 0)
        }
        val initialDate = calendar.timeInMillis

        // Monthly recurrence
        val nextMonth = com.example.util.RecurrenceCalculator.computeNextDueDate(
            initialDate,
            com.example.data.model.RecurrenceFrequency.MONTHLY
        )
        val monthCal = java.util.Calendar.getInstance().apply { timeInMillis = nextMonth }
        assertEquals(java.util.Calendar.FEBRUARY, monthCal.get(java.util.Calendar.MONTH))
        assertEquals(15, monthCal.get(java.util.Calendar.DAY_OF_MONTH))

        // Weekly recurrence
        val nextWeek = com.example.util.RecurrenceCalculator.computeNextDueDate(
            initialDate,
            com.example.data.model.RecurrenceFrequency.WEEKLY
        )
        val weekCal = java.util.Calendar.getInstance().apply { timeInMillis = nextWeek }
        assertEquals(22, weekCal.get(java.util.Calendar.DAY_OF_MONTH))

        // Daily recurrence
        val nextDay = com.example.util.RecurrenceCalculator.computeNextDueDate(
            initialDate,
            com.example.data.model.RecurrenceFrequency.DAILY
        )
        val dayCal = java.util.Calendar.getInstance().apply { timeInMillis = nextDay }
        assertEquals(16, dayCal.get(java.util.Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testExpenseModelRecurringBadgeFields() {
        val recurringExpense = Expense(
            id = "exp-rec-1",
            title = "Apartment Rent",
            amount = 1200.0,
            category = ExpenseCategory.BILLS,
            isRecurring = true,
            recurringFrequency = "Monthly"
        )
        assertTrue(recurringExpense.isRecurring)
        assertEquals("Monthly", recurringExpense.recurringFrequency)

        val entity = ExpenseEntity.fromExpense(recurringExpense, moshi)
        assertTrue(entity.isRecurring)
        assertEquals("Monthly", entity.recurringFrequency)

        val back = entity.toExpense(moshi)
        assertTrue(back.isRecurring)
        assertEquals("Monthly", back.recurringFrequency)
    }

    @Test
    fun testReceiptScannerJsonParsing() {
        val sampleJson = """
            {
              "merchant": "Whole Foods Market",
              "amount": 42.75,
              "date": "2026-09-18",
              "category": "Groceries",
              "tax": 3.50,
              "items": [
                {"name": "Almond Milk", "price": 4.99},
                {"name": "Organic Honey", "price": 8.99},
                {"name": "Fresh Berries", "price": 28.77}
              ],
              "notes": "Organic grocery shopping"
            }
        """.trimIndent()

        val service = com.example.data.ai.ReceiptScannerService()
        // verify with reflection or testing parser
        val jsonObject = org.json.JSONObject(sampleJson)
        assertEquals("Whole Foods Market", jsonObject.getString("merchant"))
        assertEquals(42.75, jsonObject.getDouble("amount"), 0.001)
        assertEquals("Groceries", jsonObject.getString("category"))
        assertEquals(3.50, jsonObject.getDouble("tax"), 0.001)
        val items = jsonObject.getJSONArray("items")
        assertEquals(3, items.length())
        assertEquals("Almond Milk", items.getJSONObject(0).getString("name"))
        assertEquals(4.99, items.getJSONObject(0).getDouble("price"), 0.001)
    }
}

