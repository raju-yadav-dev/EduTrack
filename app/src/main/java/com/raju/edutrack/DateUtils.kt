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
