package com.foodlogger.data.mock

import com.foodlogger.domain.model.ExpiryStatus
import com.foodlogger.domain.model.InventoryItem
import com.foodlogger.domain.model.Product
import com.foodlogger.domain.model.Recipe
import com.foodlogger.domain.model.RecipeIngredient
import com.foodlogger.domain.model.TimeType
import java.time.LocalDateTime

object MockDataProvider {

    fun getMockProducts(): List<Product> = listOf(
        Product(
            barcode = "5901234123457",
            name = "Whole Wheat Bread",
            brand = "Artisan Bakery",
            category = "Grains",
            servingSize = "100g",
            kcal = 265f,
            protein = 13f,
            carbs = 49f,
            fat = 3.3f
        ),
        Product(
            barcode = "5901234123458",
            name = "Greek Yogurt",
            brand = "Fage",
            category = "Dairy",
            servingSize = "100g",
            kcal = 59f,
            protein = 10f,
            carbs = 3.3f,
            fat = 0.4f
        ),
        Product(
            barcode = "5901234123459",
            name = "Organic Milk",
            brand = "LocalFarm",
            category = "Dairy",
            servingSize = "200ml",
            kcal = 134f,
            protein = 6.6f,
            carbs = 9.8f,
            fat = 7.2f
        ),
        Product(
            barcode = "5901234123460",
            name = "Free-Range Eggs",
            brand = "Happy Hens",
            category = "Protein",
            servingSize = "1 egg",
            kcal = 155f,
            protein = 13f,
            carbs = 1.1f,
            fat = 11f
        ),
        Product(
            barcode = "5901234123461",
            name = "Salmon Fillet",
            brand = "Wild Catch",
            category = "Protein",
            servingSize = "100g",
            kcal = 208f,
            protein = 20f,
            carbs = 0f,
            fat = 13f
        ),
        Product(
            barcode = "5901234123462",
            name = "Spinach",
            brand = "Fresh Greens Co",
            category = "Vegetables",
            servingSize = "100g",
            kcal = 23f,
            protein = 2.7f,
            carbs = 3.6f,
            fat = 0.4f
        ),
        Product(
            barcode = "5901234123463",
            name = "Chicken Breast",
            brand = "Premium Poultry",
            category = "Protein",
            servingSize = "100g",
            kcal = 165f,
            protein = 31f,
            carbs = 0f,
            fat = 3.6f
        ),
        Product(
            barcode = "5901234123464",
            name = "Cheddar Cheese",
            brand = "Dairy Delights",
            category = "Dairy",
            servingSize = "30g",
            kcal = 121f,
            protein = 7f,
            carbs = 0.4f,
            fat = 10f
        )
    )

    fun getMockInventoryItems(): List<InventoryItem> {
        val now = LocalDateTime.now()
        return listOf(
            InventoryItem(
                id = 1,
                barcode = "5901234123457",
                productName = "Whole Wheat Bread",
                quantity = 2f,
                unit = "loaves",
                dateBought = now.minusDays(3),
                expiryDate = now.plusDays(2),
                storageLocation = "Kitchen Counter",
                nameOverride = null,
                almostFinished = false,
                expiryStatus = ExpiryStatus.EXPIRING_SOON
            ),
            InventoryItem(
                id = 2,
                barcode = "5901234123458",
                productName = "Greek Yogurt",
                quantity = 1f,
                unit = "container",
                dateBought = now.minusDays(1),
                expiryDate = now.plusDays(15),
                storageLocation = "Fridge",
                nameOverride = null,
                almostFinished = false,
                expiryStatus = ExpiryStatus.GOOD
            ),
            InventoryItem(
                id = 3,
                barcode = "5901234123459",
                productName = "Organic Milk",
                quantity = 1f,
                unit = "liter",
                dateBought = now.minusDays(2),
                expiryDate = now.minusDays(1),
                storageLocation = "Fridge",
                nameOverride = "Fresh Organic Milk",
                almostFinished = true,
                expiryStatus = ExpiryStatus.EXPIRED
            ),
            InventoryItem(
                id = 4,
                barcode = "5901234123460",
                productName = "Free-Range Eggs",
                quantity = 6f,
                unit = "eggs",
                dateBought = now.minusDays(7),
                expiryDate = now.plusDays(8),
                storageLocation = "Fridge",
                nameOverride = null,
                almostFinished = false,
                expiryStatus = ExpiryStatus.GOOD
            ),
            InventoryItem(
                id = 5,
                barcode = "5901234123461",
                productName = "Salmon Fillet",
                quantity = 300f,
                unit = "g",
                dateBought = now.minusDays(1),
                expiryDate = now.plusDays(3),
                storageLocation = "Freezer",
                nameOverride = null,
                almostFinished = false,
                expiryStatus = ExpiryStatus.GOOD
            ),
            InventoryItem(
                id = 6,
                barcode = "5901234123462",
                productName = "Spinach",
                quantity = 200f,
                unit = "g",
                dateBought = now.minusDays(4),
                expiryDate = now.plusDays(1),
                storageLocation = "Fridge",
                nameOverride = null,
                almostFinished = true,
                expiryStatus = ExpiryStatus.EXPIRING_SOON
            ),
            InventoryItem(
                id = 7,
                barcode = "5901234123463",
                productName = "Chicken Breast",
                quantity = 500f,
                unit = "g",
                dateBought = now.minusDays(3),
                expiryDate = now.plusDays(5),
                storageLocation = "Freezer",
                nameOverride = null,
                almostFinished = false,
                expiryStatus = ExpiryStatus.GOOD
            ),
            InventoryItem(
                id = 8,
                barcode = "5901234123464",
                productName = "Cheddar Cheese",
                quantity = 150f,
                unit = "g",
                dateBought = now.minusDays(5),
                expiryDate = now.plusDays(20),
                storageLocation = "Fridge",
                nameOverride = null,
                almostFinished = false,
                expiryStatus = ExpiryStatus.GOOD
            )
        )
    }

    fun getMockRecipes(): List<Recipe> = listOf(
        Recipe(
            id = 1,
            name = "Spinach & Egg Breakfast",
            timeType = TimeType.VERY_FAST,
            ingredients = listOf(
                RecipeIngredient(id = 1, recipeId = 1, barcode = "5901234123460", productName = "Eggs", quantity = 2f, unit = "eggs"),
                RecipeIngredient(id = 2, recipeId = 1, barcode = "5901234123462", productName = "Spinach", quantity = 100f, unit = "g")
            )
        ),
        Recipe(
            id = 2,
            name = "Grilled Salmon with Veggies",
            timeType = TimeType.MODERATE,
            ingredients = listOf(
                RecipeIngredient(id = 3, recipeId = 2, barcode = "5901234123461", productName = "Salmon", quantity = 200f, unit = "g"),
                RecipeIngredient(id = 4, recipeId = 2, barcode = "5901234123462", productName = "Spinach", quantity = 150f, unit = "g")
            )
        ),
        Recipe(
            id = 3,
            name = "Slow Roasted Chicken",
            timeType = TimeType.SLOW,
            ingredients = listOf(
                RecipeIngredient(id = 5, recipeId = 3, barcode = "5901234123463", productName = "Chicken", quantity = 500f, unit = "g")
            )
        ),
        Recipe(
            id = 4,
            name = "Cheesy Eggs Toast",
            timeType = TimeType.VERY_FAST,
            ingredients = listOf(
                RecipeIngredient(id = 6, recipeId = 4, barcode = "5901234123460", productName = "Eggs", quantity = 3f, unit = "eggs"),
                RecipeIngredient(id = 7, recipeId = 4, barcode = "5901234123464", productName = "Cheese", quantity = 50f, unit = "g"),
                RecipeIngredient(id = 8, recipeId = 4, barcode = "5901234123457", productName = "Bread", quantity = 2f, unit = "slices")
            )
        ),
        Recipe(
            id = 5,
            name = "Yogurt Berry Parfait",
            timeType = TimeType.VERY_FAST,
            ingredients = listOf(
                RecipeIngredient(id = 9, recipeId = 5, barcode = "5901234123458", productName = "Greek Yogurt", quantity = 200f, unit = "g")
            )
        )
    )
}
