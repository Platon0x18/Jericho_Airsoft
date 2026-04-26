package com.masolodilov.jericho.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.masolodilov.jericho.R
import com.masolodilov.jericho.data.StatusRepository
import com.masolodilov.jericho.databinding.FragmentInventoryReceiveBinding
import com.masolodilov.jericho.model.InventoryTransferReceiveResult

class InventoryReceiveFragment : Fragment() {
    private var _binding: FragmentInventoryReceiveBinding? = null
    private val binding get() = _binding!!

    private val repository get() = (requireActivity() as TrackerProvider).repository
    private val screenHost get() = parentFragment as? InventoryScreenHost

    private var hasCameraPermission = false
    private var isScannerRunning = false
    private var scannerLockedByResult = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        scannerLockedByResult = false
        updateCameraUi()
        if (granted) {
            startScanner()
        } else {
            stopScanner()
        }
    }

    private val barcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            isScannerRunning = false
            val rawValue = result?.text.orEmpty()
            if (rawValue.isBlank()) {
                restartScannerWithMessage(getString(R.string.inventory_receive_scanner_hint))
                return
            }

            when (val receiveResult = repository.receiveInventoryTransfer(rawValue)) {
                is InventoryTransferReceiveResult.Success -> {
                    scannerLockedByResult = true
                    stopScanner()
                    binding.resultCard.visibility = View.VISIBLE
                    binding.receivedDetails.text = getString(
                        R.string.inventory_receive_result_details,
                        receiveResult.payload.title,
                        receiveResult.payload.quantity,
                        receiveResult.totalAfter,
                    )
                    binding.scanAgainButton.visibility = View.VISIBLE
                    Snackbar.make(binding.root, R.string.inventory_receive_success, Snackbar.LENGTH_SHORT).show()
                }
                is InventoryTransferReceiveResult.Error -> {
                    restartScannerWithMessage(receiveResult.message)
                }
            }
        }

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) = Unit
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInventoryReceiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener { screenHost?.showInventoryHome() }
        binding.requestPermissionButton.setOnClickListener {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        binding.scanAgainButton.setOnClickListener {
            scannerLockedByResult = false
            binding.resultCard.visibility = View.GONE
            binding.scanAgainButton.visibility = View.GONE
            startScanner()
        }

        binding.barcodeView.barcodeView.decoderFactory =
            DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
        binding.barcodeView.setStatusText(getString(R.string.inventory_receive_scanner_hint))

        hasCameraPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        updateCameraUi()
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission && !scannerLockedByResult) {
            startScanner()
        }
    }

    override fun onPause() {
        stopScanner()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateCameraUi() {
        if (_binding == null) return
        binding.permissionGroup.visibility = if (hasCameraPermission) View.GONE else View.VISIBLE
        binding.barcodeView.visibility = if (hasCameraPermission) View.VISIBLE else View.GONE
        if (!hasCameraPermission) {
            binding.resultCard.visibility = View.GONE
            binding.scanAgainButton.visibility = View.GONE
        }
    }

    private fun startScanner() {
        if (!hasCameraPermission || scannerLockedByResult || isScannerRunning || _binding == null) return
        isScannerRunning = true
        binding.barcodeView.resume()
        binding.barcodeView.decodeSingle(barcodeCallback)
    }

    private fun stopScanner() {
        isScannerRunning = false
        if (_binding != null) {
            binding.barcodeView.pause()
        }
    }

    private fun restartScannerWithMessage(message: String) {
        stopScanner()
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        binding.root.postDelayed(
            {
                if (_binding != null && hasCameraPermission && !scannerLockedByResult) {
                    startScanner()
                }
            },
            1000L,
        )
    }
}
