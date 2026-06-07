package com.raju.edutrack.screens

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import com.raju.edutrack.AppSettings
import com.raju.edutrack.Batch
import com.raju.edutrack.BatchManager
import com.raju.edutrack.Contact
import com.raju.edutrack.MessageSender
import com.raju.edutrack.Student
import com.raju.edutrack.StudentManager
import com.raju.edutrack.matchesStudentQuery
import java.util.Calendar
import java.util.Locale

@Composable 
fun BatchScreen() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var editingBatchName by remember { mutableStateOf<String?>(null) }
    var actionBatch by remember { mutableStateOf<Batch?>(null) }
    var callStudent by remember { mutableStateOf<Student?>(null) }
    var contactActionIsWhatsApp by remember { mutableStateOf(false) }
    var manageStudentsBatch by remember { mutableStateOf<Batch?>(null) }
    var studentSearchQuery by remember { mutableStateOf("") }
    var batchName by remember { mutableStateOf("") }
    var batchTime by remember { mutableStateOf("") }
    var batchMessage by remember { mutableStateOf("") }
    val studentsByBatch by remember {
        derivedStateOf {
            StudentManager.students.groupBy { student ->
                student.batchName?.lowercase().orEmpty()
            }
        }
    }

    fun startEdit(batch: Batch) {
        editingBatchName = batch.name
        batchName = batch.name
        batchTime = batch.timeText
        batchMessage = batch.messageTemplate
        showDialog = true
    }

    fun deleteBatch(batch: Batch) {
        BatchManager.removeBatch(context, batch.name)
        StudentManager.updateStudents(context) { student ->
            if (
                student.batchName?.equals(
                    batch.name,
                    ignoreCase = true
                ) == true
            ) {
                student.copy(batchName = null)
            } else {
                student
            }
        }
    }

    fun dial(number: String) {
        val sanitized = number.trim()
        if (sanitized.isNotBlank()) {
            context.startActivity(
                Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:$sanitized")
                )
            )
        }
    }

    fun openWhatsApp(number: String) {
        val sanitized = number.onlyDigits()
        if (sanitized.isNotBlank()) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/$sanitized")
                )
            )
        }
    }

    fun requestContactNumber(student: Student, isWhatsApp: Boolean) {
        contactActionIsWhatsApp = isWhatsApp
        callStudent = student
    }

    BackHandler(
        enabled = manageStudentsBatch != null && studentSearchQuery.isNotBlank()
    ) {
        studentSearchQuery = ""
    }

    fun showBatchTimePicker() {
        val calendar = Calendar.getInstance()
        val parsedTime = parseBatchTime(batchTime)
        val initialHour = parsedTime?.first ?: calendar.get(Calendar.HOUR_OF_DAY)
        val initialMinute = parsedTime?.second ?: calendar.get(Calendar.MINUTE)
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                batchTime = formatBatchTime(hourOfDay, minute)
            },
            initialHour,
            initialMinute,
            false
        ).show()
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { /* Do nothing */ },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            confirmButton = {
                Button(
                    onClick = {
                        val normalized = batchName.trim()
                        if (normalized.isNotBlank()) {
                            val current = editingBatchName
                            val updatedBatch = Batch(
                                name = normalized,
                                timeText = batchTime.trim(),
                                messageTemplate = batchMessage.trim()
                            )
                            if (current == null) {
                                BatchManager.addBatch(
                                    context,
                                    updatedBatch
                                )
                            } else if (!current.equals(normalized, true)) {
                                StudentManager.updateStudents(context) { student ->
                                    if (
                                        student.batchName?.equals(
                                            current,
                                            ignoreCase = true
                                        ) == true
                                    ) {
                                        student.copy(batchName = normalized)
                                    } else {
                                        student
                                    }
                                }
                                BatchManager.updateBatch(
                                    context,
                                    current,
                                    updatedBatch
                                )
                            } else {
                                BatchManager.updateBatch(
                                    context,
                                    current,
                                    updatedBatch
                                )
                            }
                        }
                        batchName = ""
                        batchTime = ""
                        batchMessage = ""
                        editingBatchName = null
                        showDialog = false
                    }
                ) {
                    Text(if (editingBatchName == null) "Save" else "Update")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        batchName = ""
                        batchTime = ""
                        batchMessage = ""
                        editingBatchName = null
                        showDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            },
            title = {
                Text(if (editingBatchName == null) "Add Batch" else "Edit Batch")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = batchName,
                        onValueChange = { batchName = it },
                        label = { Text("Batch name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = batchTime,
                        onValueChange = { batchTime = it },
                        label = { Text("Batch time") },
                        placeholder = { Text("Example: 5:00 PM") },
                        trailingIcon = {
                            IconButton(
                                onClick = { showBatchTimePicker() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Select time"
                                )
                            }
                        },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = batchMessage,
                        onValueChange = { batchMessage = it },
                        label = { Text("Batch message") },
                        supportingText = {
                            Text("Leave empty to use global template")
                        },
                        minLines = 3
                    )
                }
            }
        )
    }

    actionBatch?.let { batch ->
        val hasStudentsInBatch = StudentManager.students.any { student ->
            student.batchName?.equals(batch.name, ignoreCase = true) == true
        }
        AlertDialog(
            onDismissRequest = { actionBatch = null },
            confirmButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (hasStudentsInBatch) {
                        TextButton(
                            onClick = {
                                manageStudentsBatch = batch
                                actionBatch = null
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add/Remove Students")
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                startEdit(batch)
                                actionBatch = null
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit")
                        }
                        TextButton(
                            onClick = {
                                deleteBatch(batch)
                                actionBatch = null
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete")
                        }
                    }
                }
            },
            title = { Text(batch.name) },
            text = { Text("Choose an action for this batch.") }
        )
    }

    callStudent?.let { student ->
        val numbers = student.contacts
            .filter { contact -> contact.number.isNotBlank() }
        AlertDialog(
            onDismissRequest = { callStudent = null },
            confirmButton = {
                TextButton(onClick = { callStudent = null }) {
                    Text("Close")
                }
            },
            title = {
                Text(
                    "${if (contactActionIsWhatsApp) "WhatsApp" else "Call"} ${student.studentName}"
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (numbers.isEmpty()) {
                        Text("No number saved")
                    }
                    numbers.forEach { contact ->
                        ListItem(
                            headlineContent = {
                                Text(contact.label.ifBlank { "Own" })
                            },
                            supportingContent = {
                                Text(contact.number)
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        if (contactActionIsWhatsApp) {
                                            openWhatsApp(contact.number)
                                        } else {
                                            dial(contact.number)
                                        }
                                        callStudent = null
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (contactActionIsWhatsApp)
                                            Icons.Default.Send
                                        else
                                            Icons.Default.Phone,
                                        contentDescription = if (contactActionIsWhatsApp)
                                            "WhatsApp"
                                        else
                                            "Call"
                                    )
                                }
                            }
                        )
                    }
                }
            }
        )
    }

    manageStudentsBatch?.let { batch ->
        AlertDialog(
            onDismissRequest = {
                manageStudentsBatch = null
                studentSearchQuery = ""
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        manageStudentsBatch = null
                        studentSearchQuery = ""
                    }
                ) {
                    Text("Close")
                }
            },
            title = {
                Text("Add/Remove Students for ${batch.name}")
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    OutlinedTextField(
                        value = studentSearchQuery,
                        onValueChange = { studentSearchQuery = it },
                        label = { Text("Search students") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    val filteredStudents = StudentManager.students.withIndex().filter { entry ->
                        entry.value.matchesStudentQuery(studentSearchQuery)
                    }
                    if (filteredStudents.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No students found")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredStudents, key = { it.value.id }) { entry ->
                                val index = entry.index
                                val student = entry.value
                                val isChecked = student.batchName?.equals(batch.name, ignoreCase = true) == true
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val updated = student.copy(
                                                batchName = if (isChecked) null else batch.name
                                            )
                                            StudentManager.updateStudent(context, index, updated)
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            val updated = student.copy(
                                                batchName = if (checked) batch.name else null
                                            )
                                            StudentManager.updateStudent(context, index, updated)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = student.studentName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Class: ${student.className} • School: ${student.schoolName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Batch: ${student.batchName?.ifBlank { "-" } ?: "-"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Main: ${student.contacts.mainNumber().ifBlank { "-" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Batch"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = BatchManager.batches,
                key = { batch -> batch.name }
            ) { batch ->
                val batchStudents =
                    studentsByBatch[batch.name.lowercase()].orEmpty()
                val classSummary = batchStudents
                    .map { student -> student.className.trim() }
                    .filter { value -> value.isNotBlank() }
                    .distinctBy { value -> value.lowercase() }
                    .take(3)
                    .joinToString()
                val schoolSummary = batchStudents
                    .map { student -> student.schoolName.trim() }
                    .filter { value -> value.isNotBlank() }
                    .distinctBy { value -> value.lowercase() }
                    .take(3)
                    .joinToString()
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                actionBatch = batch
                            }
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = batch.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (batch.timeText.isNotBlank()) {
                                    Text(
                                        text = "Time: ${batch.timeText}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            Text(
                                text = "${batchStudents.size} students",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedButton(
                                onClick = { manageStudentsBatch = batch },
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(
                                    horizontal = 10.dp,
                                    vertical = 0.dp
                                )
                            ) {
                                Text(
                                    text = "+/-",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            }

                        if (classSummary.isNotBlank() || schoolSummary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = listOf(
                                    classSummary.takeIf { it.isNotBlank() }?.let { "Classes: $it" },
                                    schoolSummary.takeIf { it.isNotBlank() }?.let { "Schools: $it" }
                                ).filterNotNull().joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        if (batchStudents.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "No students in this batch",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (AppSettings.batchReminderMessagesEnabled.value) {
                                    FilledTonalButton(
                                        onClick = {
                                            MessageSender.sendBatchMessage(
                                                context,
                                                batch,
                                                batchStudents
                                            )
                                        }
                                    ) {
                                        Text("Send reminder")
                                    }
                                }
                            }

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            batchStudents.forEachIndexed { index, student ->
                                val mainNumber = student.contacts.mainNumber()
                                if (index > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = student.studentName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Class: ${student.className} • School: ${student.schoolName}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "Main: ${mainNumber.ifBlank { "No number" }}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            enabled = student.contacts.any { it.number.isNotBlank() },
                                            onClick = {
                                                requestContactNumber(
                                                    student = student,
                                                    isWhatsApp = false
                                                )
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = "Call"
                                            )
                                        }
                                        IconButton(
                                            enabled = student.contacts.any { it.number.isNotBlank() },
                                            onClick = {
                                                requestContactNumber(
                                                    student = student,
                                                    isWhatsApp = true
                                                )
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Send,
                                                contentDescription = "WhatsApp"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (BatchManager.batches.isEmpty()) {
                item {
                    Text(
                        text = "No batches yet",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
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

private fun String.onlyDigits(): String {
    return filter { character -> character.isDigit() }
}

private fun formatBatchTime(
    hourOfDay: Int,
    minute: Int
): String {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
    calendar.set(Calendar.MINUTE, minute)
    return java.text.SimpleDateFormat(
        "h:mm a",
        Locale.getDefault()
    ).format(calendar.time)
}

private fun parseBatchTime(text: String): Pair<Int, Int>? {
    val formats = listOf("h:mm a", "hh:mm a", "H:mm", "HH:mm")
    return formats.firstNotNullOfOrNull { pattern ->
        try {
            val parser = java.text.SimpleDateFormat(
                pattern,
                Locale.getDefault()
            ).apply {
                isLenient = false
            }
            val date = parser.parse(text.trim()) ?: return@firstNotNullOfOrNull null
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
        } catch (_: java.text.ParseException) {
            null
        }
    }
}
