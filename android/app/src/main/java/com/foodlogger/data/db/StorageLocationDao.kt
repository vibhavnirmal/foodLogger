package com.foodlogger.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageLocationDao {
    @Query("SELECT * FROM storage_locations ORDER BY name COLLATE NOCASE ASC")
    fun getAllLocationsFlow(): Flow<List<StorageLocationEntity>>

    @Query("SELECT * FROM storage_locations WHERE id = :id")
    suspend fun getById(id: Int): StorageLocationEntity?

    @Query("SELECT * FROM storage_locations WHERE lower(name) = lower(:name) LIMIT 1")
    suspend fun getByName(name: String): StorageLocationEntity?

    @Insert
    suspend fun insert(location: StorageLocationEntity): Long

    @Update
    suspend fun update(location: StorageLocationEntity)

    @Query("DELETE FROM storage_locations WHERE id = :id")
    suspend fun deleteById(id: Int)
}
