package com.masolodilov.jericho.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.masolodilov.jericho.R
import com.masolodilov.jericho.databinding.FragmentInventoryAddItemBinding
import com.masolodilov.jericho.model.InventoryAcquisitionSource
import com.masolodilov.jericho.model.InventoryCategory

class InventoryAddItemFragment : Fragment() {
    private var _binding: FragmentInventoryAddItemBinding? = null
    private val binding get() = _binding!!

    private val repository get() = (requireActivity() as TrackerProvider).repository
    private val screenHost get() = parentFragment as? InventoryScreenHost
    private val items = InventoryCategory.selectableEntries()
    private val sources = InventoryAcquisitionSource.entries.toList()
    private var selectedSource = InventoryAcquisitionSource.OPEN_WORLD

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInventoryAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener { screenHost?.showInventoryHome() }

        binding.itemInput.setSimpleItems(items.map { it.title }.toTypedArray())
        binding.itemInput.setText(InventoryCategory.BLUE_POTION.title, false)
        binding.itemInput.setOnClickListener { binding.itemInput.showDropDown() }
        binding.itemInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.itemInput.showDropDown()
            }
        }

        binding.sourceInput.setSimpleItems(sources.map { it.title }.toTypedArray())
        binding.sourceInput.setText(selectedSource.title, false)
        binding.sourceInput.setOnClickListener { binding.sourceInput.showDropDown() }
        binding.sourceInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.sourceInput.showDropDown()
            }
        }
        binding.sourceInput.setOnItemClickListener { _, _, position, _ ->
            selectedSource = sources.getOrNull(position) ?: selectedSource
            updateSourceDetailsUi()
        }

        updateSourceDetailsUi()

        binding.saveButton.setOnClickListener { addItem() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addItem() {
        val selectedTitle = binding.itemInput.text?.toString()?.trim().orEmpty()
        val quantity = binding.quantityInput.text?.toString()?.toIntOrNull()
        val category = items.firstOrNull { it.title == selectedTitle }
        val source = currentSource()
        val sourceDetails = binding.sourceDetailsInput.text?.toString()?.trim().orEmpty()

        if (category == null || quantity == null || quantity <= 0) {
            Snackbar.make(binding.root, R.string.inventory_invalid, Snackbar.LENGTH_SHORT).show()
            return
        }

        if (source.requiresDetails && sourceDetails.isBlank()) {
            Snackbar.make(binding.root, R.string.inventory_source_details_required, Snackbar.LENGTH_SHORT).show()
            return
        }

        repository.addInventoryItem(
            title = category.title,
            category = category,
            quantity = quantity,
            note = source.buildLogNote(sourceDetails),
        )
        Snackbar.make(binding.root, R.string.inventory_added, Snackbar.LENGTH_SHORT).show()
        screenHost?.showInventoryHome()
    }

    private fun updateSourceDetailsUi() {
        if (_binding == null) return
        val source = currentSource()
        binding.sourceDetailsLayout.visibility = if (source.requiresDetails) View.VISIBLE else View.GONE
        binding.sourceDetailsLayout.hint = source.detailsHint.orEmpty()
        if (!source.requiresDetails) {
            binding.sourceDetailsInput.setText("")
        }
    }

    private fun currentSource(): InventoryAcquisitionSource {
        selectedSource = InventoryAcquisitionSource.fromTitle(
            binding.sourceInput.text?.toString()?.trim().orEmpty(),
        )
        return selectedSource
    }
}
