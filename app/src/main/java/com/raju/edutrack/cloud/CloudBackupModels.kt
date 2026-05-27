package com.raju.edutrack.cloud

import com.raju.edutrack.Batch
import com.raju.edutrack.Contact
import com.raju.edutrack.Student

data class CloudContact(
    val label: String = "",
    val number: String = ""
)

data class CloudStudent(
    val studentName: String = "",
    val className: String = "",
    val schoolName: String = "",
    val contacts: List<CloudContact> = emptyList(),
    val joinDateMillis: Long = 0L,
    val lastFeePaidMillis: Long? = null,
    val batchName: String? = null,
    val feeDueAmount: Double? = null,
    val feeDueDateMillis: Long? = null,
    val advanceBalance: Double? = null
)

data class CloudBatch(
    val name: String = "",
    val timeText: String = "",
    val messageTemplate: String = ""
)

data class CloudBackupPayload(
    val schemaVersion: Int = 1,
    val updatedAtMillis: Long = 0L,
    val students: List<CloudStudent> = emptyList(),
    val batches: List<CloudBatch> = emptyList()
)

fun Student.toCloudStudent(): CloudStudent {
    return CloudStudent(
        studentName = studentName,
        className = className,
        schoolName = schoolName,
        contacts = contacts.map { contact ->
            CloudContact(
                label = contact.label,
                number = contact.number
            )
        },
        joinDateMillis = joinDateMillis,
        lastFeePaidMillis = lastFeePaidMillis,
        batchName = batchName,
        feeDueAmount = feeDueAmount,
        feeDueDateMillis = feeDueDateMillis,
        advanceBalance = advanceBalance
    )
}

fun CloudStudent.toStudent(): Student {
    return Student(
        studentName = studentName,
        className = className,
        schoolName = schoolName,
        contacts = contacts.map { contact ->
            Contact(
                label = contact.label,
                number = contact.number
            )
        },
        joinDateMillis = joinDateMillis,
        lastFeePaidMillis = lastFeePaidMillis,
        batchName = batchName,
        feeDueAmount = feeDueAmount,
        feeDueDateMillis = feeDueDateMillis,
        advanceBalance = advanceBalance
    )
}

fun Batch.toCloudBatch(): CloudBatch {
    return CloudBatch(
        name = name,
        timeText = timeText,
        messageTemplate = messageTemplate
    )
}

fun CloudBatch.toBatch(): Batch {
    return Batch(
        name = name,
        timeText = timeText,
        messageTemplate = messageTemplate
    )
}
