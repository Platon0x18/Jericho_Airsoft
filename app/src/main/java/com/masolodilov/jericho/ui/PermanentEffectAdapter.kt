package com.masolodilov.jericho.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.masolodilov.jericho.databinding.ItemPermanentEffectBinding
import com.masolodilov.jericho.model.PermanentEffectState

class PermanentEffectAdapter(
    private val onToggle: (PermanentEffectState, Boolean) -> Unit,
) : RecyclerView.Adapter<PermanentEffectAdapter.PermanentEffectViewHolder>() {
    private val items = mutableListOf<PermanentEffectState>()

    fun submit(effectStates: List<PermanentEffectState>) {
        items.clear()
        items.addAll(effectStates)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PermanentEffectViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return PermanentEffectViewHolder(ItemPermanentEffectBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: PermanentEffectViewHolder, position: Int) {
        holder.bind(items[position], onToggle)
    }

    override fun getItemCount(): Int = items.size

    class PermanentEffectViewHolder(
        private val binding: ItemPermanentEffectBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: PermanentEffectState,
            onToggle: (PermanentEffectState, Boolean) -> Unit,
        ) {
            binding.title.text = item.effect.title
            binding.description.text = item.effect.description
            binding.enabledSwitch.setOnCheckedChangeListener(null)
            binding.enabledSwitch.isChecked = item.enabled
            binding.enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
                onToggle(item, isChecked)
            }
            binding.root.setOnClickListener {
                binding.enabledSwitch.toggle()
            }
        }
    }
}
