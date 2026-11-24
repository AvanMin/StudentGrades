package com.example.studentgrades

import android.app.Application
import com.example.studentgrades.data.AppDatabase
import com.example.studentgrades.data.repository.StudentGradesRepository

class StudentGradesApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val repository: StudentGradesRepository by lazy {
        StudentGradesRepository(
            disciplineDao = database.disciplineDao(),
            gradeDao = database.gradeDao()
        )
    }
}
