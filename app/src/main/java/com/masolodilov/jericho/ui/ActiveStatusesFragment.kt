package com.masolodilov.jericho.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.masolodilov.jericho.R
import com.masolodilov.jericho.model.CureResult
import com.masolodilov.jericho.databinding.FragmentActiveBinding
import com.masolodilov.jericho.model.StatusOutcome

class ActiveStatusesFragment : Fragment(), com.masolodilov.jericho.data.StatusRepository.Listener {
    private var _binding: FragmentActiveBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ActiveStatusAdapter
    private val repository get() = (requireActivity() as TrackerProvider).repository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentActiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ActiveStatusAdapter(
            onPauseResume = { status ->
                repository.pauseOrResume(status.id)
            },
            onFinish = { status ->
                repository.finishStatus(status.id, StatusOutcome.FINISHED_MANUAL)
            },
            onCure = { status ->
                val cureOption = repository.getCureOption(status)
                if (cureOption == null) {
                    Snackbar.make(binding.root, R.string.cure_unavailable, Snackbar.LENGTH_SHORT).show()
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.dialog_cure_title)
                        .setMessage(
                            getString(
                                R.string.dialog_cure_message,
                                cureOption.itemTitle,
                                status.title,
                            ),
                        )
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .setPositiveButton(R.string.dialog_confirm_yes) { _, _ ->
                            when (val result = repository.cureStatus(status.id)) {
                                is CureResult.Success -> {
                                    Snackbar.make(
                                        binding.root,
                                        getString(
                                            R.string.status_cured_message,
                                            result.statusTitle,
                                            result.itemTitle,
                                        ),
                                        Snackbar.LENGTH_SHORT,
                                    ).show()
                                }
                                is CureResult.Unavailable -> {
                                    Snackbar.make(binding.root, result.reason, Snackbar.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .show()
                }
            },
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
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
        val items = repository.getActiveStatuses().map { status ->
            ActiveStatusUiState(
                status = status,
                showCureButton = repository.hasCureSupport(status),
                canCure = repository.getCureOption(status) != null,
            )
        }
        adapter.submit(items)
        binding.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }
}
