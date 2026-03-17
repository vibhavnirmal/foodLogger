package com.foodlogger.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class,
        InventoryEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        StorageLocationEntity::class,
        StoreEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(LocalDateTimeConverter::class)
abstract class FoodLoggerDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun recipeDao(): RecipeDao
    abstract fun recipeIngredientDao(): RecipeIngredientDao
    abstract fun storageLocationDao(): StorageLocationDao
    abstract fun storeDao(): StoreDao

    companion object {
        private val DEFAULT_STORAGE_LOCATIONS = listOf("Fridge", "Freezer", "Pantry", "Kitchen Shelf")
        private val DEFAULT_STORES = listOf(
            "Walmart" to "android.resource://com.foodlogger/drawable/store_walmart",
            "Costco" to "android.resource://com.foodlogger/drawable/store_costco"
        )

        private fun seedDefaultStorageLocations(db: SupportSQLiteDatabase) {
            DEFAULT_STORAGE_LOCATIONS.forEach { location ->
                val escaped = location.replace("'", "''")
                db.execSQL("INSERT OR IGNORE INTO storage_locations(name) VALUES('$escaped')")
            }
        }

        private fun seedDefaultStores(db: SupportSQLiteDatabase) {
            DEFAULT_STORES.forEach { (name, imageUri) ->
                val escapedName = name.replace("'", "''")
                val escapedUri = imageUri.replace("'", "''")
                db.execSQL(
                    "INSERT OR IGNORE INTO stores(name, imageUri) VALUES('$escapedName', '$escapedUri')"
                )
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS storage_locations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_storage_locations_name ON storage_locations(name)"
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO storage_locations(name)
                    SELECT DISTINCT trim(storageLocation)
                    FROM inventory
                    WHERE storageLocation IS NOT NULL AND trim(storageLocation) != ''
                    """.trimIndent()
                )

                seedDefaultStorageLocations(db)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS stores (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        imageUri TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_stores_name ON stores(name)"
                )
                db.execSQL(
                    "ALTER TABLE inventory ADD COLUMN boughtFromStoreId INTEGER"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_inventory_boughtFromStoreId ON inventory(boughtFromStoreId)"
                )

                seedDefaultStores(db)
            }
        }

        @Volatile
        private var INSTANCE: FoodLoggerDatabase? = null

        fun getDatabase(context: Context): FoodLoggerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FoodLoggerDatabase::class.java,
                    "foodlogger.db"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedDefaultStorageLocations(db)
                            seedDefaultStores(db)
                        }
                    })
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): FoodLoggerDatabase {
            return getDatabase(context)
        }
    }
}
