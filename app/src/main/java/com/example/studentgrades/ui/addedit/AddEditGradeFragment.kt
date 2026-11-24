package com.example.studentgrades.ui.addedit

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.studentgrades.R
import com.example.studentgrades.data.AppDatabase
import com.example.studentgrades.data.model.Discipline
import com.example.studentgrades.data.model.Grade
import com.example.studentgrades.databinding.FragmentAddEditGradeBinding
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddEditGradeFragment : Fragment() {

    private var _binding: FragmentAddEditGradeBinding? = null
    private val binding get() = _binding!!

    private var preselectedDisciplineId: Long = -1L
    private var editingGradeId: Long = -1L
    private var disciplinesList: List<Discipline> = emptyList()
    private var selectedWorkType: String = "Контрольная работа"
    private var selectedDateMillis: Long = System.currentTimeMillis()
    private val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preselectedDisciplineId = arguments?.getLong("disciplineId", -1L) ?: -1L
        editingGradeId = arguments?.getLong("gradeId", -1L) ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditGradeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.headerBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnCancel.setOnClickListener { findNavController().popBackStack() }

        setupGradeInputAndQuickButtons()
        setupWorkTypeButtons()
        setupDatePicker()
        loadInitialData()

        binding.btnSaveGrade.setOnClickListener { validateAndSave() }
    }

    private fun setupGradeInputAndQuickButtons() {
        binding.etGradeValue.doAfterTextChanged {
            binding.tvGradeError.visibility = View.GONE
            binding.etGradeValue.setBackgroundResource(R.drawable.bg_input_rounded)
        }

        val quickMap = mapOf(
            binding.btnGrade2 to "2",
            binding.btnGrade3 to "3",
            binding.btnGrade4 to "4",
            binding.btnGrade5 to "5"
        )
        quickMap.forEach { (btn, value) ->
            btn.setOnClickListener {
                binding.etGradeValue.setText(value)
                binding.etGradeValue.setSelection(value.length)
                binding.tvGradeError.visibility = View.GONE
                binding.etGradeValue.setBackgroundResource(R.drawable.bg_input_rounded)
            }
        }
    }

    private fun setupWorkTypeButtons() {
        val buttons = getWorkTypeButtons()
        buttons.forEach { (button, typeName) ->
            button.setOnClickListener {
                selectedWorkType = typeName
                updateWorkTypeSelection(buttons)
            }
        }
        updateWorkTypeSelection(buttons)
    }

    private fun getWorkTypeButtons(): List<Pair<MaterialButton, String>> = listOf(
        binding.btnTypeControl to "Контрольная работа",
        binding.btnTypePractice to "Практическая работа",
        binding.btnTypeLab to "Лабораторная работа",
        binding.btnTypeAnswer to "Ответ на занятии"
    )

    private fun updateWorkTypeSelection(buttons: List<Pair<MaterialButton, String>>) {
        val ctx = requireContext()
        val selectedBg = ContextCompat.getColor(ctx, R.color.primary_blue_light)
        val selectedText = ContextCompat.getColor(ctx, R.color.primary_blue)
        val normalBg = ContextCompat.getColor(ctx, R.color.bg_card)
        val normalText = ContextCompat.getColor(ctx, R.color.text_secondary)

        buttons.forEach { (button, typeName) ->
            val isSelected = typeName.equals(selectedWorkType, ignoreCase = true) ||
                    (typeName.startsWith("Контрольная") && selectedWorkType.startsWith("Контрольная")) ||
                    (typeName.startsWith("Практическая") && selectedWorkType.startsWith("Практическая")) ||
                    (typeName.startsWith("Лабораторная") && selectedWorkType.startsWith("Лабораторная")) ||
                    (typeName.startsWith("Ответ") && selectedWorkType.startsWith("Ответ"))

            button.backgroundTintList = ColorStateList.valueOf(if (isSelected) selectedBg else normalBg)
            button.setTextColor(if (isSelected) selectedText else normalText)
            button.strokeColor = ColorStateList.valueOf(
                ContextCompat.getColor(ctx, if (isSelected) R.color.primary_blue else R.color.stroke_subtle)
            )
        }
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        selectedDateMillis = calendar.timeInMillis
        binding.etDate.setText(formatter.format(Date(selectedDateMillis)))

        binding.etDate.setOnClickListener {
            calendar.timeInMillis = selectedDateMillis
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDateMillis = calendar.timeInMillis
                    binding.etDate.setText(formatter.format(Date(selectedDateMillis)))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun loadInitialData() {
        val db = AppDatabase.getInstance(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            disciplinesList = withContext(Dispatchers.IO) {
                db.disciplineDao().getAll()
            }

            if (disciplinesList.isEmpty()) {
                Toast.makeText(requireContext(), "Сначала добавьте дисциплину", Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
                return@launch
            }

            val spinnerAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                disciplinesList.map { it.name }
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.spinnerDiscipline.adapter = spinnerAdapter

            if (editingGradeId != -1L) {
                binding.tvFormTitle.text = "Редактировать оценку"
                val grade = withContext(Dispatchers.IO) {
                    db.gradeDao().getById(editingGradeId)
                }
                if (grade != null) {
                    binding.etGradeValue.setText(grade.value.toString())
                    selectedDateMillis = grade.date
                    binding.etDate.setText(formatter.format(Date(selectedDateMillis)))
                    binding.etComment.setText(grade.comment.orEmpty())
                    selectedWorkType = grade.workType
                    updateWorkTypeSelection(getWorkTypeButtons())

                    val discIndex = disciplinesList.indexOfFirst { it.id == grade.disciplineId }
                    if (discIndex >= 0) binding.spinnerDiscipline.setSelection(discIndex)
                }
            } else if (preselectedDisciplineId != -1L) {
                val discIndex = disciplinesList.indexOfFirst { it.id == preselectedDisciplineId }
                if (discIndex >= 0) binding.spinnerDiscipline.setSelection(discIndex)
            }
        }
    }

    private fun validateAndSave() {
        binding.tvGradeError.visibility = View.GONE
        binding.etGradeValue.setBackgroundResource(R.drawable.bg_input_rounded)

        val valueText = binding.etGradeValue.text?.toString()?.trim().orEmpty()
        val gradeValue = valueText.toIntOrNull()

        if (gradeValue == null || gradeValue !in 2..5) {
            binding.tvGradeError.visibility = View.VISIBLE
            binding.etGradeValue.setBackgroundResource(R.drawable.bg_input_error)
            return
        }

        val selectedDiscipline = disciplinesList[binding.spinnerDiscipline.selectedItemPosition]
        val commentText = binding.etComment.text?.toString()?.trim()?.ifEmpty { null }

        val gradeToSave = if (editingGradeId != -1L) {
            Grade(
                id = editingGradeId,
                disciplineId = selectedDiscipline.id,
                value = gradeValue,
                date = selectedDateMillis,
                workType = selectedWorkType,
                comment = commentText
            )
        } else {
            Grade(
                disciplineId = selectedDiscipline.id,
                value = gradeValue,
                date = selectedDateMillis,
                workType = selectedWorkType,
                comment = commentText
            )
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val gradeDao = AppDatabase.getInstance(requireContext()).gradeDao()
            if (editingGradeId != -1L) {
                gradeDao.update(gradeToSave)
            } else {
                gradeDao.insert(gradeToSave)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Оценка сохранена", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}