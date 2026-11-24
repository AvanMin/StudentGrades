package com.example.studentgrades.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.studentgrades.data.model.Discipline
import com.example.studentgrades.data.model.Grade

@Database(
    entities = [Discipline::class, Grade::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun disciplineDao(): DisciplineDao
    abstract fun gradeDao(): GradeDao

    companion object {
        const val DATABASE_NAME = "student_grades.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): AppDatabase {
            return Room.databaseBuilder(
                appContext,
                AppDatabase::class.java,
                DATABASE_NAME
            ).build()
        }
    }
}