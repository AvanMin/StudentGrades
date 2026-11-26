package com.example.studentgrades.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.studentgrades.data.model.Discipline
import com.example.studentgrades.data.model.Grade

// Главный класс базы данных Room со списком таблиц (сущностей) и версией схемы
@Database(
    entities = [Discipline::class, Grade::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Методы для получения объектов доступа к данным (DAO) дисциплин и оценок
    abstract fun disciplineDao(): DisciplineDao
    abstract fun gradeDao(): GradeDao

    companion object {
        // Имя файла локальной базы данных SQLite на устройстве
        const val DATABASE_NAME = "student_grades.db"

        // Потокобезопасная ссылка на единственный экземпляр БД (паттерн Singleton)
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Получение существующего экземпляра БД или его потокобезопасное создание при первом обращении
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        // Построение и инициализация базы данных Room
        private fun buildDatabase(appContext: Context): AppDatabase {
            return Room.databaseBuilder(
                appContext,
                AppDatabase::class.java,
                DATABASE_NAME
            ).build()
        }
    }
}