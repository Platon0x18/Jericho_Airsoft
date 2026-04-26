package com.masolodilov.jericho.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.masolodilov.jericho.R
import com.masolodilov.jericho.databinding.FragmentPlayerProfileBinding
import com.masolodilov.jericho.model.PlayerProfile

class PlayerProfileFragment : Fragment() {
    private var _binding: FragmentPlayerProfileBinding? = null
    private val binding get() = _binding!!

    private val repository get() = (requireActivity() as TrackerProvider).repository
    private var selectedTabIndex: Int = TAB_FIGHTER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedTabIndex = savedInstanceState?.getInt(KEY_SELECTED_TAB, TAB_FIGHTER) ?: TAB_FIGHTER
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPlayerProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTabs()
        render(repository.getPlayerProfile())

        binding.saveButton.setOnClickListener {
            repository.savePlayerProfile(
                PlayerProfile(
                    faction = binding.factionInput.text?.toString()?.trim().orEmpty(),
                    callsign = binding.callsignInput.text?.toString()?.trim().orEmpty(),
                    bloodType = binding.bloodTypeInput.text?.toString()?.trim().orEmpty(),
                    allergy = binding.allergyInput.text?.toString()?.trim().orEmpty(),
                    specialNotes = binding.specialNotesInput.text?.toString()?.trim().orEmpty(),
                ),
            )
            Snackbar.make(binding.root, R.string.profile_saved, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_TAB, selectedTabIndex)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupTabs() {
        if (binding.tabs.tabCount == 0) {
            binding.tabs.addTab(binding.tabs.newTab().setText(R.string.profile_tab_fighter))
            binding.tabs.addTab(binding.tabs.newTab().setText(R.string.profile_tab_real_data))
        }
        binding.tabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    selectedTabIndex = tab.position
                    updateTabContent()
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit

                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            },
        )
        binding.tabs.getTabAt(selectedTabIndex)?.select()
        updateTabContent()
    }

    private fun render(profile: PlayerProfile) {
        binding.factionInput.setText(profile.faction)
        binding.callsignInput.setText(profile.callsign)
        binding.bloodTypeInput.setText(profile.bloodType)
        binding.allergyInput.setText(profile.allergy)
        binding.specialNotesInput.setText(profile.specialNotes)
    }

    private fun updateTabContent() {
        binding.fighterContent.visibility = if (selectedTabIndex == TAB_FIGHTER) View.VISIBLE else View.GONE
        binding.realDataContent.visibility = if (selectedTabIndex == TAB_REAL_DATA) View.VISIBLE else View.GONE
    }

    private companion object {
        const val KEY_SELECTED_TAB = "selected_tab"
        const val TAB_FIGHTER = 0
        const val TAB_REAL_DATA = 1
    }
}
