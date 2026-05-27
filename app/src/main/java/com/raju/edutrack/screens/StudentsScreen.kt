package com.raju.edutrack.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import com.raju.edutrack.AppSettings
import com.raju.edutrack.AutoBatchMode
import com.raju.edutrack.BatchManager
import com.raju.edutrack.Contact
import com.raju.edutrack.Student
import com.raju.edutrack.StudentManager
import com.raju.edutrack.formatDate
import com.raju.edutrack.parseDateOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen() {

    val context = LocalContext.current

    var searchText by remember {
        mutableStateOf("")
    }

    val searchInteractionSource = remember {
        MutableInteractionSource()
    }

    val isSearchFocused by searchInteractionSource
        .collectIsFocusedAsState()

    var isGridView by remember {
        mutableStateOf(false)
    }

    var showDialog by remember {
        mutableStateOf(false)
    }

    var showEditDialog by remember {
        mutableStateOf(false)
    }

    val selectedStudentIndexes = remember {
        mutableStateListOf<Int>()
    }

    var studentName by remember {
        mutableStateOf("")
    }

    var className by remember {
        mutableStateOf("")
    }

    var isClassMenuExpanded by remember {
        mutableStateOf(false)
    }

    var schoolName by remember {
        mutableStateOf("")
    }

    var batchName by remember {
        mutableStateOf("")
    }

    var isBatchMenuExpanded by remember {
        mutableStateOf(false)
    }

    var joinDateText by remember {
        mutableStateOf(formatDate(System.currentTimeMillis()))
    }


    val contactEntries = remember {
        mutableStateListOf(ContactEntry(label = "Primary", number = ""))
    }

    val normalizedSearch = searchText.trim()

    val classOptions = AppSettings.getClassOptions()

    fun resetForm() {
        studentName = ""
        className = ""
        schoolName = ""
        batchName = ""
        joinDateText =
            formatDate(System.currentTimeMillis())
        contactEntries.clear()
        contactEntries.add(
            ContactEntry(
                label = "Primary",
                number = ""
            )
        )
    }

    fun loadForm(student: Student) {
        studentName = student.studentName
        className = student.className
        schoolName = student.schoolName
        batchName = student.batchName.orEmpty()
        joinDateText = formatDate(student.joinDateMillis)
        contactEntries.clear()
        if (student.contacts.isEmpty()) {
            contactEntries.add(
                ContactEntry(
                    label = "Primary",
                    number = ""
                )
            )
        } else {
            contactEntries.addAll(
                student.contacts.map { contact ->
                    ContactEntry(
                        label = contact.label,
                        number = contact.number
                    )
                }
            )
        }
    }

    val studentsWithIndex = StudentManager.students
        .withIndex()
        .filter { entry ->

        normalizedSearch.isEmpty() ||
            entry.value.studentName.contains(
                normalizedSearch,
                ignoreCase = true
            ) ||
            entry.value.className.contains(
                normalizedSearch,
                ignoreCase = true
            ) ||
            entry.value.schoolName.contains(
                normalizedSearch,
                ignoreCase = true
            ) ||
            entry.value.contacts.any { contactEntry ->
                contactEntry.number.contains(
                    normalizedSearch,
                    ignoreCase = true
                )
            }
    }

    if (showDialog || showEditDialog) {

        AlertDialog(

            onDismissRequest = {

                showDialog = false
                showEditDialog = false

            },

            confirmButton = {

                Button(

                    onClick = {

                        val contacts = contactEntries
                            .map { entry ->
                                Contact(
                                    label = entry.label.trim(),
                                    number = entry.number.trim()
                                )
                            }
                            .filter { entry ->
                                entry.number.isNotBlank()
                            }

                        val joinDateMillis =
                            parseDateOrNull(joinDateText)
                                ?: System.currentTimeMillis()

                        val normalizedClass = className.trim()
                        val normalizedSchool = schoolName.trim()
                        val manualBatch = batchName.trim()
                        val autoBatchName = when (
                            AppSettings.autoBatchMode.value
                        ) {
                            AutoBatchMode.CLASS -> normalizedClass
                            AutoBatchMode.CLASS_SCHOOL -> {
                                if (normalizedSchool.isNotBlank()) {
                                    "$normalizedClass - $normalizedSchool"
                                } else {
                                    normalizedClass
                                }
                            }
                            AutoBatchMode.NONE -> ""
                        }
                        val resolvedBatch = if (manualBatch.isNotBlank()) {
                            manualBatch
                        } else if (autoBatchName.isNotBlank()) {
                            autoBatchName
                        } else {
                            AppSettings.defaultBatchName.value.trim()
                        }

                        val effectiveDueAmount =
                            AppSettings.parseClassFeeAmount(normalizedClass)
                                ?: AppSettings.parseDefaultFeeDueAmount()

                        if (showEditDialog) {

                            val index =
                                selectedStudentIndexes.firstOrNull()
                            if (index != null &&
                                index < StudentManager.students.size
                            ) {
                                val existing =
                                    StudentManager.students[index]
                                StudentManager.updateStudent(
                                    context,
                                    index,
                                    existing.copy(
                                        studentName =
                                            studentName.trim(),
                                        className =
                                            normalizedClass,
                                        schoolName =
                                            normalizedSchool,
                                        contacts = contacts,
                                        joinDateMillis =
                                            joinDateMillis,
                                        batchName =
                                            resolvedBatch.ifBlank { null },
                                        feeDueAmount = effectiveDueAmount
                                    )
                                )
                                BatchManager.ensureBatch(
                                    context,
                                    resolvedBatch
                                )
                            }

                        } else {

                            val defaultDueDays =
                                AppSettings
                                    .parseDefaultFeeDueDays()
                            val defaultDueDateMillis =
                                if (defaultDueDays != null) {
                                    joinDateMillis +
                                        defaultDueDays *
                                        24L * 60L * 60L * 1000L
                                } else {
                                    null
                                }

                            StudentManager.addStudent(
                                context,
                                Student(

                                    studentName =
                                        studentName.trim(),

                                    className =
                                        normalizedClass,

                                    schoolName =
                                        normalizedSchool,

                                    contacts = contacts,

                                    joinDateMillis =
                                        joinDateMillis,

                                    lastFeePaidMillis = null,

                                    feeDueAmount =
                                        effectiveDueAmount,

                                    feeDueDateMillis =
                                        defaultDueDateMillis,

                                    batchName =
                                        resolvedBatch.ifBlank { null }

                                )
                            )
                            BatchManager.ensureBatch(
                                context,
                                resolvedBatch
                            )

                        }

                        resetForm()

                        showDialog = false
                        showEditDialog = false
                        selectedStudentIndexes.clear()

                    }

                ) {

                    Text(if (showEditDialog) "Update" else "Save")

                }
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        showDialog = false
                        showEditDialog = false
                        selectedStudentIndexes.clear()

                    }

                ) {

                    Text("Cancel")

                }
            },

            title = {

                Text(if (showEditDialog) "Edit Student" else "Add Student")

            },

            text = {

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                ) {

                    OutlinedTextField(

                        value = studentName,

                        onValueChange = {

                            studentName = it

                        },

                        label = {

                            Text("Student Name")

                        }

                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    ExposedDropdownMenuBox(

                        expanded = isBatchMenuExpanded,

                        onExpandedChange = { expanded ->

                            isBatchMenuExpanded = expanded

                        }

                    ) {

                        OutlinedTextField(

                            value = batchName,

                            onValueChange = { newValue ->

                                batchName = newValue

                            },

                            label = {

                                Text("Batch")

                            },

                            trailingIcon = {

                                ExposedDropdownMenuDefaults
                                    .TrailingIcon(
                                        expanded =
                                            isBatchMenuExpanded
                                    )

                            },

                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()

                        )

                        ExposedDropdownMenu(

                            expanded = isBatchMenuExpanded,

                            onDismissRequest = {

                                isBatchMenuExpanded = false

                            }

                        ) {

                            BatchManager.batches.forEach { batch ->

                                DropdownMenuItem(

                                    text = {

                                        Text(batch.name)

                                    },

                                    onClick = {

                                        batchName = batch.name
                                        isBatchMenuExpanded = false

                                    }

                                )

                            }

                        }

                    }

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    ExposedDropdownMenuBox(

                        expanded = isClassMenuExpanded,

                        onExpandedChange = { expanded ->

                            isClassMenuExpanded = expanded

                        }

                    ) {

                        OutlinedTextField(

                            value = className,

                            onValueChange = { newValue ->

                                className = newValue

                            },

                            label = {

                                Text("Class")

                            },

                            readOnly = true,

                            trailingIcon = {

                                ExposedDropdownMenuDefaults
                                    .TrailingIcon(
                                        expanded =
                                            isClassMenuExpanded
                                    )

                            },

                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()

                        )

                        ExposedDropdownMenu(

                            expanded = isClassMenuExpanded,

                            onDismissRequest = {

                                isClassMenuExpanded = false

                            }

                        ) {

                            classOptions.forEach { option ->

                                DropdownMenuItem(

                                    text = {

                                        Text(option)

                                    },

                                    onClick = {

                                        className = option
                                        isClassMenuExpanded = false

                                    }

                                )

                            }

                        }

                    }

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    OutlinedTextField(

                        value = schoolName,

                        onValueChange = {

                            schoolName = it

                        },

                        label = {

                            Text("School or College")

                        }

                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    OutlinedTextField(

                        value = joinDateText,

                        onValueChange = {

                            joinDateText = it

                        },

                        label = {

                            Text("Joining Date (dd-MM-yyyy)")

                        },

                        singleLine = true

                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Contacts",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    contactEntries.forEachIndexed { index, entry ->

                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            OutlinedTextField(

                                value = entry.label,

                                onValueChange = { newValue ->

                                    contactEntries[index] =
                                        entry.copy(label = newValue)

                                },

                                label = {

                                    Text("Label")

                                },

                                singleLine = true,

                                modifier = Modifier.fillMaxWidth()

                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            OutlinedTextField(

                                value = entry.number,

                                onValueChange = { newValue ->

                                    contactEntries[index] =
                                        entry.copy(number = newValue)

                                },

                                label = {

                                    Text("Number")

                                },

                                singleLine = true,

                                modifier = Modifier.fillMaxWidth()

                            )

                            if (contactEntries.size > 1) {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {

                                    IconButton(

                                        onClick = {

                                            contactEntries.removeAt(index)

                                        }

                                    ) {

                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null
                                        )

                                    }

                                }

                            }

                        }

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                    }

                    FilledTonalButton(

                        onClick = {

                            contactEntries.add(
                                ContactEntry(
                                    label = "",
                                    number = ""
                                )
                            )

                        }

                    ) {

                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text("Add Contact")

                    }
                }
            }
        )
    }

    Scaffold(

        topBar = {

            val selectedCount = selectedStudentIndexes.size
            if (selectedCount > 0) {

                TopAppBar(

                    title = {

                        Text("$selectedCount selected")

                    },

                    navigationIcon = {

                        IconButton(

                            onClick = {

                                selectedStudentIndexes.clear()

                            }

                        ) {

                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null
                            )

                        }

                    },

                    actions = {

                        if (selectedCount == 1) {

                            IconButton(

                                onClick = {

                                    val selectedIndex =
                                        selectedStudentIndexes.first()
                                    val student =
                                        StudentManager.students
                                            .getOrNull(selectedIndex)
                                    if (student != null) {
                                        loadForm(student)
                                        showEditDialog = true
                                    }
                                }

                            ) {

                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null
                                )

                            }

                        }

                        IconButton(

                            onClick = {

                                val toRemove =
                                    selectedStudentIndexes
                                        .toList()
                                        .sortedDescending()
                                StudentManager.removeStudents(
                                    context,
                                    toRemove
                                )
                                selectedStudentIndexes.clear()
                            }

                        ) {

                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null
                            )

                        }

                    }

                )

            }

        },

        floatingActionButton = {

            FloatingActionButton(

                onClick = {

                    resetForm()
                    showDialog = true
                    selectedStudentIndexes.clear()

                }

            ) {

                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            }
        }

    ) { paddingValues ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)

        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                OutlinedTextField(

                    value = searchText,

                    onValueChange = {

                        searchText = it

                    },

                    interactionSource = searchInteractionSource,

                    leadingIcon = {

                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )

                    },

                    placeholder = {

                        if (!isSearchFocused && searchText.isEmpty()) {

                            Text("Search for Students")

                        }

                    },

                    singleLine = true,

                    shape =
                        MaterialTheme.shapes.extraLarge,

                    modifier = Modifier.weight(1f)

                )

                FilledTonalIconButton(

                    onClick = {

                        isGridView = !isGridView

                    },

                    modifier = Modifier.size(56.dp)

                ) {

                    Icon(

                        imageVector = if (isGridView)
                            Icons.Default.List
                        else
                            Icons.Default.GridView,

                        contentDescription = null

                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            if (isGridView) {

                LazyVerticalGrid(

                    columns = GridCells.Fixed(2),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)

                ) {

                    items(
                        items = studentsWithIndex,
                        key = { entry -> entry.index }
                    ) { entry ->

                        val student = entry.value
                        val isSelected =
                            selectedStudentIndexes
                                .contains(entry.index)

                        StudentGridCard(
                            studentName = student.studentName,
                            className = student.className,
                            schoolName = student.schoolName,
                            batchName = student.batchName,
                            lastFeePaidMillis =
                                student.lastFeePaidMillis,
                            isSelected = isSelected,
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    if (selectedStudentIndexes.isNotEmpty()) {
                                        if (isSelected) {
                                            selectedStudentIndexes
                                                .remove(entry.index)
                                        } else {
                                            selectedStudentIndexes
                                                .add(entry.index)
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (isSelected) {
                                        selectedStudentIndexes
                                            .remove(entry.index)
                                    } else {
                                        selectedStudentIndexes
                                            .add(entry.index)
                                    }
                                }
                            )
                        )
                    }
                }

            } else {

                LazyColumn(

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)

                ) {

                    items(
                        items = studentsWithIndex,
                        key = { entry -> entry.index }
                    ) { entry ->

                        val student = entry.value
                        val isSelected =
                            selectedStudentIndexes
                                .contains(entry.index)
                        val primaryNumber =
                            student.contacts
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
                            lastFeePaidMillis =
                                student.lastFeePaidMillis,
                            feeDueAmount = student.feeDueAmount,
                            feeDueDateMillis = student.feeDueDateMillis,
                            isSelected = isSelected,
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    if (selectedStudentIndexes.isNotEmpty()) {
                                        if (isSelected) {
                                            selectedStudentIndexes
                                                .remove(entry.index)
                                        } else {
                                            selectedStudentIndexes
                                                .add(entry.index)
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (isSelected) {
                                        selectedStudentIndexes
                                            .remove(entry.index)
                                    } else {
                                        selectedStudentIndexes
                                            .add(entry.index)
                                    }
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentListCard(
    studentName: String,
    className: String,
    joinDateMillis: Long,
    schoolName: String,
    primaryNumber: String,
    batchName: String?,
    lastFeePaidMillis: Long?,
    feeDueAmount: Double?,
    feeDueDateMillis: Long?,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val sanitizedNumber = primaryNumber.trim()
    val batchLabel =
        batchName?.takeIf { it.isNotBlank() } ?: "-"

    ElevatedCard(

        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium
            ),

        elevation =
            CardDefaults.elevatedCardElevation(
                defaultElevation = 8.dp
            ),

        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )

    ) {

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = studentName,
                            style =
                                MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )
                    Text(
                        text = "Batch: $batchLabel",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "Joined: ${formatDate(joinDateMillis)}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = if (sanitizedNumber.isNotBlank()) {
                            "Number: $sanitizedNumber"
                        } else {
                            "Number: -"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(schoolName)

                    if (lastFeePaidMillis != null) {

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text = "Paid: ${formatDate(lastFeePaidMillis)}",
                            style = MaterialTheme.typography.bodySmall
                        )

                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = className,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 96.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    if (lastFeePaidMillis == null) {
                        Text(
                            text = "Due",
                            style = MaterialTheme.typography.labelMedium
                        )
                    } else {
                        Text(
                            text = "Paid",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {

                        IconButton(
                            enabled = sanitizedNumber.isNotBlank(),
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_DIAL,
                                    Uri.parse("tel:$sanitizedNumber")
                                )
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call"
                            )
                        }

                        IconButton(
                            enabled = sanitizedNumber.isNotBlank(),
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_SENDTO,
                                    Uri.parse("smsto:$sanitizedNumber")
                                )
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = "Message"
                            )
                        }

                        IconButton(
                            enabled = sanitizedNumber.isNotBlank(),
                            onClick = {
                                val url = "https://wa.me/${sanitizedNumber}"
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(url)
                                )
                                context.startActivity(intent)
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

            if (isSelected) {

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )

            }
        }
    }
}

@Composable
fun StudentGridCard(
    studentName: String,
    className: String,
    schoolName: String,
    batchName: String?,
    lastFeePaidMillis: Long?,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {

    ElevatedCard(

        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium
            ),

        elevation =
            CardDefaults.elevatedCardElevation(
                defaultElevation = 8.dp
            ),

        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )

    ) {

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(

                    text = studentName,

                    style =
                        MaterialTheme.typography.titleMedium,

                    fontWeight = FontWeight.Bold

                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = className,
                    style =
                        MaterialTheme.typography.bodyMedium
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = schoolName,
                    style =
                        MaterialTheme.typography.bodySmall
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )
                val batchLabel =
                    batchName?.takeIf { it.isNotBlank() } ?: "-"
                Text(
                    text = "Batch: $batchLabel",
                    style = MaterialTheme.typography.bodySmall
                )

                if (batchName?.isNotBlank() == true) {
                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )
                    Text(
                        text = "Batch: $batchName",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (lastFeePaidMillis != null) {

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "Last paid: ${formatDate(lastFeePaidMillis)}",
                        style = MaterialTheme.typography.bodySmall
                    )

                }
            }

            if (isSelected) {

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                )

            }
        }
    }
}

private data class ContactEntry(
    val label: String,
    val number: String
)
