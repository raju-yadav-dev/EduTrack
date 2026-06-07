package com.raju.edutrack

import android.content.Context
import androidx.compose.runtime.mutableStateListOf

object StudentManager {

    val students = mutableStateListOf<Student>()

    private var isLoaded = false

    fun load(context: Context) {
        if (isLoaded) {
            return
        }
        students.clear()
        students.addAll(StudentStorage.loadStudents(context))
        isLoaded = true
    }

    fun addStudent(
        context: Context,
        student: Student
    ) {
        students.add(student)
        persist(context)
    }

    fun updateStudent(
        context: Context,
        index: Int,
        student: Student,
        recordPaymentHistory: Boolean = true
    ) {
        if (index in students.indices) {
            students[index] = student
            persist(context)
        }
    }

    fun updateStudents(
        context: Context,
        transform: (Student) -> Student
    ): Int {
        var updatedCount = 0
        students.indices.forEach { index ->
            val current = students[index]
            val updated = transform(current)
            if (updated != current) {
                students[index] = updated
                updatedCount++
            }
        }
        if (updatedCount > 0) {
            persist(context)
        }
        return updatedCount
    }

    fun removeStudents(
        context: Context,
        indices: List<Int>
    ) {
        indices.sortedDescending()
            .forEach { index ->
                if (index in students.indices) {
                    students.removeAt(index)
                }
            }
        persist(context)
    }

    fun replaceAll(
        context: Context,
        replacement: List<Student>
    ) {
        students.clear()
        students.addAll(replacement)
        persist(context)
    }

    private fun persist(context: Context) {
        StudentStorage.saveStudents(context, students)
    }

}
