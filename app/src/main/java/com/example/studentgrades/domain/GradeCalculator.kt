package com.example.studentgrades.domain

import com.example.studentgrades.data.model.Grade
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
- Расчётная логика среднего балла и целевого результата согласно ТЗ.
 */
object GradeCalculator {

    // Минимально и максимально допустимые значения оценок по 5-балльной шкале
    const val MIN_GRADE = 2
    const val MAX_GRADE = 5

    // Проверка, что значение оценки попадает в допустимый диапазон от 2 до 5
    fun isValidGrade(value: Int): Boolean = value in MIN_GRADE..MAX_GRADE

    // Расчёт среднего арифметического балла по списку оценок (или null, если список пуст)
    fun calculateAverage(grades: List<Grade>): Double? {
        if (grades.isEmpty()) return null
        val sum = grades.sumOf { it.value }
        return sum.toDouble() / grades.size
    }

    // Форматирование среднего балла до двух знаков после запятой (или вывод прочерка, если оценок нет)
    fun formatAverage(average: Double?): String {
        if (average == null) return "—"
        return String.format(Locale("ru", "RU"), "%.2f", average)
    }

    /**
    - Формула калькулятора целевого среднего балла:
    - требуемая следующая оценка = целевой_балл * (N + 1) - сумма_текущих_оценок.
     */
    fun calculateTargetNextGrade(grades: List<Grade>, targetAverage: Double): TargetGradeResult {
        val count = grades.size
        val sum = grades.sumOf { it.value }
        val exactRequired = targetAverage * (count + 1) - sum
        val roundedRequired = ((exactRequired * 100.0).roundToInt()) / 100.0

        // Выбор исхода расчёта: цель недостижима (> 5), уже достигнута (<= 2) или достижима (округление вверх)
        return when {
            exactRequired > MAX_GRADE -> TargetGradeResult.UnreachableTooHigh(
                exactRequired = roundedRequired,
                maxPossibleAverage = ((sum + MAX_GRADE).toDouble() / (count + 1) * 100.0).roundToInt() / 100.0
            )
            exactRequired <= MIN_GRADE -> TargetGradeResult.AlreadyAchieved(
                exactRequired = roundedRequired,
                minGradeNeeded = MIN_GRADE
            )
            else -> TargetGradeResult.Achievable(
                exactRequired = roundedRequired,
                minIntegerGradeNeeded = ceil(exactRequired - 1e-9).toInt().coerceIn(MIN_GRADE, MAX_GRADE)
            )
        }
    }
}

// Варианты результата калькулятора целевого балла (достижима, уже достигнута, недостижима за одну оценку)
sealed interface TargetGradeResult {
    data class Achievable(val exactRequired: Double, val minIntegerGradeNeeded: Int) : TargetGradeResult
    data class AlreadyAchieved(val exactRequired: Double, val minGradeNeeded: Int) : TargetGradeResult
    data class UnreachableTooHigh(val exactRequired: Double, val maxPossibleAverage: Double) : TargetGradeResult
}