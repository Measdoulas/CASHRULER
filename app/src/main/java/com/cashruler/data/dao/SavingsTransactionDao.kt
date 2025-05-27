package com.cashruler.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cashruler.data.models.SavingsTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: SavingsTransaction): Long

    @Update
    suspend fun update(transaction: SavingsTransaction)

    @Delete
    suspend fun delete(transaction: SavingsTransaction)

    @Query("SELECT * FROM savings_transactions WHERE id = :id")
    suspend fun getById(id: Long): SavingsTransaction?

    @Query("SELECT * FROM savings_transactions WHERE projectId = :projectId ORDER BY transactionDate DESC, createdAt DESC")
    fun getTransactionsForProject(projectId: Long): Flow<List<SavingsTransaction>>

    @Query("SELECT SUM(CASE WHEN type = 'DEPOSIT' THEN amount ELSE -amount END) FROM savings_transactions WHERE projectId = :projectId")
    suspend fun getTotalAmountForProject(projectId: Long): Double?

    @Query("DELETE FROM savings_transactions WHERE projectId = :projectId")
    suspend fun deleteTransactionsForProject(projectId: Long)
}
