package com.cashruler.validation

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cashruler.data.database.AppDatabase
import com.cashruler.data.models.*
import com.cashruler.data.repositories.*
import com.cashruler.notifications.NotificationManager
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.*

/**
 * Test Runner pour la validation complète de l'application
 */
class ValidationTestRunner {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var dataValidator: DataValidator
    private lateinit var performanceValidator: PerformanceValidator
    private lateinit var results: MutableList<ValidationResult>

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dataValidator = DataValidator()
        performanceValidator = PerformanceValidator()
        results = mutableListOf()
    }

    @Test
    fun runFullValidation() = runBlocking {
        // 1. Validation des données
        validateDataOperations()
        
        // 2. Validation des performances
        validatePerformance()
        
        // 3. Validation de la sauvegarde
        validateBackupOperations()
        
        // 4. Validation des notifications
        validateNotifications()

        // Générer le rapport
        generateValidationReport()
    }

    private suspend fun validateDataOperations() {
        // Test des opérations de base de données
        val expenseRepo = ExpenseRepository(db.expenseDao())
        val incomeRepo = IncomeRepository(db.incomeDao())
        val savingsRepo = SavingsRepository(db.savingsDao())

        // Test des dépenses
        val expense = Expense(
            id = 0,
            title = "Test Expense",
            amount = 100.0,
            category = "Alimentation",
            date = Date()
        )
        results.add(dataValidator.validateExpense(expense))
        
        val dbResult = performanceValidator.validateDatabaseOperation {
            expenseRepo.addExpense(expense)
        }
        results.add(ValidationResult("DB_Operation_Expense", dbResult.isValid, dbResult.errors))

        // Test des revenus
        val income = Income(
            id = 0,
            name = "Test Income",
            amount = 1000.0,
            type = "Salaire",
            date = Date()
        )
        results.add(dataValidator.validateIncome(income))

        // Test des projets d'épargne
        val project = SavingsProject(
            id = 0,
            title = "Test Project",
            targetAmount = 5000.0,
            currentAmount = 0.0,
            startDate = Date(),
            deadline = Date(System.currentTimeMillis() + 86400000),
            frequency = SavingsFrequency.MONTHLY
        )
        results.add(dataValidator.validateSavingsProject(project))
    }

    private suspend fun validatePerformance() {
        // Test des performances d'animation
        val frameTimesMs = List(100) { 16L } // Simuler 60 FPS
        val animationResult = performanceValidator.validateAnimation(frameTimesMs)
        results.add(ValidationResult("Animation_Performance", animationResult.isValid, animationResult.errors))

        // Test de l'utilisation mémoire
        val memoryResult = performanceValidator.validateMemoryUsage()
        results.add(ValidationResult("Memory_Usage", memoryResult.isValid, memoryResult.errors))

        // Test des fuites mémoire
        val leakResult = performanceValidator.checkMemoryLeaks {
            // Simulation d'opérations intensives
            List(1000) { it.toString() }
        }
        results.add(ValidationResult("Memory_Leaks", leakResult.isValid, leakResult.errors))
    }

    private suspend fun validateBackupOperations() {
        val backupRepo = BackupRepository(
            context = context,
            database = db,
            expenseRepository = ExpenseRepository(db.expenseDao()),
            incomeRepository = IncomeRepository(db.incomeDao()),
            savingsRepository = SavingsRepository(db.savingsDao()),
            spendingLimitRepository = SpendingLimitRepository(db.spendingLimitDao())
        )

        val backupResult = performanceValidator.validateDatabaseOperation {
            val backupFile = backupRepo.exportData()
            backupRepo.importData(backupFile)
        }
        results.add(ValidationResult("Backup_Operations", backupResult.isValid, backupResult.errors))
    }

    private fun validateNotifications() {
        val notificationManager = NotificationManager(context)
        
        try {
            notificationManager.scheduleLimitCheck()
            notificationManager.scheduleSavingsReminders()
            results.add(ValidationResult("Notifications", true, emptyList()))
        } catch (e: Exception) {
            results.add(ValidationResult(
                "Notifications",
                false,
                listOf("Erreur lors de la configuration des notifications: ${e.message}")
            ))
        }
    }

    private fun generateValidationReport() {
        val reportDir = File(context.getExternalFilesDir(null), "validation-reports")
        reportDir.mkdirs()

        val reportFile = File(reportDir, "validation_report_${System.currentTimeMillis()}.txt")
        reportFile.writeText(buildString {
            appendLine("Rapport de Validation CashRuler")
            appendLine("===========================")
            appendLine("Date: ${Date()}")
            appendLine()

            results.groupBy { it.category }.forEach { (category, results) ->
                appendLine("Catégorie: $category")
                appendLine("-".repeat(40))
                
                results.forEach { result ->
                    appendLine("Status: ${if (result.isValid) "✅ SUCCÈS" else "❌ ÉCHEC"}")
                    if (result.errors.isNotEmpty()) {
                        appendLine("Erreurs:")
                        result.errors.forEach { error ->
                            appendLine("  - $error")
                        }
                    }
                    appendLine()
                }
                appendLine()
            }

            // Statistiques globales
            val totalTests = results.size
            val successfulTests = results.count { it.isValid }
            val failedTests = totalTests - successfulTests
            
            appendLine("Résumé")
            appendLine("------")
            appendLine("Tests total: $totalTests")
            appendLine("Succès: $successfulTests")
            appendLine("Échecs: $failedTests")
            appendLine("Taux de succès: ${(successfulTests.toFloat() / totalTests * 100).toInt()}%")
        })
    }

    data class ValidationResult(
        val category: String,
        val isValid: Boolean,
        val errors: List<String>
    )
}
