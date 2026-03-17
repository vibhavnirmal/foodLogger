package com.foodlogger

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.foodlogger.databinding.ActivityMainBinding
import com.foodlogger.ui.navigation.Screen
import com.foodlogger.ui.xml.BarcodeFragment
import com.foodlogger.ui.xml.InventoryFragment
import com.foodlogger.ui.xml.ProductFragment
import com.foodlogger.ui.xml.RecipeFragment
import com.foodlogger.ui.xml.SettingsFragment
import com.foodlogger.ui.xml.WishlistFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var currentScreen: Screen = Screen.INVENTORY
    private var lastMainScreen: Screen = Screen.INVENTORY
    private val mainScreens = listOf(
        Screen.INVENTORY,
        Screen.WISHLIST,
        Screen.RECIPES,
        Screen.BARCODE,
        Screen.PRODUCTS,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topAppBar)

        binding.mainPager.adapter = MainPagerAdapter()
        binding.mainPager.offscreenPageLimit = 1
        binding.mainPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val selectedScreen = mainScreens[position]
                currentScreen = selectedScreen
                lastMainScreen = selectedScreen
                binding.topAppBar.title = getString(titleFor(selectedScreen))
                val menuId = menuIdFor(selectedScreen)
                if (binding.bottomNavigation.selectedItemId != menuId) {
                    binding.bottomNavigation.selectedItemId = menuId
                }
                invalidateOptionsMenu()
            }
        })

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val screen = when (item.itemId) {
                R.id.menu_inventory -> Screen.INVENTORY
                R.id.menu_wishlist -> Screen.WISHLIST
                R.id.menu_recipes -> Screen.RECIPES
                R.id.menu_barcode -> Screen.BARCODE
                R.id.menu_products -> Screen.PRODUCTS
                else -> return@setOnItemSelectedListener false
            }
            showMainScreen(screen)
            true
        }

        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.menu_inventory
        }

        onBackPressedDispatcher.addCallback(this) {
            if (currentScreen == Screen.SETTINGS) {
                navigateToMainScreen(lastMainScreen)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
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
            showScreen(Screen.SETTINGS)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun navigateToMainScreen(screen: Screen) {
        val target = if (screen == Screen.SETTINGS) Screen.INVENTORY else screen
        val menuId = menuIdFor(target)
        binding.bottomNavigation.selectedItemId = menuId
    }

    private fun showScreen(screen: Screen) {
        if (screen == Screen.SETTINGS) {
            currentScreen = Screen.SETTINGS
            binding.topAppBar.title = getString(R.string.title_settings)
            binding.mainPager.visibility = View.GONE
            binding.settingsContainer.visibility = View.VISIBLE
            binding.bottomNavigation.visibility = View.GONE
            binding.topAppBar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)
            binding.topAppBar.setNavigationOnClickListener {
                navigateToMainScreen(lastMainScreen)
            }
            val existing = supportFragmentManager.findFragmentByTag(Screen.SETTINGS.name)
            if (existing == null) {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings_container, SettingsFragment(), Screen.SETTINGS.name)
                    .commit()
            }
            invalidateOptionsMenu()
            return
        }

        showMainScreen(screen)
    }

    private fun showMainScreen(screen: Screen) {
        val safeScreen = if (screen == Screen.SETTINGS) Screen.INVENTORY else screen
        val targetIndex = mainScreens.indexOf(safeScreen)
        if (targetIndex == -1) return

        binding.mainPager.visibility = View.VISIBLE
        binding.settingsContainer.visibility = View.GONE
        binding.bottomNavigation.visibility = View.VISIBLE
        binding.topAppBar.navigationIcon = null
        binding.topAppBar.setNavigationOnClickListener(null)

        if (binding.mainPager.currentItem != targetIndex) {
            binding.mainPager.setCurrentItem(targetIndex, false)
        }
        currentScreen = safeScreen
        lastMainScreen = safeScreen
        binding.topAppBar.title = getString(titleFor(safeScreen))
        invalidateOptionsMenu()
    }

    private fun titleFor(screen: Screen): Int = when (screen) {
        Screen.INVENTORY -> R.string.title_inventory
        Screen.WISHLIST -> R.string.title_wishlist
        Screen.RECIPES -> R.string.title_recipes
        Screen.BARCODE -> R.string.title_barcode
        Screen.PRODUCTS -> R.string.title_products
        Screen.SETTINGS -> R.string.title_settings
    }

    private fun menuIdFor(screen: Screen): Int = when (screen) {
        Screen.INVENTORY -> R.id.menu_inventory
        Screen.WISHLIST -> R.id.menu_wishlist
        Screen.RECIPES -> R.id.menu_recipes
        Screen.BARCODE -> R.id.menu_barcode
        Screen.PRODUCTS -> R.id.menu_products
        Screen.SETTINGS -> R.id.menu_inventory
    }

    private inner class MainPagerAdapter : FragmentStateAdapter(this) {
        override fun getItemCount(): Int = mainScreens.size

        override fun createFragment(position: Int): Fragment {
            return when (mainScreens[position]) {
                Screen.INVENTORY -> InventoryFragment()
                Screen.WISHLIST -> WishlistFragment()
                Screen.RECIPES -> RecipeFragment()
                Screen.BARCODE -> BarcodeFragment()
                Screen.PRODUCTS -> ProductFragment()
                Screen.SETTINGS -> InventoryFragment()
            }
        }
    }
}
