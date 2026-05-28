package com.raju.edutrack

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val dateFormat =
    SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

fun formatDate(millis: Long): String {
    return dateFormat.format(millis)
}

fun parseDateOrNull(text: String): Long? {
    return try {
        dateFormat.isLenient = false
        dateFormat.parse(text)?.time
    } catch (ex: ParseException) {
        null
    }
}

fun startOfCurrentMonthMillis(
    nowMillis: Long = System.currentTimeMillis()
): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = nowMillis
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

fun addMonths(
    baseMillis: Long,
    months: Int
): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = baseMillis
    calendar.add(Calendar.MONTH, months)
    return calendar.timeInMillis
}

fun monthsBetween(
    startMillis: Long,
    endMillis: Long
): Int {
    if (endMillis <= startMillis) {
        return 0
    }
    val start = Calendar.getInstance()
    start.timeInMillis = startMillis
    val end = Calendar.getInstance()
    end.timeInMillis = endMillis
    val startYear = start.get(Calendar.YEAR)
    val startMonth = start.get(Calendar.MONTH)
    val endYear = end.get(Calendar.YEAR)
    val endMonth = end.get(Calendar.MONTH)
    var diff = (endYear - startYear) * 12 + (endMonth - startMonth)
    if (end.get(Calendar.DAY_OF_MONTH) < start.get(Calendar.DAY_OF_MONTH)) {
        diff -= 1
    }
    return diff.coerceAtLeast(0)
}

fun effectiveMonthsUnpaid(
    student: Student,
    countFeeFromJoinDate: Boolean,
    monthlyFee: Double?,
    nowMillis: Long = System.currentTimeMillis()
): Int {
    val startOfMonth = startOfCurrentMonthMillis(nowMillis)
    val lastPaidMillis = student.lastFeePaidMillis
    val paidThisMonth = lastPaidMillis != null &&
        lastPaidMillis >= startOfMonth
    if (paidThisMonth) {
        return 0
    }
    if (countFeeFromJoinDate && student.joinDateMillis >= startOfMonth) {
        return 0
    }

    val baseMillis = lastPaidMillis ?: student.joinDateMillis
    var months = monthsBetween(baseMillis, nowMillis).coerceAtLeast(0)
    if (monthlyFee != null && monthlyFee > 0.0) {
        val advance = student.advanceBalance ?: 0.0
        val advanceMonths = (advance / monthlyFee).toInt()
        months = (months - advanceMonths).coerceAtLeast(0)
    }
    return months
}

fun isFeePending(
    student: Student,
    countFeeFromJoinDate: Boolean,
    nowMillis: Long = System.currentTimeMillis()
): Boolean {
    val startOfMonth = startOfCurrentMonthMillis(nowMillis)
    val lastPaidMillis = student.lastFeePaidMillis
    val paidThisMonth = lastPaidMillis != null &&
        lastPaidMillis >= startOfMonth
    if (paidThisMonth) {
        return false
    }
    if (!countFeeFromJoinDate) {
        return true
    }
    return student.joinDateMillis < startOfMonth
}
