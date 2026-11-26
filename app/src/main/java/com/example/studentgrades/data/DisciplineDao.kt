package com.example.studentgrades.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.studentgrades.data.model.Discipline
import com.example.studentgrades.data.model.DisciplineWithGrades
import kotlinx.coroutines.flow.Flow

// Интерфейс доступа к данным (DAO) для выполнения SQL-операций с таблицей дисциплин
@Dao
interface DisciplineDao {

    // Вставка новой дисциплины (при совпадении ID заменяет старую запись) и возврат её ID
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(discipline: Discipline): Long

    // Массовая вставка списка дисциплин в таблицу
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(disciplines: List<Discipline>): List<Long>

    // Обновление данных существующей дисциплины
    @Update
    suspend fun update(discipline: Discipline): Int

    // Удаление дисциплины по переданному объекту
    @Delete
    suspend fun delete(discipline: Discipline): Int

    // Удаление дисциплины из таблицы по её уникальному ID
    @Query("DELETE FROM disciplines WHERE id = :disciplineId")
    suspend fun deleteById(disciplineId: Long): Int

    // Реактивное наблюдение за всеми дисциплинами, отсортированными по алфавиту
    @Query("SELECT * FROM disciplines ORDER BY name ASC")
    fun observeAll(): Flow<List<Discipline>>

    // Однократное получение списка всех дисциплин по алфавиту
    @Query("SELECT * FROM disciplines ORDER BY name ASC")
    suspend fun getAll(): List<Discipline>

    // Поиск одной дисциплины по её ID
    @Query("SELECT * FROM disciplines WHERE id = :disciplineId LIMIT 1")
    suspend fun getById(disciplineId: Long): Discipline?

    // Реактивное получение всех дисциплин вместе с привязанными к ним оценками (в одной транзакции)
    @Transaction
    @Query("SELECT * FROM disciplines ORDER BY name ASC")
    fun observeAllWithGrades(): Flow<List<DisciplineWithGrades>>

    // Однократное получение конкретной дисциплины вместе со всеми её оценками по ID
    @Transaction
    @Query("SELECT * FROM disciplines WHERE id = :disciplineId LIMIT 1")
    suspend fun getWithGradesById(disciplineId: Long): DisciplineWithGrades?

    // Подсчёт общего количества дисциплин в базе данных
    @Query("SELECT COUNT(*) FROM disciplines")
    suspend fun getCount(): Int
}