package com.example.studentgrades.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studentgrades.R
import com.example.studentgrades.data.model.Discipline
import com.example.studentgrades.databinding.ItemDisciplineBinding
import java.util.Locale

// UI-модель карточки дисциплины: сам предмет, число оценок и средний балл
data class DisciplineUiItem(
    val discipline: Discipline,
    val gradesCount: Int,
    val average: Double?
)

// Адаптер для отображения списка карточек дисциплин с обработчиком нажатия на предмет
class DisciplinesAdapter(
    private val onClick: (Discipline) -> Unit
) : ListAdapter<DisciplineUiItem, DisciplinesAdapter.ViewHolder>(DiffCallback) {

    // Создание разметки одной карточки дисциплины из XML
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDisciplineBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    // Привязка данных дисциплины к карточке по её позиции в списке
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Контейнер элементов одной карточки дисциплины
    inner class ViewHolder(private val binding: ItemDisciplineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Заполнение карточки: название предмета, преподаватель, число оценок, средний балл и цвет индикатора
        fun bind(item: DisciplineUiItem) {
            val ctx = binding.root.context
            binding.tvDisciplineName.text = item.discipline.name

            // Формирование подстроки с количеством оценок и именем преподавателя (если оно указано)
            val teacherPart = if (item.discipline.teacherName.isNullOrBlank()) ""
            else " • ${item.discipline.teacherName}"
            binding.tvDisciplineSub.text = "${item.gradesCount} оценок$teacherPart"

            // Вывод среднего балла и выбор цвета текста и боковой полоски-индикатора
            val avg = item.average
            if (avg == null) {
                binding.tvDisciplineAvg.text = "—"
                val color = ContextCompat.getColor(ctx, R.color.text_secondary)
                binding.tvDisciplineAvg.setTextColor(color)
                binding.viewIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            } else {
                binding.tvDisciplineAvg.text = String.format(Locale.US, "%.2f", avg)
                val colorRes = when {
                    avg >= 4.5 -> R.color.grade_5_green
                    avg >= 4.0 -> R.color.grade_4_blue
                    else -> R.color.grade_3_orange
                }
                val color = ContextCompat.getColor(ctx, colorRes)
                binding.tvDisciplineAvg.setTextColor(color)
                binding.viewIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            }

            // Передача выбранной дисциплины в обработчик при нажатии на карточку
            binding.root.setOnClickListener { onClick(item.discipline) }
        }
    }

    // Сравнение элементов списка для плавного и эффективного обновления через DiffUtil
    companion object DiffCallback : DiffUtil.ItemCallback<DisciplineUiItem>() {
        override fun areItemsTheSame(oldItem: DisciplineUiItem, newItem: DisciplineUiItem) =
            oldItem.discipline.id == newItem.discipline.id

        override fun areContentsTheSame(oldItem: DisciplineUiItem, newItem: DisciplineUiItem) =
            oldItem == newItem
    }
}