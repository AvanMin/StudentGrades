package com.example.studentgrades.data

import com.example.studentgrades.data.model.Discipline
import com.example.studentgrades.data.model.Grade
import java.util.Calendar

/**
 * Начальный тестовый набор данных для первичной инициализации БД
 * и проверки расчётов, связей 1-N и сохранения после перезапуска.
 */
object SeedData {

    fun getInitialDisciplines(): List<Discipline> = listOf(
        Discipline(name = "Разработка мобильных приложений", teacherName = "Смирнов А.В."),
        Discipline(name = "Базы данных", teacherName = "Ковалёва Е.И."),
        Discipline(name = "Алгоритмы и структуры данных", teacherName = "Петров Д.С."),
        Discipline(name = "Компьютерные сети", teacherName = null)
    )

    fun getInitialGrades(disciplineIds: List<Long>): List<Grade> {
        if (disciplineIds.size < 4) return emptyList()
        val mobileId = disciplineIds[0]
        val dbId = disciplineIds[1]
        val algoId = disciplineIds[2]
        val netId = disciplineIds[3]

        return listOf(
            // Разработка мобильных приложений: 5, 4, 3, 5 -> средний балл 4.25
            Grade(
                disciplineId = mobileId,
                value = 5,
                date = timestamp(2026, 9, 10),
                workType = "Практическая работа",
                comment = "Настройка проекта и навигации"
            ),
            Grade(
                disciplineId = mobileId,
                value = 4,
                date = timestamp(2026, 9, 24),
                workType = "Лабораторная работа",
                comment = "Вёрстка экранов"
            ),
            Grade(
                disciplineId = mobileId,
                value = 3,
                date = timestamp(2026, 10, 8),
                workType = "Контрольная работа",
                comment = "Жизненный цикл Activity и Fragment"
            ),
            Grade(
                disciplineId = mobileId,
                value = 5,
                date = timestamp(2026, 10, 22),
                workType = "Лабораторная работа",
                comment = "Работа с локальной БД Room"
            ),

            // Базы данных: 5, 5, 4 -> средний балл 4.67
            Grade(
                disciplineId = dbId,
                value = 5,
                date = timestamp(2026, 9, 15),
                workType = "Лабораторная работа",
                comment = "Проектирование ER-диаграммы"
            ),
            Grade(
                disciplineId = dbId,
                value = 5,
                date = timestamp(2026, 10, 1),
                workType = "Практическая работа",
                comment = "SQL-запросы с JOIN и агрегацией"
            ),
            Grade(
                disciplineId = dbId,
                value = 4,
                date = timestamp(2026, 10, 19),
                workType = "Контрольная работа",
                comment = "Нормализация отношений"
            ),

            // Алгоритмы и структуры данных: 3, 4 -> средний балл 3.50
            Grade(
                disciplineId = algoId,
                value = 3,
                date = timestamp(2026, 9, 18),
                workType = "Ответ на занятии",
                comment = "Сложность сортировок"
            ),
            Grade(
                disciplineId = algoId,
                value = 4,
                date = timestamp(2026, 10, 12),
                workType = "Практическая работа",
                comment = "Реализация графов"
            ),

            // Компьютерные сети: 5 -> средний балл 5.00
            Grade(
                disciplineId = netId,
                value = 5,
                date = timestamp(2026, 10, 5),
                workType = "Лабораторная работа",
                comment = "Маршрутизация и подсети IP"
            )
        )
    }

    private fun timestamp(year: Int, monthOneBased: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthOneBased - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
