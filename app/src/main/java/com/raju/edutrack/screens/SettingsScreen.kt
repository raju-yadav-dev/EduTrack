package com.raju.edutrack.screens

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.raju.edutrack.AppSettings
import com.raju.edutrack.AutoBatchMode
import com.raju.edutrack.Batch
import com.raju.edutrack.BatchManager
import com.raju.edutrack.ClassFeeEntry
import com.raju.edutrack.Contact
import com.raju.edutrack.MessageChannel
import com.raju.edutrack.MessageSendMode
import com.raju.edutrack.Student
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
    var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var updateMessage by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var isInstalling by remember { mutableStateOf(false) }
    var expandedClassIndex by remember { mutableStateOf<Int?>(null) }
    var newClassOption by remember { mutableStateOf("") }
    var selectedSettingsSection by remember { mutableStateOf<SettingsSectionKey?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        val result = runCatching {
            val raw = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { reader -> reader.readText() }
                .orEmpty()
            importEduTrackData(context = context, raw = raw)
        }

        importMessage = result.fold(
            onSuccess = { summary ->
                "Imported ${summary.studentCount} students and ${summary.batchCount} batches"
            },
            onFailure = { error ->
                error.message ?: "Could not import data"
            }
        )
        Toast.makeText(context, importMessage, Toast.LENGTH_LONG).show()
    }

    BackHandler(enabled = selectedSettingsSection != null) {
        selectedSettingsSection = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (selectedSettingsSection == null) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )

            SettingsCategoryRow(
                icon = Icons.Default.Payments,
                title = "Fees",
                subtitle = "Due count, currency, class fee defaults",
                onClick = { selectedSettingsSection = SettingsSectionKey.FEES }
            )

            SettingsCategoryRow(
                icon = Icons.Default.School,
                title = "Classes",
                subtitle = "Manage extra class options",
                onClick = { selectedSettingsSection = SettingsSectionKey.CLASSES }
            )

            SettingsCategoryRow(
                icon = Icons.Default.Group,
                title = "Batches",
                subtitle = "Auto add students to batches",
                onClick = { selectedSettingsSection = SettingsSectionKey.BATCHES }
            )

            SettingsCategoryRow(
                icon = Icons.AutoMirrored.Filled.Message,
                title = "Messages",
                subtitle = "Fee receipts, batch reminders, send mode",
                onClick = { selectedSettingsSection = SettingsSectionKey.MESSAGES }
            )

            SettingsCategoryRow(
                icon = Icons.Default.Storage,
                title = "Data",
                subtitle = "Export and import JSON backups",
                onClick = { selectedSettingsSection = SettingsSectionKey.DATA }
            )

            SettingsCategoryRow(
                icon = Icons.Default.CloudDownload,
                title = "App Update",
                subtitle = "Check and install new releases",
                onClick = { selectedSettingsSection = SettingsSectionKey.UPDATE }
            )
            return@Column
        }

        if (selectedSettingsSection == SettingsSectionKey.FEES) {
            SettingsDetailPage(
            title = "Fees",
            onBack = { selectedSettingsSection = null }
        ) {
            SettingsGroup("Fee calculation") {
                listOf(
                    true to "From joining date",
                    false to "Starting of each month"
                ).forEach { (value, label) ->
                    RadioOption(
                        selected = AppSettings.countFeeFromJoinDate.value == value,
                        label = label,
                        onClick = {
                            AppSettings.countFeeFromJoinDate.value = value
                            AppSettings.save(context)
                        }
                    )
                }
            }

            SettingsGroup("Currency") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("₹", "$", "€", "£").forEach { symbol ->
                        RadioOption(
                            selected = AppSettings.currencySymbol.value == symbol,
                            label = symbol,
                            onClick = {
                                AppSettings.currencySymbol.value = symbol
                                AppSettings.save(context)
                            }
                        )
                    }
                }
            }

            SettingsGroup("Class fee defaults") {
                SettingSwitchRow(
                    title = "Set fee automatically",
                    subtitle = "Use the monthly fee saved for each class.",
                    checked = AppSettings.autoClassFeesEnabled.value,
                    onCheckedChange = { isChecked ->
                        AppSettings.autoClassFeesEnabled.value = isChecked
                        AppSettings.save(context)
                    }
                )

                if (AppSettings.autoClassFeesEnabled.value) {
                    val classOptions = AppSettings.getClassOptions()
                    AppSettings.classFeeEntries.forEachIndexed { index, entry ->
                        ClassFeeEditor(
                            entry = entry,
                            index = index,
                            expandedClassIndex = expandedClassIndex,
                            classOptions = classOptions,
                            onExpandedChange = { expanded ->
                                expandedClassIndex = if (expanded) index else null
                            },
                            onDismiss = { expandedClassIndex = null },
                            onClassSelected = { option ->
                                AppSettings.classFeeEntries[index] =
                                    entry.copy(className = option)
                                AppSettings.save(context)
                                expandedClassIndex = null
                            },
                            onAmountChange = { newValue ->
                                AppSettings.classFeeEntries[index] =
                                    entry.copy(amountText = newValue)
                                AppSettings.save(context)
                            },
                            onRemove = {
                                AppSettings.classFeeEntries.removeAt(index)
                                AppSettings.save(context)
                            }
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
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add class fee")
                    }
                }
            }
        }
        }

        if (selectedSettingsSection == SettingsSectionKey.CLASSES) {
            SettingsDetailPage(
            title = "Classes",
            onBack = { selectedSettingsSection = null }
        ) {
            SettingsGroup("Add class") {
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
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add class option")
                }
            }

            if (AppSettings.customClassOptions.isNotEmpty()) {
                SettingsGroup("Saved class options") {
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
                        if (index != AppSettings.customClassOptions.lastIndex) {
                            OptionDivider()
                        }
                    }
                }
            }
        }
        }

        if (selectedSettingsSection == SettingsSectionKey.BATCHES) {
            SettingsDetailPage(
            title = "Batches",
            onBack = { selectedSettingsSection = null }
        ) {
            SettingsGroup("Auto add students") {
                listOf(
                    AutoBatchMode.NONE to "Don't auto add",
                    AutoBatchMode.CLASS to "By class name",
                    AutoBatchMode.CLASS_SCHOOL to "By class + school"
                ).forEach { (mode, label) ->
                    RadioOption(
                        selected = AppSettings.autoBatchMode.value == mode,
                        label = label,
                        onClick = {
                            AppSettings.autoBatchMode.value = mode
                            AppSettings.save(context)
                        }
                    )
                }
            }

            SettingsGroup("Batch maintenance") {
                Text(
                    text = "${BatchManager.batches.size} batches saved",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilledTonalButton(
                    onClick = {
                        BatchManager.syncFromStudents(context)
                        Toast.makeText(
                            context,
                            "Batches synced from students",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sync batches from students")
                }
            }
        }
        }

        if (selectedSettingsSection == SettingsSectionKey.MESSAGES) {
            SettingsDetailPage(
            title = "Messages",
            onBack = { selectedSettingsSection = null }
        ) {
            SettingsGroup("Message actions") {
                SettingSwitchRow(
                    title = "Send after fee paid",
                    subtitle = "Includes paid amount and pending due.",
                    checked = AppSettings.feePaymentMessagesEnabled.value,
                    onCheckedChange = { isChecked ->
                        AppSettings.feePaymentMessagesEnabled.value = isChecked
                        AppSettings.save(context)
                    }
                )

                OptionDivider()

                SettingSwitchRow(
                    title = "Batch reminder button",
                    subtitle = "Show reminder action and use each batch template.",
                    checked = AppSettings.batchReminderMessagesEnabled.value,
                    onCheckedChange = { isChecked ->
                        AppSettings.batchReminderMessagesEnabled.value = isChecked
                        AppSettings.save(context)
                    }
                )
            }

            SettingsGroup("Delivery") {
                SettingsSubheading("Send by")
                listOf(
                    MessageChannel.SMS to "SMS",
                    MessageChannel.WHATSAPP to "WhatsApp",
                    MessageChannel.BOTH to "Both"
                ).forEach { (channel, label) ->
                    RadioOption(
                        selected = AppSettings.messageChannel.value == channel,
                        label = label,
                        onClick = {
                            AppSettings.messageChannel.value = channel
                            AppSettings.save(context)
                        }
                    )
                }

                OptionDivider()

                SettingsSubheading("Sending mode")
                listOf(
                    MessageSendMode.COMPOSER to "Open composer",
                    MessageSendMode.AUTO to "Auto send SMS only"
                ).forEach { (sendMode, label) ->
                    RadioOption(
                        selected = AppSettings.messageSendMode.value == sendMode,
                        label = label,
                        onClick = {
                            AppSettings.messageSendMode.value = sendMode
                            AppSettings.save(context)
                        }
                    )
                }

                Text(
                    text = "Auto send needs SMS permission. WhatsApp opens with the message ready.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            SettingsGroup("Templates") {
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

                FilledTonalButton(
                    onClick = {
                        AppSettings.feePaidMessageTemplate.value =
                            "Hi, fee payment received for {studentName}. Amount paid: {amountPaid}. Pending due: {dueAmount}."
                        AppSettings.batchMessageTemplate.value =
                            "Hi {studentName}, your {batchName} batch is scheduled at {time}."
                        AppSettings.save(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset templates")
                }
            }
        }
        }

        if (selectedSettingsSection == SettingsSectionKey.DATA) {
            SettingsDetailPage(
            title = "Data",
            onBack = { selectedSettingsSection = null }
        ) {
            SettingsGroup("JSON backup") {
                Text(
                    text = "${StudentManager.students.size} students and ${BatchManager.batches.size} batches ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SettingsSubheading("Export data")
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_TEXT, buildEduTrackExport().toString())
                            putExtra(Intent.EXTRA_SUBJECT, "EduTrack export")
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Export data")
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export JSON")
                }

                OptionDivider()

                SettingsSubheading("Import data")
                Button(
                    onClick = { importLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import JSON")
                }

                Text(
                    text = "Import replaces current students and batches with the selected backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (importMessage.isNotBlank()) {
                    Text(
                        text = importMessage,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        }

        if (selectedSettingsSection == SettingsSectionKey.UPDATE) {
            SettingsDetailPage(
            title = "App Update",
            onBack = { selectedSettingsSection = null }
        ) {
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
                            val result = UpdateManager.checkForUpdate(context)
                            updateResult = result
                            isChecking = false
                        }
                    }
                },
                enabled = !isChecking
            ) {
                Text(if (isChecking) "Checking..." else "Check for update")
            }

            when (val result = updateResult) {
                is UpdateCheckResult.UpdateAvailable -> {
                    Text(
                        text = "Update available: ${result.release.tagName}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Button(
                        onClick = {
                            if (!isInstalling && activity != null) {
                                isInstalling = true
                                updateMessage = ""
                                scope.launch {
                                    val installResult = UpdateManager.installUpdate(
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
                Text(
                    text = updateMessage,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        }
    }
}

@Composable
private fun SettingsCategoryRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    HorizontalDivider()
}

@Composable
private fun SettingsDetailPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall
        )
    }

    HorizontalDivider()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        content()
    }
}

private enum class SettingsSectionKey {
    FEES,
    CLASSES,
    BATCHES,
    MESSAGES,
    DATA,
    UPDATE
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun RadioOption(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(label)
    }
}

@Composable
private fun SettingsSubheading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun OptionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassFeeEditor(
    entry: ClassFeeEntry,
    index: Int,
    expandedClassIndex: Int?,
    classOptions: List<String>,
    onExpandedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onClassSelected: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = expandedClassIndex == index,
                    onExpandedChange = onExpandedChange
                ) {
                    OutlinedTextField(
                        value = entry.className,
                        onValueChange = {},
                        label = { Text("Class") },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expandedClassIndex == index
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedClassIndex == index,
                        onDismissRequest = onDismiss
                    ) {
                        classOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { onClassSelected(option) }
                            )
                        }
                    }
                }

                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove"
                    )
                }
            }

            OutlinedTextField(
                value = entry.amountText,
                onValueChange = onAmountChange,
                label = { Text("Monthly fee") },
                prefix = { Text(AppSettings.currencySymbol.value) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun buildEduTrackExport(): JSONObject {
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
    return root
}

private fun importEduTrackData(
    context: android.content.Context,
    raw: String
): ImportSummary {
    if (raw.isBlank()) {
        error("Selected file is empty")
    }

    val trimmed = raw.trim()
    val root = if (trimmed.startsWith("[")) {
        JSONObject().put("students", JSONArray(trimmed))
    } else {
        JSONObject(trimmed)
    }

    val students = parseStudents(root.optJSONArray("students") ?: JSONArray())
    val batchesArray = root.optJSONArray("batches")
    val batches = parseBatches(batchesArray ?: JSONArray())

    StudentManager.replaceAll(context, students)
    if (batchesArray != null) {
        BatchManager.replaceAll(context, batches)
    } else {
        BatchManager.syncFromStudents(context)
    }

    return ImportSummary(
        studentCount = students.size,
        batchCount = if (batchesArray != null) batches.size else BatchManager.batches.size
    )
}

private fun parseStudents(array: JSONArray): List<Student> {
    val students = mutableListOf<Student>()
    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index) ?: continue
        val contactsArray = item.optJSONArray("contacts") ?: JSONArray()
        val contacts = mutableListOf<Contact>()
        for (contactIndex in 0 until contactsArray.length()) {
            val contactObject = contactsArray.optJSONObject(contactIndex) ?: continue
            contacts.add(
                Contact(
                    label = contactObject.optString("label"),
                    number = contactObject.optString("number")
                )
            )
        }

        students.add(
            Student(
                studentName = item.optString("studentName"),
                className = item.optString("className"),
                schoolName = item.optString("schoolName"),
                contacts = contacts,
                joinDateMillis = item.optLong("joinDateMillis"),
                lastFeePaidMillis = item.optionalLong("lastFeePaidMillis"),
                batchName = item.optionalString("batchName"),
                feeDueAmount = item.optionalDouble("feeDueAmount"),
                feeDueDateMillis = item.optionalLong("feeDueDateMillis"),
                advanceBalance = item.optionalDouble("advanceBalance")
            )
        )
    }
    return students
}

private fun parseBatches(array: JSONArray): List<Batch> {
    val batches = mutableListOf<Batch>()
    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index) ?: continue
        val name = item.optString("name")
        if (name.isNotBlank()) {
            batches.add(
                Batch(
                    name = name,
                    timeText = item.optString("timeText"),
                    messageTemplate = item.optString("messageTemplate")
                )
            )
        }
    }
    return batches
}

private fun JSONObject.optionalString(name: String): String? {
    if (!has(name) || isNull(name)) {
        return null
    }
    return optString(name).takeIf { value -> value.isNotBlank() }
}

private fun JSONObject.optionalLong(name: String): Long? {
    if (!has(name) || isNull(name)) {
        return null
    }
    return optLong(name, -1L).takeIf { value -> value >= 0L }
}

private fun JSONObject.optionalDouble(name: String): Double? {
    if (!has(name) || isNull(name)) {
        return null
    }
    return optDouble(name, Double.NaN).takeIf { value -> !value.isNaN() }
}

private data class ImportSummary(
    val studentCount: Int,
    val batchCount: Int
)
