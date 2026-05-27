package com.raju.edutrack.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.raju.edutrack.Batch
import com.raju.edutrack.BatchManager
import com.raju.edutrack.MessageSender
import com.raju.edutrack.StudentManager

@Composable
fun BatchScreen() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var editingBatchName by remember { mutableStateOf<String?>(null) }
    var batchName by remember { mutableStateOf("") }
    var batchTime by remember { mutableStateOf("") }
    var batchMessage by remember { mutableStateOf("") }

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
                    modifier = Modifier.fillMaxWidth()
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
                                Text(
                                    text = "Students: ${batchStudents.size}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (batch.timeText.isNotBlank()) {
                                    Text(
                                        text = "Time: ${batch.timeText}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        editingBatchName = batch.name
                                        batchName = batch.name
                                        batchTime = batch.timeText
                                        batchMessage = batch.messageTemplate
                                        showDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit"
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        BatchManager.removeBatch(
                                            context,
                                            batch.name
                                        )
                                        StudentManager.students
                                            .withIndex()
                                            .filter { entry ->
                                                entry.value.batchName
                                                    ?.equals(
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
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete"
                                    )
                                }
                            }
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

                            batchStudents.forEach { student ->
                                Text(
                                    text = "• ${student.studentName} (${student.className})",
                                    style = MaterialTheme.typography.bodySmall
                                )
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
