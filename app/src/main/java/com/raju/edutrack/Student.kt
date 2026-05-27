package com.raju.edutrack

data class Student(
    val studentName: String,
    val className: String,
    val schoolName: String,
    val contacts: List<Contact>,
    val joinDateMillis: Long,
    val lastFeePaidMillis: Long?,
    val batchName: String? = null,
    val feeDueAmount: Double? = null,
    val feeDueDateMillis: Long? = null,
    val advanceBalance: Double? = null,
)