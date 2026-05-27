package com.raju.edutrack

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.raju.edutrack.AppSettings
import com.raju.edutrack.AutoBatchMode
import com.raju.edutrack.cloud.CloudBackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object BatchManager {

    val batches = mutableStateListOf<Batch>()
    private var isLoaded = false

    fun load(context: Context) {
        if (isLoaded) {
            return
        }
        batches.clear()
        batches.addAll(BatchStorage.loadBatches(context))
        autoAssignMissingBatches(context)
        if (batches.isEmpty()) {
            syncFromStudents(context)
        }
        isLoaded = true
    }

    fun addBatch(context: Context, batch: Batch) {
        if (batch.name.isBlank()) {
            return
        }
        if (batches.none { it.name.equals(batch.name, ignoreCase = true) }) {
            batches.add(batch)
            persist(context)
        }
    }

    fun updateBatch(context: Context, originalName: String, updated: Batch) {
        val index = batches.indexOfFirst { batch ->
            batch.name.equals(originalName, ignoreCase = true)
        }
        if (index >= 0 && updated.name.isNotBlank()) {
            batches[index] = updated
            persist(context)
        }
    }

    fun removeBatch(context: Context, name: String) {
        batches.removeAll { batch ->
            batch.name.equals(name, ignoreCase = true)
        }
        persist(context)
    }

    fun ensureBatch(context: Context, name: String?) {
        val normalized = name?.trim().orEmpty()
        if (normalized.isBlank()) {
            return
        }
        addBatch(context, Batch(normalized))
    }

    fun syncFromStudents(context: Context) {
        val batchNames = StudentManager.students
            .mapNotNull { student -> student.batchName }
            .map { name -> name.trim() }
            .filter { name -> name.isNotBlank() }
            .distinctBy { it.lowercase() }
        batches.clear()
        batches.addAll(batchNames.map { name -> Batch(name = name) })
        persist(context)
    }

    fun replaceAll(context: Context, replacement: List<Batch>) {
        batches.clear()
        batches.addAll(replacement)
        persist(context, autoBackup = false)
    }

    private fun autoAssignMissingBatches(context: Context) {
        val mode = AppSettings.autoBatchMode.value
        if (mode == AutoBatchMode.NONE) {
            return
        }
        StudentManager.students.withIndex()
            .filter { entry -> entry.value.batchName.isNullOrBlank() }
            .forEach { entry ->
                val student = entry.value
                val className = student.className.trim()
                val schoolName = student.schoolName.trim()
                val autoBatchName = when (mode) {
                    AutoBatchMode.CLASS -> className
                    AutoBatchMode.CLASS_SCHOOL -> {
                        if (schoolName.isNotBlank()) {
                            "$className - $schoolName"
                        } else {
                            className
                        }
                    }
                    AutoBatchMode.NONE -> ""
                }
                if (autoBatchName.isNotBlank()) {
                    StudentManager.updateStudent(
                        context,
                        entry.index,
                        student.copy(batchName = autoBatchName)
                    )
                    ensureBatch(context, autoBatchName)
                }
            }
    }

    private fun persist(context: Context, autoBackup: Boolean = true) {
        BatchStorage.saveBatches(context, batches)
        if (autoBackup) {
            CoroutineScope(Dispatchers.IO).launch {
                CloudBackupManager.autoBackupIfEnabled(context)
            }
        }
    }
}
