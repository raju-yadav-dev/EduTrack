package com.raju.edutrack.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

@Composable 
fun BatchScreen() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var editingBatchName by remember { mutableStateOf<String?>(null) }
    var actionBatch by remember { mutableStateOf<Batch?>(null) }
    var callStudent by remember { mutableStateOf<Student?>(null) }
    var batchName by remember { mutableStateOf("") }
    var batchTime by remember { mutableStateOf("") }
    var batchMessage by remember { mutableStateOf("") }

    fun startEdit(batch: Batch) {
        editingBatchName = batch.name
        batchName = batch.name
        batchTime = batch.timeText
        batchMessage = batch.messageTemplate
        showDialog = true
    }

    fun deleteBatch(batch: Batch) {
        BatchManager.removeBatch(context, batch.name)
        StudentManager.students
            .withIndex()
            .filter { entry ->
                entry.value.batchName?.equals(
                    batch.name,
                    ignoreCase = true
                ) == true
            }
            .forEach { entry ->
                StudentManager.updateStudent(
                    context,
                    entry.index,
                    entry.value.copy(batchName = null)
                )
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

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
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
                                StudentManager.students
                                    .withIndex()
                                    .filter { entry ->
                                        entry.value.batchName
                                            ?.equals(
                                                current,
                                                ignoreCase = true
                                            ) == true
                                    }
                                    .forEach { entry ->
                                        StudentManager.updateStudent(
                                            context,
                                            entry.index,
                                            entry.value.copy(batchName = normalized)
                                        )
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
        AlertDialog(
            onDismissRequest = { actionBatch = null },
            confirmButton = {
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
            },
            dismissButton = {
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
            title = { Text("Call ${student.studentName}") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    numbers.forEach { contact ->
                        ListItem(
                            headlineContent = {
                                Text(contact.label.ifBlank { "Main" })
                            },
                            supportingContent = {
                                Text(contact.number)
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        dial(contact.number)
                                        callStudent = null
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Call"
                                    )
                                }
                            }
                        )
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
                val batchStudents = StudentManager.students
                    .filter { student ->
                        student.batchName?.equals(
                            batch.name,
                            ignoreCase = true
                        ) == true
                    }

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
                                    style = MaterialTheme.typography.titleMedium
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
                        }

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        if (batchStudents.isEmpty()) {
                            Text(
                                text = "No students in this batch",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
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

                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )
                            }

                            batchStudents.forEach { student ->
                                val mainNumber = student.contacts.mainNumber()
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
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = mainNumber.ifBlank { "No number" },
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    IconButton(
                                        enabled = student.contacts.any { it.number.isNotBlank() },
                                        onClick = {
                                            val numbers = student.contacts
                                                .filter { it.number.isNotBlank() }
                                            if (numbers.size == 1) {
                                                dial(numbers.first().number)
                                            } else {
                                                callStudent = student
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Call"
                                        )
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
        contact.label.equals("Main", ignoreCase = true) &&
            contact.number.isNotBlank()
    }?.number
        ?: firstOrNull { contact ->
            contact.label.equals("Primary", ignoreCase = true) &&
                contact.number.isNotBlank()
        }?.number
        ?: firstOrNull { contact -> contact.number.isNotBlank() }?.number
        ?: ""
}
