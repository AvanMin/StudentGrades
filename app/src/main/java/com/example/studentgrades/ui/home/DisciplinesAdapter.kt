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

data class DisciplineUiItem(
    val discipline: Discipline,
    val gradesCount: Int,
    val average: Double?
)

class DisciplinesAdapter(
    private val onClick: (Discipline) -> Unit
) : ListAdapter<DisciplineUiItem, DisciplinesAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDisciplineBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemDisciplineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DisciplineUiItem) {
            val ctx = binding.root.context
            binding.tvDisciplineName.text = item.discipline.name

            val teacherPart = if (item.discipline.teacherName.isNullOrBlank()) ""
            else " • ${item.discipline.teacherName}"
            binding.tvDisciplineSub.text = "${item.gradesCount} оценок$teacherPart"

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

            binding.root.setOnClickListener { onClick(item.discipline) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<DisciplineUiItem>() {
        override fun areItemsTheSame(oldItem: DisciplineUiItem, newItem: DisciplineUiItem) =
            oldItem.discipline.id == newItem.discipline.id

        override fun areContentsTheSame(oldItem: DisciplineUiItem, newItem: DisciplineUiItem) =
            oldItem == newItem
    }
}