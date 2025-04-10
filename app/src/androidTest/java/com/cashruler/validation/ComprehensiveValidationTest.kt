package com.cashruler.validation

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.*

/**
 * Test de validation complet de l'application
 */
class ComprehensiveValidationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var dataValidator: DataValidator
    private lateinit var performanceValidator: PerformanceValidator
    private lateinit var accessibilityValidator: AccessibilityValidator
    private lateinit var securityValidator: SecurityValidator
    private lateinit var codeAnalyzer: CodeAnalyzer
    private lateinit var coherenceValidator: CoherenceValidator

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        dataValidator = DataValidator()
        performanceValidator = PerformanceValidator()
        accessibilityValidator = AccessibilityValidator(composeRule)
        securityValidator = SecurityValidator()
        codeAnalyzer = CodeAnalyzer()
        coherenceValidator = CoherenceValidator()
    }

    @Test
    fun runComprehensiveValidation() = runBlocking {
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<ValidationIssue>()

        // 1. Analyse statique du code
        val codeAnalysis = codeAnalyzer.analyzeCode()
        results.addAll(codeAnalysis.map { 
            ValidationIssue(
                category = "Code Analysis",
                component = it.component,
                severity = mapSeverity(it.severity),
                message = it.message
            )
        })

        // 2. Validation de la cohérence des données
        val coherenceResults = coherenceValidator.validateDataCoherence()
        results.addAll(coherenceResults.map {
            ValidationIssue(
                category = "Data Coherence",
                component = it.test,
                severity = if (it.isValid) IssueSeverity.OK else IssueSeverity.ERROR,
                message = it.message
            )
        })

        // 3. Tests de performance
        val performanceResults = performanceValidator.validateMemoryUsage()
        results.add(
            ValidationIssue(
                category = "Performance",
                component = "Memory Usage",
                severity = if (performanceResults.isValid) IssueSeverity.OK else IssueSeverity.WARNING,
                message = performanceResults.errors.joinToString()
            )
        )

        // 4. Tests de sécurité
        val securityResults = securityValidator.validateSecurity()
        results.addAll(securityResults.map {
            ValidationIssue(
                category = "Security",
                component = it.category,
                severity = if (it.isSecure) IssueSeverity.OK else IssueSeverity.ERROR,
                message = it.message
            )
        })

        // 5. Tests d'accessibilité
        val screens = listOf(
            "Dashboard" to listOf("dashboard_content", "quick_actions"),
            "Expenses" to listOf("expense_list", "add_expense_button"),
            "Income" to listOf("income_list", "add_income_button"),
            "Savings" to listOf("savings_projects", "add_project_button")
        )

        screens.forEach { (screen, tags) ->
            val accessibilityResults = accessibilityValidator.validateScreen(screen, tags)
            results.addAll(accessibilityResults.map {
                ValidationIssue(
                    category = "Accessibility",
                    component = "${screen}_${it.category}",
                    severity = if (it.isValid) IssueSeverity.OK else IssueSeverity.WARNING,
                    message = it.message
                )
            })
        }

        // Générer le rapport final
        generateValidationReport(results, System.currentTimeMillis() - startTime)

        // Vérifier s'il y a des problèmes critiques
        val criticalIssues = results.count { it.severity == IssueSeverity.ERROR }
        assert(criticalIssues == 0) { 
            "${criticalIssues} problèmes critiques détectés. Consultez le rapport pour plus de détails." 
        }
    }

    private fun mapSeverity(analyzerSeverity: CodeAnalyzer.Severity): IssueSeverity {
        return when (analyzerSeverity) {
            CodeAnalyzer.Severity.ERROR -> IssueSeverity.ERROR
            CodeAnalyzer.Severity.WARNING -> IssueSeverity.WARNING
            CodeAnalyzer.Severity.INFO -> IssueSeverity.INFO
        }
    }

    private fun generateValidationReport(results: List<ValidationIssue>, duration: Long) {
        val reportDir = File(context.getExternalFilesDir(null), "validation-reports")
        reportDir.mkdirs()

        val reportFile = File(reportDir, "comprehensive_validation_${Date().time}.md")
        
        reportFile.writeText(buildString {
            appendLine("# Rapport de Validation Complet - CashRuler")
            appendLine("Date: ${Date()}")
            appendLine("Durée: ${duration / 1000} secondes")
            appendLine()

            // Résumé
            val totalIssues = results.size
            val errors = results.count { it.severity == IssueSeverity.ERROR }
            val warnings = results.count { it.severity == IssueSeverity.WARNING }
            val info = results.count { it.severity == IssueSeverity.INFO }
            val ok = results.count { it.severity == IssueSeverity.OK }

            appendLine("## Résumé")
            appendLine("- Total des vérifications: $totalIssues")
            appendLine("- Erreurs: $errors")
            appendLine("- Avertissements: $warnings")
            appendLine("- Informations: $info")
            appendLine("- OK: $ok")
            appendLine()

            // Détails par catégorie
            results.groupBy { it.category }.forEach { (category, issues) ->
                appendLine("## $category")
                appendLine()
                appendLine("| Composant | Sévérité | Message |")
                appendLine("|-----------|-----------|---------|")
                
                issues.forEach { issue ->
                    val severityIcon = when (issue.severity) {
                        IssueSeverity.ERROR -> "❌"
                        IssueSeverity.WARNING -> "⚠️"
                        IssueSeverity.INFO -> "ℹ️"
                        IssueSeverity.OK -> "✅"
                    }
                    appendLine("| ${issue.component} | $severityIcon | ${issue.message} |")
                }
                appendLine()
            }

            // Recommandations
            appendLine("## Recommandations")
            appendLine()
            generateRecommendations(results).forEach {
                appendLine("- $it")
            }
        })
    }

    private fun generateRecommendations(results: List<ValidationIssue>): List<String> {
        val recommendations = mutableListOf<String>()

        // Problèmes critiques
        results.filter { it.severity == IssueSeverity.ERROR }.forEach {
            recommendations.add("⚠️ URGENT: Corriger ${it.component} - ${it.message}")
        }

        // Problèmes de performance
        results.filter { 
            it.category == "Performance" && it.severity != IssueSeverity.OK 
        }.forEach {
            recommendations.add("🚀 Optimisation: ${it.message}")
        }

        // Problèmes d'accessibilité
        results.filter { 
            it.category == "Accessibility" && it.severity != IssueSeverity.OK 
        }.forEach {
            recommendations.add("♿ Accessibilité: ${it.message}")
        }

        // Problèmes de code
        results.filter { 
            it.category == "Code Analysis" && it.severity != IssueSeverity.OK 
        }.forEach {
            recommendations.add("🔍 Code: ${it.message}")
        }

        return recommendations
    }

    data class ValidationIssue(
        val category: String,
        val component: String,
        val severity: IssueSeverity,
        val message: String
    )

    enum class IssueSeverity {
        ERROR,
        WARNING,
        INFO,
        OK
    }
}
