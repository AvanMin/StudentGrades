package com.example.studentgrades.ui.home

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.studentgrades.R
import com.example.studentgrades.data.AppDatabase
import com.example.studentgrades.data.model.Discipline
import com.example.studentgrades.databinding.FragmentFilterBinding
import com.example.studentgrades.domain.GradeSort
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FilterFragment : Fragment() {

    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!

    private var disciplines: List<Discipline> = emptyList()
    private var selectedDisciplineId: Long? = null
    private var selectedWorkType: String? = null
    private var selectedDateFrom: Long? = null
    private var selectedDateTo: Long? = null
    private var selectedSort: GradeSort = GradeSort.BY_DATE_DESC

    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Восстанавливаем текущие переданные фильтры
        arguments?.let { args ->
            val dId = args.getLong("disciplineId", -1L)
            selectedDisciplineId = if (dId == -1L) null else dId
            selectedWorkType = args.getString("workType")
            val from = args.getLong("dateFrom", -1L)
            selectedDateFrom = if (from == -1L) null else from
            val to = args.getLong("dateTo", -1L)
            selectedDateTo = if (to == -1L) null else to
            val sortName = args.getString("sort", GradeSort.BY_DATE_DESC.name)
            selectedSort = GradeSort.valueOf(sortName)
        }

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        loadDisciplines()
        setupWorkTypeButtons()
        setupDatePickers()
        setupSortButtons()
        updateUiState()

        binding.btnApply.setOnClickListener {
            val result = Bundle().apply {
                putLong("disciplineId", selectedDisciplineId ?: -1L)
                putString("workType", selectedWorkType)
                putLong("dateFrom", selectedDateFrom ?: -1L)
                putLong("dateTo", selectedDateTo ?: -1L)
                putString("sort", selectedSort.name)
            }
            findNavController().previousBackStackEntry
                ?.savedStateHandle
                ?.set("filter_result", result)
            findNavController().popBackStack()
        }

        binding.btnReset.setOnClickListener {
            selectedDisciplineId = null
            selectedWorkType = null
            selectedDateFrom = null
            selectedDateTo = null
            selectedSort = GradeSort.BY_DATE_DESC
            binding.tvSelectedDiscipline.text = "Все дисциплины"
            binding.tvDateFrom.text = ""
            binding.tvDateTo.text = ""
            updateUiState()
        }
    }

    private fun loadDisciplines() {
        val db = AppDatabase.getInstance(requireContext().applicationContext)
        viewLifecycleOwner.lifecycleScope.launch {
            disciplines = withContext(Dispatchers.IO) { db.disciplineDao().getAll() }
            val current = disciplines.find { it.id == selectedDisciplineId }
            binding.tvSelectedDiscipline.text = current?.name ?: "Все дисциплины"

            binding.cardDiscipline.setOnClickListener {
                val names = arrayOf("Все дисциплины") + disciplines.map { it.name }.toTypedArray()
                AlertDialog.Builder(requireContext())
                    .setTitle("Выберите дисциплину")
                    .setItems(names) { _, which ->
                        if (which == 0) {
                            selectedDisciplineId = null
                            binding.tvSelectedDiscipline.text = "Все дисциплины"
                        } else {
                            val chosen = disciplines[which - 1]
                            selectedDisciplineId = chosen.id
                            binding.tvSelectedDiscipline.text = chosen.name
                        }
                    }
                    .show()
            }
        }
    }

    private fun setupWorkTypeButtons() {
        binding.btnTypeAll.setOnClickListener { selectedWorkType = null; updateUiState() }
        binding.btnTypeControl.setOnClickListener { selectedWorkType = "Контрольная"; updateUiState() }
        binding.btnTypeAnswer.setOnClickListener { selectedWorkType = "Ответ"; updateUiState() }
        binding.btnTypePractical.setOnClickListener { selectedWorkType = "Практическая"; updateUiState() }
        binding.btnTypeLab.setOnClickListener { selectedWorkType = "Лабораторная"; updateUiState() }
    }

    private fun setupDatePickers() {
        selectedDateFrom?.let { binding.tvDateFrom.text = dateFormatter.format(Date(it)) }
        selectedDateTo?.let { binding.tvDateTo.text = dateFormatter.format(Date(it)) }

        binding.cardDateFrom.setOnClickListener {
            showDatePicker(selectedDateFrom, isEndOfDay = false) { ts ->
                selectedDateFrom = ts
                binding.tvDateFrom.text = dateFormatter.format(Date(ts))
            }
        }

        binding.cardDateTo.setOnClickListener {
            showDatePicker(selectedDateTo, isEndOfDay = true) { ts ->
                selectedDateTo = ts
                binding.tvDateTo.text = dateFormatter.format(Date(ts))
            }
        }
    }

    private fun setupSortButtons() {
        binding.btnSortByDate.setOnClickListener {
            selectedSort = if (selectedSort == GradeSort.BY_DATE_DESC) {
                GradeSort.BY_DATE_ASC
            } else {
                GradeSort.BY_DATE_DESC
            }
            updateUiState()
        }

        binding.btnSortByValue.setOnClickListener {
            selectedSort = if (selectedSort == GradeSort.BY_VALUE_DESC) {
                GradeSort.BY_VALUE_ASC
            } else {
                GradeSort.BY_VALUE_DESC
            }
            updateUiState()
        }
    }

    private fun updateUiState() {
        stylePillButton(binding.btnTypeAll, selectedWorkType == null)
        stylePillButton(binding.btnTypeControl, selectedWorkType == "Контрольная")
        stylePillButton(binding.btnTypeAnswer, selectedWorkType == "Ответ")
        stylePillButton(binding.btnTypePractical, selectedWorkType == "Практическая")
        stylePillButton(binding.btnTypeLab, selectedWorkType == "Лабораторная")

        val isDateSort = selectedSort == GradeSort.BY_DATE_DESC || selectedSort == GradeSort.BY_DATE_ASC
        stylePillButton(binding.btnSortByDate, isDateSort)
        stylePillButton(binding.btnSortByValue, !isDateSort)

        binding.tvSortHint.text = when (selectedSort) {
            GradeSort.BY_DATE_DESC -> "Новые оценки сначала"
            GradeSort.BY_DATE_ASC -> "Старые оценки сначала"
            GradeSort.BY_VALUE_DESC -> "Высокие оценки сначала"
            GradeSort.BY_VALUE_ASC -> "Низкие оценки сначала"
        }
    }

    private fun stylePillButton(button: MaterialButton, isSelected: Boolean) {
        val ctx = requireContext()
        if (isSelected) {
            // Светло-голубой фон и синий текст без рамки (как активная пилюля на макете)
            button.backgroundTintList = ColorStateList.valueOf(0xFFE8F0FE.toInt())
            button.setTextColor(ContextCompat.getColor(ctx, R.color.primary_blue))
            button.strokeWidth = 0
        } else {
            // Белый фон с тонкой рамкой
            button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.bg_card))
            button.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
            button.strokeWidth = (1 * resources.displayMetrics.density).toInt()
            button.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.stroke_subtle))
        }
    }

    private fun showDatePicker(initialTs: Long?, isEndOfDay: Boolean, onSelected: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply {
            if (initialTs != null) timeInMillis = initialTs
        }
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val res = Calendar.getInstance().apply {
                    set(y, m, d)
                    if (isEndOfDay) {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    } else {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                }
                onSelected(res.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}