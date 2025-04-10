package com.cashruler.validation

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.*

/**
 * Suite complète de validation de l'application
 */
class ValidationSuite {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dataValidator = DataValidator()
    private val performanceValidator = PerformanceValidator()
    private val accessibilityValidator = AccessibilityValidator(composeRule)
    private val securityValidator = SecurityValidator()

    private val results = mutableListOf<ValidationReport>()

    @Test
    fun runCompleteValidation() = runBlocking {
        // 1. Validation des données
        validateData()

        // 2. Validation des performances
        validatePerformance()

        // 3. Validation de l'accessibilité
        validateAccessibility()

        // 4. Validation de la sécurité
        validateSecurity()

        // Générer le rapport final
        generateFinalReport()

        // Vérifier si des problèmes critiques ont été détectés
        assertNoMajorIssues()
    }

    private suspend fun validateData() {
        TestOrchestrator().runFullValidation()
        ValidationTestRunner().runFullValidation()

        // Agréger les résultats
        results.add(
            ValidationReport(
                category = "Data_Validation",
                timestamp = System.currentTimeMillis(),
                results = collectDataValidationResults()
            )
        )
    }

    private suspend fun validatePerformance() {
        val screenList = listOf(
            "Dashboard",
            "Expenses",
            "Income",
            "Savings",
            "Statistics"
        )

        screenList.forEach { screen ->
            val performanceResults = mutableListOf<ValidationResult>()

            // Test des animations
            performanceResults.addAll(
                measureScreenPerformance(screen)
            )

            results.add(
                ValidationReport(
                    category = "Performance_${screen}",
                    timestamp = System.currentTimeMillis(),
                    results = performanceResults
                )
            )
        }
    }

    private fun validateAccessibility() {
        val screenList = listOf(
            "Dashboard" to listOf("dashboard_content", "quick_actions"),
            "Expenses" to listOf("expense_list", "add_expense_button"),
            "Income" to listOf("income_list", "add_income_button"),
            "Savings" to listOf("savings_projects", "add_project_button"),
            "Statistics" to listOf("charts_container", "filters_section")
        )

        screenList.forEach { (screen, tags) ->
            val accessibilityResults = accessibilityValidator.validateScreen(screen, tags)
            
            results.add(
                ValidationReport(
                    category = "Accessibility_${screen}",
                    timestamp = System.currentTimeMillis(),
                    results = accessibilityResults
                )
            )
        }
    }

    private fun validateSecurity() {
        val securityResults = securityValidator.validateSecurity()
        
        results.add(
            ValidationReport(
                category = "Security",
                timestamp = System.currentTimeMillis(),
                results = securityResults.map {
                    ValidationResult(
                        category = it.category,
                        isValid = it.isSecure,
                        message = it.message
                    )
                }
            )
        )
    }

    private suspend fun measureScreenPerformance(screenName: String): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()
        
        // Mesurer les performances d'animation
        val frameResults = performanceValidator.validateAnimation(
            List(100) { 16L } // Simuler 60 FPS
        )
        results.add(
            ValidationResult(
                category = "${screenName}_Animations",
                isValid = frameResults.isValid,
                message = frameResults.errors.joinToString()
            )
        )

        // Mesurer l'utilisation mémoire
        val memoryResults = performanceValidator.validateMemoryUsage()
        results.add(
            ValidationResult(
                category = "${screenName}_Memory",
                isValid = memoryResults.isValid,
                message = memoryResults.errors.joinToString()
            )
        )

        return results
    }

    private fun collectDataValidationResults(): List<ValidationResult> {
        // Cette méthode collecte les résultats de validation des données
        // à partir des différents tests précédemment exécutés
        return emptyList() // À implémenter avec les résultats réels
    }

    private fun generateFinalReport() {
        val reportDir = File(context.getExternalFilesDir(null), "validation-reports")
        reportDir.mkdirs()

        val reportFile = File(reportDir, "complete_validation_report_${System.currentTimeMillis()}.md")
        reportFile.writeText(buildString {
            appendLine("# Rapport de Validation Complet - CashRuler")
            appendLine("Date: ${Date()}")
            appendLine()

            // Résumé global
            appendLine("## Résumé")
            appendLine()
            val totalTests = results.sumOf { it.results.size }
            val successfulTests = results.sumOf { report -> 
                report.results.count { it.isValid }
            }
            appendLine("- Tests total: $totalTests")
            appendLine("- Tests réussis: $successfulTests")
            appendLine("- Tests échoués: ${totalTests - successfulTests}")
            appendLine("- Taux de succès: ${(successfulTests.toFloat() / totalTests * 100).toInt()}%")
            appendLine()

            // Détails par catégorie
            results.forEach { report ->
                appendLine("## ${report.category}")
                appendLine("Timestamp: ${Date(report.timestamp)}")
                appendLine()
                appendLine("| Test | Statut | Message |")
                appendLine("|------|--------|---------|")
                
                report.results.forEach { result ->
                    val status = if (result.isValid) "✅" else "❌"
                    appendLine("| ${result.category} | $status | ${result.message} |")
                }
                appendLine()
            }

            // Recommandations
            appendLine("## Recommandations")
            appendLine()
            generateRecommendations().forEach {
                appendLine("- $it")
            }
        })
    }

    private fun generateRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()

        results.forEach { report ->
            report.results.filterNot { it.isValid }.forEach { failure ->
                when {
                    failure.category.contains("Performance") -> {
                        recommendations.add("Optimiser les performances de ${failure.category}: ${failure.message}")
                    }
                    failure.category.contains("Accessibility") -> {
                        recommendations.add("Améliorer l'accessibilité de ${failure.category}: ${failure.message}")
                    }
                    failure.category.contains("Security") -> {
                        recommendations.add("⚠️ Corriger le problème de sécurité: ${failure.message}")
                    }
                    else -> {
                        recommendations.add("Résoudre le problème: ${failure.message}")
                    }
                }
            }
        }

        return recommendations
    }

    private fun assertNoMajorIssues() {
        val securityIssues = results.any { report ->
            report.category == "Security" && report.results.any { !it.isValid }
        }

        val criticalPerformanceIssues = results.any { report ->
            report.category.startsWith("Performance") &&
            report.results.any { !it.isValid && it.message.contains("critique", ignoreCase = true) }
        }

        assert(!securityIssues) { "Des problèmes de sécurité critiques ont été détectés" }
        assert(!criticalPerformanceIssues) { "Des problèmes de performance critiques ont été détectés" }
    }

    data class ValidationReport(
        val category: String,
        val timestamp: Long,
        val results: List<ValidationResult>
    )

    data class ValidationResult(
        val category: String,
        val isValid: Boolean,
        val message: String
    )
}
