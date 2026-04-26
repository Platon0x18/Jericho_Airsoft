package com.masolodilov.jericho.ui

import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.masolodilov.jericho.R
import com.masolodilov.jericho.databinding.ItemHistoryStatusBinding
import com.masolodilov.jericho.model.HistoryEntry
import com.masolodilov.jericho.model.InventoryAction

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
    private val items = mutableListOf<HistoryEntry>()

    fun submit(historyItems: List<HistoryEntry>) {
        items.clear()
        items.addAll(historyItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return HistoryViewHolder(ItemHistoryStatusBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class HistoryViewHolder(
        private val binding: ItemHistoryStatusBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryEntry) {
            when (item) {
                is HistoryEntry.Status -> {
                    val status = item.item
                    binding.title.text = status.title
                    binding.outcome.text = "${status.category.title} • ${status.outcome.title}"
                    binding.finishedAt.text =
                        "${TimeFormatters.dateTime(status.finishedAtMillis)} • ${TimeFormatters.durationLabel(status.durationMillis / 60_000L)}"
                    val note = status.note
                    binding.note.visibility = if (note.isNullOrBlank()) View.GONE else View.VISIBLE
                    binding.note.text = if (note.isNullOrBlank()) "" else {
                        if (status.outcome == com.masolodilov.jericho.model.StatusOutcome.AUTO_TRANSITION) {
                            binding.root.context.getString(R.string.label_next_stage, note)
                        } else {
                            note
                        }
                    }
                }
                is HistoryEntry.Inventory -> {
                    val log = item.item
                    binding.title.text = log.title
                    binding.outcome.text = "${log.category.title} • ${log.action.title}"
                    binding.finishedAt.text = TimeFormatters.dateTime(log.happenedAtMillis)
                    binding.note.visibility = View.VISIBLE
                    val summary = when (log.action) {
                        InventoryAction.ACQUIRED -> {
                            binding.root.context.getString(
                                R.string.history_inventory_acquired,
                                log.quantity,
                                log.totalAfter,
                            )
                        }
                        InventoryAction.SPENT -> {
                            binding.root.context.getString(
                                R.string.history_inventory_spent,
                                log.quantity,
                                log.totalAfter,
                            )
                        }
                        InventoryAction.TRANSFERRED -> {
                            binding.root.context.getString(
                                R.string.history_inventory_transferred,
                                log.quantity,
                                log.totalAfter,
                            )
                        }
                        InventoryAction.RECEIVED_QR -> {
                            binding.root.context.getString(
                                R.string.history_inventory_received_qr,
                                log.quantity,
                                log.totalAfter,
                            )
                        }
                    }
                    binding.note.text = buildList {
                        add(summary)
                        log.note?.takeIf { it.isNotBlank() }?.let(::add)
                    }.joinToString("\n")
                }
            }
        }
    }
}
