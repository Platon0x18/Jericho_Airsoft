package com.masolodilov.jericho.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.masolodilov.jericho.R
import com.masolodilov.jericho.data.PresetRow
import com.masolodilov.jericho.data.StatusRepository
import com.masolodilov.jericho.databinding.DialogCustomStatusBinding
import com.masolodilov.jericho.databinding.FragmentPresetsBinding
import com.masolodilov.jericho.model.StartResult
import com.masolodilov.jericho.model.StatusCategory

class PresetsFragment : Fragment(), StatusRepository.Listener {
    private var _binding: FragmentPresetsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PresetListAdapter
    private val repository get() = (requireActivity() as TrackerProvider).repository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPresetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PresetListAdapter { row ->
            handlePresetClick(row)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        adapter.submit(repository.presetRows())

        binding.addCustomStatus.setOnClickListener {
            showCustomStatusDialog()
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
        adapter.submit(repository.presetRows())
    }

    private fun showCustomStatusDialog() {
        val dialogBinding = DialogCustomStatusBinding.inflate(layoutInflater)
        val categories = repository.categoryTitles()

        dialogBinding.categoryInput.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories),
        )
        dialogBinding.categoryInput.setText(StatusCategory.CUSTOM.title, false)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_custom_title)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_start) { _, _ ->
                val title = dialogBinding.nameInput.text?.toString()?.trim().orEmpty()
                val minutes = dialogBinding.minutesInput.text?.toString()?.toLongOrNull()
                val category = StatusCategory.fromTitle(
                    dialogBinding.categoryInput.text?.toString().orEmpty(),
                )

                if (title.isBlank() || minutes == null || minutes <= 0L) {
                    Snackbar.make(binding.root, R.string.custom_status_invalid, Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                when (val result = repository.startCustom(title = title, durationMinutes = minutes, category = category)) {
                    StartResult.Started -> {
                        Snackbar.make(binding.root, R.string.status_started, Snackbar.LENGTH_SHORT).show()
                    }
                    is StartResult.StartedAndCured -> {
                        Snackbar.make(
                            binding.root,
                            getString(
                                R.string.status_started_and_cured_message,
                                result.statusTitle,
                                result.itemTitle,
                            ),
                            Snackbar.LENGTH_SHORT,
                        ).show()
                    }
                    is StartResult.Blocked -> {
                        Snackbar.make(binding.root, result.reason, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun handlePresetClick(row: PresetRow.Item) {
        if (row.blockedReason != null) {
            Snackbar.make(binding.root, row.blockedReason, Snackbar.LENGTH_SHORT).show()
            return
        }

        val cureOption = repository.getImmediateCureOption(row.preset)
        if (cureOption == null) {
            startPresetStandard(row)
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_start_cure_title)
            .setMessage(
                getString(
                    R.string.dialog_start_cure_message,
                    cureOption.itemTitle,
                    row.preset.title,
                ),
            )
            .setPositiveButton(R.string.button_apply_item) { _, _ ->
                handleStartResult(repository.startPresetWithImmediateCure(row.preset))
            }
            .setNegativeButton(R.string.button_start_without_item) { _, _ ->
                startPresetStandard(row)
            }
            .show()
    }

    private fun startPresetStandard(row: PresetRow.Item) {
        handleStartResult(repository.startPreset(row.preset))
    }

    private fun handleStartResult(result: StartResult) {
        when (result) {
            StartResult.Started -> {
                Snackbar.make(binding.root, R.string.status_started, Snackbar.LENGTH_SHORT).show()
            }
            is StartResult.StartedAndCured -> {
                Snackbar.make(
                    binding.root,
                    getString(
                        R.string.status_started_and_cured_message,
                        result.statusTitle,
                        result.itemTitle,
                    ),
                    Snackbar.LENGTH_SHORT,
                ).show()
            }
            is StartResult.Blocked -> {
                Snackbar.make(binding.root, result.reason, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}
