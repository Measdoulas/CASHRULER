package com.cashruler.validation

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.cashruler.data.database.AppDatabase
import com.cashruler.data.models.*
import com.cashruler.data.repositories.*
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.math.abs

/**
 * Validateur de cohérence globale de l'application
 */
class CoherenceValidator {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase
    private val results = mutableListOf<CoherenceResult>()

    init {
        setupDatabase()
    }

    private fun setupDatabase() {
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    /**
     * Valide la cohérence globale des données
     */
    suspend fun validateDataCoherence(): List<CoherenceResult> {
        results.clear()

        // 1. Validation des relations
        validateRelations()

        // 2. Validation des calculs
        validateCalculations()

        // 3. Validation des contraintes métier
        validateBusinessRules()

        // 4. Validation des limites
        validateLimits()

        return results
    }

    private suspend fun validateRelations() {
        val expenseRepo = ExpenseRepository(db.expenseDao())
        val savingsRepo = SavingsRepository(db.savingsDao())

        // Créer un projet d'épargne
        val project = SavingsProject(
            id = 0,
            title = "Test Project",
            targetAmount = 1000.0,
            currentAmount = 0.0,
            startDate = Date(),
            deadline = Date(System.currentTimeMillis() + 86400000),
            frequency = SavingsFrequency.MONTHLY
        )
        savingsRepo.addProject(project)

        // Ajouter des transactions
        val transaction = SavingsTransaction(
            id = 0,
            projectId = 1,
            amount = 100.0,
            date = Date()
        )
        savingsRepo.addTransaction(transaction)

        // Vérifier la mise à jour du montant
        val updatedProject = savingsRepo.getProjectById(1)
        results.add(
            CoherenceResult(
                "Project_Transaction_Relation",
                updatedProject?.value?.currentAmount == 100.0,
                "La mise à jour du montant du projet devrait refléter les transactions"
            )
        )
    }

    private suspend fun validateCalculations() {
        val incomeRepo = IncomeRepository(db.incomeDao())
        val expenseRepo = ExpenseRepository(db.expenseDao())

        // Ajouter des revenus et dépenses
        val income = Income(
            id = 0,
            name = "Test Income",
            amount = 1000.0,
            type = "Salaire",
            date = Date()
        )
        incomeRepo.addIncome(income)

        val expense = Expense(
            id = 0,
            title = "Test Expense",
            amount = 300.0,
            category = "Alimentation",
            date = Date()
        )
        expenseRepo.addExpense(expense)

        // Vérifier le solde
        val incomes = incomeRepo.getAllIncomesList()
        val expenses = expenseRepo.getAllExpensesList()
        val balance = incomes.sumOf { it.amount } - expenses.sumOf { it.amount }

        results.add(
            CoherenceResult(
                "Balance_Calculation",
                abs(balance - 700.0) < 0.01,
                "Le calcul du solde devrait être correct"
            )
        )
    }

    private suspend fun validateBusinessRules() {
        val spendingLimitRepo = SpendingLimitRepository(db.spendingLimitDao())
        val expenseRepo = ExpenseRepository(db.expenseDao())

        // Créer une limite de dépenses
        val limit = SpendingLimit(
            id = 0,
            category = "Alimentation",
            amount = 500.0,
            startDate = Date(),
            frequency = SpendingLimitFrequency.MONTHLY
        )
        spendingLimitRepo.addLimit(limit)

        // Ajouter des dépenses
        val expense1 = Expense(id = 0, title = "Grocery", amount = 300.0, category = "Alimentation", date = Date())
        val expense2 = Expense(id = 1, title = "Restaurant", amount = 250.0, category = "Alimentation", date = Date())
        
        expenseRepo.addExpense(expense1)
        expenseRepo.addExpense(expense2)

        // Vérifier le dépassement
        val categoryExpenses = expenseRepo.getExpensesByCategory("Alimentation")
        val totalExpense = categoryExpenses.sumOf { it.amount }
        val limitObj = spendingLimitRepo.getLimitByCategory("Alimentation")

        results.add(
            CoherenceResult(
                "Spending_Limit_Rule",
                totalExpense > limitObj?.value?.amount ?: 0.0,
                "La règle de limite de dépenses devrait être détectée"
            )
        )
    }

    private suspend fun validateLimits() {
        val savingsRepo = SavingsRepository(db.savingsDao())

        // Tester les limites de montants
        try {
            val invalidProject = SavingsProject(
                id = 0,
                title = "Invalid Project",
                targetAmount = -1000.0, // Montant négatif invalide
                currentAmount = 0.0,
                startDate = Date(),
                deadline = Date(),
                frequency = SavingsFrequency.MONTHLY
            )
            savingsRepo.addProject(invalidProject)
            results.add(
                CoherenceResult(
                    "Amount_Limits",
                    false,
                    "Les montants négatifs devraient être rejetés"
                )
            )
        } catch (e: Exception) {
            results.add(
                CoherenceResult(
                    "Amount_Limits",
                    true,
                    "Les montants négatifs sont correctement rejetés"
                )
            )
        }
    }

    data class CoherenceResult(
        val test: String,
        val isValid: Boolean,
        val message: String
    ) {
        fun toValidationResult() = ValidationResult(
            category = "Coherence_$test",
            isValid = isValid,
            message = message
        )
    }
}
