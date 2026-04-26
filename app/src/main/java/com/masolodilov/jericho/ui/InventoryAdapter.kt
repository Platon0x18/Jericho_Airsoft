package com.masolodilov.jericho.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.masolodilov.jericho.databinding.ItemInventoryBinding
import com.masolodilov.jericho.model.InventoryItem

class InventoryAdapter : RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {
    private val items = mutableListOf<InventoryItem>()

    fun submit(inventoryItems: List<InventoryItem>) {
        items.clear()
        items.addAll(inventoryItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return InventoryViewHolder(ItemInventoryBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class InventoryViewHolder(
        private val binding: ItemInventoryBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InventoryItem) {
            binding.title.text = item.title
            binding.category.text = item.category.title
            binding.quantity.text = item.quantity.toString()
        }
    }
}
