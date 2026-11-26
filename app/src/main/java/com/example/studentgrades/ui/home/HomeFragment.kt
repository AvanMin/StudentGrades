package com.example.studentgrades.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.studentgrades.R
import com.example.studentgrades.data.AppDatabase
import com.example.studentgrades.data.model.Discipline
import com.example.studentgrades.databinding.FragmentHomeBinding
import com.example.studentgrades.domain.GradeFilter
import com.example.studentgrades.domain.GradeSort
import com.example.studentgrades.domain.Sprint4Analytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: DisciplinesAdapter
    private var hasDisciplines: Boolean = false
    private var cachedDisciplines: List<Discipline> = emptyList()

    private val currentFilter = MutableStateFlow(GradeFilter())
    private val currentSort = MutableStateFlow(GradeSort.BY_DATE_DESC)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnFilter.setOnClickListener {
            val f = currentFilter.value
            val args = bundleOf(
                "disciplineId" to (f.disciplineId ?: -1L),
                "workType" to f.workType,
                "dateFrom" to (f.dateFrom ?: -1L),
                "dateTo" to (f.dateTo ?: -1L),
                "sort" to currentSort.value.name
            )
            findNavController().navigate(R.id.action_homeFragment_to_filterFragment, args)
        }

        binding.btnQuickSort.setOnClickListener {
            showQuickSortDialog()
        }

        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Bundle>("filter_result")
            ?.observe(viewLifecycleOwner) { bundle ->
                val dId = bundle.getLong("disciplineId", -1L)
                val from = bundle.getLong("dateFrom", -1L)
                val to = bundle.getLong("dateTo", -1L)
                val sortName = bundle.getString("sort") ?: GradeSort.BY_DATE_DESC.name
                currentSort.value = GradeSort.valueOf(sortName)
                currentFilter.value = GradeFilter(
                    disciplineId = if (dId == -1L) null else dId,
                    workType = bundle.getString("workType"),
                    dateFrom = if (from == -1L) null else from,
                    dateTo = if (to == -1L) null else to
                )
            }

        adapter = DisciplinesAdapter { discipline ->
            val args = bundleOf("disciplineId" to discipline.id)
            findNavController().navigate(
                R.id.action_homeFragment_to_disciplineGradesFragment,
                args
            )
        }
        binding.rvDisciplines.adapter = adapter

        binding.fabAdd.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Что добавить?")
                .setItems(arrayOf("Новую оценку", "Новую дисциплину")) { _, which ->
                    if (which == 0) {
                        if (!hasDisciplines) {
                            Toast.makeText(
                                requireContext(),
                                "Сначала добавьте хотя бы одну дисциплину",
                                Toast.LENGTH_SHORT
                            ).show()
                            showAddDisciplineDialog()
                        } else {
                            findNavController().navigate(R.id.action_homeFragment_to_addEditGradeFragment)
                        }
                    } else {
                        showAddDisciplineDialog()
                    }
                }
                .show()
        }

        observeData()
    }

    private fun showQuickSortDialog() {
        val options = arrayOf(
            "По дате (новые сначала)",
            "По дате (старые сначала)",
            "По баллу (высокие сначала)",
            "По баллу (низкие сначала)",
            "По одной дисциплине..."
        )
        val sortValues = arrayOf(
            GradeSort.BY_DATE_DESC,
            GradeSort.BY_DATE_ASC,
            GradeSort.BY_VALUE_DESC,
            GradeSort.BY_VALUE_ASC
        )

        val checkedIndex = if (currentFilter.value.disciplineId != null) {
            4
        } else {
            sortValues.indexOf(currentSort.value).coerceAtLeast(0)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Сортировка дисциплин")
            .setSingleChoiceItems(options, checkedIndex) { dialog, which ->
                dialog.dismiss()
                if (which == 4) {
                    showSingleDisciplinePickerDialog()
                } else {
                    currentSort.value = sortValues[which]
                    if (currentFilter.value.disciplineId != null) {
                        currentFilter.value = currentFilter.value.copy(disciplineId = null)
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showSingleDisciplinePickerDialog() {
        if (cachedDisciplines.isEmpty()) {
            Toast.makeText(requireContext(), "Список дисциплин пуст", Toast.LENGTH_SHORT).show()
            return
        }

        val names = arrayOf("Все дисциплины") + cachedDisciplines.map { it.name }.toTypedArray()
        val currentId = currentFilter.value.disciplineId
        val checkedIndex = if (currentId == null) {
            0
        } else {
            val idx = cachedDisciplines.indexOfFirst { it.id == currentId }
            if (idx >= 0) idx + 1 else 0
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Выберите дисциплину")
            .setSingleChoiceItems(names, checkedIndex) { dialog, which ->
                val selectedId = if (which == 0) null else cachedDisciplines[which - 1].id
                currentFilter.value = currentFilter.value.copy(disciplineId = selectedId)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun observeData() {
        val db = AppDatabase.getInstance(requireContext().applicationContext)
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                db.disciplineDao().observeAllWithGrades(),
                currentFilter,
                currentSort
            ) { items, filter, sort ->
                Triple(items, filter, sort)
            }.collect { (items, filter, sort) ->
                hasDisciplines = items.isNotEmpty()
                cachedDisciplines = items.map { it.discipline }
                updateFilterAndSortLabels(filter, sort)

                val isFilterActive = filter.disciplineId != null ||
                        !filter.workType.isNullOrBlank() ||
                        filter.dateFrom != null ||
                        filter.dateTo != null

                val filteredItems = items
                    .filter { filter.disciplineId == null || it.discipline.id == filter.disciplineId }
                    .mapNotNull { dwg ->
                        val matchingGrades = Sprint4Analytics.filterGrades(dwg.grades, filter)
                        if (isFilterActive && filter.disciplineId == null && matchingGrades.isEmpty()) {
                            null
                        } else {
                            val sortedGrades = Sprint4Analytics.sortGrades(matchingGrades, sort)
                            val avg = Sprint4Analytics.calculateAverage(sortedGrades)
                            val uiItem = DisciplineUiItem(dwg.discipline, sortedGrades.size, avg)
                            uiItem to sortedGrades
                        }
                    }

                val sortedItems = when (sort) {
                    GradeSort.BY_DATE_DESC -> filteredItems.sortedByDescending { (_, grades) ->
                        grades.maxOfOrNull { it.date } ?: Long.MIN_VALUE
                    }
                    GradeSort.BY_DATE_ASC -> filteredItems.sortedBy { (_, grades) ->
                        grades.minOfOrNull { it.date } ?: Long.MAX_VALUE
                    }
                    GradeSort.BY_VALUE_DESC -> filteredItems.sortedByDescending { (uiItem, _) ->
                        uiItem.average ?: -1.0
                    }
                    GradeSort.BY_VALUE_ASC -> filteredItems.sortedBy { (uiItem, _) ->
                        uiItem.average ?: Double.MAX_VALUE
                    }
                }

                val uiItems = sortedItems.map { it.first }
                val allFilteredGrades = sortedItems.flatMap { it.second }

                adapter.submitList(uiItems)
                binding.tvEmptyHome.visibility = if (uiItems.isEmpty()) View.VISIBLE else View.GONE
                binding.tvDisciplinesCount.text = "Дисциплин: ${uiItems.size}"
                binding.tvSubjectsBadge.text = formatSubjectsCount(uiItems.size)
                binding.tvGradesCount.text = "Оценок: ${allFilteredGrades.size}"

                val totalAvg = Sprint4Analytics.calculateAverage(allFilteredGrades)
                binding.tvTotalAvg.text = if (totalAvg == null) {
                    "—"
                } else {
                    String.format(Locale.US, "%.2f", totalAvg)
                }

                val weightedAvg = Sprint4Analytics.calculateWeightedAverage(allFilteredGrades)
                binding.tvWeightedAvg.text = if (weightedAvg == null) {
                    "—"
                } else {
                    String.format(Locale.US, "%.2f", weightedAvg)
                }
            }
        }
    }

    private fun updateFilterAndSortLabels(filter: GradeFilter, sort: GradeSort) {
        val selectedDiscipline = cachedDisciplines.find { it.id == filter.disciplineId }
        binding.tvQuickSort.text = if (selectedDiscipline != null) {
            "⇅ ${selectedDiscipline.name}"
        } else {
            when (sort) {
                GradeSort.BY_DATE_DESC -> "⇅ По дате ↓"
                GradeSort.BY_DATE_ASC -> "⇅ По дате ↑"
                GradeSort.BY_VALUE_DESC -> "⇅ По баллу ↓"
                GradeSort.BY_VALUE_ASC -> "⇅ По баллу ↑"
            }
        }

        val isFilterActive = filter.disciplineId != null ||
                !filter.workType.isNullOrBlank() ||
                filter.dateFrom != null ||
                filter.dateTo != null
        val ctx = requireContext()
        if (isFilterActive) {
            binding.btnFilter.setCardBackgroundColor(0xFFE8F0FE.toInt())
            binding.btnFilter.strokeColor = ContextCompat.getColor(ctx, R.color.primary_blue)
        } else {
            binding.btnFilter.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.bg_card))
            binding.btnFilter.strokeColor = ContextCompat.getColor(ctx, R.color.stroke_subtle)
        }
    }

    private fun showAddDisciplineDialog() {
        val context = requireContext()
        val appContext = context.applicationContext

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val etName = EditText(context).apply {
            hint = "Название дисциплины *"
        }
        val etTeacher = EditText(context).apply {
            hint = "ФИО преподавателя (необязательно)"
        }
        layout.addView(etName)
        layout.addView(etTeacher)

        AlertDialog.Builder(context)
            .setTitle("Новая дисциплина")
            .setView(layout)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = etName.text.toString().trim()
                val teacher = etTeacher.text.toString().trim().ifEmpty { null }

                if (name.isEmpty()) {
                    Toast.makeText(appContext, "Введите название дисциплины", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        AppDatabase.getInstance(appContext)
                            .disciplineDao()
                            .insert(Discipline(name = name, teacherName = teacher))

                        withContext(Dispatchers.Main) {
                            Toast.makeText(appContext, "Дисциплина «$name» добавлена", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(appContext, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun formatSubjectsCount(count: Int): String {
        val mod10 = count % 10
        val mod100 = count % 100
        val word = when {
            mod100 in 11..14 -> "предметов"
            mod10 == 1 -> "предмет"
            mod10 in 2..4 -> "предмета"
            else -> "предметов"
        }
        return "$count $word"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}