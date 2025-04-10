package com.cashruler.validation

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.File
import java.lang.reflect.Method

/**
 * Analyseur de code pour détecter les problèmes potentiels
 */
class CodeAnalyzer {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val results = mutableListOf<AnalysisResult>()

    /**
     * Lance une analyse complète du code
     */
    suspend fun analyzeCode(): List<AnalysisResult> {
        results.clear()

        // 1. Vérification des cycles de vie
        analyzeLifecycles()

        // 2. Vérification des fuites de ressources
        analyzeResourceLeaks()

        // 3. Vérification des concurrences
        analyzeConcurrency()

        // 4. Vérification des nullabilités
        analyzeNullSafety()

        // 5. Vérification des performances
        analyzePerformance()

        return results
    }

    private fun analyzeLifecycles() {
        // Vérifier les ViewModels
        checkViewModelLifecycles()

        // Vérifier les Composables
        checkComposableLifecycles()

        // Vérifier les Workers
        checkWorkerLifecycles()
    }

    private fun checkViewModelLifecycles() {
        val viewModelClasses = listOf(
            "ExpenseViewModel",
            "IncomeViewModel",
            "SavingsViewModel",
            "StatisticsViewModel",
            "DashboardViewModel"
        )

        viewModelClasses.forEach { className ->
            try {
                val clazz = Class.forName("com.cashruler.ui.viewmodels.$className")
                val hasCleanup = clazz.declaredMethods.any { 
                    it.name == "onCleared" || it.name.contains("cleanup", ignoreCase = true)
                }

                if (!hasCleanup) {
                    results.add(
                        AnalysisResult(
                            component = className,
                            severity = Severity.WARNING,
                            message = "Pas de méthode de nettoyage trouvée dans le ViewModel"
                        )
                    )
                }
            } catch (e: Exception) {
                results.add(
                    AnalysisResult(
                        component = className,
                        severity = Severity.ERROR,
                        message = "Erreur lors de l'analyse du ViewModel: ${e.message}"
                    )
                )
            }
        }
    }

    private fun checkComposableLifecycles() {
        val screens = listOf(
            "DashboardScreen",
            "ExpensesScreen",
            "IncomeScreen",
            "SavingsScreen",
            "StatisticsScreen"
        )

        screens.forEach { screen ->
            try {
                val clazz = Class.forName("com.cashruler.ui.screens.$screen")
                checkComposableDisposalEffects(clazz)
            } catch (e: Exception) {
                results.add(
                    AnalysisResult(
                        component = screen,
                        severity = Severity.ERROR,
                        message = "Erreur lors de l'analyse du Composable: ${e.message}"
                    )
                )
            }
        }
    }

    private fun checkComposableDisposalEffects(clazz: Class<*>) {
        val hasDisposalEffect = clazz.declaredMethods.any { method ->
            method.annotations.any { 
                it.annotationClass.simpleName?.contains("Composable") == true
            } && method.body?.contains("DisposableEffect") == true
        }

        if (!hasDisposalEffect) {
            results.add(
                AnalysisResult(
                    component = clazz.simpleName,
                    severity = Severity.INFO,
                    message = "Pas de DisposableEffect trouvé, vérifier si nécessaire"
                )
            )
        }
    }

    private fun checkWorkerLifecycles() {
        val workers = listOf(
            "LimitCheckWorker",
            "SavingsReminderWorker"
        )

        workers.forEach { worker ->
            try {
                val clazz = Class.forName("com.cashruler.notifications.workers.$worker")
                val hasCleanup = clazz.declaredMethods.any { 
                    it.name == "onStopped" || it.name.contains("cleanup", ignoreCase = true)
                }

                if (!hasCleanup) {
                    results.add(
                        AnalysisResult(
                            component = worker,
                            severity = Severity.WARNING,
                            message = "Pas de gestion de l'arrêt trouvée dans le Worker"
                        )
                    )
                }
            } catch (e: Exception) {
                results.add(
                    AnalysisResult(
                        component = worker,
                        severity = Severity.ERROR,
                        message = "Erreur lors de l'analyse du Worker: ${e.message}"
                    )
                )
            }
        }
    }

    private fun analyzeResourceLeaks() {
        // Vérifier les Flows
        checkFlowCollections()

        // Vérifier les Coroutines
        checkCoroutineScopes()

        // Vérifier les fichiers et curseurs
        checkResourceClosing()
    }

    private fun checkFlowCollections() {
        val repositories = listOf(
            "ExpenseRepository",
            "IncomeRepository",
            "SavingsRepository",
            "SpendingLimitRepository"
        )

        repositories.forEach { repo ->
            try {
                val clazz = Class.forName("com.cashruler.data.repositories.$repo")
                val flowMethods = clazz.declaredMethods.filter { 
                    it.returnType == Flow::class.java 
                }

                flowMethods.forEach { method ->
                    if (!hasErrorHandling(method)) {
                        results.add(
                            AnalysisResult(
                                component = "$repo.${method.name}",
                                severity = Severity.WARNING,
                                message = "Flow sans gestion d'erreur"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                results.add(
                    AnalysisResult(
                        component = repo,
                        severity = Severity.ERROR,
                        message = "Erreur lors de l'analyse des Flows: ${e.message}"
                    )
                )
            }
        }
    }

    private fun checkCoroutineScopes() {
        val viewModels = listOf(
            "ExpenseViewModel",
            "IncomeViewModel",
            "SavingsViewModel",
            "StatisticsViewModel"
        )

        viewModels.forEach { viewModel ->
            try {
                val clazz = Class.forName("com.cashruler.ui.viewmodels.$viewModel")
                val hasScopeManagement = clazz.declaredFields.any { 
                    it.type.toString().contains("CoroutineScope") 
                }

                if (!hasScopeManagement) {
                    results.add(
                        AnalysisResult(
                            component = viewModel,
                            severity = Severity.WARNING,
                            message = "Pas de gestion explicite du CoroutineScope"
                        )
                    )
                }
            } catch (e: Exception) {
                results.add(
                    AnalysisResult(
                        component = viewModel,
                        severity = Severity.ERROR,
                        message = "Erreur lors de l'analyse des Coroutines: ${e.message}"
                    )
                )
            }
        }
    }

    private fun checkResourceClosing() {
        val hasUnclosedResources = File(context.codeCacheDir, "generated").walkTopDown()
            .filter { it.extension == "kt" }
            .any { file ->
                file.readText().contains("Cursor") && 
                !file.readText().contains(".close()")
            }

        if (hasUnclosedResources) {
            results.add(
                AnalysisResult(
                    component = "Resource Management",
                    severity = Severity.ERROR,
                    message = "Ressources potentiellement non fermées détectées"
                )
            )
        }
    }

    private fun analyzeNullSafety() {
        val components = listOf(
            "ui/components",
            "data/repositories",
            "ui/viewmodels"
        )

        components.forEach { component ->
            val path = "com/cashruler/$component"
            val unsafeNullChecks = File(context.codeCacheDir, path)
                .walkTopDown()
                .filter { it.extension == "kt" }
                .count { file ->
                    val content = file.readText()
                    content.contains("!!")
                }

            if (unsafeNullChecks > 0) {
                results.add(
                    AnalysisResult(
                        component = component,
                        severity = Severity.WARNING,
                        message = "Utilisation de !! détectée ($unsafeNullChecks occurrences)"
                    )
                )
            }
        }
    }

    private fun analyzePerformance() {
        // Vérifier les listes non optimisées
        checkListUsage()

        // Vérifier les calculs lourds sur le thread principal
        checkMainThreadOperations()

        // Vérifier les requêtes DB non optimisées
        checkDatabaseQueries()
    }

    private fun checkListUsage() {
        val components = listOf(
            "ui/screens",
            "ui/components",
            "data/repositories"
        )

        components.forEach { component ->
            val path = "com/cashruler/$component"
            val inefficientListOps = File(context.codeCacheDir, path)
                .walkTopDown()
                .filter { it.extension == "kt" }
                .count { file ->
                    val content = file.readText()
                    content.contains("toList()") || 
                    content.contains("forEach") ||
                    (content.contains("List<") && !content.contains("LazyColumn"))
                }

            if (inefficientListOps > 0) {
                results.add(
                    AnalysisResult(
                        component = component,
                        severity = Severity.INFO,
                        message = "Opérations sur liste potentiellement non optimisées"
                    )
                )
            }
        }
    }

    private fun checkMainThreadOperations() {
        val viewModels = listOf(
            "ExpenseViewModel",
            "IncomeViewModel",
            "SavingsViewModel",
            "StatisticsViewModel"
        )

        viewModels.forEach { viewModel ->
            try {
                val clazz = Class.forName("com.cashruler.ui.viewmodels.$viewModel")
                val suspendFunctions = clazz.declaredMethods.count { 
                    it.isSuspend() 
                }

                if (suspendFunctions == 0) {
                    results.add(
                        AnalysisResult(
                            component = viewModel,
                            severity = Severity.WARNING,
                            message = "Possible opérations lourdes sur le thread principal"
                        )
                    )
                }
            } catch (e: Exception) {
                results.add(
                    AnalysisResult(
                        component = viewModel,
                        severity = Severity.ERROR,
                        message = "Erreur lors de l'analyse des opérations: ${e.message}"
                    )
                )
            }
        }
    }

    private fun checkDatabaseQueries() {
        val daos = listOf(
            "ExpenseDao",
            "IncomeDao",
            "SavingsDao",
            "SpendingLimitDao"
        )

        daos.forEach { dao ->
            try {
                val clazz = Class.forName("com.cashruler.data.dao.$dao")
                val queries = clazz.declaredMethods.filter { 
                    it.isAnnotationPresent(androidx.room.Query::class.java)
                }

                queries.forEach { query ->
                    if (!isQueryOptimized(query)) {
                        results.add(
                            AnalysisResult(
                                component = "$dao.${query.name}",
                                severity = Severity.WARNING,
                                message = "Requête potentiellement non optimisée"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                results.add(
                    AnalysisResult(
                        component = dao,
                        severity = Severity.ERROR,
                        message = "Erreur lors de l'analyse des requêtes: ${e.message}"
                    )
                )
            }
        }
    }

    private fun hasErrorHandling(method: Method): Boolean {
        return method.annotations.any { 
            it.annotationClass.simpleName?.contains("Throws") == true 
        } || method.body?.contains(".catch") == true
    }

    private fun Method.isSuspend(): Boolean {
        return this.modifiers.toString().contains("suspend")
    }

    private fun isQueryOptimized(method: Method): Boolean {
        val query = method.getAnnotation(androidx.room.Query::class.java)
        val queryStr = query.value.lowercase()
        
        return !queryStr.contains("select *") &&
               !queryStr.contains("like '%") &&
               !queryStr.contains("order by") || queryStr.contains("index")
    }

    private val Method.body: String?
        get() = try {
            this.declaringClass
                .getResourceAsStream("${this.declaringClass.simpleName}.class")
                ?.bufferedReader()
                ?.readText()
        } catch (e: Exception) {
            null
        }

    data class AnalysisResult(
        val component: String,
        val severity: Severity,
        val message: String
    )

    enum class Severity {
        INFO,
        WARNING,
        ERROR
    }
}
