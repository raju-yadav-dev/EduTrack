package com.raju.edutrack

import android.content.Context
import androidx.compose.runtime.mutableStateListOf

object PaymentHistoryManager {

    val history = mutableStateListOf<PaymentHistoryEntry>()
    private var isLoaded = false

    fun load(context: Context) {
        if (isLoaded) {
            return
        }
        reload(context)
        isLoaded = true
    }

    fun reload(context: Context) {
        history.clear()
        history.addAll(PaymentHistoryStorage.loadHistory(context))
    }

    fun addEntry(
        context: Context,
        entry: PaymentHistoryEntry
    ) {
        history.add(0, entry)
        persist(context)
    }

    fun deleteAndReverse(
        context: Context,
        entry: PaymentHistoryEntry
    ) {
        val studentIndex = StudentManager.students.indexOfFirst { student ->
            student.id == entry.studentId
        }
        if (studentIndex >= 0) {
            val student = StudentManager.students[studentIndex]
            StudentManager.updateStudent(
                context,
                studentIndex,
                student.copy(
                    lastFeePaidMillis = entry.beforeLastFeePaidMillis,
                    feeDueDateMillis = entry.beforeFeeDueDateMillis,
                    advanceBalance = entry.beforeAdvanceBalance
                ),
                recordPaymentHistory = false
            )
        }
        history.removeAll { item -> item.id == entry.id }
        persist(context)
    }

    fun replaceAll(
        context: Context,
        replacement: List<PaymentHistoryEntry>
    ) {
        history.clear()
        history.addAll(replacement)
        persist(context)
    }

    private fun persist(context: Context) {
        PaymentHistoryStorage.saveHistory(context, history)
    }
}
