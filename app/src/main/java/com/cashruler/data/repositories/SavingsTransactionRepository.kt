package com.cashruler.data.repositories

import com.cashruler.data.dao.SavingsTransactionDao
import com.cashruler.data.models.SavingsTransaction
import com.cashruler.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface SavingsTransactionRepositoryInterface {
    suspend fun addTransaction(transaction: SavingsTransaction): Long
    suspend fun updateTransaction(transaction: SavingsTransaction)
    suspend fun deleteTransaction(transaction: SavingsTransaction)
    suspend fun getTransactionById(id: Long): SavingsTransaction?
    fun getTransactionsForProject(projectId: Long): Flow<List<SavingsTransaction>>
    suspend fun getTotalAmountForProject(projectId: Long): Double
    suspend fun deleteTransactionsForProject(projectId: Long)
}

@Singleton
class SavingsTransactionRepository @Inject constructor(
    private val savingsTransactionDao: SavingsTransactionDao,
    @IODispatcher private val dispatcher: CoroutineDispatcher
) : SavingsTransactionRepositoryInterface {

    override suspend fun addTransaction(transaction: SavingsTransaction): Long = withContext(dispatcher) {
        return@withContext savingsTransactionDao.insert(transaction)
    }

    override suspend fun updateTransaction(transaction: SavingsTransaction) = withContext(dispatcher) {
        // Assuming SavingsTransaction has an `updatedAt` or similar field that should be set.
        // If not, this can be a direct call. For now, let's assume no such field or it's handled in model.
        savingsTransactionDao.update(transaction)
    }

    override suspend fun deleteTransaction(transaction: SavingsTransaction) = withContext(dispatcher) {
        savingsTransactionDao.delete(transaction)
    }

    override suspend fun getTransactionById(id: Long): SavingsTransaction? = withContext(dispatcher) {
        return@withContext savingsTransactionDao.getById(id)
    }

    override fun getTransactionsForProject(projectId: Long): Flow<List<SavingsTransaction>> {
        // As with other repositories, assuming DAO handles dispatching for Flow,
        // or .flowOn(dispatcher) could be added if necessary.
        return savingsTransactionDao.getTransactionsForProject(projectId)
    }

    override suspend fun getTotalAmountForProject(projectId: Long): Double = withContext(dispatcher) {
        return@withContext savingsTransactionDao.getTotalAmountForProject(projectId) ?: 0.0
    }

    override suspend fun deleteTransactionsForProject(projectId: Long) = withContext(dispatcher) {
        savingsTransactionDao.deleteTransactionsForProject(projectId)
    }
}
