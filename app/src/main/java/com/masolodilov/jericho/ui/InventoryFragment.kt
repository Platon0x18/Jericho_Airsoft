package com.masolodilov.jericho.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.masolodilov.jericho.R
import com.masolodilov.jericho.data.StatusRepository
import com.masolodilov.jericho.databinding.FragmentInventoryBinding

class InventoryFragment : Fragment(), StatusRepository.Listener, InventoryScreenHost {
    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: InventoryAdapter
    private lateinit var backCallback: OnBackPressedCallback
    private val repository get() = (requireActivity() as TrackerProvider).repository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                showInventoryHome()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

        adapter = InventoryAdapter()

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.addInventoryItem.setOnClickListener { showChildScreen(InventoryAddItemFragment()) }
        binding.openTransferScreen.setOnClickListener { showChildScreen(InventoryTransferFragment()) }
        binding.openReceiveScreen.setOnClickListener { showChildScreen(InventoryReceiveFragment()) }

        setChildScreenVisible(
            childFragmentManager.findFragmentById(R.id.inventory_child_container) != null,
        )
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
        val items = repository.getInventoryItems()
        adapter.submit(items)
        binding.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun showInventoryHome() {
        childFragmentManager.findFragmentById(R.id.inventory_child_container)?.let { fragment ->
            childFragmentManager.beginTransaction()
                .remove(fragment)
                .commit()
        }
        setChildScreenVisible(false)
    }

    private fun showChildScreen(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.inventory_child_container, fragment, fragment::class.java.simpleName)
            .commit()
        setChildScreenVisible(true)
    }

    private fun setChildScreenVisible(isVisible: Boolean) {
        if (_binding == null) return
        binding.inventoryHomeContent.visibility = if (isVisible) View.GONE else View.VISIBLE
        binding.inventoryChildContainer.visibility = if (isVisible) View.VISIBLE else View.GONE
        binding.addInventoryItem.visibility = if (isVisible) View.GONE else View.VISIBLE
        backCallback.isEnabled = isVisible
    }
}
