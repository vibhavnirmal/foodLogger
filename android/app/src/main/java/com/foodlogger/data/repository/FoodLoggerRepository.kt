package com.foodlogger.data.repository

import androidx.room.withTransaction
import com.foodlogger.data.db.FoodLoggerDatabase
import com.foodlogger.data.db.InventoryEntity
import com.foodlogger.data.db.ProductEntity
import com.foodlogger.data.db.RecipeEntity
import com.foodlogger.data.db.RecipeIngredientEntity
import com.foodlogger.data.db.TimeType
import com.foodlogger.domain.model.ExpiryStatus
import com.foodlogger.domain.model.InventoryItem
import com.foodlogger.domain.model.Product
import com.foodlogger.domain.model.Recipe
import com.foodlogger.domain.model.RecipeIngredient
import com.foodlogger.domain.model.RecipeIngredientDraft
import com.foodlogger.domain.model.Store
import com.foodlogger.domain.model.StorageLocation
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDateTime
import javax.inject.Inject

class FoodLoggerRepository @Inject constructor(
    private val database: FoodLoggerDatabase
) {
    private val httpClient = OkHttpClient()

    // PRODUCT operations
    suspend fun addProduct(product: Product) {
        database.productDao().insertProduct(
            ProductEntity(
                barcode = product.barcode,
                name = product.name,
                brand = product.brand,
                category = product.category,
                servingSize = product.servingSize,
                kcal = product.kcal,
                protein = product.protein,
                carbs = product.carbs,
                fat = product.fat
            )
        )
    }

    suspend fun getProduct(barcode: String): Product? {
        val normalizedBarcode = barcode.filter { it.isDigit() }

        val localProduct = database.productDao().getProductByBarcode(normalizedBarcode)?.toProduct()
        if (localProduct != null) {
            return localProduct
        }

        val remoteProduct = fetchOpenFoodFactsProduct(normalizedBarcode)
        if (remoteProduct != null) {
            addProduct(remoteProduct)
        }

        return remoteProduct
    }

    fun getAllProducts(): Flow<List<Product>> {
        return database.productDao().getAllProducts().combine(database.inventoryDao().getAllInventory()) { products, _ ->
            products.map { it.toProduct() }
        }
    }

    fun searchProducts(query: String): Flow<List<Product>> {
        return if (query.isBlank()) {
            getAllProducts()
        } else {
            database.productDao().searchProducts(query).combine(database.inventoryDao().getAllInventory()) { products, _ ->
                products.map { it.toProduct() }
            }
        }
    }

    suspend fun updateProduct(product: Product) {
        database.productDao().updateProduct(
            ProductEntity(
                barcode = product.barcode,
                name = product.name,
                brand = product.brand,
                category = product.category,
                servingSize = product.servingSize,
                kcal = product.kcal,
                protein = product.protein,
                carbs = product.carbs,
                fat = product.fat,
                lastUpdated = LocalDateTime.now()
            )
        )
    }

    suspend fun deleteProduct(barcode: String) {
        database.productDao().deleteProductByBarcode(barcode)
    }

    // INVENTORY operations
    suspend fun addInventoryItem(
        barcode: String,
        quantity: Float,
        unit: String,
        dateBought: LocalDateTime?,
        expiryDate: LocalDateTime?,
        storageLocation: String?,
        boughtFromStoreId: Int?,
        nameOverride: String?
    ): Long {
        return database.inventoryDao().insertInventory(
            InventoryEntity(
                barcode = barcode,
                quantity = quantity,
                unit = unit,
                dateBought = dateBought,
                expiryDate = expiryDate,
                storageLocation = storageLocation,
                boughtFromStoreId = boughtFromStoreId,
                nameOverride = nameOverride,
                almostFinished = false
            )
        )
    }

    suspend fun addProductWithInventory(
        product: Product,
        quantity: Float,
        unit: String,
        dateBought: LocalDateTime?,
        expiryDate: LocalDateTime?,
        storageLocation: String?,
        boughtFromStoreId: Int?,
        nameOverride: String?
    ): Long {
        return database.withTransaction {
            addProduct(product)
            addInventoryItem(
                barcode = product.barcode,
                quantity = quantity,
                unit = unit,
                dateBought = dateBought,
                expiryDate = expiryDate,
                storageLocation = storageLocation,
                boughtFromStoreId = boughtFromStoreId,
                nameOverride = nameOverride
            )
        }
    }

    fun getAllInventory(): Flow<List<InventoryItem>> {
        return combine(
            database.inventoryDao().getAllInventory(),
            database.productDao().getAllProducts(),
            database.storeDao().getAllStoresFlow()
        ) { inventory, products, stores ->
            val productMap = products.associateBy { it.barcode }
            val storeMap = stores.associateBy { it.id }
            inventory.map { inventoryEntity ->
                val product = productMap[inventoryEntity.barcode]
                val store = inventoryEntity.boughtFromStoreId?.let { storeMap[it] }
                inventoryEntity.toInventoryItem(
                    productName = product?.name ?: inventoryEntity.barcode,
                    storeName = store?.name,
                    storeImageUri = store?.imageUri
                )
            }
        }
    }

    fun getAlmostFinishedItems(): Flow<List<InventoryItem>> {
        return combine(
            database.inventoryDao().getAlmostFinishedItems(),
            database.productDao().getAllProducts(),
            database.storeDao().getAllStoresFlow()
        ) { inventory, products, stores ->
            val productMap = products.associateBy { it.barcode }
            val storeMap = stores.associateBy { it.id }
            inventory.map { inventoryEntity ->
                val product = productMap[inventoryEntity.barcode]
                val store = inventoryEntity.boughtFromStoreId?.let { storeMap[it] }
                inventoryEntity.toInventoryItem(
                    productName = product?.name ?: inventoryEntity.barcode,
                    storeName = store?.name,
                    storeImageUri = store?.imageUri
                )
            }
        }
    }

    suspend fun updateInventoryItem(
        id: Int,
        quantity: Float,
        expiryDate: LocalDateTime?,
        storageLocation: String?,
        boughtFromStoreId: Int?,
        nameOverride: String?,
        almostFinished: Boolean
    ) {
        val existingItem = database.inventoryDao().getInventoryById(id) ?: return
        database.inventoryDao().updateInventory(
            existingItem.copy(
                quantity = quantity,
                expiryDate = expiryDate,
                storageLocation = storageLocation,
                boughtFromStoreId = boughtFromStoreId,
                nameOverride = nameOverride,
                almostFinished = almostFinished
            )
        )
    }

    suspend fun deleteInventoryItem(id: Int) {
        database.inventoryDao().deleteInventoryById(id)
    }

    // STORAGE LOCATION operations
    fun getAllStorageLocations(): Flow<List<StorageLocation>> {
        return database.storageLocationDao().getAllLocationsFlow().combine(database.inventoryDao().getAllInventory()) { locations, _ ->
            locations.map { StorageLocation(id = it.id, name = it.name) }
        }
    }

    suspend fun addStorageLocation(name: String) {
        val normalizedName = name.trim()
        require(normalizedName.isNotBlank()) { "Storage location name is required" }
        require(database.storageLocationDao().getByName(normalizedName) == null) { "Storage location already exists" }

        database.storageLocationDao().insert(
            com.foodlogger.data.db.StorageLocationEntity(name = normalizedName)
        )
    }

    suspend fun renameStorageLocation(locationId: Int, newName: String) {
        val normalizedName = newName.trim()
        require(normalizedName.isNotBlank()) { "Storage location name is required" }

        val existing = database.storageLocationDao().getById(locationId)
            ?: throw IllegalArgumentException("Storage location not found")

        val duplicate = database.storageLocationDao().getByName(normalizedName)
        require(duplicate == null || duplicate.id == locationId) { "Storage location already exists" }

        if (existing.name != normalizedName) {
            database.withTransaction {
                database.storageLocationDao().update(existing.copy(name = normalizedName))
                database.inventoryDao().renameStorageLocationReferences(existing.name, normalizedName)
            }
        }
    }

    suspend fun deleteStorageLocation(locationId: Int) {
        val existing = database.storageLocationDao().getById(locationId)
            ?: throw IllegalArgumentException("Storage location not found")

        val usageCount = database.inventoryDao().countByStorageLocation(existing.name)
        require(usageCount == 0) { "Location is used by inventory items. Reassign those items before deleting." }

        database.storageLocationDao().deleteById(locationId)
    }

    // STORE operations
    fun getAllStores(): Flow<List<Store>> {
        return database.storeDao().getAllStoresFlow().combine(database.inventoryDao().getAllInventory()) { stores, _ ->
            stores.map { Store(id = it.id, name = it.name, imageUri = it.imageUri) }
        }
    }

    suspend fun addStore(name: String, imageUri: String? = null) {
        val normalizedName = name.trim()
        require(normalizedName.isNotBlank()) { "Store name is required" }
        require(database.storeDao().getByName(normalizedName) == null) { "Store already exists" }

        database.storeDao().insert(
            com.foodlogger.data.db.StoreEntity(name = normalizedName, imageUri = imageUri)
        )
    }

    suspend fun renameStore(storeId: Int, newName: String) {
        val normalizedName = newName.trim()
        require(normalizedName.isNotBlank()) { "Store name is required" }

        val existing = database.storeDao().getById(storeId)
            ?: throw IllegalArgumentException("Store not found")

        val duplicate = database.storeDao().getByName(normalizedName)
        require(duplicate == null || duplicate.id == storeId) { "Store already exists" }

        if (existing.name != normalizedName) {
            database.storeDao().update(existing.copy(name = normalizedName))
        }
    }

    suspend fun updateStoreImage(storeId: Int, imageUri: String?) {
        val existing = database.storeDao().getById(storeId)
            ?: throw IllegalArgumentException("Store not found")
        database.storeDao().update(existing.copy(imageUri = imageUri))
    }

    suspend fun deleteStore(storeId: Int) {
        val existing = database.storeDao().getById(storeId)
            ?: throw IllegalArgumentException("Store not found")

        val usageCount = database.inventoryDao().countByBoughtFromStoreId(existing.id)
        require(usageCount == 0) { "Store is used by inventory items. Reassign those items before deleting." }

        database.storeDao().deleteById(storeId)
    }

    // RECIPE operations
    suspend fun addRecipe(name: String, timeType: String, ingredients: List<RecipeIngredientDraft>): Long {
        require(ingredients.isNotEmpty()) { "Recipe must include at least one ingredient" }

        return database.withTransaction {
            ingredients.forEach { ingredient ->
                require(database.productDao().getProductByBarcode(ingredient.barcode) != null) {
                    "Unknown product for barcode ${ingredient.barcode}"
                }
            }

            val recipeEntity = RecipeEntity(
                name = name,
                timeType = TimeType.valueOf(timeType.uppercase())
            )
            val recipeId = database.recipeDao().insertRecipe(recipeEntity)

            ingredients.forEach { ingredient ->
                database.recipeIngredientDao().insertIngredient(
                    RecipeIngredientEntity(
                        recipeId = recipeId.toInt(),
                        barcode = ingredient.barcode,
                        quantity = ingredient.quantity,
                        unit = ingredient.unit
                    )
                )
            }

            recipeId
        }
    }

    fun getAllRecipes(): Flow<List<Recipe>> {
        return combine(
            database.recipeDao().getAllRecipes(),
            database.recipeIngredientDao().getAllIngredientsFlow(),
            database.productDao().getAllProducts()
        ) { recipes, ingredients, products ->
            val productMap = products.associateBy { it.barcode }
            val ingredientsByRecipeId = ingredients.groupBy { it.recipeId }

            recipes.map { recipe ->
                recipe.toRecipe(
                    ingredients = ingredientsByRecipeId[recipe.id].orEmpty().map { ingredient ->
                        ingredient.toRecipeIngredient(
                            productName = productMap[ingredient.barcode]?.name ?: ingredient.barcode
                        )
                    }
                )
            }
        }
    }

    suspend fun deleteRecipe(id: Int) {
        database.recipeDao().deleteRecipeById(id)
    }

    // Extension functions for mapping
    private fun ProductEntity.toProduct(): Product = Product(
        barcode = barcode,
        name = name,
        brand = brand,
        category = category,
        servingSize = servingSize,
        kcal = kcal,
        protein = protein,
        carbs = carbs,
        fat = fat,
        createdAt = createdAt,
        lastUpdated = lastUpdated
    )

    private fun InventoryEntity.toInventoryItem(
        productName: String,
        storeName: String?,
        storeImageUri: String?
    ): InventoryItem {
        val now = LocalDateTime.now()
        val expiryStatus = when {
            expiryDate == null -> ExpiryStatus.GOOD
            expiryDate.isBefore(now) -> ExpiryStatus.EXPIRED
            expiryDate.isBefore(now.plusDays(7)) -> ExpiryStatus.EXPIRING_SOON
            else -> ExpiryStatus.GOOD
        }
        
        return InventoryItem(
            id = id,
            barcode = barcode,
            productName = productName,
            quantity = quantity,
            unit = unit,
            dateBought = dateBought,
            expiryDate = expiryDate,
            storageLocation = storageLocation,
            boughtFromStoreId = boughtFromStoreId,
            boughtFromStoreName = storeName,
            boughtFromStoreImageUri = storeImageUri,
            nameOverride = nameOverride,
            almostFinished = almostFinished,
            dateCreated = dateCreated,
            expiryStatus = expiryStatus
        )
    }

    private fun RecipeEntity.toRecipe(ingredients: List<RecipeIngredient>): Recipe = Recipe(
        id = id,
        name = name,
        timeType = com.foodlogger.domain.model.TimeType.valueOf(timeType.name),
        ingredients = ingredients,
        createdAt = createdAt
    )

    private fun RecipeIngredientEntity.toRecipeIngredient(productName: String): RecipeIngredient = RecipeIngredient(
        id = id,
        recipeId = recipeId,
        barcode = barcode,
        productName = productName,
        quantity = quantity,
        unit = unit,
    )

    private suspend fun fetchOpenFoodFactsProduct(barcode: String): Product? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://world.openfoodfacts.org/api/v2/product/$barcode.json")
            .header("User-Agent", "foodLogger/1.0 (Android)")
            .build()

        runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use null
                }

                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    return@use null
                }

                val root = JsonParser.parseString(body).asJsonObject
                if (root.get("status")?.asInt != 1) {
                    return@use null
                }

                val productJson = root.getAsJsonObject("product") ?: return@use null
                val productName = productJson.getStringOrNull("product_name") ?: return@use null

                Product(
                    barcode = barcode,
                    name = productName,
                    brand = productJson.getStringOrNull("brands"),
                    category = productJson.getStringOrNull("categories"),
                    servingSize = productJson.getStringOrNull("serving_size"),
                    kcal = productJson.getAsJsonObject("nutriments")?.getFloatOrNull("energy-kcal_100g"),
                    protein = productJson.getAsJsonObject("nutriments")?.getFloatOrNull("proteins_100g"),
                    carbs = productJson.getAsJsonObject("nutriments")?.getFloatOrNull("carbohydrates_100g"),
                    fat = productJson.getAsJsonObject("nutriments")?.getFloatOrNull("fat_100g")
                )
            }
        }.getOrNull()
    }

    private fun com.google.gson.JsonObject.getStringOrNull(key: String): String? {
        val element = get(key) ?: return null
        if (element.isJsonNull) {
            return null
        }

        return element.asString
            .trim()
            .takeIf { it.isNotEmpty() }
    }

    private fun com.google.gson.JsonObject.getFloatOrNull(key: String): Float? {
        val element = get(key) ?: return null
        if (element.isJsonNull) {
            return null
        }

        return runCatching { element.asFloat }.getOrNull()
    }
}
