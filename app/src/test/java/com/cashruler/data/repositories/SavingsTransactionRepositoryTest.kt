package com.cashruler.data.repositories

import com.cashruler.data.dao.SavingsTransactionDao
import com.cashruler.data.models.SavingsTransaction
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

@ExperimentalCoroutinesApi
class SavingsTransactionRepositoryTest {

    private lateinit var repository: SavingsTransactionRepository
    private val mockDao: SavingsTransactionDao = mockk(relaxed = true)
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = SavingsTransactionRepository(mockDao, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `addTransaction calls dao insert`() = runTest(testDispatcher) {
        val transaction = SavingsTransaction(projectId = 1L, amount = 100.0, type = "DEPOSIT", transactionDate = Date())
        coEvery { mockDao.insert(transaction) } returns 1L

        val resultId = repository.addTransaction(transaction)

        coVerify { mockDao.insert(transaction) }
        assertEquals(1L, resultId)
    }

    @Test
    fun `updateTransaction calls dao update`() = runTest(testDispatcher) {
        val transaction = SavingsTransaction(id = 1L, projectId = 1L, amount = 150.0, type = "DEPOSIT", transactionDate = Date())
        coEvery { mockDao.update(transaction) } just Runs

        repository.updateTransaction(transaction)

        coVerify { mockDao.update(transaction) }
    }

    @Test
    fun `deleteTransaction calls dao delete`() = runTest(testDispatcher) {
        val transaction = SavingsTransaction(id = 1L, projectId = 1L, amount = 100.0, type = "DEPOSIT", transactionDate = Date())
        coEvery { mockDao.delete(transaction) } just Runs

        repository.deleteTransaction(transaction)

        coVerify { mockDao.delete(transaction) }
    }

    @Test
    fun `getTransactionById calls dao getById`() = runTest(testDispatcher) {
        val transactionId = 1L
        val expectedTransaction = SavingsTransaction(id = transactionId, projectId = 1L, amount = 100.0, type = "DEPOSIT", transactionDate = Date())
        coEvery { mockDao.getById(transactionId) } returns expectedTransaction

        val result = repository.getTransactionById(transactionId)

        coVerify { mockDao.getById(transactionId) }
        assertEquals(expectedTransaction, result)
    }

    @Test
    fun `getTransactionsForProject returns flow from dao`() = runTest(testDispatcher) {
        val projectId = 1L
        val transactions = listOf(
            SavingsTransaction(id = 1L, projectId = projectId, amount = 100.0, type = "DEPOSIT", transactionDate = Date()),
            SavingsTransaction(id = 2L, projectId = projectId, amount = 50.0, type = "WITHDRAWAL", transactionDate = Date())
        )
        coEvery { mockDao.getTransactionsForProject(projectId) } returns flowOf(transactions)

        val resultFlow = repository.getTransactionsForProject(projectId)
        
        assertEquals(transactions, resultFlow.first())
        coVerify { mockDao.getTransactionsForProject(projectId) } 
    }

    @Test
    fun `getTotalAmountForProject_daoReturnsNull_returnsZero`() = runTest(testDispatcher) {
        val projectId = 1L
        coEvery { mockDao.getTotalAmountForProject(projectId) } returns null

        val total = repository.getTotalAmountForProject(projectId)

        assertEquals(0.0, total, 0.001)
        coVerify { mockDao.getTotalAmountForProject(projectId) }
    }

    @Test
    fun `getTotalAmountForProject_daoReturnsValue_returnsValue`() = runTest(testDispatcher) {
        val projectId = 1L
        val expectedTotal = 250.75
        coEvery { mockDao.getTotalAmountForProject(projectId) } returns expectedTotal

        val total = repository.getTotalAmountForProject(projectId)

        assertEquals(expectedTotal, total, 0.001)
        coVerify { mockDao.getTotalAmountForProject(projectId) }
    }
    
    @Test
    fun `deleteTransactionsForProject calls dao method`() = runTest(testDispatcher) {
        val projectId = 1L
        coEvery { mockDao.deleteTransactionsForProject(projectId) } just Runs

        repository.deleteTransactionsForProject(projectId)

        coVerify { mockDao.deleteTransactionsForProject(projectId) }
    }
}
