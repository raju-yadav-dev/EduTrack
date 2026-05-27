package com.raju.edutrack.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import com.raju.edutrack.AppSettings
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.raju.edutrack.BatchManager
import com.raju.edutrack.effectiveMonthsUnpaid
import com.raju.edutrack.StudentManager
import java.util.Calendar

@Composable
fun HomeScreen(navController: NavController) {

    val students = StudentManager.students

    val studentCount = students.size
    val schoolCount = students
        .map { it.schoolName }
        .distinct()
        .size

    val batchCount = BatchManager.batches.size

        val pendingCount = students.count { student ->
            val monthlyFee =
                student.feeDueAmount
                    ?: AppSettings.parseClassFeeAmount(student.className)
                    ?: AppSettings.parseDefaultFeeDueAmount()
            effectiveMonthsUnpaid(
                student = student,
                countFeeFromJoinDate =
                    AppSettings.countFeeFromJoinDate.value,
                monthlyFee = monthlyFee
            ) > 0
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        DashboardHeader()

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "Overview",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            OverviewCard(
                onClick = {
                    navController.navigate(
                        "overview/Students"
                    )
                },
                title = "Students",
                value = studentCount.toString(),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null
                    )
                },
                modifier = Modifier.weight(1f)
            )

            OverviewCard(
                onClick = {
                    navController.navigate(
                        "overview/Schools"
                    )
                },
                title = "Schools",
                value = schoolCount.toString(),
                icon = {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            OverviewCard(
                onClick = {
                    navController.navigate(
                        "overview/Batches"
                    )
                },
                title = "Batches",
                value = batchCount.toString(),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null
                    )
                },
                modifier = Modifier.weight(1f)
            )

            OverviewCard(
                onClick = {
                    navController.navigate(
                        "overview/Pending"
                    )
                },
                title = "Pending",
                value = pendingCount.toString(),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Text(
            text = "Recent Students",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        LazyRow(
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            items(students) { student ->

                ElevatedCard(
                    modifier = Modifier.width(220.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(15.dp)
                    ) {

                        Text(
                            text = student.studentName,
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(student.className)

                        Text(student.schoolName)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardHeader() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement =
                Arrangement.SpaceBetween
        ) {

            Column {
                val currentHour = Calendar
                    .getInstance()
                    .get(Calendar.HOUR_OF_DAY)

                val greeting = when {

                    currentHour < 12 -> "Good Morning 👋"

                    currentHour < 17 -> "Good Afternoon ☀️"

                    currentHour < 21 -> "Good Evening 👋"

                    else -> "Good Night 🌙"

                }

                Text(
                    text = greeting,
                    style =
                        MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.72f)
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "EduTrack Dashboard",
                    style =
                        MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }

            Text(
                text = "Manage Students Professionally",
                color = Color.White
            )
        }
    }
}

@Composable
fun OverviewCard(
    title: String,
    value: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        elevation =
            CardDefaults.elevatedCardElevation(
                defaultElevation = 10.dp
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {

            icon()

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = value,
                style =
                    MaterialTheme.typography.headlineSmall
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(text = title)
        }
    }
}
