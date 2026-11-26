package com.example.studentgrades.data.repository

import com.example.studentgrades.data.DisciplineDao
import com.example.studentgrades.data.GradeDao
import com.example.studentgrades.data.model.Discipline
import com.example.studentgrades.data.model.DisciplineWithGrades
import com.example.studentgrades.data.model.Grade
import com.example.studentgrades.domain.GradeCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Репозиторий для работы с локальным хранилищем дисциплин и оценок.
 * Инкапсулирует DAO-операции, валидацию данных и логику удаления связанных записей.
 */
class StudentGradesRepository(
    private val disciplineDao: DisciplineDao,
    private val gradeDao: GradeDao
) {

    // --- Операции с дисциплинами ---

    fun observeAllDisciplines(): Flow<List<Discipline>> = disciplineDao.observeAll()

    fun observeDisciplinesWithGrades(): Flow<List<DisciplineWithGrades>> =
        disciplineDao.observeAllWithGrades()

    suspend fun getAllDisciplines(): List<Discipline> = disciplineDao.getAll()

    suspend fun getDisciplineById(id: Long): Discipline? = disciplineDao.getById(id)

    suspend fun getDisciplineWithGrades(id: Long): DisciplineWithGrades? =
        disciplineDao.getWithGradesById(id)

    suspend fun addDiscipline(name: String, teacherName: String? = null): Long {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Название дисциплины не может быть пустым" }
        val cleanTeacher = teacherName?.trim()?.takeIf { it.isNotEmpty() }
        return disciplineDao.insert(Discipline(name = cleanName, teacherName = cleanTeacher))
    }

    suspend fun updateDiscipline(discipline: Discipline): Boolean {
        val cleanName = discipline.name.trim()
        require(cleanName.isNotEmpty()) { "Название дисциплины не может быть пустым" }
        val cleanTeacher = discipline.teacherName?.trim()?.takeIf { it.isNotEmpty() }
        return disciplineDao.update(
            discipline.copy(name = cleanName, teacherName = cleanTeacher)
        ) > 0
    }

    suspend fun getGradesCountForDiscipline(disciplineId: Long): Int =
        gradeDao.getCountByDiscipline(disciplineId)

    /**
     * Удаление дисциплины с осознанной обработкой связи 1-N:
     * если [allowCascade] == false и у дисциплины есть оценки, возвращается [DeleteDisciplineResult.HasGrades].
     * если [allowCascade] == true или оценок нет — дисциплина удаляется (вместе со связанными оценками по FK CASCADE).
     */
    suspend fun deleteDiscipline(
        disciplineId: Long,
        allowCascade: Boolean = false
    ): DeleteDisciplineResult {
        val gradesCount = gradeDao.getCountByDiscipline(disciplineId)
        if (gradesCount > 0 && !allowCascade) {
            return DeleteDisciplineResult.HasGrades(gradesCount)
        }
        val deleted = disciplineDao.deleteById(disciplineId) > 0
        return if (deleted) {
            DeleteDisciplineResult.Deleted(deletedGradesCount = gradesCount)
        } else {
            DeleteDisciplineResult.NotFound
        }
    }

    // --- Операции с оценками ---

    fun observeAllGrades(): Flow<List<Grade>> = gradeDao.observeAll()

    fun observeGradesByDiscipline(disciplineId: Long): Flow<List<Grade>> =
        gradeDao.observeByDiscipline(disciplineId)

    fun observeFilteredGrades(filter: GradeFilter): Flow<List<Grade>> =
        gradeDao.observeAll().map { grades ->
            applyFilterAndSort(grades, filter)
        }

    suspend fun getAllGrades(): List<Grade> = gradeDao.getAll()

    suspend fun getGradesByDiscipline(disciplineId: Long): List<Grade> =
        gradeDao.getByDiscipline(disciplineId)

    suspend fun getFilteredGrades(filter: GradeFilter): List<Grade> =
        applyFilterAndSort(gradeDao.getAll(), filter)

    suspend fun getGradeById(gradeId: Long): Grade? = gradeDao.getById(gradeId)

    suspend fun addGrade(
        disciplineId: Long,
        value: Int,
        date: Long,
        workType: String,
        comment: String? = null
    ): Long {
        validateGradeInput(disciplineId, value, workType)
        val cleanComment = comment?.trim()?.takeIf { it.isNotEmpty() }
        return gradeDao.insert(
            Grade(
                disciplineId = disciplineId,
                value = value,
                date = date,
                workType = workType.trim(),
                comment = cleanComment
            )
        )
    }

    suspend fun updateGrade(grade: Grade): Boolean {
        validateGradeInput(grade.disciplineId, grade.value, grade.workType)
        val cleanComment = grade.comment?.trim()?.takeIf { it.isNotEmpty() }
        return gradeDao.update(
            grade.copy(
                workType = grade.workType.trim(),
                comment = cleanComment
            )
        ) > 0
    }

    suspend fun deleteGrade(grade: Grade): Boolean = gradeDao.delete(grade) > 0

    suspend fun deleteGradeById(gradeId: Long): Boolean = gradeDao.deleteById(gradeId) > 0

    // --- Агрегация и статистика ---

    suspend fun getDisciplineAverage(disciplineId: Long): Double? =
        gradeDao.getAverageByDiscipline(disciplineId)

    suspend fun getOverallAverage(): Double? = gradeDao.getOverallAverage()

    suspend fun getTotalGradesCount(): Int = gradeDao.getTotalCount()

    // --- Внутренние вспомогательные методы ---

    private fun applyFilterAndSort(grades: List<Grade>, filter: GradeFilter): List<Grade> {
        val filtered = grades.filter { grade ->
            val matchesDiscipline = filter.disciplineId == null ||
                    grade.disciplineId == filter.disciplineId

            val matchesWorkType = filter.workType.isNullOrBlank() ||
                    grade.workType.equals(filter.workType.trim(), ignoreCase = true)

            val matchesDateFrom = filter.dateFrom == null ||
                    grade.date >= filter.dateFrom

            val matchesDateTo = filter.dateTo == null ||
                    grade.date <= filter.dateTo

            matchesDiscipline && matchesWorkType && matchesDateFrom && matchesDateTo
        }

        return when (filter.sortBy) {
            GradeSortOption.DATE_DESC -> filtered.sortedByDescending { it.date }
            GradeSortOption.DATE_ASC -> filtered.sortedBy { it.date }
            GradeSortOption.VALUE_DESC -> filtered.sortedWith(
                compareByDescending<Grade> { it.value }.thenByDescending { it.date }
            )
            GradeSortOption.VALUE_ASC -> filtered.sortedWith(
                compareBy<Grade> { it.value }.thenByDescending { it.date }
            )
        }
    }

    private suspend fun validateGradeInput(disciplineId: Long, value: Int, workType: String) {
        require(GradeCalculator.isValidGrade(value)) {
            "Оценка должна находиться в диапазоне от ${GradeCalculator.MIN_GRADE} до ${GradeCalculator.MAX_GRADE}"
        }
        require(workType.trim().isNotEmpty()) { "Тип учебной работы обязателен для заполнения" }
        require(disciplineDao.getById(disciplineId) != null) {
            "Дисциплина с id=$disciplineId не найдена"
        }
    }
}

enum class GradeSortOption {
    DATE_DESC,
    DATE_ASC,
    VALUE_DESC,
    VALUE_ASC
}

data class GradeFilter(
    val disciplineId: Long? = null,
    val workType: String? = null,
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val sortBy: GradeSortOption = GradeSortOption.DATE_DESC
) {
    val isActive: Boolean
        get() = disciplineId != null ||
                !workType.isNullOrBlank() ||
                dateFrom != null ||
                dateTo != null ||
                sortBy != GradeSortOption.DATE_DESC
}

sealed interface DeleteDisciplineResult {
    data class Deleted(val deletedGradesCount: Int) : DeleteDisciplineResult
    data class HasGrades(val gradesCount: Int) : DeleteDisciplineResult
    data object NotFound : DeleteDisciplineResult
}