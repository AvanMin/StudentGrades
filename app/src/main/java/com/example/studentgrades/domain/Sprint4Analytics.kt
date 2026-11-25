package com.example.studentgrades.domain

import com.example.studentgrades.data.model.Grade
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class GradeSort {
    BY_DATE_DESC,
    BY_DATE_ASC,
    BY_VALUE_DESC,
    BY_VALUE_ASC
}

data class GradeFilter(
    val disciplineId: Long? = null,
    val workType: String? = null,
    val dateFrom: Long? = null,
    val dateTo: Long? = null
)

data class PeriodAverage(
    val periodLabel: String,
    val year: Int,
    val month: Int,
    val gradeCount: Int,
    val average: Double
)

object Sprint4Analytics {

    private val ruLocale = Locale.Builder().setLanguage("ru").build()

    /** Безопасный расчёт среднего балла (защита от деления на 0). */
    fun calculateAverage(grades: List<Grade>): Double? {
        if (grades.isEmpty()) return null
        return grades.sumOf { it.value }.toDouble() / grades.size
    }

    /** Форматирование среднего балла. */
    fun formatAverage(average: Double?): String {
        return if (average == null) "Нет оценок" else String.format(ruLocale, "%.2f", average)
    }

    /** Фильтрация оценок по дисциплине, типу работы и датам. */
    fun filterGrades(grades: List<Grade>, filter: GradeFilter): List<Grade> {
        return grades.filter { grade ->
            val okDiscipline = filter.disciplineId == null || grade.disciplineId == filter.disciplineId
            val okType = filter.workType.isNullOrBlank() ||
                    grade.workType.equals(filter.workType, ignoreCase = true)
            val okFrom = filter.dateFrom == null || grade.date >= filter.dateFrom
            val okTo = filter.dateTo == null || grade.date <= filter.dateTo
            okDiscipline && okType && okFrom && okTo
        }
    }

    /** Сортировка оценок по дате или значению. */
    fun sortGrades(grades: List<Grade>, sort: GradeSort): List<Grade> {
        return when (sort) {
            GradeSort.BY_DATE_DESC -> grades.sortedByDescending { it.date }
            GradeSort.BY_DATE_ASC -> grades.sortedBy { it.date }
            GradeSort.BY_VALUE_DESC -> grades.sortedByDescending { it.value }
            GradeSort.BY_VALUE_ASC -> grades.sortedBy { it.value }
        }
    }

    /** Группировка оценок по месяцам для экрана «Динамика». */
    fun calculatePeriodAverages(grades: List<Grade>): List<PeriodAverage> {
        if (grades.isEmpty()) return emptyList()

        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("LLLL", ruLocale)

        return grades
            .groupBy { grade ->
                calendar.timeInMillis = grade.date
                calendar.get(Calendar.YEAR) to calendar.get(Calendar.MONTH)
            }
            .map { (yearMonth, list) ->
                val (year, monthIndex) = yearMonth
                calendar.set(year, monthIndex, 1)
                val label = monthFormat.format(calendar.time)
                    .replaceFirstChar { it.uppercase(ruLocale) }

                PeriodAverage(
                    periodLabel = label,
                    year = year,
                    month = monthIndex + 1,
                    gradeCount = list.size,
                    average = list.sumOf { it.value }.toDouble() / list.size
                )
            }
            .sortedWith(compareBy({ it.year }, { it.month }))
    }

    fun calculateTrend(periods: List<PeriodAverage>): Double? {
        if (periods.size < 2) return null
        return periods.last().average - periods.first().average
    }

    fun trendText(trend: Double?): String = when {
        trend == null  -> "Недостаточно данных для сравнения"
        trend > 0.001  -> "Успеваемость растёт"
        trend < -0.001 -> "Успеваемость снижается"
        else           -> "Без изменений"
    }

    fun formatTrend(trend: Double?, firstPeriodLabel: String?): String {
        if (trend == null || firstPeriodLabel == null) return "—"
        val sign = if (trend >= 0) "+" else ""
        return String.format(
            ruLocale,
            "%s%.2f с %s",
            sign,
            trend,
            firstPeriodLabel.lowercase(ruLocale)
        )
    }
}