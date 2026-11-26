package com.example.studentgrades.ui.disciplines

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.studentgrades.R
import com.example.studentgrades.data.AppDatabase
import com.example.studentgrades.databinding.FragmentDisciplinesBinding
import com.example.studentgrades.ui.home.DisciplineUiItem
import com.example.studentgrades.ui.home.DisciplinesAdapter
import kotlinx.coroutines.launch

class DisciplinesFragment : Fragment() {

    private var _binding: FragmentDisciplinesBinding? = null
    private val binding get() = _binding!!

    // Создание разметки экрана из XML
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDisciplinesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Настройка списка и переход к оценкам при нажатии на предмет
        val adapter = DisciplinesAdapter { discipline ->
            val args = bundleOf("disciplineId" to discipline.id)
            findNavController().navigate(R.id.disciplineGradesFragment, args)
        }
        binding.rvDisciplines.adapter = adapter

        val db = AppDatabase.getInstance(requireContext().applicationContext)

        // Подписка на обновления дисциплин и оценок из БД
        viewLifecycleOwner.lifecycleScope.launch {
            db.disciplineDao().observeAllWithGrades().collect { items ->
                val uiItems = items.map { dwg ->
                    val grades = dwg.grades
                    val avg = if (grades.isEmpty()) null else grades.map { it.value }.average()
                    DisciplineUiItem(dwg.discipline, grades.size, avg)
                }

                // Показываем заглушку, если предметов нет, иначе — обновляем список
                if (uiItems.isEmpty()) {
                    binding.tvEmptyDisciplines.visibility = View.VISIBLE
                    binding.rvDisciplines.visibility = View.GONE
                } else {
                    binding.tvEmptyDisciplines.visibility = View.GONE
                    binding.rvDisciplines.visibility = View.VISIBLE
                    adapter.submitList(uiItems)
                }
            }
        }
    }

    // Очистка binding при закрытии экрана
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}