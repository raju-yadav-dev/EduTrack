package com.raju.edutrack

import java.util.UUID

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
    val id: String = UUID.randomUUID().toString()
)
