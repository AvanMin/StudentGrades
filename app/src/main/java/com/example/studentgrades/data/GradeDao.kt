package com.example.studentgrades.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.studentgrades.data.model.Grade
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(grade: Grade): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(grades: List<Grade>): List<Long>

    @Update
    suspend fun update(grade: Grade): Int

    @Delete
    suspend fun delete(grade: Grade): Int

    @Query("DELETE FROM grades WHERE id = :gradeId")
    suspend fun deleteById(gradeId: Long): Int

    @Query("SELECT * FROM grades ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<Grade>>

    @Query("SELECT * FROM grades ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<Grade>

    @Query("SELECT * FROM grades WHERE discipline_id = :disciplineId ORDER BY date DESC, id DESC")
    fun observeByDiscipline(disciplineId: Long): Flow<List<Grade>>

    @Query("SELECT * FROM grades WHERE discipline_id = :disciplineId ORDER BY date DESC, id DESC")
    suspend fun getByDiscipline(disciplineId: Long): List<Grade>

    @Query("SELECT * FROM grades WHERE id = :gradeId LIMIT 1")
    suspend fun getById(gradeId: Long): Grade?

    @Query("SELECT COUNT(*) FROM grades WHERE discipline_id = :disciplineId")
    suspend fun getCountByDiscipline(disciplineId: Long): Int

    @Query("SELECT COUNT(*) FROM grades")
    suspend fun getTotalCount(): Int

    @Query("SELECT AVG(CAST(value AS REAL)) FROM grades WHERE discipline_id = :disciplineId")
    suspend fun getAverageByDiscipline(disciplineId: Long): Double?

    @Query("SELECT AVG(CAST(value AS REAL)) FROM grades")
    suspend fun getOverallAverage(): Double?
}
