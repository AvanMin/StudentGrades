package com.example.studentgrades.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class DisciplineWithGrades(
    @Embedded
    val discipline: Discipline,

    @Relation(
        parentColumn = "id",
        entityColumn = "discipline_id"
    )
    val grades: List<Grade>
) {
    val gradesCount: Int
        get() = grades.size

    val averageScore: Double?
        get() = if (grades.isEmpty()) null else grades.sumOf { it.value }.toDouble() / grades.size
}
