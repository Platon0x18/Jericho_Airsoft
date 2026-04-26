package com.masolodilov.jericho.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.activity.OnBackPressedCallback
import com.masolodilov.jericho.R
import com.masolodilov.jericho.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), TrackerProvider {
    override val repository by lazy { (application as TrackerProvider).repository }

    private lateinit var binding: ActivityMainBinding
    private var currentBottomNavItemId: Int = R.id.menu_presets
    private var isProfileScreenVisible: Boolean = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        maybeRequestNotificationPermission()
        setupToolbarNavigation()
        setupBackHandling()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            currentBottomNavItemId = item.itemId
            showScreen(item.itemId)
            true
        }

        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.menu_presets
        } else {
            currentBottomNavItemId = savedInstanceState.getInt(KEY_BOTTOM_NAV_ITEM, R.id.menu_presets)
            isProfileScreenVisible = savedInstanceState.getBoolean(KEY_PROFILE_VISIBLE, false)
            binding.bottomNavigation.menu.findItem(currentBottomNavItemId).isChecked = true
            if (isProfileScreenVisible) {
                showProfileScreen()
            } else {
                showScreen(currentBottomNavItemId)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_BOTTOM_NAV_ITEM, currentBottomNavItemId)
        outState.putBoolean(KEY_PROFILE_VISIBLE, isProfileScreenVisible)
    }

    private fun showScreen(itemId: Int) {
        isProfileScreenVisible = false
        val fragment = when (itemId) {
            R.id.menu_active -> ActiveStatusesFragment()
            R.id.menu_history -> HistoryFragment()
            R.id.menu_permanent -> PermanentEffectsFragment()
            R.id.menu_inventory -> InventoryFragment()
            else -> PresetsFragment()
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment, fragment::class.java.simpleName)
            .commit()

        updateToolbar(itemId)
    }

    private fun showProfileScreen() {
        isProfileScreenVisible = true
        val fragment = PlayerProfileFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment, fragment::class.java.simpleName)
            .commit()
        updateToolbar(PROFILE_SCREEN_ID)
    }

    private fun updateToolbar(itemId: Int) {
        binding.toolbar.title =
            when (itemId) {
                R.id.menu_active -> getString(R.string.screen_active)
                R.id.menu_history -> getString(R.string.screen_history)
                R.id.menu_permanent -> getString(R.string.screen_permanent)
                R.id.menu_inventory -> getString(R.string.screen_inventory)
                PROFILE_SCREEN_ID -> getString(R.string.screen_profile)
                else -> getString(R.string.screen_presets)
            }
        updateToolbarNavigationIcon()
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupToolbarNavigation() {
        binding.toolbar.setNavigationOnClickListener {
            if (isProfileScreenVisible) {
                showScreen(currentBottomNavItemId)
            } else {
                showProfileScreen()
            }
        }
        updateToolbarNavigationIcon()
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isProfileScreenVisible) {
                        showScreen(currentBottomNavItemId)
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )
    }

    private fun updateToolbarNavigationIcon() {
        if (isProfileScreenVisible) {
            binding.toolbar.navigationIcon = AppCompatResources.getDrawable(this, android.R.drawable.ic_media_previous)
            binding.toolbar.navigationContentDescription = getString(R.string.content_description_close_profile)
        } else {
            binding.toolbar.navigationIcon = AppCompatResources.getDrawable(this, android.R.drawable.ic_menu_myplaces)
            binding.toolbar.navigationContentDescription = getString(R.string.content_description_open_profile)
        }
    }

    private companion object {
        const val PROFILE_SCREEN_ID = -1
        const val KEY_BOTTOM_NAV_ITEM = "bottom_nav_item"
        const val KEY_PROFILE_VISIBLE = "profile_visible"
    }
}
