package com.foodlogger.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ReceiptDao {
    @Insert
    suspend fun insertReceipt(receipt: ReceiptEntity): Long

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getReceiptById(id: Int): ReceiptEntity?

    @Query("SELECT * FROM receipts ORDER BY dateShopped DESC, dateScanned DESC")
    fun getAllReceiptsSortedByDate(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts ORDER BY dateScanned DESC")
    fun getAllReceipts(): Flow<List<ReceiptEntity>>

    @Query("UPDATE receipts SET dateShopped = :dateShopped, storeId = :storeId, storeName = :storeName, totalAmount = :totalAmount WHERE id = :id")
    suspend fun updateReceipt(id: Int, dateShopped: LocalDateTime?, storeId: Int?, storeName: String?, totalAmount: Float?)

    @Query("DELETE FROM receipts WHERE id = :id")
    suspend fun deleteReceipt(id: Int)

    @Query("SELECT * FROM receipts WHERE storeId = :storeId AND date(dateShopped) = date(:dateShopped) LIMIT 1")
    suspend fun findByDateAndStore(dateShopped: LocalDateTime, storeId: Int): ReceiptEntity?
}
