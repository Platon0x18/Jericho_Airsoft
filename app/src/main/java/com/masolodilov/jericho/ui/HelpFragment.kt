package com.masolodilov.jericho.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.masolodilov.jericho.R
import com.masolodilov.jericho.databinding.FragmentHelpBinding
import com.masolodilov.jericho.databinding.ItemHelpSectionBinding

class HelpFragment : Fragment() {
    private var _binding: FragmentHelpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderSections()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderSections() {
        val sections =
            listOf(
                HelpSection(
                    title = getString(R.string.help_section_quick_start_title),
                    body = getString(R.string.help_section_quick_start_body),
                ),
                HelpSection(
                    title = getString(R.string.help_section_wounds_title),
                    body = getString(R.string.help_section_wounds_body),
                ),
                HelpSection(
                    title = getString(R.string.help_section_diseases_title),
                    body = getString(R.string.help_section_diseases_body),
                ),
                HelpSection(
                    title = getString(R.string.help_section_combat_title),
                    body = getString(R.string.help_section_combat_body),
                ),
                HelpSection(
                    title = getString(R.string.help_section_capture_title),
                    body = getString(R.string.help_section_capture_body),
                ),
                HelpSection(
                    title = getString(R.string.help_section_classes_title),
                    body = getString(R.string.help_section_classes_body),
                ),
                HelpSection(
                    title = getString(R.string.help_section_locations_title),
                    body = getString(R.string.help_section_locations_body),
                ),
            )

        binding.sectionsContainer.removeAllViews()
        sections.forEach { section ->
            val sectionBinding =
                ItemHelpSectionBinding.inflate(layoutInflater, binding.sectionsContainer, false)
            sectionBinding.title.text = section.title
            sectionBinding.body.text = section.body
            binding.sectionsContainer.addView(sectionBinding.root)
        }
    }

    private data class HelpSection(
        val title: String,
        val body: String,
    )
}
