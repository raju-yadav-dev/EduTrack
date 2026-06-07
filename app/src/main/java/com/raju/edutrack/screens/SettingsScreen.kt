package com.raju.edutrack.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raju.edutrack.AppSettings
import com.raju.edutrack.AutoBatchMode
import com.raju.edutrack.Batch
import com.raju.edutrack.BatchManager
import com.raju.edutrack.ClassFeeEntry
import com.raju.edutrack.Contact
import com.raju.edutrack.MessageChannel
import com.raju.edutrack.MessageSendMode
import com.raju.edutrack.PaymentHistoryEntry
import com.raju.edutrack.PaymentHistoryManager
import com.raju.edutrack.Student
import com.raju.edutrack.StudentManager
import com.raju.edutrack.formatDate
import com.raju.edutrack.update.UpdateCheckResult
import com.raju.edutrack.update.UpdateConfig
import com.raju.edutrack.update.UpdateInstallResult
import com.raju.edutrack.update.UpdateManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
    var paymentHistoryToDelete by remember { mutableStateOf<PaymentHistoryEntry?>(null) }
    var paymentHistorySearchQuery by remember { mutableStateOf("") }
    var paymentHistoryFilter by remember { mutableStateOf(PaymentHistoryFilter.ALL_TIME) }
    var paymentHistoryRefreshToken by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        PaymentHistoryManager.reload(context)
    }

    LaunchedEffect(selectedSettingsSection, paymentHistoryRefreshToken) {
        if (selectedSettingsSection == SettingsSectionKey.PAYMENT_HISTORY) {
            PaymentHistoryManager.reload(context)
        }
    }

    LaunchedEffect(selectedSettingsSection) {
        scrollState.scrollTo(0)
    }

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
                "Imported ${summary.studentCount} students, ${summary.batchCount} batches and ${summary.paymentHistoryCount} payments"
            },
            onFailure = { error ->
                error.message ?: "Could not import data"
            }
        )
        Toast.makeText(context, importMessage, Toast.LENGTH_LONG).show()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        val result = runCatching {
            context.contentResolver.openOutputStream(uri)
                ?.bufferedWriter()
                ?.use { writer ->
                    writer.write(buildEduTrackExport().toString(2))
                } ?: error("Could not open export file")
        }
        val message = result.fold(
            onSuccess = { "EduTrack backup exported" },
            onFailure = { error ->
                error.message ?: "Could not export backup"
            }
        )
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    BackHandler(enabled = selectedSettingsSection != null) {
        selectedSettingsSection = null
    }

    when (selectedSettingsSection) {
        null -> SettingsHomePage(
            onOpenFees = { selectedSettingsSection = SettingsSectionKey.FEES },
            onOpenPaymentHistory = { selectedSettingsSection = SettingsSectionKey.PAYMENT_HISTORY },
            onOpenClasses = { selectedSettingsSection = SettingsSectionKey.CLASSES },
            onOpenBatches = { selectedSettingsSection = SettingsSectionKey.BATCHES },
            onOpenMessages = { selectedSettingsSection = SettingsSectionKey.MESSAGES },
            onOpenData = { selectedSettingsSection = SettingsSectionKey.DATA },
            onOpenUpdate = { selectedSettingsSection = SettingsSectionKey.UPDATE }
        )

        SettingsSectionKey.FEES -> SettingsFeesPage(
            context = context,
            expandedClassIndex = expandedClassIndex,
            onBack = { selectedSettingsSection = null },
            onExpandedClassIndexChange = { expandedClassIndex = it }
        )

        SettingsSectionKey.PAYMENT_HISTORY -> SettingsPaymentHistoryPage(
            context = context,
            paymentHistorySearchQuery = paymentHistorySearchQuery,
            onPaymentHistorySearchQueryChange = { paymentHistorySearchQuery = it },
            paymentHistoryFilter = paymentHistoryFilter,
            onPaymentHistoryFilterChange = { paymentHistoryFilter = it },
            onBack = {
                selectedSettingsSection = null
                paymentHistorySearchQuery = ""
                paymentHistoryFilter = PaymentHistoryFilter.ALL_TIME
            },
            onDeleteRequested = { paymentHistoryToDelete = it },
            onRefreshRequested = { paymentHistoryRefreshToken++ }
        )

        SettingsSectionKey.CLASSES -> SettingsClassesPage(
            context = context,
            newClassOption = newClassOption,
            onNewClassOptionChange = { newClassOption = it },
            onBack = { selectedSettingsSection = null }
        )

        SettingsSectionKey.BATCHES -> SettingsBatchesPage(
            context = context,
            onBack = { selectedSettingsSection = null }
        )

        SettingsSectionKey.MESSAGES -> SettingsMessagesPage(
            context = context,
            onBack = { selectedSettingsSection = null }
        )

        SettingsSectionKey.DATA -> SettingsDataPage(
            context = context,
            importLauncher = importLauncher,
            exportLauncher = exportLauncher,
            importMessage = importMessage,
            onBack = { selectedSettingsSection = null }
        )

        SettingsSectionKey.UPDATE -> SettingsUpdatePage(
            context = context,
            activity = activity,
            isChecking = isChecking,
            isInstalling = isInstalling,
            updateResult = updateResult,
            updateMessage = updateMessage,
            onBack = { selectedSettingsSection = null },
            onCheckRequested = {
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
            onInstallRequested = { releaseUrl ->
                if (!isInstalling && activity != null) {
                    isInstalling = true
                    updateMessage = ""
                    scope.launch {
                        val installResult = UpdateManager.installUpdate(
                            activity,
                            releaseUrl
                        )
                        updateMessage = when (installResult) {
                            UpdateInstallResult.Started -> "Installer opened"
                            UpdateInstallResult.NeedsPermission -> "Allow install permission"
                            is UpdateInstallResult.Failed -> installResult.message
                        }
                        isInstalling = false
                    }
                } else if (activity == null) {
                    updateMessage = "No activity context"
                }
            }
        )
    }

    paymentHistoryToDelete?.let { entry ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { paymentHistoryToDelete = null },
            confirmButton = {
                Button(
                    onClick = {
                        PaymentHistoryManager.deleteAndReverse(context, entry)
                        paymentHistoryToDelete = null
                        paymentHistoryRefreshToken++
                    }
                ) {
                    Text("Delete and reverse")
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentHistoryToDelete = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Reverse payment?") },
            text = {
                Text(
                    "This removes the payment entry and restores ${entry.studentName}'s previous fee state."
                )
            }
        )
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

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        content()
    }
}

@Composable
private fun SettingsHomePage(
    onOpenFees: () -> Unit,
    onOpenPaymentHistory: () -> Unit,
    onOpenClasses: () -> Unit,
    onOpenBatches: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenData: () -> Unit,
    onOpenUpdate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        SettingsCategoryRow(
            icon = Icons.Default.Payments,
            title = "Fees",
            subtitle = "Due count, currency, class fee defaults",
            onClick = onOpenFees
        )

        SettingsCategoryRow(
            icon = Icons.Default.Payments,
            title = "Payment History",
            subtitle = "View payments and long press to reverse",
            onClick = onOpenPaymentHistory
        )

        SettingsCategoryRow(
            icon = Icons.Default.School,
            title = "Classes",
            subtitle = "Manage extra class options",
            onClick = onOpenClasses
        )

        SettingsCategoryRow(
            icon = Icons.Default.Group,
            title = "Batches",
            subtitle = "Auto add students to batches",
            onClick = onOpenBatches
        )

        SettingsCategoryRow(
            icon = Icons.AutoMirrored.Filled.Message,
            title = "Messages",
            subtitle = "Fee receipts, batch reminders, send mode",
            onClick = onOpenMessages
        )

        SettingsCategoryRow(
            icon = Icons.Default.Storage,
            title = "Data",
            subtitle = "Export and import JSON backups",
            onClick = onOpenData
        )

        SettingsCategoryRow(
            icon = Icons.Default.CloudDownload,
            title = "App Update",
            subtitle = "Check and install new releases",
            onClick = onOpenUpdate
        )
    }
}

@Composable
private fun SettingsFeesPage(
    context: android.content.Context,
    expandedClassIndex: Int?,
    onBack: () -> Unit,
    onExpandedClassIndexChange: (Int?) -> Unit
) {
    val pageScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(pageScrollState)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SettingsDetailPage(
            title = "Fees",
            onBack = onBack
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
                                onExpandedClassIndexChange(if (expanded) index else null)
                            },
                            onDismiss = { onExpandedClassIndexChange(null) },
                            onClassSelected = { option ->
                                AppSettings.classFeeEntries[index] =
                                    entry.copy(className = option)
                                AppSettings.save(context)
                                onExpandedClassIndexChange(null)
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
}

@Composable
private fun SettingsPaymentHistoryPage(
    context: android.content.Context,
    paymentHistorySearchQuery: String,
    onPaymentHistorySearchQueryChange: (String) -> Unit,
    paymentHistoryFilter: PaymentHistoryFilter,
    onPaymentHistoryFilterChange: (PaymentHistoryFilter) -> Unit,
    onBack: () -> Unit,
    onDeleteRequested: (PaymentHistoryEntry) -> Unit,
    onRefreshRequested: () -> Unit
) {
    LaunchedEffect(Unit) {
        PaymentHistoryManager.reload(context)
    }

    val pageScrollState = rememberScrollState()
    val allHistory by remember {
        derivedStateOf { PaymentHistoryManager.history.toList() }
    }
    val nowMillis = System.currentTimeMillis()
    val filteredHistory = allHistory
        .asSequence()
        .filter { entry -> matchesPaymentHistoryQuery(entry, paymentHistorySearchQuery) }
        .filter { entry -> matchesPaymentHistoryFilter(entry, paymentHistoryFilter, nowMillis) }
        .sortedByDescending { entry -> entry.paidAtMillis }
        .toList()
    val stats = paymentHistoryStats(allHistory, nowMillis)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(pageScrollState)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SettingsDetailPage(
            title = "Payment History",
            onBack = onBack
        ) {
            Text(
                text = "${allHistory.size} payment${if (allHistory.size == 1) "" else "s"} loaded",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentHistoryStatCard(
                    title = "Recorded",
                    value = stats.totalPayments.toString(),
                    modifier = Modifier.weight(1f)
                )
                PaymentHistoryStatCard(
                    title = "Collected",
                    value = "${AppSettings.currencySymbol.value}${"%.2f".format(stats.totalAmountCollected)}",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentHistoryStatCard(
                    title = "Today",
                    value = "${AppSettings.currencySymbol.value}${"%.2f".format(stats.amountCollectedToday)}",
                    modifier = Modifier.weight(1f)
                )
                PaymentHistoryStatCard(
                    title = "This month",
                    value = "${AppSettings.currencySymbol.value}${"%.2f".format(stats.amountCollectedThisMonth)}",
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = paymentHistorySearchQuery,
                onValueChange = onPaymentHistorySearchQueryChange,
                label = { Text("Search payments") },
                placeholder = { Text("Student, class, batch or phone") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentHistoryFilterChip(
                        label = "Today",
                        selected = paymentHistoryFilter == PaymentHistoryFilter.TODAY,
                        onClick = { onPaymentHistoryFilterChange(PaymentHistoryFilter.TODAY) },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentHistoryFilterChip(
                        label = "This week",
                        selected = paymentHistoryFilter == PaymentHistoryFilter.THIS_WEEK,
                        onClick = { onPaymentHistoryFilterChange(PaymentHistoryFilter.THIS_WEEK) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentHistoryFilterChip(
                        label = "This month",
                        selected = paymentHistoryFilter == PaymentHistoryFilter.THIS_MONTH,
                        onClick = { onPaymentHistoryFilterChange(PaymentHistoryFilter.THIS_MONTH) },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentHistoryFilterChip(
                        label = "All time",
                        selected = paymentHistoryFilter == PaymentHistoryFilter.ALL_TIME,
                        onClick = { onPaymentHistoryFilterChange(PaymentHistoryFilter.ALL_TIME) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (filteredHistory.isEmpty()) {
                Text(
                    text = if (allHistory.isEmpty()) "No payment history yet" else "No payments match this search or filter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val groupedHistory = filteredHistory
                    .groupBy { entry -> startOfDayMillis(entry.paidAtMillis) }
                    .toSortedMap(compareByDescending { value -> value })

                groupedHistory.forEach { (dayStartMillis, entries) ->
                    Text(
                        text = formatDate(dayStartMillis),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Text(
                        text = "${entries.size} payment${if (entries.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    entries.forEach { entry ->
                        PaymentHistoryCard(
                            entry = entry,
                            onLongPressDelete = {
                                onDeleteRequested(entry)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsClassesPage(
    context: android.content.Context,
    newClassOption: String,
    onNewClassOptionChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val pageScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(pageScrollState)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SettingsDetailPage(
            title = "Classes",
            onBack = onBack
        ) {
            SettingsGroup("Add class") {
                OutlinedTextField(
                    value = newClassOption,
                    onValueChange = onNewClassOptionChange,
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
                            onNewClassOptionChange("")
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
                            OutlinedIconButton(
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
}

@Composable
private fun SettingsBatchesPage(
    context: android.content.Context,
    onBack: () -> Unit
) {
    val pageScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(pageScrollState)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SettingsDetailPage(
            title = "Batches",
            onBack = onBack
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
}

@Composable
private fun SettingsMessagesPage(
    context: android.content.Context,
    onBack: () -> Unit
) {
    val pageScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(pageScrollState)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SettingsDetailPage(
            title = "Messages",
            onBack = onBack
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
}

@Composable
private fun SettingsDataPage(
    context: android.content.Context,
    importLauncher: androidx.activity.compose.ManagedActivityResultLauncher<String, android.net.Uri?>,
    exportLauncher: androidx.activity.compose.ManagedActivityResultLauncher<String, android.net.Uri?>,
    importMessage: String,
    onBack: () -> Unit
) {
    val pageScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(pageScrollState)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SettingsDetailPage(
            title = "Data",
            onBack = onBack
        ) {
            SettingsGroup("JSON backup") {
                Text(
                    text = "${StudentManager.students.size} students, ${BatchManager.batches.size} batches and ${PaymentHistoryManager.history.size} payments ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SettingsSubheading("Export data")
                Button(
                    onClick = {
                        exportLauncher.launch("edutrack-backup.json")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export backup file")
                }

                OptionDivider()

                SettingsSubheading("Import data")
                Button(
                    onClick = { importLauncher.launch("application/json") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import backup file")
                }

                Text(
                    text = "Import replaces current app data with the selected EduTrack backup.",
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
}

@Composable
private fun SettingsUpdatePage(
    context: android.content.Context,
    activity: Activity?,
    isChecking: Boolean,
    isInstalling: Boolean,
    updateResult: UpdateCheckResult?,
    updateMessage: String,
    onBack: () -> Unit,
    onCheckRequested: () -> Unit,
    onInstallRequested: (String) -> Unit
) {
    val pageScrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(pageScrollState)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SettingsDetailPage(
            title = "App Update",
            onBack = onBack
        ) {
            if (!UpdateConfig.isConfigured) {
                Text(
                    text = "Set UpdateConfig.owner and UpdateConfig.repo",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = onCheckRequested,
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
                        onClick = { onInstallRequested(result.release.downloadUrl) },
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


private enum class PaymentHistoryFilter(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This week"),
    THIS_MONTH("This month"),
    ALL_TIME("All time")
}

private data class PaymentHistoryStats(
    val totalPayments: Int,
    val totalAmountCollected: Double,
    val amountCollectedToday: Double,
    val amountCollectedThisMonth: Double
)

@Composable
private fun PaymentHistoryStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PaymentHistoryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(label)
        }
    }
}

private fun paymentHistoryStats(
    history: List<PaymentHistoryEntry>,
    nowMillis: Long
): PaymentHistoryStats {
    val startOfToday = startOfDayMillis(nowMillis)
    val startOfMonth = startOfMonthMillis(nowMillis)
    return PaymentHistoryStats(
        totalPayments = history.size,
        totalAmountCollected = history.sumOf { entry -> entry.amountPaid },
        amountCollectedToday = history
            .filter { entry -> entry.paidAtMillis >= startOfToday }
            .sumOf { entry -> entry.amountPaid },
        amountCollectedThisMonth = history
            .filter { entry -> entry.paidAtMillis >= startOfMonth }
            .sumOf { entry -> entry.amountPaid }
    )
}

private fun matchesPaymentHistoryQuery(
    entry: PaymentHistoryEntry,
    query: String
): Boolean {
    val normalizedQuery = query.trim().lowercase()
    if (normalizedQuery.isBlank()) {
        return true
    }
    val searchText = listOf(
        entry.studentName,
        entry.className,
        entry.schoolName,
        entry.batchName.orEmpty(),
        entry.mainNumber.orEmpty(),
        entry.amountPaid.toString(),
        entry.remainingDue?.toString().orEmpty()
    ).joinToString(separator = " ") { value -> value.lowercase() }
    return searchText.contains(normalizedQuery)
}

private fun matchesPaymentHistoryFilter(
    entry: PaymentHistoryEntry,
    filter: PaymentHistoryFilter,
    nowMillis: Long
): Boolean {
    val paidAtMillis = entry.paidAtMillis
    return when (filter) {
        PaymentHistoryFilter.TODAY -> paidAtMillis >= startOfDayMillis(nowMillis)
        PaymentHistoryFilter.THIS_WEEK -> paidAtMillis >= startOfWeekMillis(nowMillis)
        PaymentHistoryFilter.THIS_MONTH -> paidAtMillis >= startOfMonthMillis(nowMillis)
        PaymentHistoryFilter.ALL_TIME -> true
    }
}

private fun startOfDayMillis(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun startOfWeekMillis(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun startOfMonthMillis(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatPaymentDateTime(millis: Long): String {
    return java.text.SimpleDateFormat(
        "dd MMM yyyy, h:mm a",
        Locale.getDefault()
    ).format(java.util.Date(millis))
}

private enum class SettingsSectionKey {
    FEES,
    PAYMENT_HISTORY,
    CLASSES,
    BATCHES,
    MESSAGES,
    DATA,
    UPDATE
}

@Composable
private fun PaymentHistoryCard(
    entry: PaymentHistoryEntry,
    onLongPressDelete: () -> Unit
) {
    val currency = AppSettings.currencySymbol.value
    val paymentTime = formatPaymentDateTime(entry.paidAtMillis)
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPressDelete
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.studentName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedIconButton(onClick = onLongPressDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete and reverse payment"
                    )
                }
            }
            Text(
                text = "Class: ${entry.className} • School: ${entry.schoolName}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = listOf(
                    entry.batchName?.takeIf { it.isNotBlank() }?.let { "Batch: $it" },
                    entry.mainNumber?.takeIf { it.isNotBlank() }?.let { "Main: $it" }
                ).filterNotNull().joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Paid: $currency${"%.2f".format(entry.amountPaid)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            entry.remainingDue?.let { due ->
                Text(
                    text = "Remaining due: $currency${"%.2f".format(due)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Paid on $paymentTime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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

                OutlinedIconButton(onClick = onRemove) {
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
    root.put("schema", "edutrack.backup")
    root.put("version", 2)
    root.put("settings", buildSettingsExport())
    val studentsArray = JSONArray()
    StudentManager.students.forEach { student ->
        val item = JSONObject()
        item.put("id", student.id)
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

    val paymentHistoryArray = JSONArray()
    PaymentHistoryManager.history.forEach { entry ->
        val item = JSONObject()
        item.put("id", entry.id)
        item.put("studentId", entry.studentId)
        item.put("studentName", entry.studentName)
        item.put("className", entry.className)
        item.put("schoolName", entry.schoolName)
        entry.batchName?.let { value -> item.put("batchName", value) }
        entry.mainNumber?.let { value -> item.put("mainNumber", value) }
        item.put("amountPaid", entry.amountPaid)
        entry.remainingDue?.let { value -> item.put("remainingDue", value) }
        item.put("paidAtMillis", entry.paidAtMillis)
        entry.beforeLastFeePaidMillis?.let { value -> item.put("beforeLastFeePaidMillis", value) }
        entry.beforeFeeDueDateMillis?.let { value -> item.put("beforeFeeDueDateMillis", value) }
        entry.beforeAdvanceBalance?.let { value -> item.put("beforeAdvanceBalance", value) }
        entry.afterLastFeePaidMillis?.let { value -> item.put("afterLastFeePaidMillis", value) }
        entry.afterFeeDueDateMillis?.let { value -> item.put("afterFeeDueDateMillis", value) }
        entry.afterAdvanceBalance?.let { value -> item.put("afterAdvanceBalance", value) }
        paymentHistoryArray.put(item)
    }

    root.put("students", studentsArray)
    root.put("batches", batchesArray)
    root.put("paymentHistory", paymentHistoryArray)
    return root
}

private fun buildSettingsExport(): JSONObject {
    val settings = JSONObject()
    settings.put("countFeeFromJoinDate", AppSettings.countFeeFromJoinDate.value)
    settings.put("defaultFeeDueAmountText", AppSettings.defaultFeeDueAmountText.value)
    settings.put("defaultFeeDueDaysText", AppSettings.defaultFeeDueDaysText.value)
    settings.put("currencySymbol", AppSettings.currencySymbol.value)
    settings.put("autoBatchMode", AppSettings.autoBatchMode.value.name)
    settings.put("defaultBatchName", AppSettings.defaultBatchName.value)
    settings.put("autoAdvanceFeeDueDate", AppSettings.autoAdvanceFeeDueDate.value)
    settings.put("autoClassFeesEnabled", AppSettings.autoClassFeesEnabled.value)
    settings.put("confirmFullFeePaid", AppSettings.confirmFullFeePaid.value)
    settings.put("feePaymentMessagesEnabled", AppSettings.feePaymentMessagesEnabled.value)
    settings.put("batchReminderMessagesEnabled", AppSettings.batchReminderMessagesEnabled.value)
    settings.put("messageChannel", AppSettings.messageChannel.value.name)
    settings.put("messageSendMode", AppSettings.messageSendMode.value.name)
    settings.put("feePaidMessageTemplate", AppSettings.feePaidMessageTemplate.value)
    settings.put("batchMessageTemplate", AppSettings.batchMessageTemplate.value)

    val classFees = JSONArray()
    AppSettings.classFeeEntries.forEach { entry ->
        classFees.put(
            JSONObject()
                .put("className", entry.className)
                .put("amountText", entry.amountText)
        )
    }
    settings.put("classFeeEntries", classFees)

    val customClasses = JSONArray()
    AppSettings.customClassOptions.forEach { option ->
        customClasses.put(option)
    }
    settings.put("customClassOptions", customClasses)
    return settings
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
    val paymentHistoryArray = root.optJSONArray("paymentHistory")
    val paymentHistory = parsePaymentHistory(paymentHistoryArray ?: JSONArray())
    root.optJSONObject("settings")?.let { settings ->
        applyImportedSettings(context, settings)
    }

    StudentManager.replaceAll(context, students)
    if (batchesArray != null) {
        BatchManager.replaceAll(context, batches)
    } else {
        BatchManager.syncFromStudents(context)
    }
    PaymentHistoryManager.replaceAll(context, paymentHistory)

    return ImportSummary(
        studentCount = students.size,
        batchCount = if (batchesArray != null) batches.size else BatchManager.batches.size,
        paymentHistoryCount = paymentHistory.size
    )
}

private fun applyImportedSettings(
    context: android.content.Context,
    settings: JSONObject
) {
    AppSettings.countFeeFromJoinDate.value =
        settings.optBoolean("countFeeFromJoinDate", AppSettings.countFeeFromJoinDate.value)
    AppSettings.defaultFeeDueAmountText.value =
        settings.optString("defaultFeeDueAmountText", AppSettings.defaultFeeDueAmountText.value)
    AppSettings.defaultFeeDueDaysText.value =
        settings.optString("defaultFeeDueDaysText", AppSettings.defaultFeeDueDaysText.value)
    AppSettings.currencySymbol.value =
        settings.optString("currencySymbol", AppSettings.currencySymbol.value)
    AppSettings.defaultBatchName.value =
        settings.optString("defaultBatchName", AppSettings.defaultBatchName.value)
    AppSettings.autoAdvanceFeeDueDate.value =
        settings.optBoolean("autoAdvanceFeeDueDate", AppSettings.autoAdvanceFeeDueDate.value)
    AppSettings.autoClassFeesEnabled.value =
        settings.optBoolean("autoClassFeesEnabled", AppSettings.autoClassFeesEnabled.value)
    AppSettings.confirmFullFeePaid.value =
        settings.optBoolean("confirmFullFeePaid", AppSettings.confirmFullFeePaid.value)
    AppSettings.feePaymentMessagesEnabled.value =
        settings.optBoolean("feePaymentMessagesEnabled", AppSettings.feePaymentMessagesEnabled.value)
    AppSettings.batchReminderMessagesEnabled.value =
        settings.optBoolean("batchReminderMessagesEnabled", AppSettings.batchReminderMessagesEnabled.value)
    AppSettings.feePaidMessageTemplate.value =
        settings.optString("feePaidMessageTemplate", AppSettings.feePaidMessageTemplate.value)
    AppSettings.batchMessageTemplate.value =
        settings.optString("batchMessageTemplate", AppSettings.batchMessageTemplate.value)

    settings.optionalString("autoBatchMode")?.let { value ->
        AppSettings.autoBatchMode.value =
            AutoBatchMode.values()
                .firstOrNull { mode -> mode.name == value }
                ?: AppSettings.autoBatchMode.value
    }
    settings.optionalString("messageChannel")?.let { value ->
        AppSettings.messageChannel.value =
            MessageChannel.values()
                .firstOrNull { channel -> channel.name == value }
                ?: AppSettings.messageChannel.value
    }
    settings.optionalString("messageSendMode")?.let { value ->
        AppSettings.messageSendMode.value =
            MessageSendMode.values()
                .firstOrNull { mode -> mode.name == value }
                ?: AppSettings.messageSendMode.value
    }

    settings.optJSONArray("classFeeEntries")?.let { array ->
        AppSettings.classFeeEntries.clear()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            AppSettings.classFeeEntries.add(
                ClassFeeEntry(
                    className = item.optString("className"),
                    amountText = item.optString("amountText")
                )
            )
        }
    }

    settings.optJSONArray("customClassOptions")?.let { array ->
        AppSettings.customClassOptions.clear()
        for (index in 0 until array.length()) {
            val option = array.optString(index)
            if (option.isNotBlank()) {
                AppSettings.customClassOptions.add(option)
            }
        }
    }

    AppSettings.save(context)
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
                advanceBalance = item.optionalDouble("advanceBalance"),
                id = item.optionalString("id")
                    ?: legacyImportedStudentId(item, index)
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

private fun parsePaymentHistory(array: JSONArray): List<PaymentHistoryEntry> {
    val history = mutableListOf<PaymentHistoryEntry>()
    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index) ?: continue
        val paidAtMillis = item.optLong("paidAtMillis")
        history.add(
            PaymentHistoryEntry(
                id = item.optionalString("id")
                    ?: "imported-payment-$index-$paidAtMillis",
                studentId = item.optString("studentId"),
                studentName = item.optString("studentName"),
                className = item.optString("className"),
                schoolName = item.optString("schoolName"),
                batchName = item.optionalString("batchName"),
                mainNumber = item.optionalString("mainNumber"),
                amountPaid = item.optDouble("amountPaid", 0.0),
                remainingDue = item.optionalDouble("remainingDue"),
                paidAtMillis = paidAtMillis,
                beforeLastFeePaidMillis = item.optionalLong("beforeLastFeePaidMillis"),
                beforeFeeDueDateMillis = item.optionalLong("beforeFeeDueDateMillis"),
                beforeAdvanceBalance = item.optionalDouble("beforeAdvanceBalance"),
                afterLastFeePaidMillis = item.optionalLong("afterLastFeePaidMillis"),
                afterFeeDueDateMillis = item.optionalLong("afterFeeDueDateMillis"),
                afterAdvanceBalance = item.optionalDouble("afterAdvanceBalance")
            )
        )
    }
    return history
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

private fun legacyImportedStudentId(
    item: JSONObject,
    index: Int
): String {
    val seed = listOf(
        item.optString("studentName"),
        item.optString("className"),
        item.optString("schoolName"),
        item.optLong("joinDateMillis", 0L).toString(),
        index.toString()
    ).joinToString(separator = "|")
    return seed.hashCode().toUInt().toString(radix = 16)
}

private data class ImportSummary(
    val studentCount: Int,
    val batchCount: Int,
    val paymentHistoryCount: Int
)
