package com.foodlogger.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int

    @Query("SELECT * FROM products WHERE barcode = :barcode")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): ProductEntity?

    @Query("SELECT * FROM products WHERE lower(trim(name)) = lower(trim(:name)) ORDER BY id ASC")
    suspend fun getProductsByNormalizedName(name: String): List<ProductEntity>

    @Query("SELECT lower(trim(name)) FROM products WHERE lower(trim(name)) IN (:normalizedNames)")
    suspend fun getExistingNormalizedNames(normalizedNames: List<String>): List<String>

    @Query(
        """
        SELECT * FROM products
        WHERE lower(trim(name)) IN (
            SELECT lower(trim(name))
            FROM products
            GROUP BY lower(trim(name))
            HAVING COUNT(*) > 1
        )
        ORDER BY lower(trim(name)) ASC, id ASC
        """
    )
    suspend fun getDuplicateNameProducts(): List<ProductEntity>

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE barcode = :barcode")
    suspend fun deleteProductByBarcode(barcode: String)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Int)
}
