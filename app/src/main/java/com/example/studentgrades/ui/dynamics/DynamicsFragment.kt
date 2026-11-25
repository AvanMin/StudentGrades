package com.example.studentgrades.ui.dynamics

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.studentgrades.data.AppDatabase
import com.example.studentgrades.databinding.FragmentDynamicsBinding
import com.example.studentgrades.domain.Sprint4Analytics
import kotlinx.coroutines.launch
import java.util.Locale

class DynamicsFragment : Fragment() {

    private var _binding: FragmentDynamicsBinding? = null
    private val binding get() = _binding!!

    private val ruLocale = Locale.Builder().setLanguage("ru").build()

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

        viewLifecycleOwner.lifecycleScope.launch {
            db.gradeDao().observeAll().collect { grades ->
                val periods = Sprint4Analytics.calculatePeriodAverages(grades)

                if (periods.isEmpty()) {
                    binding.tvEmptyDynamics.visibility = View.VISIBLE
                    binding.layoutDynamicsContent.visibility = View.GONE
                    return@collect
                }

                binding.tvEmptyDynamics.visibility = View.GONE
                binding.layoutDynamicsContent.visibility = View.VISIBLE

                val trend = Sprint4Analytics.calculateTrend(periods)
                val firstLabel = periods.firstOrNull()?.periodLabel
                binding.tvTrendTitle.text = Sprint4Analytics.trendText(trend)
                binding.tvTrendDelta.text = Sprint4Analytics.formatTrend(trend, firstLabel)

                binding.containerPeriods.removeAllViews()
                periods.forEachIndexed { index, period ->
                    val delta = if (index == 0) null else period.average - periods[index - 1].average
                    val deltaText = delta?.let { String.format(ruLocale, " (%+.2f)", it) } ?: ""

                    val row = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(8, 16, 8, 16)
                    }

                    val tvMonth = TextView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f)
                        text = "${period.periodLabel} ${period.year}"
                        textSize = 15f
                    }

                    val tvCount = TextView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        text = "${period.gradeCount} оц."
                        textSize = 14f
                    }

                    val tvAvg = TextView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f)
                        text = String.format(ruLocale, "%.2f%s", period.average, deltaText)
                        textSize = 15f
                        setTypeface(null, Typeface.BOLD)
                    }

                    row.addView(tvMonth)
                    row.addView(tvCount)
                    row.addView(tvAvg)
                    binding.containerPeriods.addView(row)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}