package com.example.studentgrades

import android.app.Application
import com.example.studentgrades.data.AppDatabase
import com.example.studentgrades.data.repository.StudentGradesRepository

// Главный класс приложения, создающийся один раз при запуске процесса
class StudentGradesApp : Application() {

    // Ленивая (при первом обращении) инициализация единого экземпляра базы данных Room
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    // Ленивая инициализация репозитория с передачей DAO для дисциплин и оценок
    val repository: StudentGradesRepository by lazy {
        StudentGradesRepository(
            disciplineDao = database.disciplineDao(),
            gradeDao = database.gradeDao()
        )
    }
}