package com.example.studentgrades.ui.grades

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.studentgrades.R
import com.example.studentgrades.databinding.BottomSheetFilterGradesBinding
import com.example.studentgrades.domain.GradeFilter
import com.example.studentgrades.domain.GradeSort
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FilterGradesBottomSheet(
    private val currentFilter: GradeFilter,
    private val currentSort: GradeSort,
    private val onApply: (GradeFilter, GradeSort) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFilterGradesBinding? = null
    private val binding get() = _binding!!

    private var selectedDateFrom: Long? = currentFilter.dateFrom
    private var selectedDateTo: Long? = currentFilter.dateTo
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFilterGradesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        restoreCurrentState()
        setupDatePickers()

        binding.btnApplyFilter.setOnClickListener {
            val workType = when (binding.chipGroupWorkType.checkedChipId) {
                R.id.chipWorkControl -> "Контрольная"
                R.id.chipWorkAnswer -> "Ответ"
                R.id.chipWorkPractical -> "Практическая"
                R.id.chipWorkLab -> "Лабораторная"
                else -> null
            }

            val sortOption = when (binding.chipGroupSort.checkedChipId) {
                R.id.chipSortDateAsc -> GradeSort.BY_DATE_ASC
                R.id.chipSortValueDesc -> GradeSort.BY_VALUE_DESC
                R.id.chipSortValueAsc -> GradeSort.BY_VALUE_ASC
                else -> GradeSort.BY_DATE_DESC
            }

            onApply(
                currentFilter.copy(
                    workType = workType,
                    dateFrom = selectedDateFrom,
                    dateTo = selectedDateTo
                ),
                sortOption
            )
            dismiss()
        }

        binding.btnResetFilter.setOnClickListener {
            onApply(
                GradeFilter(disciplineId = currentFilter.disciplineId),
                GradeSort.BY_DATE_DESC
            )
            dismiss()
        }
    }

    private fun restoreCurrentState() {
        val workChipId = when (currentFilter.workType) {
            "Контрольная" -> R.id.chipWorkControl
            "Ответ" -> R.id.chipWorkAnswer
            "Практическая" -> R.id.chipWorkPractical
            "Лабораторная" -> R.id.chipWorkLab
            else -> R.id.chipWorkAll
        }
        binding.chipGroupWorkType.check(workChipId)

        val sortChipId = when (currentSort) {
            GradeSort.BY_DATE_ASC -> R.id.chipSortDateAsc
            GradeSort.BY_VALUE_DESC -> R.id.chipSortValueDesc
            GradeSort.BY_VALUE_ASC -> R.id.chipSortValueAsc
            GradeSort.BY_DATE_DESC -> R.id.chipSortDateDesc
        }
        binding.chipGroupSort.check(sortChipId)

        selectedDateFrom?.let { binding.etDateFrom.setText(dateFormatter.format(Date(it))) }
        selectedDateTo?.let { binding.etDateTo.setText(dateFormatter.format(Date(it))) }
    }

    private fun setupDatePickers() {
        binding.etDateFrom.setOnClickListener {
            showDatePicker(selectedDateFrom, isEndOfDay = false) { timestamp ->
                selectedDateFrom = timestamp
                binding.etDateFrom.setText(dateFormatter.format(Date(timestamp)))
            }
        }

        binding.etDateTo.setOnClickListener {
            showDatePicker(selectedDateTo, isEndOfDay = true) { timestamp ->
                selectedDateTo = timestamp
                binding.etDateTo.setText(dateFormatter.format(Date(timestamp)))
            }
        }
    }

    private fun showDatePicker(
        initialTimestamp: Long?,
        isEndOfDay: Boolean,
        onDateSelected: (Long) -> Unit
    ) {
        val calendar = Calendar.getInstance().apply {
            if (initialTimestamp != null) timeInMillis = initialTimestamp
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val resultCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
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
                onDateSelected(resultCal.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}