package com.raju.edutrack.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raju.edutrack.Contact
import com.raju.edutrack.Student
import com.raju.edutrack.StudentManager
import com.raju.edutrack.formatDate
import com.raju.edutrack.parseDateOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen() {

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

    var joinDateText by remember {
        mutableStateOf(formatDate(System.currentTimeMillis()))
    }

    val contactEntries = remember {
        mutableStateListOf(ContactEntry(label = "Primary", number = ""))
    }

    val normalizedSearch = searchText.trim()

    val classOptions = listOf(
        "IV",
        "V",
        "VI",
        "VII",
        "VIII",
        "IX",
        "X",
        "XI",
        "XII",
        "1st Year",
        "2nd Year",
        "3rd Year",
        "4th Year"
    )

    fun resetForm() {
        studentName = ""
        className = ""
        schoolName = ""
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

                        if (showEditDialog) {

                            val index =
                                selectedStudentIndexes.firstOrNull()
                            if (index != null &&
                                index < StudentManager.students.size
                            ) {
                                val existing =
                                    StudentManager.students[index]
                                StudentManager.students[index] =
                                    existing.copy(
                                        studentName =
                                            studentName.trim(),
                                        className =
                                            className.trim(),
                                        schoolName =
                                            schoolName.trim(),
                                        contacts = contacts,
                                        joinDateMillis =
                                            joinDateMillis
                                    )
                            }

                        } else {

                            StudentManager.students.add(

                                Student(

                                    studentName =
                                        studentName.trim(),

                                    className =
                                        className.trim(),

                                    schoolName =
                                        schoolName.trim(),

                                    contacts = contacts,

                                    joinDateMillis =
                                        joinDateMillis,

                                    lastFeePaidMillis = null

                                )
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

                Column {

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
                                toRemove.forEach { index ->
                                    if (index <
                                        StudentManager.students.size
                                    ) {
                                        StudentManager.students
                                            .removeAt(index)
                                    }
                                }
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

                        StudentListCard(
                            studentName = student.studentName,
                            className = student.className,
                            schoolName = student.schoolName,
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
            }
        }
    }
}

@Composable
fun StudentListCard(
    studentName: String,
    className: String,
    schoolName: String,
    lastFeePaidMillis: Long?,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {

    ElevatedCard(

        modifier = modifier.fillMaxWidth(),

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
                modifier = Modifier.padding(20.dp)
            ) {

                Text(

                    text = studentName,

                    style =
                        MaterialTheme.typography.titleLarge,

                    fontWeight = FontWeight.Bold

                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(className)

                Text(schoolName)

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
    lastFeePaidMillis: Long?,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {

    ElevatedCard(

        modifier = modifier
            .fillMaxWidth()
            .height(170.dp),

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
