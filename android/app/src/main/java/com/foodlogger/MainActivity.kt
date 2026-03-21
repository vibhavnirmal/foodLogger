package com.foodlogger

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.foodlogger.databinding.ActivityMainBinding
import com.foodlogger.ui.navigation.Screen
import com.foodlogger.ui.xml.AddBottomSheet
import com.foodlogger.ui.xml.HomeFragment
import com.foodlogger.ui.xml.InventoryFragment
import com.foodlogger.ui.xml.ProductFragment
import com.foodlogger.ui.xml.ReceiptCaptureActivity
import com.foodlogger.ui.xml.RecipeFragment
import com.foodlogger.ui.xml.ReceiptsFragment
import com.foodlogger.ui.xml.SettingsFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var currentScreen: Screen = Screen.HOME
    private var previousScreen: Screen = Screen.HOME
    private var isInProducts: Boolean = false
    private var isNavigating: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topAppBar)

        setupBottomNavigation()

        if (savedInstanceState == null) {
            val navigateTo = intent.getStringExtra("navigate_to")
            if (navigateTo == "inventory") {
                showScreen(Screen.INVENTORY, fromNav = false)
                binding.bottomNavigation.selectedItemId = R.id.menu_inventory
            } else {
                showScreen(Screen.HOME, fromNav = false)
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (isNavigating) return@setOnItemSelectedListener true
            
            when (item.itemId) {
                R.id.menu_home -> showScreen(Screen.HOME, fromNav = true)
                R.id.menu_inventory -> showScreen(Screen.INVENTORY, fromNav = true)
                R.id.menu_add -> showAddBottomSheet()
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
            binding.topAppBar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)
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
        binding.topAppBar.navigationIcon = null
        binding.topAppBar.setNavigationOnClickListener(null)
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

    fun navigateToReceiptScan() {
        startActivity(android.content.Intent(this, ReceiptCaptureActivity::class.java))
    }

    private fun navigateBackFromSubScreen() {
        showScreen(Screen.HOME, fromNav = false)
    }
}
