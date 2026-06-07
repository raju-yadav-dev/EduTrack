package com.raju.edutrack.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import com.raju.edutrack.AppSettings
import com.raju.edutrack.BottomNavItem
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.raju.edutrack.BatchManager
import com.raju.edutrack.effectiveMonthsUnpaid
import com.raju.edutrack.StudentManager
import java.util.Calendar

@Composable
fun HomeScreen(
    navController: NavController,
    onOpenStudents: () -> Unit = {
        navController.navigate(BottomNavItem.Students.route)
    },
    onOpenBatches: () -> Unit = {
        navController.navigate(BottomNavItem.Batches.route)
    }
) {

    val students = StudentManager.students

    val dashboardStats by remember {
        derivedStateOf {
            val studentCount = students.size
            val schoolCount = students
                .map { student -> student.schoolName }
                .filter { school -> school.isNotBlank() }
                .distinctBy { school -> school.lowercase() }
                .size
            val batchCount = BatchManager.batches.size
            val pendingCount = students.count { student ->
                val monthlyFee =
                    student.feeDueAmount ?: if (
                        AppSettings.autoClassFeesEnabled.value
                    ) {
                        AppSettings.parseClassFeeAmount(student.className)
                    } else {
                        null
                    }
                effectiveMonthsUnpaid(
                    student = student,
                    countFeeFromJoinDate =
                        AppSettings.countFeeFromJoinDate.value,
                    monthlyFee = monthlyFee
                ) > 0
            }
            DashboardStats(
                studentCount = studentCount,
                schoolCount = schoolCount,
                batchCount = batchCount,
                pendingCount = pendingCount
            )
        }
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
            modifier = Modifier.height(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            OverviewCard(
                onClick = onOpenStudents,
                title = "Students",
                value = dashboardStats.studentCount.toString(),
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
                value = dashboardStats.schoolCount.toString(),
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
                onClick = onOpenBatches,
                title = "Batch",
                value = dashboardStats.batchCount.toString(),
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
                        "overview/Dues"
                    )
                },
                title = "Dues",
                value = dashboardStats.pendingCount.toString(),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private data class DashboardStats(
    val studentCount: Int,
    val schoolCount: Int,
    val batchCount: Int,
    val pendingCount: Int
)

@Composable
fun DashboardHeader() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                MaterialTheme.colorScheme.surface
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "EduTrack Dashboard",
                    style =
                        MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Manage Students Professionally",
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
