package com.masolodilov.jericho.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.masolodilov.jericho.data.StatusRepository
import com.masolodilov.jericho.databinding.FragmentPermanentEffectsBinding

class PermanentEffectsFragment : Fragment(), StatusRepository.Listener {
    private var _binding: FragmentPermanentEffectsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PermanentEffectAdapter
    private val repository get() = (requireActivity() as TrackerProvider).repository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPermanentEffectsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PermanentEffectAdapter { item, enabled ->
            repository.setPermanentEffectEnabled(item.effect.id, enabled)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        adapter.submit(repository.getPermanentEffects())
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
        adapter.submit(repository.getPermanentEffects())
    }
}
