package com.foodlogger.data.db

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideFoodLoggerDatabase(
        @ApplicationContext context: Context
    ): FoodLoggerDatabase {
        return FoodLoggerDatabase.getInstance(context)
    }

    @Singleton
    @Provides
    fun provideProductDao(database: FoodLoggerDatabase): ProductDao {
        return database.productDao()
    }

    @Singleton
    @Provides
    fun provideInventoryDao(database: FoodLoggerDatabase): InventoryDao {
        return database.inventoryDao()
    }

    @Singleton
    @Provides
    fun provideRecipeDao(database: FoodLoggerDatabase): RecipeDao {
        return database.recipeDao()
    }

    @Singleton
    @Provides
    fun provideRecipeIngredientDao(database: FoodLoggerDatabase): RecipeIngredientDao {
        return database.recipeIngredientDao()
    }

    @Singleton
    @Provides
    fun provideStorageLocationDao(database: FoodLoggerDatabase): StorageLocationDao {
        return database.storageLocationDao()
    }

    @Singleton
    @Provides
    fun provideStoreDao(database: FoodLoggerDatabase): StoreDao {
        return database.storeDao()
    }
}
