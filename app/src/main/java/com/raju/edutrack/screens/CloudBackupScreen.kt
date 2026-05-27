package com.raju.edutrack.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.raju.edutrack.StudentManager
import com.raju.edutrack.BatchManager
import com.raju.edutrack.cloud.CloudBackupManager
import com.raju.edutrack.cloud.CloudSyncStatus
import kotlinx.coroutines.launch

@Composable
fun CloudBackupScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val state by CloudBackupManager.uiState
    var showRestoreDialog by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            CloudBackupManager.finishSignIn(result.data)
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDialog = false
                        scope.launch {
                            CloudBackupManager.restoreLatest(context)
                        }
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestoreDialog = false }
                ) {
                    Text("Cancel")
                }
            },
            title = { Text("Restore cloud backup?") },
            text = {
                Text("This replaces local students, batches, contacts, and pending records with the latest cloud backup.")
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Cloud Backup",
            style = MaterialTheme.typography.headlineMedium
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (state.accountEmail.isBlank()) {
                                "Google account"
                            } else {
                                state.accountEmail
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Private Firestore backup for this account",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                }

                if (state.accountEmail.isBlank()) {
                    Button(
                        onClick = {
                            val intent = CloudBackupManager.getSignInIntent(context)
                            if (activity != null && intent != null) {
                                signInLauncher.launch(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign in with Google")
                    }
                } else {
                    OutlinedButton(
                        onClick = { CloudBackupManager.signOut(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign out")
                    }
                }
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Automatic backup",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Sync changes after local data is saved",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = state.isAutoBackupEnabled,
                        onCheckedChange = { enabled ->
                            CloudBackupManager.setAutoBackup(context, enabled)
                        }
                    )
                }

                Text(
                    text = "Last backup: ${state.lastBackupText}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(statusText(state.status)) },
                        leadingIcon = {
                            StatusIcon(state.status)
                        }
                    )
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                "${StudentManager.students.size} students, ${BatchManager.batches.size} batches"
                            )
                        }
                    )
                }

                if (state.status == CloudSyncStatus.Syncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (state.message.isNotBlank()) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.status == CloudSyncStatus.Error) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        CloudBackupManager.backupNow(context)
                    }
                },
                enabled = state.status != CloudSyncStatus.Syncing,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Backup,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Backup")
            }

            FilledTonalButton(
                onClick = { showRestoreDialog = true },
                enabled = state.status != CloudSyncStatus.Syncing,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Restore")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Cloud data is stored under users/{uid}/backups/latest in Firestore, so every Google account only sees its own EduTrack backup.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun StatusIcon(status: CloudSyncStatus) {
    when (status) {
        CloudSyncStatus.SignedOut -> Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null
        )
        CloudSyncStatus.Ready -> Icon(
            imageVector = Icons.Default.CloudSync,
            contentDescription = null
        )
        CloudSyncStatus.Syncing -> CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
        )
        CloudSyncStatus.Synced -> Icon(
            imageVector = Icons.Default.CloudDone,
            contentDescription = null
        )
        CloudSyncStatus.Error -> Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null
        )
    }
}

private fun statusText(status: CloudSyncStatus): String {
    return when (status) {
        CloudSyncStatus.SignedOut -> "Signed out"
        CloudSyncStatus.Ready -> "Ready"
        CloudSyncStatus.Syncing -> "Syncing"
        CloudSyncStatus.Synced -> "Synced"
        CloudSyncStatus.Error -> "Needs attention"
    }
}
