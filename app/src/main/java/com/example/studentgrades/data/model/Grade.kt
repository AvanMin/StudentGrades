package com.example.studentgrades.data.model

data class Grade(
    val id: Long = 0,
    val disciplineId: Long,
    val value: Int,
    val date: Long,
    val workType: String,
    val comment: String? = null
)