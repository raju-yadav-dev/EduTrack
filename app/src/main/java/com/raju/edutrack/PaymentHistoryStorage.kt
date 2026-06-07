package com.raju.edutrack

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private const val PAYMENT_HISTORY_FILE_NAME = "payment_history.json"

object PaymentHistoryStorage {

    fun loadHistory(context: Context): List<PaymentHistoryEntry> {
        val file = File(context.filesDir, PAYMENT_HISTORY_FILE_NAME)
        if (!file.exists()) {
            return emptyList()
        }

        val raw = file.readText()
        if (raw.isBlank()) {
            return emptyList()
        }

        val array = JSONArray(raw)
        val history = mutableListOf<PaymentHistoryEntry>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            history.add(
                PaymentHistoryEntry(
                    id = item.optString("id"),
                    studentId = item.optString("studentId"),
                    studentName = item.optString("studentName"),
                    className = item.optString("className"),
                    schoolName = item.optString("schoolName"),
                    batchName = item.optionalString("batchName"),
                    mainNumber = item.optionalString("mainNumber"),
                    amountPaid = item.optDouble("amountPaid", 0.0),
                    remainingDue = item.optionalDouble("remainingDue"),
                    paidAtMillis = item.optLong("paidAtMillis"),
                    beforeLastFeePaidMillis = item.optionalLong("beforeLastFeePaidMillis"),
                    beforeFeeDueDateMillis = item.optionalLong("beforeFeeDueDateMillis"),
                    beforeAdvanceBalance = item.optionalDouble("beforeAdvanceBalance"),
                    afterLastFeePaidMillis = item.optionalLong("afterLastFeePaidMillis"),
                    afterFeeDueDateMillis = item.optionalLong("afterFeeDueDateMillis"),
                    afterAdvanceBalance = item.optionalDouble("afterAdvanceBalance")
                )
            )
        }
        return history
    }

    fun saveHistory(
        context: Context,
        history: List<PaymentHistoryEntry>
    ) {
        val array = JSONArray()
        history.forEach { entry ->
            val item = JSONObject()
            item.put("id", entry.id)
            item.put("studentId", entry.studentId)
            item.put("studentName", entry.studentName)
            item.put("className", entry.className)
            item.put("schoolName", entry.schoolName)
            entry.batchName?.let { value -> item.put("batchName", value) }
            entry.mainNumber?.let { value -> item.put("mainNumber", value) }
            item.put("amountPaid", entry.amountPaid)
            entry.remainingDue?.let { value -> item.put("remainingDue", value) }
            item.put("paidAtMillis", entry.paidAtMillis)
            entry.beforeLastFeePaidMillis?.let { value -> item.put("beforeLastFeePaidMillis", value) }
            entry.beforeFeeDueDateMillis?.let { value -> item.put("beforeFeeDueDateMillis", value) }
            entry.beforeAdvanceBalance?.let { value -> item.put("beforeAdvanceBalance", value) }
            entry.afterLastFeePaidMillis?.let { value -> item.put("afterLastFeePaidMillis", value) }
            entry.afterFeeDueDateMillis?.let { value -> item.put("afterFeeDueDateMillis", value) }
            entry.afterAdvanceBalance?.let { value -> item.put("afterAdvanceBalance", value) }
            array.put(item)
        }

        val file = File(context.filesDir, PAYMENT_HISTORY_FILE_NAME)
        file.writeAtomically(array.toString())
    }
}

private fun JSONObject.optionalString(name: String): String? {
    if (!has(name) || isNull(name)) {
        return null
    }
    return optString(name).takeIf { value -> value.isNotBlank() }
}

private fun JSONObject.optionalLong(name: String): Long? {
    if (!has(name) || isNull(name)) {
        return null
    }
    return optLong(name, -1L).takeIf { value -> value >= 0L }
}

private fun JSONObject.optionalDouble(name: String): Double? {
    if (!has(name) || isNull(name)) {
        return null
    }
    return optDouble(name, Double.NaN).takeIf { value -> !value.isNaN() }
}

private fun File.writeAtomically(text: String) {
    val tempFile = File(parentFile, "$name.tmp")
    tempFile.writeText(text)
    if (!tempFile.renameTo(this)) {
        tempFile.copyTo(this, overwrite = true)
        tempFile.delete()
    }
}
