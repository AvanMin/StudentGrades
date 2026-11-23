package com.example.studentgrades.data.local

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

@Dao
interface DisciplineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(discipline: Discipline): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(disciplines: List<Discipline>): List<Long>

    @Update
    suspend fun update(discipline: Discipline): Int

    @Delete
    suspend fun delete(discipline: Discipline): Int

    @Query("DELETE FROM disciplines WHERE id = :disciplineId")
    suspend fun deleteById(disciplineId: Long): Int

    @Query("SELECT * FROM disciplines ORDER BY name ASC")
    fun observeAll(): Flow<List<Discipline>>

    @Query("SELECT * FROM disciplines ORDER BY name ASC")
    suspend fun getAll(): List<Discipline>

    @Query("SELECT * FROM disciplines WHERE id = :disciplineId LIMIT 1")
    suspend fun getById(disciplineId: Long): Discipline?

    @Transaction
    @Query("SELECT * FROM disciplines ORDER BY name ASC")
    fun observeAllWithGrades(): Flow<List<DisciplineWithGrades>>

    @Transaction
    @Query("SELECT * FROM disciplines WHERE id = :disciplineId LIMIT 1")
    suspend fun getWithGradesById(disciplineId: Long): DisciplineWithGrades?

    @Query("SELECT COUNT(*) FROM disciplines")
    suspend fun getCount(): Int
}
