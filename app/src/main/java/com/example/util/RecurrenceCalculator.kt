package com.example.util

import com.example.data.model.RecurrenceFrequency
import java.util.Calendar

object RecurrenceCalculator {

    /**
     * Calculates the next due date based on the current due date and recurrence frequency.
     */
    fun computeNextDueDate(currentDueDate: Long, frequency: RecurrenceFrequency): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentDueDate
        }

        when (frequency) {
            RecurrenceFrequency.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RecurrenceFrequency.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrenceFrequency.BIWEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 2)
            RecurrenceFrequency.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RecurrenceFrequency.YEARLY -> calendar.add(Calendar.YEAR, 1)
        }

        return calendar.timeInMillis
    }
}
