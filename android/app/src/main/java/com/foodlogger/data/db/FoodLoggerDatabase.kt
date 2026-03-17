package com.foodlogger.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProductEntity::class,
        InventoryEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class
    ],
    version = 1
)
@TypeConverters(LocalDateTimeConverter::class)
abstract class FoodLoggerDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun recipeDao(): RecipeDao
    abstract fun recipeIngredientDao(): RecipeIngredientDao

    companion object {
        @Volatile
        private var INSTANCE: FoodLoggerDatabase? = null

        fun getDatabase(context: Context): FoodLoggerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FoodLoggerDatabase::class.java,
                    "foodlogger.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): FoodLoggerDatabase {
            return getDatabase(context)
        }
    }
}
