package com.example.studentgrades.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Таблица «disciplines» в базе данных Room для хранения учебных дисциплин
@Entity(tableName = "disciplines")
data class Discipline(
    // Уникальный идентификатор дисциплины с автоматической генерацией
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    // Название учебной дисциплины
    @ColumnInfo(name = "name")
    val name: String,

    // ФИО преподавателя (необязательное поле)
    @ColumnInfo(name = "teacher_name")
    val teacherName: String? = null
)