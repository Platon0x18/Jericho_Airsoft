package com.masolodilov.jericho.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.masolodilov.jericho.R
import com.masolodilov.jericho.data.StatusRepository
import com.masolodilov.jericho.databinding.FragmentInventoryTransferBinding
import com.masolodilov.jericho.model.InventoryItem
import com.masolodilov.jericho.model.InventoryTransferCreateResult

class InventoryTransferFragment : Fragment(), StatusRepository.Listener {
    private var _binding: FragmentInventoryTransferBinding? = null
    private val binding get() = _binding!!

    private val repository get() = (requireActivity() as TrackerProvider).repository
    private val screenHost get() = parentFragment as? InventoryScreenHost
    private val availableItems = mutableListOf<InventoryItem>()
    private var selectedItemId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInventoryTransferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener { screenHost?.showInventoryHome() }
        binding.generateQrButton.setOnClickListener { confirmTransfer() }
        binding.quantityInput.setText("1")
        binding.itemInput.setOnClickListener { binding.itemInput.showDropDown() }
        binding.itemInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.itemInput.showDropDown()
            }
        }
        binding.itemInput.setOnItemClickListener { _, _, position, _ ->
            selectedItemId = availableItems.getOrNull(position)?.id
        }
    }

    override fun onStart() {
        super.onStart()
        repository.addListener(this)
    }

    override fun onStop() {
        super.onStop()
        repository.removeListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStatusStateChanged() {
        if (_binding == null) return
        availableItems.clear()
        availableItems += repository.getInventoryItems()

        binding.emptyView.visibility = if (availableItems.isEmpty()) View.VISIBLE else View.GONE
        binding.formContainer.visibility = if (availableItems.isEmpty()) View.GONE else View.VISIBLE

        binding.itemInput.setSimpleItems(availableItems.map { it.dropdownLabel() }.toTypedArray())

        val selectedItem = availableItems.firstOrNull { it.id == selectedItemId } ?: availableItems.firstOrNull()
        selectedItemId = selectedItem?.id
        binding.itemInput.setText(selectedItem?.dropdownLabel().orEmpty(), false)
    }

    private fun confirmTransfer() {
        val item = selectedInventoryItem()
        val quantity = binding.quantityInput.text?.toString()?.toIntOrNull()

        if (item == null || quantity == null || quantity <= 0 || quantity > item.quantity) {
            Snackbar.make(binding.root, R.string.inventory_transfer_invalid, Snackbar.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_transfer_confirm_title)
            .setMessage(getString(R.string.dialog_transfer_confirm_message, quantity, item.title))
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_confirm_yes) { _, _ ->
                createTransfer(item, quantity)
            }
            .show()
    }

    private fun createTransfer(item: InventoryItem, quantity: Int) {
        when (val result = repository.createInventoryTransfer(item.id, quantity)) {
            is InventoryTransferCreateResult.Success -> {
                binding.qrResultCard.visibility = View.VISIBLE
                binding.transferDetails.text = getString(
                    R.string.inventory_transfer_result_details,
                    result.payload.title,
                    result.payload.quantity,
                )
                binding.qrImage.setImageBitmap(generateQrBitmap(result.qrContent))
                Snackbar.make(binding.root, R.string.inventory_transfer_created, Snackbar.LENGTH_SHORT).show()
            }
            is InventoryTransferCreateResult.Error -> {
                Snackbar.make(binding.root, result.message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun selectedInventoryItem(): InventoryItem? {
        return availableItems.firstOrNull { it.id == selectedItemId }
            ?: availableItems.firstOrNull { it.dropdownLabel() == binding.itemInput.text?.toString().orEmpty() }
    }

    private fun generateQrBitmap(content: String): Bitmap {
        val size = (resources.displayMetrics.widthPixels * 0.72f).toInt().coerceAtLeast(360)
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val pixels = IntArray(size * size)

        for (y in 0 until size) {
            val offset = y * size
            for (x in 0 until size) {
                pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }

        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, size, 0, 0, size, size)
        }
    }

    private fun InventoryItem.dropdownLabel(): String = "$title • $quantity шт."
}
