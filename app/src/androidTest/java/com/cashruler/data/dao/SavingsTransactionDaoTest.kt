package com.cashruler.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cashruler.data.database.AppDatabase
import com.cashruler.data.database.Converters
import com.cashruler.data.models.SavingsProject
import com.cashruler.data.models.SavingsTransaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Date

@RunWith(AndroidJUnit4::class)
class SavingsTransactionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var savingsProjectDao: SavingsDao // For setting up project for FK
    private lateinit var savingsTransactionDao: SavingsTransactionDao

    private var testProjectId: Long = -1L

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(Converters())
            .build()
        savingsProjectDao = db.savingsDao()
        savingsTransactionDao = db.savingsTransactionDao()

        // Insert a dummy project to satisfy foreign key constraints
        val project = SavingsProject(title = "Test Project", description = "For Transactions", targetAmount = 1000.0)
        testProjectId = savingsProjectDao.insert(project)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetById() = runBlocking {
        val transaction = SavingsTransaction(projectId = testProjectId, amount = 100.0, type = SavingsTransaction.TYPE_DEPOSIT, transactionDate = Date(), description = "Initial Deposit")
        val id = savingsTransactionDao.insert(transaction)
        
        val retrieved = savingsTransactionDao.getById(id)
        assertNotNull(retrieved)
        assertEquals(id, retrieved?.id)
        assertEquals(testProjectId, retrieved?.projectId)
        assertEquals(100.0, retrieved?.amount, 0.0)
        assertEquals("Initial Deposit", retrieved?.description)
    }

    @Test
    @Throws(Exception::class)
    fun updateAndGetById() = runBlocking {
        val transaction = SavingsTransaction(projectId = testProjectId, amount = 50.0, type = SavingsTransaction.TYPE_DEPOSIT, transactionDate = Date(), description = "To Update")
        val id = savingsTransactionDao.insert(transaction)

        val retrieved = savingsTransactionDao.getById(id)!!
        val updatedTransaction = retrieved.copy(amount = 75.0, description = "Updated Description")
        savingsTransactionDao.update(updatedTransaction)

        val updatedRetrieved = savingsTransactionDao.getById(id)
        assertNotNull(updatedRetrieved)
        assertEquals(75.0, updatedRetrieved?.amount, 0.0)
        assertEquals("Updated Description", updatedRetrieved?.description)
    }

    @Test
    @Throws(Exception::class)
    fun deleteAndGetById() = runBlocking {
        val transaction = SavingsTransaction(projectId = testProjectId, amount = 200.0, type = SavingsTransaction.TYPE_DEPOSIT, transactionDate = Date())
        val id = savingsTransactionDao.insert(transaction)
        
        assertNotNull(savingsTransactionDao.getById(id)) // Ensure it's there

        val toDelete = savingsTransactionDao.getById(id)!!
        savingsTransactionDao.delete(toDelete)

        assertNull(savingsTransactionDao.getById(id)) // Should be null after delete
    }

    @Test
    @Throws(Exception::class)
    fun getTransactionsForProject_returnsCorrectlyOrderedList() = runBlocking {
        val date1 = Date(System.currentTimeMillis() - 200000) // Older
        val date2 = Date(System.currentTimeMillis() - 100000) // Middle
        val date3 = Date() // Newest

        val t1 = SavingsTransaction(projectId = testProjectId, amount = 10.0, type = SavingsTransaction.TYPE_DEPOSIT, transactionDate = date1, description = "T1")
        val t2 = SavingsTransaction(projectId = testProjectId, amount = 20.0, type = SavingsTransaction.TYPE_DEPOSIT, transactionDate = date2, description = "T2")
        val t3 = SavingsTransaction(projectId = testProjectId, amount = 30.0, type = SavingsTransaction.TYPE_DEPOSIT, transactionDate = date3, description = "T3")
        
        savingsTransactionDao.insert(t2) // Insert out of order
        savingsTransactionDao.insert(t1)
        savingsTransactionDao.insert(t3)

        val transactions = savingsTransactionDao.getTransactionsForProject(testProjectId).first()
        assertEquals(3, transactions.size)
        assertEquals("T3", transactions[0].description) // Newest first
        assertEquals("T2", transactions[1].description)
        assertEquals("T1", transactions[2].description)
    }

    @Test
    @Throws(Exception::class)
    fun getTotalAmountForProject_calculatesCorrectly() = runBlocking {
        // Test with no transactions
        var total = savingsTransactionDao.getTotalAmountForProject(testProjectId)
        assertNull(total) // DAO returns null if no transactions, repository handles this

        // Add deposits
        savingsTransactionDao.insert(SavingsTransaction(projectId = testProjectId, amount = 100.0, type = SavingsTransaction.TYPE_DEPOSIT, transactionDate = Date()))
        savingsTransactionDao.insert(SavingsTransaction(projectId = testProjectId, amount = 50.0, type = SavingsTransaction.TYPE_DEPOSIT, transactionDate = Date()))
        total = savingsTransactionDao.getTotalAmountForProject(testProjectId)
        assertEquals(150.0, total, 0.0)

        // Add a withdrawal
        savingsTransactionDao.insert(SavingsTransaction(projectId = testProjectId, amount = 30.0, type = SavingsTransaction.TYPE_WITHDRAWAL, transactionDate = Date()))
        total = savingsTransactionDao.getTotalAmountForProject(testProjectId)
        assertEquals(120.0, total, 0.0) // 150 - 30

        // Add another withdrawal making it negative (if business logic allows, DB sum will reflect)
         savingsTransactionDao.insert(SavingsTransaction(projectId = testProjectId, amount = 150.0, type = SavingsTransaction.TYPE_WITHDRAWAL, transactionDate = Date()))
        total = savingsTransactionDao.getTotalAmountForProject(testProjectId)
        assertEquals(-30.0, total, 0.0) // 120 - 150
    }
    
    @Test
    @Throws(Exception::class)
    fun deleteTransactionsForProject_deletesAllForProject() = runBlocking {
        val otherProjectId = savingsProjectDao.insert(SavingsProject(title = "Other Project", description = "...", targetAmount = 500.0))

        savingsTransactionDao.insert(SavingsTransaction(projectId = testProjectId, amount = 10.0, type = SavingsTransaction.TYPE_DEPOSIT, transactionDate = Date()))
        savingsTransactionDao.insert(SavingsTransaction(projectId = testProjectId, amount = 20.0, type = SavingsTransaction.TYPE_DEPOSIT, transactionDate = Date()))
        savingsTransactionDao.insert(SavingsTransaction(projectId = otherProjectId, amount = 30.0, type = SavingsTransaction.TYPE_DEPOSIT, transactionDate = Date()))

        assertEquals(2, savingsTransactionDao.getTransactionsForProject(testProjectId).first().size)
        assertEquals(1, savingsTransactionDao.getTransactionsForProject(otherProjectId).first().size)

        savingsTransactionDao.deleteTransactionsForProject(testProjectId)

        assertEquals(0, savingsTransactionDao.getTransactionsForProject(testProjectId).first().size)
        assertEquals(1, savingsTransactionDao.getTransactionsForProject(otherProjectId).first().size) // Ensure only target project's transactions are deleted
    }
}
