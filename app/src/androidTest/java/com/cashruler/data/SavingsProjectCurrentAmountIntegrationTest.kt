package com.cashruler.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cashruler.data.dao.SavingsDao
import com.cashruler.data.dao.SavingsTransactionDao
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
class SavingsProjectCurrentAmountIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var savingsDao: SavingsDao
    private lateinit var savingsTransactionDao: SavingsTransactionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(Converters())
            .build()
        savingsDao = db.savingsDao()
        savingsTransactionDao = db.savingsTransactionDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun updateSavingsProjectCurrentAmount_reflectsTransactionSum() = runBlocking {
        // 1. Create a SavingsProject
        val project = SavingsProject(
            title = "Vacation Fund",
            description = "Trip to Hawaii",
            targetAmount = 2000.0,
            currentAmount = 0.0 // Initial amount, will be updated
        )
        val projectId = savingsDao.insert(project)
        assertNotEquals(-1L, projectId) // Ensure project was inserted

        // 2. Add several SavingsTransaction records
        val transactions = listOf(
            SavingsTransaction(projectId = projectId, amount = 500.0, type = SavingsTransaction.TYPE_DEPOSIT, transactionDate = Date(), description = "Paycheck deposit"),
            SavingsTransaction(projectId = projectId, amount = 200.0, type = SavingsTransaction.TYPE_DEPOSIT, transactionDate = Date(), description = "Side hustle"),
            SavingsTransaction(projectId = projectId, amount = 50.0, type = SavingsTransaction.TYPE_WITHDRAWAL, transactionDate = Date(), description = "Flight booking fee"),
            SavingsTransaction(projectId = projectId, amount = 100.0, type = SavingsTransaction.TYPE_DEPOSIT, transactionDate = Date(), description = "Birthday gift")
        )

        for (transaction in transactions) {
            savingsTransactionDao.insert(transaction)
        }

        // Expected sum: 500 + 200 - 50 + 100 = 750
        val expectedCurrentAmount = 750.0

        // 3. Simulate updating SavingsProject.currentAmount
        //    (as done in SavingsViewModel: getTotalAmountForProject then update)
        val calculatedTotal = savingsTransactionDao.getTotalAmountForProject(projectId)
        assertNotNull("Calculated total should not be null", calculatedTotal)
        assertEquals(expectedCurrentAmount, calculatedTotal!!, 0.001)

        val projectToUpdate = savingsDao.getProjectById(projectId).first() // Get the latest project state
        assertNotNull("Project to update should not be null", projectToUpdate)
        
        val updatedProject = projectToUpdate!!.copy(currentAmount = calculatedTotal, updatedAt = Date())
        savingsDao.update(updatedProject)

        // 4. Fetch the SavingsProject and assert its currentAmount
        val finalProjectState = savingsDao.getProjectById(projectId).first()
        assertNotNull("Final project state should not be null", finalProjectState)
        assertEquals(expectedCurrentAmount, finalProjectState!!.currentAmount, 0.001)
        assertTrue("UpdatedAt should be recent", finalProjectState.updatedAt.time >= updatedProject.updatedAt.time - 1000 /* allow small diff */ )
    }

    @Test
    @Throws(Exception::class)
    fun updateSavingsProjectCurrentAmount_withNoTransactions_isZero() = runBlocking {
        val project = SavingsProject(title = "New Fund", targetAmount = 500.0, currentAmount = 100.0) // Start with non-zero
        val projectId = savingsDao.insert(project)

        val expectedCurrentAmount = 0.0 // Since no transactions, sum should be 0 (or null handled as 0 by repo)

        val calculatedTotal = savingsTransactionDao.getTotalAmountForProject(projectId) // Should be null
        assertNull("Calculated total should be null for no transactions", calculatedTotal)
        
        val projectToUpdate = savingsDao.getProjectById(projectId).first()!!
        // Simulate repository logic of treating null total as 0.0
        val actualTotalForUpdate = calculatedTotal ?: 0.0 
        val updatedProject = projectToUpdate.copy(currentAmount = actualTotalForUpdate, updatedAt = Date())
        savingsDao.update(updatedProject)

        val finalProjectState = savingsDao.getProjectById(projectId).first()!!
        assertEquals(expectedCurrentAmount, finalProjectState.currentAmount, 0.001)
    }
}
