package com.foodlogger

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsetsController
import android.widget.PopupMenu
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.foodlogger.databinding.ActivityMainBinding
import com.foodlogger.util.ThemeManager
import com.foodlogger.ui.navigation.Screen
import com.foodlogger.ui.xml.AddBottomSheet
import com.foodlogger.ui.xml.HomeFragment
import com.foodlogger.ui.xml.InventoryFragment
import com.foodlogger.ui.xml.ProductFragment
import com.foodlogger.ui.xml.ReceiptCaptureActivity
import com.foodlogger.ui.xml.RecipeFragment
import com.foodlogger.ui.xml.ReceiptsFragment
import com.foodlogger.ui.xml.ShoppingListFragment
import com.foodlogger.ui.xml.SettingsFragment
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var currentScreen: Screen = Screen.HOME
    private var previousScreen: Screen = Screen.HOME
    private var isInProducts: Boolean = false
    private var isNavigating: Boolean = false

    companion object {
        private const val PREFS_NAME = "main_activity_prefs"
        private const val KEY_CURRENT_SCREEN = "current_screen"
        private const val STATE_CURRENT_SCREEN = "state_current_screen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyThemeFromPreferences(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        setupSystemUI()

        setupBottomNavigation()

        val savedScreen = savedInstanceState?.getString(STATE_CURRENT_SCREEN)
            ?: getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_CURRENT_SCREEN, null)

        val navigateTo = intent.getStringExtra("navigate_to")
        if (navigateTo == "inventory") {
            showScreen(Screen.INVENTORY, fromNav = false)
            binding.bottomNavigation.selectedItemId = R.id.menu_inventory
        } else if (savedScreen != null) {
            try {
                val screen = Screen.valueOf(savedScreen)
                showScreen(screen, fromNav = false)
                when (screen) {
                    Screen.HOME -> binding.bottomNavigation.selectedItemId = R.id.menu_home
                    Screen.INVENTORY -> binding.bottomNavigation.selectedItemId = R.id.menu_inventory
                    Screen.RECIPES -> binding.bottomNavigation.selectedItemId = R.id.menu_recipes
                    Screen.HISTORY -> binding.bottomNavigation.selectedItemId = R.id.menu_history
                    Screen.SETTINGS, Screen.PRODUCTS -> {
                        // These are not bottom-nav destinations; keep their current UI state.
                    }
                }
            } catch (e: Exception) {
                showScreen(Screen.HOME, fromNav = false)
            }
        } else {
            showScreen(Screen.HOME, fromNav = false)
        }

        syncTopBarWithCurrentScreen()
    }

    override fun onStop() {
        super.onStop()
        saveCurrentScreenState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_CURRENT_SCREEN, currentScreen.name)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        syncTopBarWithCurrentScreen()
    }

    fun saveCurrentScreen() {
        saveCurrentScreenState()
    }

    private fun saveCurrentScreenState() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_CURRENT_SCREEN, currentScreen.name)
            .commit()
    }

    private fun setupSystemUI() {
        val currentMode = ThemeManager.getThemeMode(this)
        val isDarkMode = currentMode == ThemeManager.THEME_DARK
        
        window.insetsController?.setSystemBarsAppearance(
            if (isDarkMode) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        )
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (isNavigating) return@setOnItemSelectedListener true
            
            when (item.itemId) {
                R.id.menu_home -> showScreen(Screen.HOME, fromNav = true)
                R.id.menu_inventory -> showScreen(Screen.INVENTORY, fromNav = true)
                R.id.menu_recipes -> showScreen(Screen.RECIPES, fromNav = true)
                R.id.menu_history -> showScreen(Screen.HISTORY, fromNav = true)
            }
            true
        }
    }

    private fun showAddBottomSheet() {
        val bottomSheet = AddBottomSheet()
        bottomSheet.show(supportFragmentManager, AddBottomSheet.TAG)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_top_app_bar, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_settings)?.isVisible = currentScreen != Screen.SETTINGS
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_settings) {
            showScreen(Screen.SETTINGS, fromNav = false)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (currentScreen == Screen.SETTINGS) {
            goBack()
            return
        }
        super.onBackPressed()
    }

    fun showScreen(screen: Screen, fromNav: Boolean = false) {
        isNavigating = true
        isInProducts = false
        
        if (screen == Screen.SETTINGS) {
            previousScreen = currentScreen
            currentScreen = Screen.SETTINGS
            binding.topAppBar.title = getString(R.string.title_settings)
            binding.fragmentContainer.visibility = View.GONE
            binding.settingsContainer.visibility = View.VISIBLE
            binding.bottomNavigation.visibility = View.GONE
            setSettingsNavigationIcon()
            binding.topAppBar.setNavigationOnClickListener { goBack() }
            
            if (supportFragmentManager.findFragmentByTag("SETTINGS") == null) {
                supportFragmentManager.commit {
                    replace(R.id.settings_container, SettingsFragment(), "SETTINGS")
                }
            }
            invalidateOptionsMenu()
            isNavigating = false
            return
        }

        currentScreen = screen
        binding.fragmentContainer.visibility = View.VISIBLE
        binding.settingsContainer.visibility = View.GONE
        binding.bottomNavigation.visibility = View.VISIBLE
        binding.topAppBar.navigationIcon = null
        binding.topAppBar.setNavigationOnClickListener(null)

        binding.topAppBar.title = getString(titleFor(screen))

        val fragmentTag = screen.name
        val existingFragment = supportFragmentManager.findFragmentByTag(fragmentTag)
        
        if (existingFragment == null) {
            supportFragmentManager.commit {
                replace(R.id.fragmentContainer, createFragmentFor(screen), fragmentTag)
            }
        }

        updateBottomNavSelection(screen)

        invalidateOptionsMenu()
        isNavigating = false
    }

    private fun setSettingsNavigationIcon() {
        binding.topAppBar.navigationIcon = AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back)
        val iconColor = MaterialColors.getColor(binding.topAppBar, com.google.android.material.R.attr.colorOnSurface)
        binding.topAppBar.navigationIcon?.setTint(iconColor)
    }

    private fun syncTopBarWithCurrentScreen() {
        if (!::binding.isInitialized) return

        if (currentScreen == Screen.SETTINGS) {
            binding.topAppBar.title = getString(R.string.title_settings)
            setSettingsNavigationIcon()
            binding.topAppBar.setNavigationOnClickListener { goBack() }
        } else {
            binding.topAppBar.title = getString(titleFor(currentScreen))
            binding.topAppBar.navigationIcon = null
            binding.topAppBar.setNavigationOnClickListener(null)
        }
    }

    private fun goBack() {
        showScreen(previousScreen, fromNav = false)
    }

    private fun updateBottomNavSelection(screen: Screen) {
        val menuItemId = when (screen) {
            Screen.HOME -> R.id.menu_home
            Screen.INVENTORY -> R.id.menu_inventory
            Screen.RECIPES -> R.id.menu_recipes
            Screen.HISTORY -> R.id.menu_history
            else -> return
        }
        binding.bottomNavigation.selectedItemId = menuItemId
    }

    private fun createFragmentFor(screen: Screen): Fragment {
        return when (screen) {
            Screen.HOME -> HomeFragment()
            Screen.INVENTORY -> InventoryFragment()
            Screen.PRODUCTS -> ProductFragment()
            Screen.RECIPES -> RecipeFragment()
            Screen.HISTORY -> ReceiptsFragment()
            Screen.SETTINGS -> HomeFragment()
        }
    }

    private fun titleFor(screen: Screen): Int = when (screen) {
        Screen.HOME -> R.string.title_home
        Screen.INVENTORY -> R.string.title_inventory
        Screen.PRODUCTS -> R.string.title_products
        Screen.RECIPES -> R.string.title_recipes
        Screen.HISTORY -> R.string.title_history
        Screen.SETTINGS -> R.string.title_settings
    }

    fun navigateToProducts() {
        isNavigating = true
        currentScreen = Screen.PRODUCTS
        binding.topAppBar.title = getString(R.string.title_products)
        binding.topAppBar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.topAppBar.setNavigationOnClickListener { onBackPressed() }
        binding.bottomNavigation.visibility = View.VISIBLE
        binding.fragmentContainer.visibility = View.VISIBLE
        binding.settingsContainer.visibility = View.GONE
        
        if (supportFragmentManager.findFragmentByTag("PRODUCTS") == null) {
            supportFragmentManager.commit {
                replace(R.id.fragmentContainer, ProductFragment(), "PRODUCTS")
            }
        }
        invalidateOptionsMenu()
        isNavigating = false
    }

    fun navigateToInventory() {
        isNavigating = true
        isInProducts = false
        showScreen(Screen.INVENTORY, fromNav = false)
    }

    fun navigateToShoppingList() {
        isNavigating = true
        isInProducts = false
        currentScreen = Screen.HOME

        binding.fragmentContainer.visibility = View.VISIBLE
        binding.settingsContainer.visibility = View.GONE
        binding.bottomNavigation.visibility = View.VISIBLE
        binding.topAppBar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.topAppBar.setNavigationOnClickListener { onBackPressed() }
        binding.topAppBar.title = getString(R.string.title_shopping_list)

        if (supportFragmentManager.findFragmentByTag("SHOPPING_LIST") == null) {
            supportFragmentManager.commit {
                replace(R.id.fragmentContainer, ShoppingListFragment(), "SHOPPING_LIST")
            }
        }
        invalidateOptionsMenu()
        isNavigating = false
    }

    fun navigateToReceiptScan() {
        startActivity(android.content.Intent(this, ReceiptCaptureActivity::class.java))
    }

    fun navigateToAddInventory() {
        startActivity(android.content.Intent(this, com.foodlogger.ui.xml.AddInventoryActivity::class.java))
    }

    fun navigateToAddRecipe() {
        showScreen(Screen.RECIPES, fromNav = true)
        binding.fragmentContainer.postDelayed({
            supportFragmentManager.findFragmentById(R.id.fragmentContainer)?.let {
                (it as? RecipeFragment)?.showCreateDialog()
            }
        }, 100)
    }

    private fun navigateBackFromSubScreen() {
        showScreen(Screen.HOME, fromNav = false)
    }
}
