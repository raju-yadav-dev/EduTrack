package com.raju.edutrack.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raju.edutrack.Contact
import com.raju.edutrack.Student
import com.raju.edutrack.StudentManager

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

    val students = StudentManager.students.filter {

        normalizedSearch.isEmpty() ||
            it.studentName.contains(
                normalizedSearch,
                ignoreCase = true
            ) ||
            it.className.contains(
                normalizedSearch,
                ignoreCase = true
            ) ||
            it.schoolName.contains(
                normalizedSearch,
                ignoreCase = true
            ) ||
            it.contacts.any { contactEntry ->
                contactEntry.number.contains(
                    normalizedSearch,
                    ignoreCase = true
                )
            }
    }

    if (showDialog) {

        AlertDialog(

            onDismissRequest = {

                showDialog = false

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

                        StudentManager.students.add(

                            Student(

                                studentName = studentName.trim(),

                                className = className.trim(),

                                schoolName = schoolName.trim(),

                                contacts = contacts

                            )
                        )

                        studentName = ""
                        className = ""
                        schoolName = ""
                        contactEntries.clear()
                        contactEntries.add(
                            ContactEntry(
                                label = "Primary",
                                number = ""
                            )
                        )

                        showDialog = false

                    }

                ) {

                    Text("Save")

                }
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        showDialog = false

                    }

                ) {

                    Text("Cancel")

                }
            },

            title = {

                Text("Add Student")

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

                    Text(
                        text = "Contacts",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    contactEntries.forEachIndexed { index, entry ->

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)

                        ) {

                            Column(

                                modifier = Modifier.weight(1f)

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

                                    modifier = Modifier.fillMaxWidth()

                                )

                            }

                            if (contactEntries.size > 1) {

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

                            } else {

                                Spacer(
                                    modifier = Modifier.size(48.dp)
                                )

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

        floatingActionButton = {

            FloatingActionButton(

                onClick = {

                    showDialog = true

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

                    items(students) { student ->

                        StudentGridCard(
                            studentName = student.studentName,
                            className = student.className,
                            schoolName = student.schoolName
                        )
                    }
                }

            } else {

                LazyColumn(

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)

                ) {

                    items(students) { student ->

                        StudentListCard(
                            studentName = student.studentName,
                            className = student.className,
                            schoolName = student.schoolName
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
    schoolName: String
) {

    ElevatedCard(

        modifier = Modifier.fillMaxWidth(),

        elevation =
            CardDefaults.elevatedCardElevation(
                defaultElevation = 8.dp
            )

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
        }
    }
}

@Composable
fun StudentGridCard(
    studentName: String,
    className: String,
    schoolName: String
) {

    ElevatedCard(

        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),

        elevation =
            CardDefaults.elevatedCardElevation(
                defaultElevation = 8.dp
            )

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
        }
    }
}

private data class ContactEntry(
    val label: String,
    val number: String
)
