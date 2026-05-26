package com.raju.edutrack

data class Student(
    val studentName: String,
    val className: String,
    val schoolName: String,
    val contacts: List<Contact>,
)