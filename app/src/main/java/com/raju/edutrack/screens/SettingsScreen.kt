package com.raju.edutrack.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.raju.edutrack.AppSettings
import com.raju.edutrack.AutoBatchMode
import com.raju.edutrack.BatchManager
import com.raju.edutrack.ClassFeeEntry
import com.raju.edutrack.MessageChannel
import com.raju.edutrack.MessageSendMode
import com.raju.edutrack.StudentManager
import com.raju.edutrack.update.UpdateCheckResult
import com.raju.edutrack.update.UpdateConfig
import com.raju.edutrack.update.UpdateInstallResult
import com.raju.edutrack.update.UpdateManager
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var updateResult by remember {
        mutableStateOf<UpdateCheckResult?>(null)
    }
    var updateMessage by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var isInstalling by remember { mutableStateOf(false) }
    var expandedClassIndex by remember { mutableStateOf<Int?>(null) }
    var newClassOption by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        HorizontalDivider()

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Count Fee",
            style = MaterialTheme.typography.titleMedium
        )

        listOf(
            true to "From joining date",
            false to "Starting of each month"
        ).forEach { (value, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = AppSettings.countFeeFromJoinDate.value == value,
                    onClick = {
                        AppSettings.countFeeFromJoinDate.value = value
                        AppSettings.save(context)
                    }
                )
                Text(label)
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Fee Display",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("₹", "$", "€", "£").forEach { symbol ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = AppSettings.currencySymbol.value == symbol,
                        onClick = {
                            AppSettings.currencySymbol.value = symbol
                            AppSettings.save(context)
                        }
                    )
                    Text(symbol)
                }
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Batch Settings",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Auto add students to batch",
            style = MaterialTheme.typography.bodySmall
        )

        val batchModeOptions = listOf(
            AutoBatchMode.NONE to "Don't auto add",
            AutoBatchMode.CLASS to "By class name",
            AutoBatchMode.CLASS_SCHOOL to "By class + school"
        )

        batchModeOptions.forEach { (mode, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = AppSettings.autoBatchMode.value == mode,
                    onClick = {
                        AppSettings.autoBatchMode.value = mode
                        AppSettings.save(context)
                    }
                )
                Text(label)
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Message Settings",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text("Send after fee paid")
                Text(
                    text = "Message includes paid amount and pending due.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = AppSettings.feePaymentMessagesEnabled.value,
                onCheckedChange = { isChecked ->
                    AppSettings.feePaymentMessagesEnabled.value = isChecked
                    AppSettings.save(context)
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text("Batch reminder button")
                Text(
                    text = "Show reminder action in batches and use each batch template when sent.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = AppSettings.batchReminderMessagesEnabled.value,
                onCheckedChange = { isChecked ->
                    AppSettings.batchReminderMessagesEnabled.value = isChecked
                    AppSettings.save(context)
                }
            )
        }

        Text(
            text = "Send by",
            style = MaterialTheme.typography.bodySmall
        )
        listOf(
            MessageChannel.SMS to "SMS",
            MessageChannel.WHATSAPP to "WhatsApp",
            MessageChannel.BOTH to "Both"
        ).forEach { (channel, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = AppSettings.messageChannel.value == channel,
                    onClick = {
                        AppSettings.messageChannel.value = channel
                        AppSettings.save(context)
                    }
                )
                Text(label)
            }
        }

        Text(
            text = "Sending mode",
            style = MaterialTheme.typography.bodySmall
        )
        listOf(
            MessageSendMode.COMPOSER to "Open composer",
            MessageSendMode.AUTO to "Auto send SMS only"
        ).forEach { (sendMode, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = AppSettings.messageSendMode.value == sendMode,
                    onClick = {
                        AppSettings.messageSendMode.value = sendMode
                        AppSettings.save(context)
                    }
                )
                Text(label)
            }
        }

        Text(
            text = "Auto send needs SMS permission. WhatsApp messages cannot be sent automatically by another app; EduTrack opens WhatsApp with the message ready.",
            style = MaterialTheme.typography.bodySmall
        )

        OutlinedTextField(
            value = AppSettings.feePaidMessageTemplate.value,
            onValueChange = { newValue ->
                AppSettings.feePaidMessageTemplate.value = newValue
                AppSettings.save(context)
            },
            label = { Text("Fee paid message") },
            supportingText = {
                Text("{studentName}, {amountPaid}, {dueAmount}, {batchName}, {time}")
            },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = AppSettings.batchMessageTemplate.value,
            onValueChange = { newValue ->
                AppSettings.batchMessageTemplate.value = newValue
                AppSettings.save(context)
            },
            label = { Text("Global batch message") },
            supportingText = {
                Text("{studentName}, {amountPaid}, {dueAmount}, {batchName}, {time}")
            },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Class Fees",
            style = MaterialTheme.typography.titleMedium
        )

        val classOptions = AppSettings.getClassOptions()

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text("Set fee automatically")
                Text(
                    text = "Use the monthly fee saved for each class when adding students.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = AppSettings.autoClassFeesEnabled.value,
                onCheckedChange = { isChecked ->
                    AppSettings.autoClassFeesEnabled.value = isChecked
                    AppSettings.save(context)
                }
            )
        }

        if (AppSettings.autoClassFeesEnabled.value) {
            AppSettings.classFeeEntries.forEachIndexed { index, entry ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = expandedClassIndex == index,
                                onExpandedChange = { expanded ->
                                    expandedClassIndex =
                                        if (expanded) index else null
                                }
                            ) {
                                OutlinedTextField(
                                    value = entry.className,
                                    onValueChange = {},
                                    label = { Text("Class") },
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded =
                                                expandedClassIndex == index
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .menuAnchor()
                                )

                                ExposedDropdownMenu(
                                    expanded = expandedClassIndex == index,
                                    onDismissRequest = {
                                        expandedClassIndex = null
                                    }
                                ) {
                                    classOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                AppSettings
                                                    .classFeeEntries[index] =
                                                    entry.copy(
                                                        className = option
                                                    )
                                                AppSettings.save(context)
                                                expandedClassIndex = null
                                            }
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    AppSettings.classFeeEntries.removeAt(index)
                                    AppSettings.save(context)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove"
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        OutlinedTextField(
                            value = entry.amountText,
                            onValueChange = { newValue ->
                                AppSettings.classFeeEntries[index] =
                                    entry.copy(amountText = newValue)
                                AppSettings.save(context)
                            },
                            label = { Text("Monthly fee") },
                            prefix = {
                                Text(AppSettings.currencySymbol.value)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            FilledTonalButton(
                onClick = {
                    AppSettings.classFeeEntries.add(
                        ClassFeeEntry(className = "", amountText = "")
                    )
                    AppSettings.save(context)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(
                    modifier = Modifier.width(6.dp)
                )
                Text("Add class fee")
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "More Class Options",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = newClassOption,
            onValueChange = { newClassOption = it },
            label = { Text("Add class option") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        FilledTonalButton(
            onClick = {
                val normalized = newClassOption.trim()
                if (normalized.isNotBlank()) {
                    AppSettings.customClassOptions.add(normalized)
                    AppSettings.save(context)
                    newClassOption = ""
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )
            Spacer(
                modifier = Modifier.width(6.dp)
            )
            Text("Add class option")
        }

        if (AppSettings.customClassOptions.isNotEmpty()) {
            AppSettings.customClassOptions.forEachIndexed { index, option ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(option)
                    IconButton(
                        onClick = {
                            AppSettings.customClassOptions.removeAt(index)
                            AppSettings.save(context)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove"
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Data",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Button(
            onClick = {
                val root = JSONObject()
                val studentsArray = JSONArray()
                StudentManager.students.forEach { student ->
                    val item = JSONObject()
                    item.put("studentName", student.studentName)
                    item.put("className", student.className)
                    item.put("schoolName", student.schoolName)
                    item.put("joinDateMillis", student.joinDateMillis)
                    student.lastFeePaidMillis?.let { value ->
                        item.put("lastFeePaidMillis", value)
                    }
                    student.batchName?.let { value ->
                        item.put("batchName", value)
                    }
                    student.feeDueAmount?.let { value ->
                        item.put("feeDueAmount", value)
                    }
                    student.feeDueDateMillis?.let { value ->
                        item.put("feeDueDateMillis", value)
                    }
                    student.advanceBalance?.let { value ->
                        item.put("advanceBalance", value)
                    }
                    val contactsArray = JSONArray()
                    student.contacts.forEach { contact ->
                        val contactObject = JSONObject()
                        contactObject.put("label", contact.label)
                        contactObject.put("number", contact.number)
                        contactsArray.put(contactObject)
                    }
                    item.put("contacts", contactsArray)
                    studentsArray.put(item)
                }
                val batchesArray = JSONArray()
                BatchManager.batches.forEach { batch ->
                    val item = JSONObject()
                    item.put("name", batch.name)
                    item.put("timeText", batch.timeText)
                    item.put("messageTemplate", batch.messageTemplate)
                    batchesArray.put(item)
                }
                root.put("students", studentsArray)
                root.put("batches", batchesArray)

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_TEXT, root.toString())
                    putExtra(Intent.EXTRA_SUBJECT, "EduTrack export")
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Export data")
                )
            }
        ) {
            Text("Export data JSON")
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "App Update",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        if (!UpdateConfig.isConfigured) {
            Text(
                text = "Set UpdateConfig.owner and UpdateConfig.repo",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = {
                if (!isChecking) {
                    isChecking = true
                    updateMessage = ""
                    scope.launch {
                        val result =
                            UpdateManager.checkForUpdate(context)
                        updateResult = result
                        isChecking = false
                    }
                }
            },
            enabled = !isChecking
        ) {
            Text(if (isChecking) "Checking..." else "Check for update")
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        when (val result = updateResult) {
            is UpdateCheckResult.UpdateAvailable -> {
                Text(
                    text = "Update available: ${result.release.tagName}",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Button(
                    onClick = {
                        if (!isInstalling && activity != null) {
                            isInstalling = true
                            updateMessage = ""
                            scope.launch {
                                val installResult =
                                    UpdateManager.installUpdate(
                                        activity,
                                        result.release.downloadUrl
                                    )
                                updateMessage = when (installResult) {
                                    UpdateInstallResult.Started ->
                                        "Installer opened"
                                    UpdateInstallResult.NeedsPermission ->
                                        "Allow install permission"
                                    is UpdateInstallResult.Failed ->
                                        installResult.message
                                }
                                isInstalling = false
                            }
                        } else if (activity == null) {
                            updateMessage = "No activity context"
                        }
                    },
                    enabled = !isInstalling
                ) {
                    Text(if (isInstalling) "Installing..." else "Install update")
                }
            }

            is UpdateCheckResult.UpToDate -> {
                Text(
                    text = "You are up to date (${result.currentVersion})",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            is UpdateCheckResult.NotConfigured -> {
                Text(
                    text = result.message,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            is UpdateCheckResult.Failed -> {
                Text(
                    text = result.message,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            null -> Unit
        }

        if (updateMessage.isNotBlank()) {
            Spacer(
                modifier = Modifier.height(6.dp)
            )
            Text(
                text = updateMessage,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
