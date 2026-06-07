package com.raju.edutrack

import java.util.UUID

data class PaymentHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val studentId: String,
    val studentName: String,
    val className: String,
    val schoolName: String,
    val batchName: String?,
    val mainNumber: String?,
    val amountPaid: Double,
    val remainingDue: Double?,
    val paidAtMillis: Long,
    val beforeLastFeePaidMillis: Long?,
    val beforeFeeDueDateMillis: Long?,
    val beforeAdvanceBalance: Double?,
    val afterLastFeePaidMillis: Long?,
    val afterFeeDueDateMillis: Long?,
    val afterAdvanceBalance: Double?
)
