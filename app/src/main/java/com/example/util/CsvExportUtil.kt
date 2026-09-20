package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Expense
import com.example.data.model.TransactionType
import java.io.File
import java.io.FileWriter
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for exporting expense and income records into a standard CSV financial report format.
 */
object CsvExportUtil {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val filenameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * Converts a list of expenses into a formatted CSV string.
     * Escapes commas, quotes, and newlines according to standard RFC 4180 rules.
     */
    fun generateCsvString(expenses: List<Expense>): String {
        val stringBuilder = StringBuilder()
        
        // CSV Header
        stringBuilder.append("ID,Date,Type,Category,Title,Merchant,Amount,Tax,Notes,IsScannedReceipt,ItemizedCount,ItemsDetail\n")

        val totalExpenses = expenses.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val totalIncome = expenses.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val netSavings = totalIncome - totalExpenses

        for (expense in expenses) {
            val formattedDate = dateFormat.format(Date(expense.date))
            val itemsDetail = expense.items.joinToString(" ; ") { "${it.name} ($${"%.2f".format(it.price)})" }

            val row = listOf(
                escapeCsv(expense.id),
                escapeCsv(formattedDate),
                escapeCsv(expense.type.name),
                escapeCsv(expense.category.displayName),
                escapeCsv(expense.title),
                escapeCsv(expense.merchantName ?: ""),
                "%.2f".format(Locale.US, expense.amount),
                "%.2f".format(Locale.US, expense.taxAmount ?: 0.0),
                escapeCsv(expense.note),
                if (expense.isReceiptScanned) "YES" else "NO",
                expense.items.size.toString(),
                escapeCsv(itemsDetail)
            )

            stringBuilder.append(row.joinToString(",")).append("\n")
        }

        // Summary Rows
        stringBuilder.append("\n")
        stringBuilder.append("SUMMARY STATISTICS\n")
        stringBuilder.append("Total Records,${expenses.size}\n")
        stringBuilder.append("Total Expenses,$${"%.2f".format(Locale.US, totalExpenses)}\n")
        stringBuilder.append("Total Income,$${"%.2f".format(Locale.US, totalIncome)}\n")
        stringBuilder.append("Net Balance,$${"%.2f".format(Locale.US, netSavings)}\n")

        return stringBuilder.toString()
    }

    /**
     * Escapes standard CSV special characters (commas, quotes, newlines).
     */
    fun escapeCsv(value: String): String {
        var result = value
        if (result.contains("\"")) {
            result = result.replace("\"", "\"\"")
        }
        if (result.contains(",") || result.contains("\n") || result.contains("\r") || result.contains("\"")) {
            result = "\"$result\""
        }
        return result
    }

    /**
     * Creates a temporary CSV file in the app cache directory for sharing.
     */
    fun createCsvFile(context: Context, expenses: List<Expense>): File {
        val timestamp = filenameDateFormat.format(Date())
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val file = File(reportsDir, "expenses_report_$timestamp.csv")
        FileWriter(file).use { writer ->
            writer.write(generateCsvString(expenses))
        }
        return file
    }

    /**
     * Writes CSV data directly into an OutputStream (e.g. from createDocument SAF picker).
     */
    fun writeCsvToStream(outputStream: OutputStream, expenses: List<Expense>) {
        outputStream.bufferedWriter().use { writer ->
            writer.write(generateCsvString(expenses))
        }
    }

    /**
     * Creates an Android sharing Intent using FileProvider to attach the generated CSV file.
     */
    fun createShareIntent(context: Context, file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Financial Expense Report (${file.name})")
            putExtra(
                Intent.EXTRA_TEXT,
                "Here is your exported financial expense report attached as a CSV file."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
