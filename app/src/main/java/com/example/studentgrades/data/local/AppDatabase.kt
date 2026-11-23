package com.example.studentgrades.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.studentgrades.data.model.Discipline
import com.example.studentgrades.data.model.Grade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
            )
                .addCallback(SeedDatabaseCallback())
                .build()
        }

        /**
         * Наполняет базу стартовым набором дисциплин и оценок при первом создании файла БД.
         * При последующих запусках приложения используются сохранённые на диске данные.
         */
        private class SeedDatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    val database = INSTANCE ?: return@launch
                    val disciplineDao = database.disciplineDao()
                    val gradeDao = database.gradeDao()
                    if (disciplineDao.getCount() == 0) {
                        val ids = disciplineDao.insertAll(SeedData.getInitialDisciplines())
                        val grades = SeedData.getInitialGrades(ids)
                        if (grades.isNotEmpty()) {
                            gradeDao.insertAll(grades)
                        }
                    }
                }
            }
        }
    }
}
