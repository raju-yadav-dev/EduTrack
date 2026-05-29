package com.raju.edutrack.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raju.edutrack.AppSettings
import com.raju.edutrack.BatchManager
import com.raju.edutrack.MessageSender
import com.raju.edutrack.StudentManager
import com.raju.edutrack.addMonths
import com.raju.edutrack.effectiveMonthsUnpaid
import com.raju.edutrack.formatDate
import com.raju.edutrack.isFeePending
import com.raju.edutrack.monthsBetween

@Composable
fun OverviewScreen(
    title: String
) {
    val students = StudentManager.students
    var showPayDialog by remember { mutableStateOf(false) }
    var payAmountText by remember { mutableStateOf("") }
    var payStudentIndex by remember { mutableStateOf<Int?>(null) }
    var payStudent by remember { mutableStateOf<com.raju.edutrack.Student?>(null) }
    val nowMillis = System.currentTimeMillis()
    val pendingStudents = students
        .withIndex()
        .filter { entry ->
            val student = entry.value
            val monthlyFee =
                student.feeDueAmount ?: if (
                    AppSettings.autoClassFeesEnabled.value
                ) {
                    AppSettings.parseClassFeeAmount(student.className)
                } else {
                    null
                }
            val unpaid = effectiveMonthsUnpaid(
                student = student,
                countFeeFromJoinDate =
                    AppSettings.countFeeFromJoinDate.value,
                monthlyFee = monthlyFee,
                nowMillis = nowMillis
            )
            unpaid > 0 && isFeePending(
                student = student,
                countFeeFromJoinDate =
                    AppSettings.countFeeFromJoinDate.value
            )
        }

    val context = LocalContext.current
    val currency = AppSettings.currencySymbol.value
    val schoolGroups = students
        .filter { student -> student.schoolName.isNotBlank() }
        .groupBy { student -> student.schoolName }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)
    val data = when (title) {

        "Students" -> students.map { student ->

            OverviewItem(
                title = student.studentName,
                subtitle = student.className,
                meta = student.schoolName
            )

        }

        "Schools" -> emptyList()

        "Batches" -> BatchManager.batches
            .sortedBy { batch -> batch.name.lowercase() }
            .map { batch ->
                val count = students.count { student ->
                    student.batchName?.equals(
                        batch.name,
                        ignoreCase = true
                    ) == true
                }
                OverviewItem(
                    title = batch.name,
                    subtitle = "Students: $count",
                    meta = ""
                )
            }

        "Fee" -> emptyList()

        else -> emptyList()

    }
    if (showPayDialog && payStudent != null && payStudentIndex != null) {
        AlertDialog(
            onDismissRequest = {
                showPayDialog = false
                payAmountText = ""
                payStudentIndex = null
                payStudent = null
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountPaid = payAmountText.trim().toDoubleOrNull()
                        val student = payStudent
                        val index = payStudentIndex
                        if (amountPaid != null && amountPaid > 0 && student != null && index != null) {
                            val baseMillis =
                                student.lastFeePaidMillis
                                    ?: student.joinDateMillis
                            val monthlyFee =
                                student.feeDueAmount ?: if (
                                    AppSettings.autoClassFeesEnabled.value
                                ) {
                                    AppSettings.parseClassFeeAmount(
                                        student.className
                                    )
                                } else {
                                    null
                                }
                            if (monthlyFee != null && monthlyFee > 0.0) {
                                val monthsUnpaid = monthsBetween(
                                    baseMillis,
                                    nowMillis
                                ).coerceAtLeast(1)
                                val existingAdvance =
                                    student.advanceBalance ?: 0.0
                                val totalAvailable = amountPaid + existingAdvance
                                val monthsCovered = minOf(
                                    monthsUnpaid,
                                    (totalAvailable / monthlyFee).toInt()
                                )
                                val newPaidMillis = addMonths(
                                    baseMillis,
                                    monthsCovered
                                )
                                val remainderAdvance =
                                    totalAvailable - (monthsCovered * monthlyFee)
                                val newAdvance =
                                    remainderAdvance.takeIf { it > 0.0 }
                                val nextDueDate = if (
                                    AppSettings.autoAdvanceFeeDueDate.value
                                ) {
                                    addMonths(newPaidMillis, 1)
                                } else {
                                    student.feeDueDateMillis
                                }
                                val updatedStudent = student.copy(
                                    lastFeePaidMillis = newPaidMillis,
                                    feeDueDateMillis = nextDueDate,
                                    advanceBalance = newAdvance
                                )
                                StudentManager.updateStudent(
                                    context,
                                    index,
                                    updatedStudent
                                )
                                val remainingDue = (
                                    (monthsUnpaid - monthsCovered) * monthlyFee -
                                        (newAdvance ?: 0.0)
                                    ).coerceAtLeast(0.0)
                                MessageSender.sendFeePaidMessage(
                                    context = context,
                                    student = updatedStudent,
                                    amountPaid = amountPaid,
                                    dueAmount = remainingDue,
                                    batch = BatchManager.batches.firstOrNull { batch ->
                                        updatedStudent.batchName?.let { name ->
                                            batch.name.equals(name, ignoreCase = true)
                                        } == true
                                    }
                                )
                            }
                        }
                        showPayDialog = false
                        payAmountText = ""
                        payStudentIndex = null
                        payStudent = null
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPayDialog = false
                        payAmountText = ""
                        payStudentIndex = null
                        payStudent = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            title = { Text("Paid amount") },
            text = {
                Column {
                    Text(
                        text = "Enter the amount paid",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = payAmountText,
                        onValueChange = { payAmountText = it },
                        label = { Text("Amount") },
                        singleLine = true
                    )
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style =
                MaterialTheme.typography.headlineMedium
        )
        Spacer(
            modifier = Modifier.height(20.dp)
        )
        val showEmptyState = when (title) {
            "Fee" -> pendingStudents.isEmpty()
            "Schools" -> schoolGroups.isEmpty()
            else -> data.isEmpty()
        }

        if (showEmptyState) {

            Text(
                text = "No data available",
                style = MaterialTheme.typography.bodyLarge
            )

        } else {

            LazyColumn(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                if (title == "Fee") {

                    items(pendingStudents) { entry ->

                        val index = entry.index
                        val student = entry.value
                        val lastPaidBase =
                            student.lastFeePaidMillis
                                ?: student.joinDateMillis
                        val dueAmount =
                            student.feeDueAmount ?: if (
                                AppSettings.autoClassFeesEnabled.value
                            ) {
                                AppSettings.parseClassFeeAmount(
                                    student.className
                                )
                            } else {
                                null
                            }
                        val monthsUnpaid = effectiveMonthsUnpaid(
                            student = student,
                            countFeeFromJoinDate =
                                AppSettings.countFeeFromJoinDate.value,
                            monthlyFee = dueAmount,
                            nowMillis = nowMillis
                        )

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement =
                                    Arrangement.spacedBy(12.dp)
                            ) {
                                Column(
                                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                ) {
                                    TextButton(
                                        onClick = {
                                            val nextDueDate =
                                                if (AppSettings
                                                    .autoAdvanceFeeDueDate
                                                    .value
                                                ) {
                                                    addMonths(
                                                        System.currentTimeMillis(),
                                                        1
                                                    )
                                                } else {
                                                    student.feeDueDateMillis
                                                }
                                            val updatedStudent =
                                                student.copy(
                                                    lastFeePaidMillis =
                                                        System.currentTimeMillis(),
                                                    feeDueDateMillis =
                                                        nextDueDate,
                                                    advanceBalance = null
                                                )
                                            StudentManager.updateStudent(
                                                context,
                                                index,
                                                updatedStudent
                                            )
                                            val fullDueAmount =
                                                dueAmount?.let { amount ->
                                                    (
                                                        amount * monthsUnpaid -
                                                            (student.advanceBalance ?: 0.0)
                                                        ).coerceAtLeast(0.0)
                                                } ?: 0.0
                                            MessageSender.sendFeePaidMessage(
                                                context = context,
                                                student = updatedStudent,
                                                amountPaid = fullDueAmount,
                                                dueAmount = 0.0,
                                                batch = BatchManager.batches.firstOrNull { batch ->
                                                    updatedStudent.batchName?.let { name ->
                                                        batch.name.equals(
                                                            name,
                                                            ignoreCase = true
                                                        )
                                                    } == true
                                                }
                                            )
                                        }
                                    ) {
                                        Text("Paid fully")
                                    }

                                    TextButton(
                                        onClick = {
                                            payStudentIndex = index
                                            payStudent = student
                                            payAmountText = ""
                                            showPayDialog = true
                                        }
                                    ) {
                                        Text("Pay amount")
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        verticalAlignment =
                                            androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = student.studentName,
                                            style =
                                                MaterialTheme.typography.titleMedium
                                        )

                                        Spacer(
                                            modifier = Modifier.width(10.dp)
                                        )

                                        Text(
                                            text = student.className,
                                            style =
                                                MaterialTheme.typography.labelMedium
                                        )
                                    }

                                    Spacer(
                                        modifier = Modifier.height(4.dp)
                                    )
                                    Text(student.schoolName)

                                    Spacer(
                                        modifier = Modifier.height(4.dp)
                                    )

                                    val monthlyFeeText = dueAmount?.let { amount ->
                                        "$currency${"%.2f".format(amount)}"
                                    } ?: "-"
                                    val advance = student.advanceBalance ?: 0.0
                                    val remainingAdvance = if (
                                        dueAmount != null && dueAmount > 0.0
                                    ) {
                                        val advanceMonths = (advance / dueAmount).toInt()
                                        advance - (advanceMonths * dueAmount)
                                    } else {
                                        advance
                                    }
                                    val effectiveMonthsUnpaid = monthsUnpaid
                                    val totalDueText = dueAmount?.let { amount ->
                                        val total = (amount * effectiveMonthsUnpaid) -
                                            remainingAdvance
                                        val normalized = total.coerceAtLeast(0.0)
                                        "$currency${"%.2f".format(normalized)}"
                                    } ?: "-"
                                    Text(
                                        text = "Monthly: $monthlyFeeText",
                                        style =
                                            MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Total due: $totalDueText",
                                        style =
                                            MaterialTheme.typography.bodySmall
                                    )
                                    student.lastFeePaidMillis?.let { lastPaid ->
                                        Text(
                                            text = "Paid: ${formatDate(lastPaid)}",
                                            style =
                                                MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    if (advance > 0.0) {
                                        Text(
                                            text = "Advance: $currency${"%.2f".format(advance)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Text(
                                        text = "$effectiveMonthsUnpaid months unpaid",
                                        style =
                                            MaterialTheme.typography.bodySmall
                                    )

                                }
                            }
                        }
                    }

                } else {

                    if (title == "Schools") {
                        schoolGroups.forEach { (school, groupStudents) ->
                            item {
                                Text(
                                    text = school,
                                    style =
                                        MaterialTheme.typography.titleMedium
                                )
                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )
                            }

                            items(groupStudents) { student ->
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Row(
                                            verticalAlignment =
                                                androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = student.studentName,
                                                style =
                                                    MaterialTheme.typography.titleMedium
                                            )

                                            Spacer(
                                                modifier = Modifier.width(10.dp)
                                            )

                                            Text(
                                                text = student.className,
                                                style =
                                                    MaterialTheme.typography.labelMedium
                                            )
                                        }

                                        if (student.batchName?.isNotBlank() == true) {
                                            Spacer(
                                                modifier = Modifier.height(4.dp)
                                            )
                                            Text(
                                                text = "Batch: ${student.batchName}",
                                                style =
                                                    MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(
                                    modifier = Modifier.height(12.dp)
                                )
                            }
                        }
                    } else if (title == "Students") {
                        items(students.withIndex().toList()) { entry ->
                            val student = entry.value
                            val primaryNumber =
                                student.contacts
                                    .firstOrNull { contact ->
                                        contact.label.equals(
                                            "Main",
                                            ignoreCase = true
                                        ) && contact.number.isNotBlank()
                                    }
                                    ?.number
                                    ?: student.contacts
                                        .firstOrNull { contact ->
                                            contact.label.equals(
                                                "Primary",
                                                ignoreCase = true
                                            ) && contact.number.isNotBlank()
                                        }
                                        ?.number
                                    ?: student.contacts
                                        .firstOrNull { contact ->
                                            contact.number.isNotBlank()
                                        }
                                        ?.number
                                    ?: ""
                            StudentListCard(
                                studentName = student.studentName,
                                className = student.className,
                                joinDateMillis = student.joinDateMillis,
                                schoolName = student.schoolName,
                                primaryNumber = primaryNumber,
                                batchName = student.batchName,
                                lastFeePaidMillis = student.lastFeePaidMillis,
                                feeDueAmount = student.feeDueAmount,
                                feeDueDateMillis = student.feeDueDateMillis,
                                feePending = false,
                                isSelected = false
                            )
                        }
                    } else {
                        items(data) { item ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        style =
                                            MaterialTheme.typography.titleMedium
                                    )

                                    if (item.subtitle.isNotBlank()) {

                                        Spacer(
                                            modifier = Modifier.height(4.dp)
                                        )

                                        Text(item.subtitle)

                                    }

                                    if (item.meta.isNotBlank()) {

                                        Text(item.meta)

                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
    }
}

private data class OverviewItem(
    val title: String,
    val subtitle: String,
    val meta: String
)
