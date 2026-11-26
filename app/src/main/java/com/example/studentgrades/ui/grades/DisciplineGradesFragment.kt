package com.example.studentgrades.ui.grades

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.studentgrades.R
import com.example.studentgrades.data.AppDatabase
import com.example.studentgrades.data.model.Discipline
import com.example.studentgrades.data.model.Grade
import com.example.studentgrades.databinding.FragmentDisciplineGradesBinding
import com.example.studentgrades.domain.GradeFilter
import com.example.studentgrades.domain.GradeSort
import com.example.studentgrades.domain.Sprint4Analytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class DisciplineGradesFragment : Fragment() {

    private var _binding: FragmentDisciplineGradesBinding? = null
    private val binding get() = _binding!!

    private var disciplineId: Long = -1L
    private var currentDiscipline: Discipline? = null
    private var currentGradesCount: Int = 0
    private lateinit var adapter: GradesAdapter
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    private lateinit var filterState: MutableStateFlow<GradeFilter>
    private val sortState = MutableStateFlow(GradeSort.BY_DATE_DESC)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disciplineId = arguments?.getLong("disciplineId", -1L) ?: -1L
        filterState = MutableStateFlow(GradeFilter(disciplineId = disciplineId))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDisciplineGradesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.headerBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnDeleteDiscipline.setOnClickListener { confirmDeleteDiscipline() }
        binding.tvGradesInfo.setOnClickListener { openFilterDialog() }

        adapter = GradesAdapter(
            onEditClick = { grade ->
                val args = bundleOf(
                    "disciplineId" to disciplineId,
                    "gradeId" to grade.id
                )
                findNavController().navigate(
                    R.id.action_disciplineGradesFragment_to_addEditGradeFragment,
                    args
                )
            },
            onDeleteClick = { grade -> confirmDeleteGrade(grade) }
        )
        binding.rvGrades.adapter = adapter

        binding.btnAddGradeBottom.setOnClickListener {
            val args = bundleOf(
                "disciplineId" to disciplineId,
                "gradeId" to -1L
            )
            findNavController().navigate(
                R.id.action_disciplineGradesFragment_to_addEditGradeFragment,
                args
            )
        }

        loadDisciplineAndGrades()
    }

    private fun openFilterDialog() {
        FilterGradesBottomSheet(
            currentFilter = filterState.value,
            currentSort = sortState.value,
            onApply = { newFilter, newSort ->
                filterState.value = newFilter
                sortState.value = newSort
            }
        ).show(childFragmentManager, "FilterGradesBottomSheet")
    }

    private fun loadDisciplineAndGrades() {
        val db = AppDatabase.getInstance(requireContext().applicationContext)
        viewLifecycleOwner.lifecycleScope.launch {
            val discipline = withContext(Dispatchers.IO) {
                db.disciplineDao().getById(disciplineId)
            }
            currentDiscipline = discipline
            if (discipline != null) {
                binding.tvDisciplineName.text = discipline.name
                binding.tvTeacherName.text = discipline.teacherName.orEmpty()
                binding.tvTeacherName.visibility =
                    if (discipline.teacherName.isNullOrBlank()) View.GONE else View.VISIBLE
            }

            combine(
                db.gradeDao().observeByDiscipline(disciplineId),
                filterState,
                sortState
            ) { allGrades, filter, sort ->
                val filtered = Sprint4Analytics.filterGrades(allGrades, filter)
                Sprint4Analytics.sortGrades(filtered, sort)
            }.collect { grades ->
                currentGradesCount = grades.size
                adapter.submitList(grades)
                binding.tvEmptyGrades.visibility = if (grades.isEmpty()) View.VISIBLE else View.GONE

                val filterHint = if (hasActiveSubFilters()) " (фильтр)" else ""
                val avg = Sprint4Analytics.calculateAverage(grades)

                binding.tvDisciplineAvg.text = if (avg == null) "—" else Sprint4Analytics.formatAverage(avg)
                binding.tvGradesInfo.text = "Средний балл • ${grades.size} оценок$filterHint"
            }
        }
    }

    private fun hasActiveSubFilters(): Boolean {
        val f = filterState.value
        return !f.workType.isNullOrBlank() || f.dateFrom != null || f.dateTo != null
    }

    private fun confirmDeleteDiscipline() {
        val discipline = currentDiscipline ?: return
        val appContext = requireContext().applicationContext
        val message = if (currentGradesCount > 0) {
            "Удалить дисциплину «${discipline.name}» вместе со всеми её оценками ($currentGradesCount шт.)?"
        } else {
            "Удалить дисциплину «${discipline.name}»?"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Удаление дисциплины")
            .setMessage(message)
            .setPositiveButton("Удалить") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    AppDatabase.getInstance(appContext)
                        .disciplineDao()
                        .deleteById(discipline.id)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(appContext, "Дисциплина удалена", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun confirmDeleteGrade(grade: Grade) {
        val appContext = requireContext().applicationContext
        val formattedDate = dateFormatter.format(Date(grade.date))
        AlertDialog.Builder(requireContext())
            .setTitle("Удаление оценки")
            .setMessage("Удалить оценку «${grade.value}» (${grade.workType}) от $formattedDate?")
            .setPositiveButton("Удалить") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    AppDatabase.getInstance(appContext).gradeDao().delete(grade)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}