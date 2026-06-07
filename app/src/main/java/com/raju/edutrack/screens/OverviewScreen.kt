package com.raju.edutrack.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.raju.edutrack.AppSettings
import com.raju.edutrack.BatchManager
import com.raju.edutrack.Contact
import com.raju.edutrack.MessageSender
import com.raju.edutrack.PaymentHistoryEntry
import com.raju.edutrack.PaymentHistoryManager
import com.raju.edutrack.StudentManager
import com.raju.edutrack.addMonths
import com.raju.edutrack.effectiveMonthsUnpaid
import com.raju.edutrack.formatDate
import com.raju.edutrack.isFeePending
import com.raju.edutrack.monthsBetween
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun OverviewScreen(
    title: String
) {
    val students = StudentManager.students
    var showPayDialog by remember { mutableStateOf(false) }
    var payAmountText by remember { mutableStateOf("") }
    var payStudentIndex by remember { mutableStateOf<Int?>(null) }
    var payStudent by remember { mutableStateOf<com.raju.edutrack.Student?>(null) }
    var confirmPaidStudent by remember { mutableStateOf<IndexedValue<com.raju.edutrack.Student>?>(null) }
    var dontAskFullPaidAgain by remember { mutableStateOf(false) }
    var sortOldestFirst by remember { mutableStateOf(true) }
    val nowMillis = System.currentTimeMillis()

    val context = LocalContext.current
    val currency = AppSettings.currencySymbol.value
    val overviewData by remember(title, nowMillis) {
        derivedStateOf {
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
            val sortedPendingStudents = pendingStudents.sortedBy { entry ->
                val student = entry.value
                student.lastFeePaidMillis ?: student.joinDateMillis
            }.let { sorted ->
                if (sortOldestFirst) sorted else sorted.asReversed()
            }
            val totalPendingAmount = pendingStudents.sumOf { entry ->
                totalDueForStudent(entry.value, nowMillis)
            }
            val schoolGroups = students
                .filter { student -> student.schoolName.isNotBlank() }
                .groupBy { student -> student.schoolName }
                .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            val items = when (title) {
                "Students" -> students.map { student ->
                    OverviewItem(
                        title = student.studentName,
                        subtitle = student.className,
                        meta = student.schoolName
                    )
                }
                "Schools" -> emptyList()
                "Batch", "Batches" -> BatchManager.batches
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
                "Due", "Dues" -> emptyList()
                else -> emptyList()
            }
            OverviewData(
                pendingStudents = sortedPendingStudents,
                totalPendingAmount = totalPendingAmount,
                schoolGroups = schoolGroups,
                items = items
            )
        }
    }

    fun clearPayDialog() {
        showPayDialog = false
        payAmountText = ""
        payStudentIndex = null
        payStudent = null
    }

    fun markFullyPaid(entry: IndexedValue<com.raju.edutrack.Student>) {
        val index = entry.index
        val student = entry.value
        val monthlyFee = monthlyFeeForStudent(student)
        val monthsUnpaid = effectiveMonthsUnpaid(
            student = student,
            countFeeFromJoinDate =
                AppSettings.countFeeFromJoinDate.value,
            monthlyFee = monthlyFee,
            nowMillis = nowMillis
        )
        val nextDueDate =
            if (AppSettings.autoAdvanceFeeDueDate.value) {
                addMonths(System.currentTimeMillis(), 1)
            } else {
                student.feeDueDateMillis
            }
        val updatedStudent =
            student.copy(
                lastFeePaidMillis = System.currentTimeMillis(),
                feeDueDateMillis = nextDueDate,
                advanceBalance = null
            )
        StudentManager.updateStudent(
            context,
            index,
            updatedStudent,
            recordPaymentHistory = false
        )
        val fullDueAmount =
            monthlyFee?.let { amount ->
                (
                    amount * monthsUnpaid -
                        (student.advanceBalance ?: 0.0)
                    ).coerceAtLeast(0.0)
            } ?: 0.0
        PaymentHistoryManager.addEntry(
            context,
            student.paymentHistoryEntry(
                amountPaid = fullDueAmount,
                remainingDue = 0.0,
                updatedStudent = updatedStudent
            )
        )
        MessageSender.sendFeePaidMessage(
            context = context,
            student = updatedStudent,
            amountPaid = fullDueAmount,
            dueAmount = 0.0,
            batch = BatchManager.batches.firstOrNull { batch ->
                updatedStudent.batchName?.let { name ->
                    batch.name.equals(name, ignoreCase = true)
                } == true
            }
        )
    }

    if (showPayDialog && payStudent != null && payStudentIndex != null) {
        AlertDialog(
            onDismissRequest = { clearPayDialog() },
            confirmButton = {
                Button(
                    onClick = {
                        val amountPaid = payAmountText.trim().toDoubleOrNull()
                        val student = payStudent
                        val index = payStudentIndex
                        if (amountPaid != null && amountPaid != 0.0 && student != null && index != null) {
                            val baseMillis =
                                student.lastFeePaidMillis
                                    ?: student.joinDateMillis
                            val monthlyFee = monthlyFeeForStudent(student)
                            if (monthlyFee != null && monthlyFee > 0.0) {
                                val monthsUnpaid = monthsBetween(
                                    baseMillis,
                                    nowMillis
                                ).coerceAtLeast(1)
                                val existingAdvance =
                                    student.advanceBalance ?: 0.0
                                val totalAvailable = amountPaid + existingAdvance
                                val monthsCovered = if (totalAvailable > 0.0) {
                                    minOf(
                                        monthsUnpaid,
                                        (totalAvailable / monthlyFee).toInt()
                                    )
                                } else {
                                    0
                                }
                                val newPaidMillis = if (monthsCovered > 0) {
                                    addMonths(baseMillis, monthsCovered)
                                } else {
                                    student.lastFeePaidMillis
                                }
                                val remainderAdvance = if (totalAvailable > 0.0) {
                                    totalAvailable - (monthsCovered * monthlyFee)
                                } else {
                                    totalAvailable
                                }
                                val newAdvance =
                                    remainderAdvance.takeIf { it != 0.0 }
                                val nextDueDate = if (
                                    monthsCovered > 0 &&
                                    AppSettings.autoAdvanceFeeDueDate.value
                                ) {
                                    addMonths(baseMillis, monthsCovered + 1)
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
                                    updatedStudent,
                                    recordPaymentHistory = false
                                )
                                val remainingDue = (
                                    (monthsUnpaid - monthsCovered) * monthlyFee -
                                        (newAdvance ?: 0.0)
                                    ).coerceAtLeast(0.0)
                                PaymentHistoryManager.addEntry(
                                    context,
                                    student.paymentHistoryEntry(
                                        amountPaid = amountPaid,
                                        remainingDue = remainingDue,
                                        updatedStudent = updatedStudent
                                    )
                                )
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
                        clearPayDialog()
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { clearPayDialog() }
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
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true
                    )
                }
            }
        )
    }

    confirmPaidStudent?.let { entry ->
        AlertDialog(
            onDismissRequest = {
                confirmPaidStudent = null
                dontAskFullPaidAgain = false
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dontAskFullPaidAgain) {
                            AppSettings.confirmFullFeePaid.value = false
                            AppSettings.save(context)
                        }
                        markFullyPaid(entry)
                        confirmPaidStudent = null
                        dontAskFullPaidAgain = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmPaidStudent = null
                        dontAskFullPaidAgain = false
                    }
                ) {
                    Text("Cancel")
                }
            },
            title = { Text("Mark fully paid?") },
            text = {
                Column {
                    Text("This will clear the full pending due for ${entry.value.studentName}.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = dontAskFullPaidAgain,
                            onCheckedChange = { dontAskFullPaidAgain = it }
                        )
                        Text("Don't ask next time")
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = if (title == "Due") "Dues" else title,
                style =
                    MaterialTheme.typography.headlineMedium
            )
            if (title == "Due" || title == "Dues") {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$currency${"%.2f".format(overviewData.totalPendingAmount)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${overviewData.pendingStudents.size} students",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        Spacer(
            modifier = Modifier.height(12.dp)
        )
        val showEmptyState = when (title) {
            "Due", "Dues" -> overviewData.pendingStudents.isEmpty()
            "Schools" -> overviewData.schoolGroups.isEmpty()
            else -> overviewData.items.isEmpty()
        }

        if (title == "Due" || title == "Dues") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        sortOldestFirst = !sortOldestFirst
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (sortOldestFirst) {
                            "Old first"
                        } else {
                            "New first"
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
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
                if (title == "Due" || title == "Dues") {

                    items(overviewData.pendingStudents) { entry ->

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
                                            val entry = IndexedValue(
                                                index,
                                                student
                                            )
                                            if (AppSettings.confirmFullFeePaid.value) {
                                                confirmPaidStudent = entry
                                            } else {
                                                markFullyPaid(entry)
                                            }
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(8.dp)
                                    ) {
                                        DueAmountChip(
                                            label = "Monthly",
                                            value = monthlyFeeText,
                                            modifier = Modifier.weight(1f)
                                        )
                                        DueAmountChip(
                                            label = "Total",
                                            value = totalDueText,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    val monthRange =
                                        student.monthRangeLabel(monthsUnpaid)
                                    if (monthRange.isNotBlank()) {
                                        Text(
                                            text = monthRange,
                                            style =
                                                MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                        )
                                    }
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
                        overviewData.schoolGroups.forEach { (school, groupStudents) ->
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
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment =
                                                androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = student.studentName,
                                                style =
                                                    MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
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
                                            modifier = Modifier.height(6.dp)
                                        )

                                        val mainNumber =
                                            student.contacts.mainNumber()
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
                                        Text(
                                            text = "Main: ${mainNumber.ifBlank { "-" }}",
                                            style =
                                                MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "Monthly fee: ${
                                                monthlyFee?.let { amount ->
                                                    "$currency${"%.2f".format(amount)}"
                                                } ?: "-"
                                            }",
                                            style =
                                                MaterialTheme.typography.bodySmall
                                        )
                                        student.feeDueDateMillis?.let { dueDate ->
                                            Text(
                                                text = "Due date: ${formatDate(dueDate)}",
                                                style =
                                                    MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        student.lastFeePaidMillis?.let { paidDate ->
                                            Text(
                                                text = "Last paid: ${formatDate(paidDate)}",
                                                style =
                                                    MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        student.advanceBalance
                                            ?.takeIf { amount -> amount > 0.0 }
                                            ?.let { advance ->
                                                Text(
                                                    text = "Advance: $currency${"%.2f".format(advance)}",
                                                    style =
                                                        MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        if (student.contacts.size > 1) {
                                            Text(
                                                text = "Contacts: ${student.contacts.size}",
                                                style =
                                                    MaterialTheme.typography.bodySmall
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
                                            "Own",
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
                        items(overviewData.items) { item ->
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

private data class OverviewData(
    val pendingStudents: List<IndexedValue<com.raju.edutrack.Student>>,
    val totalPendingAmount: Double,
    val schoolGroups: Map<String, List<com.raju.edutrack.Student>>,
    val items: List<OverviewItem>
)

@Composable
private fun DueAmountChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun monthlyFeeForStudent(
    student: com.raju.edutrack.Student
): Double? {
    return student.feeDueAmount ?: if (
        AppSettings.autoClassFeesEnabled.value
    ) {
        AppSettings.parseClassFeeAmount(student.className)
    } else {
        null
    }
}

private fun totalDueForStudent(
    student: com.raju.edutrack.Student,
    nowMillis: Long
): Double {
    val monthlyFee = monthlyFeeForStudent(student) ?: return 0.0
    val monthsUnpaid = effectiveMonthsUnpaid(
        student = student,
        countFeeFromJoinDate = AppSettings.countFeeFromJoinDate.value,
        monthlyFee = monthlyFee,
        nowMillis = nowMillis
    )
    val advance = student.advanceBalance ?: 0.0
    val advanceRemainder = if (monthlyFee > 0.0) {
        val advanceMonths = (advance / monthlyFee).toInt()
        advance - (advanceMonths * monthlyFee)
    } else {
        advance
    }
    return (monthlyFee * monthsUnpaid - advanceRemainder)
        .coerceAtLeast(0.0)
}

private fun com.raju.edutrack.Student.paymentHistoryEntry(
    amountPaid: Double,
    remainingDue: Double?,
    updatedStudent: com.raju.edutrack.Student
): PaymentHistoryEntry {
    return PaymentHistoryEntry(
        studentId = id,
        studentName = studentName,
        className = className,
        schoolName = schoolName,
        batchName = batchName,
        mainNumber = contacts.mainNumber().ifBlank { null },
        amountPaid = amountPaid,
        remainingDue = remainingDue,
        paidAtMillis = System.currentTimeMillis(),
        beforeLastFeePaidMillis = lastFeePaidMillis,
        beforeFeeDueDateMillis = feeDueDateMillis,
        beforeAdvanceBalance = advanceBalance,
        afterLastFeePaidMillis = updatedStudent.lastFeePaidMillis,
        afterFeeDueDateMillis = updatedStudent.feeDueDateMillis,
        afterAdvanceBalance = updatedStudent.advanceBalance
    )
}

private fun com.raju.edutrack.Student.monthRangeLabel(monthsUnpaid: Int): String {
    if (monthsUnpaid <= 0) {
        return ""
    }
    val startMillis = lastFeePaidMillis ?: joinDateMillis
    val endMillis = addMonths(startMillis, monthsUnpaid)
    val formatter = SimpleDateFormat("MM/yyyy", Locale.getDefault())
    return "${formatter.format(startMillis)}-${formatter.format(endMillis)}"
}

private fun List<Contact>.mainNumber(): String {
    return firstOrNull { contact ->
        contact.label.equals("Own", ignoreCase = true) &&
            contact.number.isNotBlank()
    }?.number
        ?: firstOrNull { contact ->
            contact.label.equals("Primary", ignoreCase = true) &&
                contact.number.isNotBlank()
        }?.number
        ?: firstOrNull { contact -> contact.number.isNotBlank() }?.number
        ?: ""
}
