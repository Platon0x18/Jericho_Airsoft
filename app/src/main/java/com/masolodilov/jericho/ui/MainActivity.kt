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
    private var currentOverlayScreen: Int = OVERLAY_NONE
    private var helpReturnToProfile: Boolean = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        maybeRequestNotificationPermission()
        setupToolbarNavigation()
        setupToolbarActions()
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
            currentOverlayScreen = savedInstanceState.getInt(KEY_OVERLAY_SCREEN, OVERLAY_NONE)
            helpReturnToProfile = savedInstanceState.getBoolean(KEY_HELP_RETURN_TO_PROFILE, false)
            binding.bottomNavigation.menu.findItem(currentBottomNavItemId).isChecked = true
            when (currentOverlayScreen) {
                OVERLAY_PROFILE -> showProfileScreen()
                OVERLAY_HELP -> showHelpScreen(helpReturnToProfile)
                else -> showScreen(currentBottomNavItemId)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_BOTTOM_NAV_ITEM, currentBottomNavItemId)
        outState.putInt(KEY_OVERLAY_SCREEN, currentOverlayScreen)
        outState.putBoolean(KEY_HELP_RETURN_TO_PROFILE, helpReturnToProfile)
    }

    private fun showScreen(itemId: Int) {
        currentOverlayScreen = OVERLAY_NONE
        helpReturnToProfile = false
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
        currentOverlayScreen = OVERLAY_PROFILE
        helpReturnToProfile = false
        val fragment = PlayerProfileFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment, fragment::class.java.simpleName)
            .commit()
        updateToolbar(PROFILE_SCREEN_ID)
    }

    private fun showHelpScreen(returnToProfile: Boolean = currentOverlayScreen == OVERLAY_PROFILE) {
        currentOverlayScreen = OVERLAY_HELP
        helpReturnToProfile = returnToProfile
        val fragment = HelpFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment, fragment::class.java.simpleName)
            .commit()
        updateToolbar(HELP_SCREEN_ID)
    }

    private fun updateToolbar(itemId: Int) {
        binding.toolbar.title =
            when (itemId) {
                R.id.menu_active -> getString(R.string.screen_active)
                R.id.menu_history -> getString(R.string.screen_history)
                R.id.menu_permanent -> getString(R.string.screen_permanent)
                R.id.menu_inventory -> getString(R.string.screen_inventory)
                PROFILE_SCREEN_ID -> getString(R.string.screen_profile)
                HELP_SCREEN_ID -> getString(R.string.screen_help)
                else -> getString(R.string.screen_presets)
            }
        updateToolbarNavigationIcon()
        updateToolbarActionsState()
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupToolbarNavigation() {
        binding.toolbar.setNavigationOnClickListener {
            when (currentOverlayScreen) {
                OVERLAY_HELP -> closeHelpScreen()
                OVERLAY_PROFILE -> showScreen(currentBottomNavItemId)
                else -> showProfileScreen()
            }
        }
        updateToolbarNavigationIcon()
    }

    private fun setupToolbarActions() {
        binding.toolbar.inflateMenu(R.menu.main_toolbar_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_help -> {
                    showHelpScreen()
                    true
                }
                else -> false
            }
        }
        updateToolbarActionsState()
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when (currentOverlayScreen) {
                        OVERLAY_HELP -> closeHelpScreen()
                        OVERLAY_PROFILE -> showScreen(currentBottomNavItemId)
                        else -> {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            },
        )
    }

    private fun updateToolbarNavigationIcon() {
        if (currentOverlayScreen == OVERLAY_NONE) {
            binding.toolbar.navigationIcon = AppCompatResources.getDrawable(this, android.R.drawable.ic_menu_myplaces)
            binding.toolbar.navigationContentDescription = getString(R.string.content_description_open_profile)
        } else {
            binding.toolbar.navigationIcon = AppCompatResources.getDrawable(this, android.R.drawable.ic_media_previous)
            binding.toolbar.navigationContentDescription = getString(R.string.content_description_navigate_back)
        }
    }

    private fun updateToolbarActionsState() {
        binding.toolbar.menu.findItem(R.id.action_help)?.isVisible = currentOverlayScreen != OVERLAY_HELP
    }

    private fun closeHelpScreen() {
        if (helpReturnToProfile) {
            showProfileScreen()
        } else {
            showScreen(currentBottomNavItemId)
        }
    }

    private companion object {
        const val PROFILE_SCREEN_ID = -1
        const val HELP_SCREEN_ID = -2
        const val OVERLAY_NONE = 0
        const val OVERLAY_PROFILE = 1
        const val OVERLAY_HELP = 2
        const val KEY_BOTTOM_NAV_ITEM = "bottom_nav_item"
        const val KEY_OVERLAY_SCREEN = "overlay_screen"
        const val KEY_HELP_RETURN_TO_PROFILE = "help_return_to_profile"
    }
}
