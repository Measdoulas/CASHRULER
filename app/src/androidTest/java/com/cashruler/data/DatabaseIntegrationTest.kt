package com.cashruler.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cashruler.data.database.AppDatabase
import com.cashruler.data.models.*
import com.cashruler.data.repositories.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var incomeRepository: IncomeRepository
    private lateinit var savingsRepository: SavingsRepository
    private lateinit var spendingLimitRepository: SpendingLimitRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()

        expenseRepository = ExpenseRepository(db.expenseDao())
        incomeRepository = IncomeRepository(db.incomeDao())
        savingsRepository = SavingsRepository(db.savingsDao())
        spendingLimitRepository = SpendingLimitRepository(db.spendingLimitDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun createExpenseAndLimit_VerifyLimitUpdated() = runTest {
        // Créer une limite de dépenses
        val limit = SpendingLimit(
            id = 0,
            category = "Alimentation",
            amount = 500.0,
            currentAmount = 0.0,
            startDate = Date(),
            frequency = SpendingLimitFrequency.MONTHLY
        )
        spendingLimitRepository.addLimit(limit)

        // Ajouter une dépense dans la même catégorie
        val expense = Expense(
            id = 0,
            title = "Courses",
            amount = 100.0,
            category = "Alimentation",
            date = Date()
        )
        expenseRepository.addExpense(expense)

        // Vérifier que la limite est mise à jour
        val updatedLimit = spendingLimitRepository.getLimitByCategory("Alimentation").first()
        assertNotNull(updatedLimit)
        assertEquals(100.0, updatedLimit.currentAmount)
    }

    @Test
    fun createSavingsProjectWithTransactions() = runTest {
        // Créer un projet d'épargne
        val project = SavingsProject(
            id = 0,
            title = "Vacances",
            targetAmount = 1000.0,
            currentAmount = 0.0,
            startDate = Date(),
            deadline = Date(System.currentTimeMillis() + 86400000),
            frequency = SavingsFrequency.MONTHLY
        )
        savingsRepository.addProject(project)

        // Ajouter des transactions
        val transaction1 = SavingsTransaction(
            id = 0,
            projectId = 1, // L'ID sera 1 car c'est le premier projet
            amount = 200.0,
            date = Date()
        )
        val transaction2 = SavingsTransaction(
            id = 0,
            projectId = 1,
            amount = 300.0,
            date = Date()
        )
        
        savingsRepository.addTransaction(transaction1)
        savingsRepository.addTransaction(transaction2)

        // Vérifier le montant total
        val updatedProject = savingsRepository.getProjectById(1).first()
        assertNotNull(updatedProject)
        assertEquals(500.0, updatedProject.currentAmount)
    }

    @Test
    fun calculateBalanceFromIncomesAndExpenses() = runTest {
        // Ajouter des revenus
        val income1 = Income(
            id = 0,
            name = "Salaire",
            amount = 2000.0,
            type = "Fixe",
            date = Date()
        )
        val income2 = Income(
            id = 0,
            name = "Freelance",
            amount = 500.0,
            type = "Variable",
            date = Date()
        )
        incomeRepository.addIncome(income1)
        incomeRepository.addIncome(income2)

        // Ajouter des dépenses
        val expense1 = Expense(
            id = 0,
            title = "Loyer",
            amount = 800.0,
            category = "Logement",
            date = Date()
        )
        val expense2 = Expense(
            id = 0,
            title = "Courses",
            amount = 200.0,
            category = "Alimentation",
            date = Date()
        )
        expenseRepository.addExpense(expense1)
        expenseRepository.addExpense(expense2)

        // Vérifier le solde
        val totalIncome = incomeRepository.getAllIncomesList().sumOf { it.amount }
        val totalExpenses = expenseRepository.getAllExpensesList().sumOf { it.amount }
        val balance = totalIncome - totalExpenses

        assertEquals(2500.0, totalIncome)
        assertEquals(1000.0, totalExpenses)
        assertEquals(1500.0, balance)
    }

    @Test
    fun deletingProjectRemovesAssociatedTransactions() = runTest {
        // Créer un projet avec des transactions
        val project = SavingsProject(
            id = 0,
            title = "Test",
            targetAmount = 1000.0,
            currentAmount = 0.0,
            startDate = Date(),
            deadline = Date(),
            frequency = SavingsFrequency.MONTHLY
        )
        savingsRepository.addProject(project)

        val transaction = SavingsTransaction(
            id = 0,
            projectId = 1,
            amount = 100.0,
            date = Date()
        )
        savingsRepository.addTransaction(transaction)

        // Supprimer le projet
        val savedProject = savingsRepository.getProjectById(1).first()
        assertNotNull(savedProject)
        savingsRepository.deleteProject(savedProject)

        // Vérifier que les transactions sont supprimées
        val transactions = savingsRepository.getProjectTransactions(1).first()
        assertTrue(transactions.isEmpty())
    }
}
