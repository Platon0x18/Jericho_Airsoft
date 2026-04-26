package com.masolodilov.jericho.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.masolodilov.jericho.data.PresetRow
import com.masolodilov.jericho.databinding.ItemStatusHeaderBinding
import com.masolodilov.jericho.databinding.ItemStatusPresetBinding

class PresetListAdapter(
    private val onPresetClick: (PresetRow.Item) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<PresetRow>()

    fun submit(rows: List<PresetRow>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PresetRow.Header -> VIEW_TYPE_HEADER
            is PresetRow.Item -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(ItemStatusHeaderBinding.inflate(inflater, parent, false))
            else -> PresetViewHolder(ItemStatusPresetBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = items[position]) {
            is PresetRow.Header -> (holder as HeaderViewHolder).bind(row)
            is PresetRow.Item -> (holder as PresetViewHolder).bind(row, onPresetClick)
        }
    }

    class HeaderViewHolder(
        private val binding: ItemStatusHeaderBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: PresetRow.Header) {
            binding.root.text = row.title
        }
    }

    class PresetViewHolder(
        private val binding: ItemStatusPresetBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PresetRow.Item, onClick: (PresetRow.Item) -> Unit) {
            val context = binding.root.context
            val isBlocked = item.blockedReason != null
            binding.title.text = item.preset.title
            binding.description.text = item.preset.description
            binding.duration.text = TimeFormatters.durationLabel(item.preset.durationMinutes)
            binding.badge.visibility = if (item.blockedBadge == null) View.GONE else View.VISIBLE
            binding.badge.text = item.blockedBadge
            binding.root.strokeColor = ContextCompat.getColor(
                context,
                if (isBlocked) com.masolodilov.jericho.R.color.danger_500 else com.masolodilov.jericho.R.color.sand_300,
            )
            binding.root.setCardBackgroundColor(
                ContextCompat.getColor(
                    context,
                    if (isBlocked) com.masolodilov.jericho.R.color.sand_50 else com.masolodilov.jericho.R.color.white,
                ),
            )
            binding.root.alpha = if (isBlocked) 0.85f else 1f
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ITEM = 1
    }
}
