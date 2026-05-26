package com.raju.edutrack.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raju.edutrack.AppSettings
import com.raju.edutrack.StudentManager
import com.raju.edutrack.formatDate
import com.raju.edutrack.isFeePending

@Composable
fun OverviewScreen(
    title: String
) {
    val students = StudentManager.students
    val pendingStudents = students
        .withIndex()
        .filter { entry ->
            isFeePending(
                student = entry.value,
                countFeeFromJoinDate =
                    AppSettings.countFeeFromJoinDate.value
            )
        }

    val data = when (title) {

        "Students" -> students.map { student ->

            OverviewItem(
                title = student.studentName,
                subtitle = student.className,
                meta = student.schoolName
            )

        }

        "Schools" -> students
            .map { it.schoolName }
            .filter { it.isNotBlank() }
            .distinct()
            .map { schoolName ->

                OverviewItem(
                    title = schoolName,
                    subtitle = "",
                    meta = ""
                )

            }

        "Batches" -> students
            .map { it.className }
            .filter { it.isNotBlank() }
            .distinct()
            .map { className ->

                OverviewItem(
                    title = className,
                    subtitle = "",
                    meta = ""
                )

            }

        "Pending" -> emptyList()

        else -> emptyList()

    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style =
                MaterialTheme.typography.headlineMedium
        )
        Spacer(
            modifier = Modifier.height(20.dp)
        )
        val showEmptyState =
            if (title == "Pending") {
                pendingStudents.isEmpty()
            } else {
                data.isEmpty()
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
                if (title == "Pending") {

                    items(pendingStudents) { entry ->

                        val index = entry.index
                        val student = entry.value

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement =
                                    Arrangement.spacedBy(12.dp)
                            ) {
                                Checkbox(
                                    checked = false,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            StudentManager.students[index] =
                                                student.copy(
                                                    lastFeePaidMillis =
                                                        System.currentTimeMillis()
                                                )
                                        }
                                    }
                                )

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = student.studentName,
                                        style =
                                            MaterialTheme.typography.titleMedium
                                    )

                                    Spacer(
                                        modifier = Modifier.height(4.dp)
                                    )

                                    Text(student.className)
                                    Text(student.schoolName)

                                    val lastPaid =
                                        student.lastFeePaidMillis
                                    if (lastPaid != null) {

                                        Spacer(
                                            modifier = Modifier.height(4.dp)
                                        )

                                        Text(
                                            text = "Last paid: ${formatDate(lastPaid)}",
                                            style =
                                                MaterialTheme.typography.bodySmall
                                        )

                                    }
                                }
                            }
                        }
                    }

                } else {

                    items(data) { item ->
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

private data class OverviewItem(
    val title: String,
    val subtitle: String,
    val meta: String
)