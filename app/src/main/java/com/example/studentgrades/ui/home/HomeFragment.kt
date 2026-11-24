package com.example.studentgrades.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.studentgrades.R
import com.example.studentgrades.data.AppDatabase
import com.example.studentgrades.data.model.Discipline
import com.example.studentgrades.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: DisciplinesAdapter
    private var hasDisciplines: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

    private fun observeData() {
        val db = AppDatabase.getInstance(requireContext().applicationContext)
        viewLifecycleOwner.lifecycleScope.launch {
            db.disciplineDao().observeAllWithGrades().collect { items ->
                hasDisciplines = items.isNotEmpty()

                val uiItems = items.map { dwg ->
                    val grades = dwg.grades
                    val avg = if (grades.isEmpty()) null else grades.map { it.value }.average()
                    DisciplineUiItem(dwg.discipline, grades.size, avg)
                }

                adapter.submitList(uiItems)
                binding.tvEmptyHome.visibility = if (uiItems.isEmpty()) View.VISIBLE else View.GONE
                binding.tvDisciplinesCount.text = "Дисциплин: ${uiItems.size}"
                binding.tvSubjectsBadge.text = formatSubjectsCount(uiItems.size)

                val allGrades = items.flatMap { it.grades }
                binding.tvGradesCount.text = "Оценок: ${allGrades.size}"

                binding.tvTotalAvg.text = if (allGrades.isEmpty()) "—"
                else String.format(Locale.US, "%.2f", allGrades.map { it.value }.average())
            }
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