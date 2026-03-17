package com.foodlogger.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface InventoryDao {
    @Insert
    suspend fun insertInventory(inventory: InventoryEntity): Long

    @Query("SELECT COUNT(*) FROM inventory")
    suspend fun getInventoryCount(): Int

    @Query("SELECT * FROM inventory WHERE id = :id")
    suspend fun getInventoryById(id: Int): InventoryEntity?

    @Query("SELECT COUNT(*) FROM inventory WHERE storageLocation = :locationName")
    suspend fun countByStorageLocation(locationName: String): Int

    @Query("SELECT COUNT(*) FROM inventory WHERE boughtFromStoreId = :storeId")
    suspend fun countByBoughtFromStoreId(storeId: Int): Int

    @Query("UPDATE inventory SET storageLocation = :newName WHERE storageLocation = :oldName")
    suspend fun renameStorageLocationReferences(oldName: String, newName: String)

    @Query("""
        SELECT * FROM inventory 
        ORDER BY 
            CASE 
                WHEN expiryDate IS NULL THEN 1
                WHEN expiryDate < datetime('now') THEN 0
                WHEN expiryDate < datetime('now', '+7 days') THEN 1
                ELSE 2
            END ASC,
            expiryDate ASC
    """)
    fun getAllInventory(): Flow<List<InventoryEntity>>

    @Query("""
        SELECT * FROM inventory 
        WHERE storageLocation = :location
        ORDER BY expiryDate ASC
    """)
    fun getInventoryByLocation(location: String): Flow<List<InventoryEntity>>

    @Query("""
        SELECT * FROM inventory 
        WHERE almostFinished = 1
        ORDER BY dateCreated DESC
    """)
    fun getAlmostFinishedItems(): Flow<List<InventoryEntity>>

    @Update
    suspend fun updateInventory(inventory: InventoryEntity)

    @Delete
    suspend fun deleteInventory(inventory: InventoryEntity)

    @Query("DELETE FROM inventory WHERE id = :id")
    suspend fun deleteInventoryById(id: Int)
}
