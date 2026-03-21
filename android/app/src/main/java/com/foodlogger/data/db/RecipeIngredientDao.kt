package com.foodlogger.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeIngredientDao {
    @Insert
    suspend fun insertIngredient(ingredient: RecipeIngredientEntity)

    @Query("SELECT * FROM recipe_ingredients")
    fun getAllIngredientsFlow(): Flow<List<RecipeIngredientEntity>>

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun getIngredientsForRecipe(recipeId: Int): List<RecipeIngredientEntity>

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId")
    fun getIngredientsForRecipeFlow(recipeId: Int): Flow<List<RecipeIngredientEntity>>

    @Query("UPDATE recipe_ingredients SET productId = :newProductId WHERE productId = :oldProductId")
    suspend fun reassignProductId(oldProductId: Int, newProductId: Int)

    @Update
    suspend fun updateIngredient(ingredient: RecipeIngredientEntity)

    @Delete
    suspend fun deleteIngredient(ingredient: RecipeIngredientEntity)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsForRecipe(recipeId: Int)
}
