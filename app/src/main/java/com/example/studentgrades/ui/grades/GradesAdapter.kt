package com.example.studentgrades.ui.grades

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studentgrades.R
import com.example.studentgrades.data.model.Grade
import com.example.studentgrades.databinding.ItemGradeBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GradesAdapter(
    private val onEditClick: (Grade) -> Unit,
    private val onDeleteClick: (Grade) -> Unit
) : ListAdapter<Grade, GradesAdapter.ViewHolder>(DiffCallback) {

    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGradeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemGradeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Grade) {
            val ctx = binding.root.context
            binding.tvGradeBadge.text = item.value.toString()
            binding.tvWorkType.text = item.workType
            binding.tvGradeDate.text = dateFormatter.format(Date(item.date))

            val (textColorRes, bgColorRes) = when (item.value) {
                5 -> R.color.grade_5_green to R.color.grade_5_bg
                4 -> R.color.grade_4_blue to R.color.grade_4_bg
                3 -> R.color.grade_3_orange to R.color.grade_3_bg
                else -> R.color.grade_2_red to R.color.grade_2_bg
            }
            binding.tvGradeBadge.setTextColor(ContextCompat.getColor(ctx, textColorRes))
            binding.tvGradeBadge.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(ctx, bgColorRes))

            if (item.comment.isNullOrBlank()) {
                binding.tvGradeComment.visibility = View.GONE
            } else {
                binding.tvGradeComment.visibility = View.VISIBLE
                binding.tvGradeComment.text = item.comment
            }

            binding.btnEditGrade.setOnClickListener { onEditClick(item) }
            binding.btnDeleteGrade.setOnClickListener { onDeleteClick(item) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Grade>() {
        override fun areItemsTheSame(oldItem: Grade, newItem: Grade) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Grade, newItem: Grade) = oldItem == newItem
    }
}