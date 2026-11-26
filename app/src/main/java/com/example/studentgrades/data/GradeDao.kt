package com.example.studentgrades.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.studentgrades.data.model.Grade
import kotlinx.coroutines.flow.Flow

// Интерфейс доступа к данным (DAO) для выполнения SQL-операций с таблицей оценок
@Dao
interface GradeDao {

    // Вставка новой оценки (при совпадении ID заменяет старую запись) и возврат её ID
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(grade: Grade): Long

    // Массовая вставка списка оценок в таблицу
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(grades: List<Grade>): List<Long>

    // Обновление данных существующей оценки
    @Update
    suspend fun update(grade: Grade): Int

    // Удаление оценки по переданному объекту
    @Delete
    suspend fun delete(grade: Grade): Int

    // Удаление оценки из таблицы по её уникальному ID
    @Query("DELETE FROM grades WHERE id = :gradeId")
    suspend fun deleteById(gradeId: Long): Int

    // Реактивное наблюдение за всеми оценками (новые сверху)
    @Query("SELECT * FROM grades ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<Grade>>

    // Однократное получение списка всех оценок (новые сверху)
    @Query("SELECT * FROM grades ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<Grade>

    // Реактивное наблюдение за оценками конкретной дисциплины (новые сверху)
    @Query("SELECT * FROM grades WHERE discipline_id = :disciplineId ORDER BY date DESC, id DESC")
    fun observeByDiscipline(disciplineId: Long): Flow<List<Grade>>

    // Однократное получение оценок конкретной дисциплины (новые сверху)
    @Query("SELECT * FROM grades WHERE discipline_id = :disciplineId ORDER BY date DESC, id DESC")
    suspend fun getByDiscipline(disciplineId: Long): List<Grade>

    // Поиск одной оценки по её ID
    @Query("SELECT * FROM grades WHERE id = :gradeId LIMIT 1")
    suspend fun getById(gradeId: Long): Grade?

    // Подсчёт количества оценок по конкретной дисциплине
    @Query("SELECT COUNT(*) FROM grades WHERE discipline_id = :disciplineId")
    suspend fun getCountByDiscipline(disciplineId: Long): Int

    // Подсчёт общего количества оценок в базе данных
    @Query("SELECT COUNT(*) FROM grades")
    suspend fun getTotalCount(): Int

    // Расчёт среднего балла по конкретной дисциплине средствами SQLite
    @Query("SELECT AVG(CAST(value AS REAL)) FROM grades WHERE discipline_id = :disciplineId")
    suspend fun getAverageByDiscipline(disciplineId: Long): Double?

    // Расчёт общего среднего балла по всем оценкам средствами SQLite
    @Query("SELECT AVG(CAST(value AS REAL)) FROM grades")
    suspend fun getOverallAverage(): Double?
}