package com.example.studentgrades.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "grades",
    foreignKeys = [
        ForeignKey(
            entity = Discipline::class,
            parentColumns = ["id"],
            childColumns = ["discipline_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["discipline_id"])]
)
data class Grade(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "discipline_id")
    val disciplineId: Long,

    @ColumnInfo(name = "value")
    val value: Int,

    @ColumnInfo(name = "date")
    val date: Long,

    @ColumnInfo(name = "work_type")
    val workType: String,

    @ColumnInfo(name = "comment")
    val comment: String? = null
)
