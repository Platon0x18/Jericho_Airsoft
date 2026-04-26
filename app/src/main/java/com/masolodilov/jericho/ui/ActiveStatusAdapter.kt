package com.masolodilov.jericho.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.masolodilov.jericho.R
import com.masolodilov.jericho.databinding.ItemActiveStatusBinding
import com.masolodilov.jericho.model.TrackedStatus

data class ActiveStatusUiState(
    val status: TrackedStatus,
    val showCureButton: Boolean,
    val canCure: Boolean,
)

class ActiveStatusAdapter(
    private val onPauseResume: (TrackedStatus) -> Unit,
    private val onFinish: (TrackedStatus) -> Unit,
    private val onCure: (TrackedStatus) -> Unit,
) : RecyclerView.Adapter<ActiveStatusAdapter.ActiveStatusViewHolder>() {
    private val items = mutableListOf<ActiveStatusUiState>()

    fun submit(statuses: List<ActiveStatusUiState>) {
        items.clear()
        items.addAll(statuses)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveStatusViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ActiveStatusViewHolder(ItemActiveStatusBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ActiveStatusViewHolder, position: Int) {
        holder.bind(items[position], onPauseResume, onFinish, onCure)
    }

    override fun getItemCount(): Int = items.size

    class ActiveStatusViewHolder(
        private val binding: ItemActiveStatusBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: ActiveStatusUiState,
            onPauseResume: (TrackedStatus) -> Unit,
            onFinish: (TrackedStatus) -> Unit,
            onCure: (TrackedStatus) -> Unit,
        ) {
            val status = item.status
            val now = System.currentTimeMillis()
            binding.title.text = status.title
            binding.category.text = "${status.category.title} • ${TimeFormatters.durationLabel(status.durationMillis / 60_000L)}"
            binding.remaining.text = TimeFormatters.countdown(status.remainingAt(now))
            binding.progress.progress = status.progressPercentAt(now)
            binding.pauseResumeButton.setText(
                if (status.isPaused()) R.string.button_resume else R.string.button_pause,
            )
            binding.pauseResumeButton.setOnClickListener { onPauseResume(status) }
            binding.finishButton.setOnClickListener { onFinish(status) }
            binding.cureButton.visibility = if (item.showCureButton) View.VISIBLE else View.GONE
            binding.cureButton.isEnabled = item.canCure
            binding.cureButton.alpha = if (item.canCure) 1f else 0.45f
            binding.cureButton.setOnClickListener { onCure(status) }
        }
    }
}
