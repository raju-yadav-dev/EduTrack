package com.raju.edutrack

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private const val STUDENTS_FILE_NAME = "data.json"
private const val LEGACY_STUDENTS_FILE_NAME = "students.json"

object StudentStorage {

    fun loadStudents(context: Context): List<Student> {
        val primaryFile = File(context.filesDir, STUDENTS_FILE_NAME)
        val legacyFile = File(context.filesDir, LEGACY_STUDENTS_FILE_NAME)
        val (file, usedLegacy) = when {
            primaryFile.exists() -> primaryFile to false
            legacyFile.exists() -> legacyFile to true
            else -> return emptyList()
        }

        val raw = file.readText()
        if (raw.isBlank()) {
            return emptyList()
        }

        val students = mutableListOf<Student>()
        val array = JSONArray(raw)
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val contactsArray = item.optJSONArray("contacts")
            val contacts = mutableListOf<Contact>()
            if (contactsArray != null) {
                for (contactIndex in 0 until contactsArray.length()) {
                    val contactObject =
                        contactsArray.optJSONObject(contactIndex)
                            ?: continue
                    contacts.add(
                        Contact(
                            label = contactObject
                                .optString("label"),
                            number = contactObject
                                .optString("number")
                        )
                    )
                }
            }

            val lastPaidMillis =
                item.optLong("lastFeePaidMillis", -1L)
                    .takeIf { value -> value >= 0L }

            val feeDueAmount =
                item.optDouble("feeDueAmount", Double.NaN)
                    .takeIf { value -> !value.isNaN() }

            val feeDueDateMillis =
                item.optLong("feeDueDateMillis", -1L)
                    .takeIf { value -> value >= 0L }

            val advanceBalance =
                item.optDouble("advanceBalance", Double.NaN)
                    .takeIf { value -> !value.isNaN() }

            students.add(
                Student(
                    studentName =
                        item.optString("studentName"),
                    className =
                        item.optString("className"),
                    schoolName =
                        item.optString("schoolName"),
                    contacts = contacts,
                    joinDateMillis =
                        item.optLong("joinDateMillis"),
                    lastFeePaidMillis = lastPaidMillis,
                    batchName = item.optionalString("batchName"),
                    feeDueAmount = feeDueAmount,
                    feeDueDateMillis = feeDueDateMillis,
                    advanceBalance = advanceBalance,
                    id = item.optionalString("id")
                        ?: legacyStudentId(item, index)
                )
            )
        }

        if (usedLegacy) {
            saveStudents(context, students)
        }

        return students
    }

    fun saveStudents(
        context: Context,
        students: List<Student>
    ) {
        val array = JSONArray()
        students.forEach { student ->
            val item = JSONObject()
            item.put("id", student.id)
            item.put("studentName", student.studentName)
            item.put("className", student.className)
            item.put("schoolName", student.schoolName)
            item.put("joinDateMillis", student.joinDateMillis)

            student.lastFeePaidMillis?.let { value ->
                item.put("lastFeePaidMillis", value)
            }

            student.batchName?.let { value ->
                item.put("batchName", value)
            }

            student.feeDueAmount?.let { value ->
                item.put("feeDueAmount", value)
            }

            student.feeDueDateMillis?.let { value ->
                item.put("feeDueDateMillis", value)
            }

            student.advanceBalance?.let { value ->
                if (value > 0.0) {
                    item.put("advanceBalance", value)
                }
            }

            val contactsArray = JSONArray()
            student.contacts.forEach { contact ->
                val contactObject = JSONObject()
                contactObject.put("label", contact.label)
                contactObject.put("number", contact.number)
                contactsArray.put(contactObject)
            }
            item.put("contacts", contactsArray)

            array.put(item)
        }

        val file = File(context.filesDir, STUDENTS_FILE_NAME)
        file.writeAtomically(array.toString())
    }
}

private fun JSONObject.optionalString(name: String): String? =
    optString(name).takeIf { value -> value.isNotBlank() }

private fun legacyStudentId(
    item: JSONObject,
    index: Int
): String {
    val seed = listOf(
        item.optString("studentName"),
        item.optString("className"),
        item.optString("schoolName"),
        item.optLong("joinDateMillis", 0L).toString(),
        index.toString()
    ).joinToString(separator = "|")
    return seed.hashCode().toUInt().toString(radix = 16)
}

private fun File.writeAtomically(text: String) {
    val tempFile = File(parentFile, "$name.tmp")
    tempFile.writeText(text)
    if (!tempFile.renameTo(this)) {
        tempFile.copyTo(this, overwrite = true)
        tempFile.delete()
    }
}
