package com.raju.edutrack.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raju.edutrack.AppSettings

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Count fee from joining date",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(
                    modifier = Modifier.height(4.dp)
                )
                Text(
                    text = "If enabled, new students are not pending in their joining month.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Switch(
                checked = AppSettings.countFeeFromJoinDate.value,
                onCheckedChange = { isChecked ->
                    AppSettings.countFeeFromJoinDate.value = isChecked
                }
            )
        }
    }
}