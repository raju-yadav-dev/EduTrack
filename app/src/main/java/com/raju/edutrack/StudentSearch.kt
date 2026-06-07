package com.raju.edutrack

fun Student.matchesStudentQuery(query: String): Boolean {
    val normalized = query.trim()
    if (normalized.isBlank()) {
        return true
    }
    return searchableStudentDetails().contains(normalized, ignoreCase = true)
}

fun Student.searchableStudentDetails(): String {
    return buildString {
        append(studentName)
        append(' ')
        append(className)
        append(' ')
        append(schoolName)
        append(' ')
        append(batchName.orEmpty())
        append(' ')
        append(formatDate(joinDateMillis))
        append(' ')
        lastFeePaidMillis?.let { millis ->
            append(formatDate(millis))
            append(' ')
        }
        feeDueAmount?.let { amount ->
            append(amount)
            append(' ')
        }
        feeDueDateMillis?.let { millis ->
            append(formatDate(millis))
            append(' ')
        }
        contacts.forEach { contact ->
            append(contact.label)
            append(' ')
            append(contact.number)
            append(' ')
        }
    }
}
