package com.example.studentgrades.ui.goal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.studentgrades.data.AppDatabase
import com.example.studentgrades.databinding.FragmentGoalBinding
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.ceil

class GoalFragment : Fragment() {

    private var _binding: FragmentGoalBinding? = null
    private val binding get() = _binding!!

    private var currentCount: Int = 0
    private var currentSum: Int = 0
    private var currentAvg: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnCalculate.setOnClickListener {
            calculateGoal()
        }

        observeGrades()
    }

    private fun observeGrades() {
        val db = AppDatabase.getInstance(requireContext().applicationContext)
        viewLifecycleOwner.lifecycleScope.launch {
            db.gradeDao().observeAll().collect { grades ->
                currentCount = grades.size
                currentSum = grades.sumOf { it.value }
                currentAvg = if (currentCount == 0) 0.0 else currentSum.toDouble() / currentCount

                binding.tvCurrentAvg.text = if (currentCount == 0) "—"
                else String.format(Locale.US, "%.2f", currentAvg)
                binding.tvGradesCount.text = formatGradesWord(currentCount)
                binding.tvGradesSum.text = "Сумма: $currentSum"

                // Автоматический расчёт при открытии экрана, если оценки уже есть
                if (currentCount > 0 && binding.etTargetAvg.text.isNotBlank()) {
                    calculateGoal()
                }
            }
        }
    }

    private fun calculateGoal() {
        val rawInput = binding.etTargetAvg.text.toString().replace(',', '.').trim()
        val target = rawInput.toDoubleOrNull()

        if (target == null || target < 1.0 || target > 5.0) {
            Toast.makeText(
                requireContext(),
                "Введите желаемый балл от 1.00 до 5.00",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Формула для одной следующей оценки: (currentSum + x) / (currentCount + 1) = target
        val exactNeeded = target * (currentCount + 1) - currentSum
        val maxPossibleAvg = (currentSum + 5.0) / (currentCount + 1)

        val targetFormatted = String.format(Locale.US, "%.2f", target)
        val maxPossibleFormatted = String.format(Locale.US, "%.2f", maxPossibleAvg)

        when {
            // Случай 1: Цель недостижима за 1 работу (нужна оценка больше 5)
            exactNeeded > 5.0001 -> {
                val exactNeededFormatted = String.format(Locale.US, "%.2f", exactNeeded)

                binding.cardStatusAlert.visibility = View.VISIBLE
                binding.cardStatusAlert.setCardBackgroundColor(0xFFFDECEA.toInt())
                binding.tvStatusTitle.setTextColor(0xFFC5221F.toInt())
                binding.tvStatusDescription.setTextColor(0xFFC5221F.toInt())
                binding.tvStatusTitle.text = "Цель недостижима"
                binding.tvStatusDescription.text =
                    "Для $targetFormatted нужна оценка $exactNeededFormatted.\n" +
                            "Максимум за одну работу — 5.\n" +
                            "Средний балл станет не выше $maxPossibleFormatted."

                binding.cardNeededGrade.visibility = View.VISIBLE
                binding.tvNeededSubtitleTop.text = "Если снизить цель до $maxPossibleFormatted"
                binding.tvNeededGradeValue.text = "Нужна оценка: 5"
            }

            // Случай 2: Цель уже достигнута (достаточно даже минимальной оценки)
            exactNeeded <= 1.0 -> {
                binding.cardStatusAlert.visibility = View.VISIBLE
                binding.cardStatusAlert.setCardBackgroundColor(0xFFE6F4EA.toInt())
                binding.tvStatusTitle.setTextColor(0xFF137333.toInt())
                binding.tvStatusDescription.setTextColor(0xFF137333.toInt())
                binding.tvStatusTitle.text = "Цель уже достигнута!"
                binding.tvStatusDescription.text =
                    "Ваш текущий средний балл уже позволяет удерживать $targetFormatted."

                binding.cardNeededGrade.visibility = View.GONE
            }

            // Случай 3: Цель достижима (нужна оценка от 2 до 5)
            else -> {
                val requiredIntGrade = ceil(exactNeeded - 0.0001).toInt().coerceIn(2, 5)
                val resultingAvg = (currentSum + requiredIntGrade).toDouble() / (currentCount + 1)
                val resultingFormatted = String.format(Locale.US, "%.2f", resultingAvg)

                binding.cardStatusAlert.visibility = View.GONE
                binding.cardNeededGrade.visibility = View.VISIBLE
                binding.tvNeededSubtitleTop.text =
                    "Для достижения $targetFormatted (итог будет $resultingFormatted)"
                binding.tvNeededGradeValue.text = "Нужна оценка: $requiredIntGrade"
            }
        }
    }

    private fun formatGradesWord(count: Int): String {
        val mod10 = count % 10
        val mod100 = count % 100
        val word = when {
            mod100 in 11..14 -> "оценок"
            mod10 == 1 -> "оценка"
            mod10 in 2..4 -> "оценки"
            else -> "оценок"
        }
        return "$count $word"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}