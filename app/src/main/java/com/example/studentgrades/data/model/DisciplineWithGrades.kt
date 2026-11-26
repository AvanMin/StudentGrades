package com.example.studentgrades.data.model

import androidx.room.Embedded
import androidx.room.Relation

// Модель связи «один ко многим» в Room: объединяет дисциплину и список всех её оценок
data class DisciplineWithGrades(
    // Встроенная основная сущность дисциплины
    @Embedded
    val discipline: Discipline,

    // Автоматическая подгрузка всех оценок, у которых discipline_id совпадает с id дисциплины
    @Relation(
        parentColumn = "id",
        entityColumn = "discipline_id"
    )
    val grades: List<Grade>
) {
    // Вычисляемое свойство: общее количество оценок по дисциплине
    val gradesCount: Int
        get() = grades.size

    // Вычисляемое свойство: средний балл по дисциплине (или null, если оценок нет)
    val averageScore: Double?
        get() = if (grades.isEmpty()) null else grades.sumOf { it.value }.toDouble() / grades.size
}