package com.foodlogger.ui.xml

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.foodlogger.R
import com.foodlogger.databinding.FragmentSettingsBinding
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        binding.settingsViewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2

            override fun createFragment(position: Int): Fragment {
                return if (position == 0) {
                    SettingsLocationsTabFragment()
                } else {
                    SettingsStoresTabFragment()
                }
            }
        }

        TabLayoutMediator(binding.settingsTabLayout, binding.settingsViewPager) { tab, position ->
            tab.text = if (position == 0) {
                getString(R.string.settings_storage_locations)
            } else {
                getString(R.string.settings_stores)
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
