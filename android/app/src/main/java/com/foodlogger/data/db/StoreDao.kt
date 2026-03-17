package com.foodlogger.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(store: StoreEntity): Long

    @Update
    suspend fun update(store: StoreEntity)

    @Query("DELETE FROM stores WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM stores ORDER BY name COLLATE NOCASE ASC")
    fun getAllStoresFlow(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM stores WHERE id = :id")
    suspend fun getById(id: Int): StoreEntity?

    @Query("SELECT * FROM stores WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): StoreEntity?
}
