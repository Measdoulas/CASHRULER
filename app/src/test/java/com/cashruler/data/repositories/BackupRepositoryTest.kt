package com.cashruler.data.repositories

import android.content.Context
import com.cashruler.data.database.AppDatabase
import com.cashruler.data.models.*
import com.google.gson.Gson
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var incomeRepository: IncomeRepository
    private lateinit var savingsRepository: SavingsRepository
    private lateinit var spendingLimitRepository: SpendingLimitRepository
    private lateinit var repository: BackupRepository
    private lateinit var backupFolder: File

    @Before
    fun setup() {
        context = mockk {
            every { filesDir } returns tempFolder.root
        }
        database = mockk(relaxed = true)
        expenseRepository = mockk()
        incomeRepository = mockk()
        savingsRepository = mockk()
        spendingLimitRepository = mockk()

        repository = BackupRepository(
            context = context,
            database = database,
            expenseRepository = expenseRepository,
            incomeRepository = incomeRepository,
            savingsRepository = savingsRepository,
            spendingLimitRepository = spendingLimitRepository
        )

        backupFolder = File(tempFolder.root, "backups").apply { mkdirs() }
    }

    @Test
    fun `exportData creates backup file with current data`() = runTest {
        // Given
        val expenses = listOf(createExpense(1))
        val incomes = listOf(createIncome(1))
        val savingsProjects = listOf(createSavingsProject(1))
        val spendingLimits = listOf(createSpendingLimit(1))

        coEvery { expenseRepository.getAllExpensesList() } returns expenses
        coEvery { incomeRepository.getAllIncomesList() } returns incomes
        coEvery { savingsRepository.getAllProjectsList() } returns savingsProjects
        coEvery { spendingLimitRepository.getAllLimitsList() } returns spendingLimits

        // When
        val backupPath = repository.exportData()

        // Then
        val backupFile = File(backupPath)
        assertTrue(backupFile.exists())
        
        val backupContent = backupFile.readText()
        val backupData = Gson().fromJson(backupContent, BackupRepository.BackupData::class.java)

        assertEquals(expenses, backupData.expenses)
        assertEquals(incomes, backupData.incomes)
        assertEquals(savingsProjects, backupData.savingsProjects)
        assertEquals(spendingLimits, backupData.spendingLimits)
        assertEquals(BackupRepository.BACKUP_VERSION, backupData.version)
    }

    @Test
    fun `importData restores data from backup file`() = runTest {
        // Given
        val backupData = BackupRepository.BackupData(
            expenses = listOf(createExpense(1)),
            incomes = listOf(createIncome(1)),
            savingsProjects = listOf(createSavingsProject(1)),
            spendingLimits = listOf(createSpendingLimit(1))
        )

        val backupFile = File(backupFolder, "test_backup.json").apply {
            writeText(Gson().toJson(backupData))
        }

        coEvery { expenseRepository.insertExpenses(any()) } returns Unit
        coEvery { incomeRepository.insertIncomes(any()) } returns Unit
        coEvery { savingsRepository.insertProjects(any()) } returns Unit
        coEvery { spendingLimitRepository.insertLimits(any()) } returns Unit

        // When
        repository.importData(backupFile.absolutePath)

        // Then
        coVerify { 
            database.clearAllTables()
            expenseRepository.insertExpenses(backupData.expenses)
            incomeRepository.insertIncomes(backupData.incomes)
            savingsRepository.insertProjects(backupData.savingsProjects)
            spendingLimitRepository.insertLimits(backupData.spendingLimits)
        }
    }

    @Test
    fun `listBackups returns list of available backups`() = runTest {
        // Given
        val backupData1 = createBackupFile("backup1.json", System.currentTimeMillis())
        val backupData2 = createBackupFile("backup2.json", System.currentTimeMillis() + 1000)

        // When
        val backups = repository.listBackups()

        // Then
        assertEquals(2, backups.size)
        assertTrue(backups.any { it.file.name == "backup1.json" })
        assertTrue(backups.any { it.file.name == "backup2.json" })
    }

    private fun createBackupFile(name: String, timestamp: Long): File {
        val data = BackupRepository.BackupData(
            expenses = emptyList(),
            incomes = emptyList(),
            savingsProjects = emptyList(),
            spendingLimits = emptyList(),
            timestamp = timestamp
        )
        return File(backupFolder, name).apply {
            writeText(Gson().toJson(data))
        }
    }

    private fun createExpense(
        id: Long,
        amount: Double = 100.0,
        category: String = "Food"
    ) = Expense(
        id = id,
        amount = amount,
        title = "Test",
        category = category,
        date = Date()
    )

    private fun createIncome(
        id: Long,
        amount: Double = 1000.0,
        type: String = "Salary"
    ) = Income(
        id = id,
        amount = amount,
        name = "Test",
        type = type,
        date = Date()
    )

    private fun createSavingsProject(
        id: Long,
        currentAmount: Double = 0.0,
        targetAmount: Double = 1000.0
    ) = SavingsProject(
        id = id,
        title = "Test",
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        startDate = Date(),
        deadline = Date(System.currentTimeMillis() + 86400000),
        frequency = SavingsFrequency.MONTHLY
    )

    private fun createSpendingLimit(
        id: Long,
        amount: Double = 500.0,
        category: String = "Food"
    ) = SpendingLimit(
        id = id,
        category = category,
        amount = amount,
        startDate = Date(),
        frequency = SpendingLimitFrequency.MONTHLY
    )
}
