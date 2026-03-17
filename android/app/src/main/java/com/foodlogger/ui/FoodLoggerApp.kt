package com.foodlogger.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.foodlogger.ui.navigation.Screen
import com.foodlogger.ui.screens.InventoryScreen
import com.foodlogger.ui.screens.WishlistScreen
import com.foodlogger.ui.screens.RecipeScreen
import com.foodlogger.ui.screens.BarcodeScreen
import com.foodlogger.ui.screens.ProductScreen

@Composable
fun FoodLoggerApp() {
    var currentScreen by remember { mutableStateOf(Screen.INVENTORY) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == Screen.INVENTORY,
                    onClick = { currentScreen = Screen.INVENTORY },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inventory") },
                    label = { Text("Inventory") }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.WISHLIST,
                    onClick = { currentScreen = Screen.WISHLIST },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Wishlist") },
                    label = { Text("Wishlist") }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.RECIPES,
                    onClick = { currentScreen = Screen.RECIPES },
                    icon = { Icon(Icons.Default.LocalDining, contentDescription = "Recipes") },
                    label = { Text("Recipes") }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.BARCODE,
                    onClick = { currentScreen = Screen.BARCODE },
                    icon = { Icon(Icons.Default.QrCode2, contentDescription = "Barcode") },
                    label = { Text("Barcode") }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.PRODUCTS,
                    onClick = { currentScreen = Screen.PRODUCTS },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Products") },
                    label = { Text("Products") }
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                Screen.INVENTORY -> InventoryScreen()
                Screen.WISHLIST -> WishlistScreen()
                Screen.RECIPES -> RecipeScreen()
                Screen.BARCODE -> BarcodeScreen()
                Screen.PRODUCTS -> ProductScreen()
            }
        }
    }
}
