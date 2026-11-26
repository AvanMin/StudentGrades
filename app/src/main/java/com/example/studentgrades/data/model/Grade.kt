package com.example.studentgrades.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Таблица «grades» в базе данных Room для хранения оценок
@Entity(
    tableName = "grades",
    // Внешний ключ: привязка оценки к дисциплине с каскадным удалением оценок при удалении предмета
    foreignKeys = [
        ForeignKey(
            entity = Discipline::class,
            parentColumns = ["id"],
            childColumns = ["discipline_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // Индекс по discipline_id для быстрого поиска оценок конкретного предмета
    indices = [Index(value = ["discipline_id"])]
)
data class Grade(
    // Уникальный идентификатор оценки с автогенерацией
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    // ID дисциплины, к которой относится оценка
    @ColumnInfo(name = "discipline_id")
    val disciplineId: Long,

    // Числовое значение оценки (от 2 до 5)
    @ColumnInfo(name = "value")
    val value: Int,

    // Дата получения оценки в миллисекундах (Unix timestamp)
    @ColumnInfo(name = "date")
    val date: Long,

    // Тип учебной работы (контрольная, лабораторная, практическая, ответ)
    @ColumnInfo(name = "work_type")
    val workType: String,

    // Необязательный текстовый комментарий к оценке
    @ColumnInfo(name = "comment")
    val comment: String? = null
)