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
        StoreEntity::class,
        ReceiptEntity::class,
        CategoryEntity::class
    ],
    version = 10,
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
    abstract fun receiptDao(): ReceiptDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        private val DEFAULT_STORAGE_LOCATIONS = listOf("Fridge", "Freezer", "Pantry", "Kitchen Shelf")
        private val DEFAULT_STORES = listOf(
            "Walmart",
            "Costco",
            "Kroger",
            "Trader Joe's"
        )
        private val DEFAULT_CATEGORIES = listOf(
            "Produce", "Dairy", "Bakery",
            "Frozen", "Beverages", "Snacks", "Condiments", "Canned Goods",
            "Grains", "Pasta", "Breakfast", "Household", "Personal Care"
        )

        private fun seedDefaultStorageLocations(db: SupportSQLiteDatabase) {
            DEFAULT_STORAGE_LOCATIONS.forEach { location ->
                val escaped = location.replace("'", "''")
                db.execSQL("INSERT OR IGNORE INTO storage_locations(name) VALUES('$escaped')")
            }
        }

        private fun seedDefaultStores(db: SupportSQLiteDatabase) {
            DEFAULT_STORES.forEach { name ->
                val escapedName = name.replace("'", "''")
                db.execSQL(
                    "INSERT OR IGNORE INTO stores(name, imageUri) VALUES('$escapedName', NULL)"
                )
            }
        }

        private fun seedDefaultCategories(db: SupportSQLiteDatabase) {
            DEFAULT_CATEGORIES.forEach { category ->
                val escaped = category.replace("'", "''")
                db.execSQL("INSERT OR IGNORE INTO categories(name) VALUES('$escaped')")
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys = OFF")
                db.execSQL("PRAGMA legacy_alter_table = ON")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS products_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        barcode TEXT,
                        name TEXT NOT NULL,
                        brand TEXT,
                        category TEXT,
                        servingSize TEXT,
                        kcal REAL,
                        protein REAL,
                        carbs REAL,
                        fat REAL,
                        createdAt TEXT NOT NULL,
                        lastUpdated TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO products_new (barcode, name, brand, category, servingSize, kcal, protein, carbs, fat, createdAt, lastUpdated)
                    SELECT barcode, name, brand, category, servingSize, kcal, protein, carbs, fat, createdAt, lastUpdated FROM products
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE products")
                db.execSQL("ALTER TABLE products_new RENAME TO products")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_barcode ON products(barcode)")

                db.execSQL("ALTER TABLE inventory ADD COLUMN productId INTEGER")

                db.execSQL(
                    """
                    UPDATE inventory SET productId = (
                        SELECT p.id FROM products p WHERE p.barcode = inventory.barcode LIMIT 1
                    )
                    """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_productId ON inventory(productId)")
                db.execSQL("ALTER TABLE recipe_ingredients ADD COLUMN productId INTEGER")

                db.execSQL(
                    """
                    UPDATE recipe_ingredients SET productId = (
                        SELECT p.id FROM products p WHERE p.barcode = recipe_ingredients.barcode LIMIT 1
                    )
                    """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_ingredients_productId ON recipe_ingredients(productId)")
                db.execSQL("PRAGMA foreign_keys = ON")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS receipts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        imagePath TEXT NOT NULL,
                        storeName TEXT,
                        totalAmount REAL,
                        dateScanned TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("ALTER TABLE inventory ADD COLUMN receiptId INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_receiptId ON inventory(receiptId)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(receipts)")
                val existingColumns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    existingColumns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()
                
                if (!existingColumns.contains("dateShopped")) {
                    db.execSQL("ALTER TABLE receipts ADD COLUMN dateShopped TEXT")
                }
                if (!existingColumns.contains("storeId")) {
                    db.execSQL("ALTER TABLE receipts ADD COLUMN storeId INTEGER")
                }
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(products)")
                val existingColumns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    existingColumns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if (!existingColumns.contains("imagePath")) {
                    db.execSQL("ALTER TABLE products ADD COLUMN imagePath TEXT")
                }
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inventory RENAME TO inventory_old")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS inventory (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER NOT NULL,
                        dateBought TEXT,
                        expiryDate TEXT,
                        storageLocation TEXT,
                        boughtFromStoreId INTEGER,
                        nameOverride TEXT,
                        almostFinished INTEGER NOT NULL DEFAULT 0,
                        imageUri TEXT,
                        dateCreated TEXT NOT NULL,
                        receiptId INTEGER,
                        FOREIGN KEY(productId) REFERENCES products(id) ON DELETE CASCADE,
                        FOREIGN KEY(receiptId) REFERENCES receipts(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_productId ON inventory(productId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_expiryDate ON inventory(expiryDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_boughtFromStoreId ON inventory(boughtFromStoreId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_receiptId ON inventory(receiptId)")
                db.execSQL("""
                    INSERT INTO inventory (id, productId, dateBought, expiryDate, storageLocation, boughtFromStoreId, nameOverride, almostFinished, imageUri, dateCreated, receiptId)
                    SELECT id, productId, dateBought, expiryDate, storageLocation, boughtFromStoreId, nameOverride, almostFinished, imageUri, dateCreated, receiptId FROM inventory_old
                """.trimIndent())
                db.execSQL("DROP TABLE inventory_old")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipe_ingredients RENAME TO recipe_ingredients_old")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS recipe_ingredients (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recipeId INTEGER NOT NULL,
                        productId INTEGER NOT NULL,
                        FOREIGN KEY(recipeId) REFERENCES recipes(id) ON DELETE CASCADE,
                        FOREIGN KEY(productId) REFERENCES products(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_ingredients_recipeId ON recipe_ingredients(recipeId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_ingredients_productId ON recipe_ingredients(productId)")
                db.execSQL("""
                    INSERT INTO recipe_ingredients (id, recipeId, productId)
                    SELECT id, recipeId, productId FROM recipe_ingredients_old
                """.trimIndent())
                db.execSQL("DROP TABLE recipe_ingredients_old")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name ON categories(name)")
                seedDefaultCategories(db)
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
                            db.execSQL("PRAGMA foreign_keys = ON")
                            seedDefaultStorageLocations(db)
                            seedDefaultStores(db)
                            seedDefaultCategories(db)
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA foreign_keys = ON")
                        }
                    })
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
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
