package com.raju.edutrack.cloud

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.raju.edutrack.BatchManager
import com.raju.edutrack.R
import com.raju.edutrack.StudentManager
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CloudSyncStatus {
    SignedOut,
    Ready,
    Syncing,
    Synced,
    Error
}

data class CloudBackupUiState(
    val status: CloudSyncStatus = CloudSyncStatus.SignedOut,
    val accountEmail: String = "",
    val isAutoBackupEnabled: Boolean = false,
    val lastBackupMillis: Long? = null,
    val message: String = ""
) {
    val lastBackupText: String
        get() {
            val value = lastBackupMillis ?: return "Never"
            val formatter = SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
            )
            return formatter.format(Date(value))
        }
}

object CloudBackupManager {

    val uiState = mutableStateOf(CloudBackupUiState())
    private var realtimeListener: ListenerRegistration? = null
    private var isApplyingRemoteBackup = false

    private const val PREFS_NAME = "cloud_backup_settings"
    private const val KEY_AUTO_BACKUP = "autoBackup"
    private const val KEY_LAST_BACKUP = "lastBackupMillis"

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val user = if (isFirebaseConfigured(context)) {
            FirebaseAuth.getInstance().currentUser
        } else {
            null
        }
        uiState.value = uiState.value.copy(
            status = if (user == null) {
                CloudSyncStatus.SignedOut
            } else {
                CloudSyncStatus.Ready
            },
            accountEmail = user?.email.orEmpty(),
            isAutoBackupEnabled = prefs.getBoolean(KEY_AUTO_BACKUP, false),
            lastBackupMillis = prefs.getLong(KEY_LAST_BACKUP, -1L)
                .takeIf { value -> value > 0L }
        )
        if (user != null) {
            startRealtimeSync(context.applicationContext)
        }
    }

    fun getSignInIntent(context: Context): Intent? {
        if (!isFirebaseConfigured(context)) {
            showError("Add google-services.json and Firebase config first.")
            return null
        }

        val clientId = context.getString(R.string.default_web_client_id)
        if (clientId.isBlank() || clientId == "replace_with_web_client_id") {
            showError("Set default_web_client_id from Firebase console.")
            return null
        }
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, options).signInIntent
    }

    suspend fun finishSignIn(data: Intent?) {
        runCatching {
            uiState.value = uiState.value.copy(
                status = CloudSyncStatus.Syncing,
                message = "Signing in..."
            )
            val account = GoogleSignIn
                .getSignedInAccountFromIntent(data)
                .await()
            val credential = GoogleAuthProvider
                .getCredential(account.idToken, null)
            val result = FirebaseAuth.getInstance()
                .signInWithCredential(credential)
                .await()
            uiState.value = uiState.value.copy(
                status = CloudSyncStatus.Ready,
                accountEmail = result.user?.email.orEmpty(),
                message = "Signed in"
            )
            result.user?.let {
                startRealtimeSync(FirebaseApp.getInstance().applicationContext)
            }
        }.onFailure { error ->
            showError(error.message ?: "Google sign-in failed")
        }
    }

    fun signOut(context: Context) {
        if (!isFirebaseConfigured(context)) {
            showError("Add google-services.json and Firebase config first.")
            return
        }
        realtimeListener?.remove()
        realtimeListener = null
        FirebaseAuth.getInstance().signOut()
        uiState.value = uiState.value.copy(
            status = CloudSyncStatus.SignedOut,
            accountEmail = "",
            message = "Signed out"
        )
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.DEFAULT_SIGN_IN
        ).signOut()
    }

    fun setAutoBackup(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_BACKUP, enabled)
            .apply()
        uiState.value = uiState.value.copy(isAutoBackupEnabled = enabled)
    }

    suspend fun backupNow(context: Context) {
        if (!isFirebaseConfigured(context)) {
            showError("Add google-services.json and Firebase config first.")
            return
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            showError("Sign in with Google first.")
            return
        }
        runCatching {
            uiState.value = uiState.value.copy(
                status = CloudSyncStatus.Syncing,
                message = "Backing up..."
            )
            val now = System.currentTimeMillis()
            val payload = CloudBackupPayload(
                updatedAtMillis = now,
                students = StudentManager.students.map { student ->
                    student.toCloudStudent()
                },
                batches = BatchManager.batches.map { batch ->
                    batch.toCloudBatch()
                }
            )
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("backups")
                .document("latest")
                .set(payload)
                .await()
            saveLastBackup(context, now)
            uiState.value = uiState.value.copy(
                status = CloudSyncStatus.Synced,
                lastBackupMillis = now,
                message = "Backup complete"
            )
        }.onFailure { error ->
            showError(error.message ?: "Backup failed")
        }
    }

    suspend fun restoreLatest(context: Context) {
        if (!isFirebaseConfigured(context)) {
            showError("Add google-services.json and Firebase config first.")
            return
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            showError("Sign in with Google first.")
            return
        }
        runCatching {
            uiState.value = uiState.value.copy(
                status = CloudSyncStatus.Syncing,
                message = "Restoring..."
            )
            val payload = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("backups")
                .document("latest")
                .get()
                .await()
                .toObject(CloudBackupPayload::class.java)
                ?: throw IllegalStateException("No cloud backup found")
            StudentManager.replaceAll(
                context,
                payload.students.map { student -> student.toStudent() }
            )
            BatchManager.replaceAll(
                context,
                payload.batches.map { batch -> batch.toBatch() }
            )
            uiState.value = uiState.value.copy(
                status = CloudSyncStatus.Synced,
                lastBackupMillis = payload.updatedAtMillis,
                message = "Restore complete"
            )
        }.onFailure { error ->
            showError(error.message ?: "Restore failed")
        }
    }

    suspend fun autoBackupIfEnabled(context: Context) {
        if (uiState.value.isAutoBackupEnabled && !isApplyingRemoteBackup) {
            backupNow(context)
        }
    }

    private fun startRealtimeSync(context: Context) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        realtimeListener?.remove()
        realtimeListener = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("backups")
            .document("latest")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    showError(error.message ?: "Realtime sync failed")
                    return@addSnapshotListener
                }
                val payload = snapshot?.toObject(CloudBackupPayload::class.java)
                    ?: return@addSnapshotListener
                val localBackupMillis = uiState.value.lastBackupMillis ?: 0L
                if (payload.updatedAtMillis <= localBackupMillis) {
                    return@addSnapshotListener
                }
                isApplyingRemoteBackup = true
                try {
                    StudentManager.replaceAll(
                        context,
                        payload.students.map { student -> student.toStudent() }
                    )
                    BatchManager.replaceAll(
                        context,
                        payload.batches.map { batch -> batch.toBatch() }
                    )
                    saveLastBackup(context, payload.updatedAtMillis)
                    uiState.value = uiState.value.copy(
                        status = CloudSyncStatus.Synced,
                        lastBackupMillis = payload.updatedAtMillis,
                        message = "Cloud sync applied"
                    )
                } finally {
                    isApplyingRemoteBackup = false
                }
            }
    }

    private fun saveLastBackup(context: Context, millis: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_BACKUP, millis)
            .apply()
    }

    private fun showError(message: String) {
        uiState.value = uiState.value.copy(
            status = CloudSyncStatus.Error,
            message = message
        )
    }

    private fun isFirebaseConfigured(context: Context): Boolean {
        return FirebaseApp.getApps(context).isNotEmpty()
    }
}
