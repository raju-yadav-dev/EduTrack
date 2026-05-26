package com.raju.edutrack.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raju.edutrack.StudentManager

@Composable
fun OverviewScreen(
    title: String
) {
    val students = StudentManager.students

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

        "Pending" -> students
            .filter { it.contacts.isEmpty() }
            .map { student ->

                OverviewItem(
                    title = student.studentName,
                    subtitle = student.className,
                    meta = student.schoolName
                )

            }

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
        if (data.isEmpty()) {

            Text(
                text = "No data available",
                style = MaterialTheme.typography.bodyLarge
            )

        } else {

            LazyColumn(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
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

private data class OverviewItem(
    val title: String,
    val subtitle: String,
    val meta: String
)