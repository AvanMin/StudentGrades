package com.example.studentgrades.ui.dynamics

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.studentgrades.data.AppDatabase
import com.example.studentgrades.databinding.FragmentDynamicsBinding
import com.example.studentgrades.domain.PeriodAverage
import com.example.studentgrades.domain.Sprint4Analytics
import kotlinx.coroutines.launch
import java.util.Locale

class DynamicsFragment : Fragment() {

    private var _binding: FragmentDynamicsBinding? = null
    private val binding get() = _binding!!

    // Создание разметки экрана из XML
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDynamicsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getInstance(requireContext().applicationContext)

        // Подписка на поток данных из БД и пересчёт динамики при изменении оценок
        viewLifecycleOwner.lifecycleScope.launch {
            db.disciplineDao().observeAllWithGrades().collect { items ->
                val allGrades = items.flatMap { it.grades }
                val periods = Sprint4Analytics.calculatePeriodAverages(allGrades)

                // Показываем заглушку, если оценок ещё нет, и скрываем блок аналитики
                if (periods.isEmpty()) {
                    binding.tvEmptyDynamics.visibility = View.VISIBLE
                    binding.layoutDynamicsContent.visibility = View.GONE
                    return@collect
                }

                binding.tvEmptyDynamics.visibility = View.GONE
                binding.layoutDynamicsContent.visibility = View.VISIBLE

                // Расчёт и вывод общего тренда (рост / снижение / без изменений)
                val trend = Sprint4Analytics.calculateTrend(periods)
                binding.tvTrendTitle.text = Sprint4Analytics.trendText(trend)
                binding.tvTrendDelta.text = Sprint4Analytics.formatTrend(
                    trend = trend,
                    firstPeriodLabel = periods.firstOrNull()?.periodLabel
                )

                // Подбор цвета индикатора тренда (зелёный — рост, красный — спад, синий — стабильно)
                val deltaColor = when {
                    trend == null -> Color.parseColor("#6B7280")
                    trend > 0.001 -> Color.parseColor("#16A34A")
                    trend < -0.001 -> Color.parseColor("#DC2626")
                    else -> Color.parseColor("#2563EB")
                }
                binding.tvTrendDelta.setTextColor(deltaColor)

                // Отрисовка списка месяцев со средними баллами
                renderPeriods(periods)
            }
        }
    }

    // Динамическое создание строк по месяцам внутри контейнера containerPeriods
    private fun renderPeriods(periods: List<PeriodAverage>) {
        val container = binding.containerPeriods
        container.removeAllViews()
        val ctx = requireContext()
        val density = resources.displayMetrics.density

        periods.forEachIndexed { index, period ->
            // Горизонтальная строка для одного месяца
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val vPad = (10 * density).toInt()
                setPadding(0, vPad, 0, vPad)
            }

            // Левая колонка: название месяца с годом и количество оценок
            val leftCol = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val tvMonth = TextView(ctx).apply {
                text = "${period.periodLabel} ${period.year}"
                setTextColor(Color.parseColor("#111827"))
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
            }

            val tvCount = TextView(ctx).apply {
                text = formatGradesCount(period.gradeCount)
                setTextColor(Color.parseColor("#6B7280"))
                textSize = 12f
            }

            leftCol.addView(tvMonth)
            leftCol.addView(tvCount)

            // Правая часть строки: средний балл за месяц
            val tvAvg = TextView(ctx).apply {
                text = String.format(Locale.US, "%.2f", period.average)
                setTextColor(Color.parseColor("#2563EB"))
                textSize = 17f
                setTypeface(null, Typeface.BOLD)
            }

            row.addView(leftCol)
            row.addView(tvAvg)
            container.addView(row)

            // Тонкая линия-разделитель между строками месяцев
            if (index < periods.lastIndex) {
                val divider = View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (1 * density).toInt().coerceAtLeast(1)
                    )
                    setBackgroundColor(Color.parseColor("#E5E7EB"))
                }
                container.addView(divider)
            }
        }
    }

    // Склонение слова «оценка» в зависимости от числа
    private fun formatGradesCount(count: Int): String {
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

    // Очистка binding при уничтожении View экрана
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}