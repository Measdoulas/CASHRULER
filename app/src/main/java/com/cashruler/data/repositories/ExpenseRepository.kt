package com.cashruler.data.repositories

import com.cashruler.data.dao.CategoryDao
import com.cashruler.data.dao.ExpenseDao
import com.cashruler.data.models.CategoryEntity
import com.cashruler.data.models.Expense
import com.cashruler.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository pour la gestion des dépenses
 */
@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao, // Injection de CategoryDao
    @IODispatcher private val dispatcher: CoroutineDispatcher
) {
    /**
     * Récupère toutes les dépenses
     */
    fun getAllExpenses() = expenseDao.getAllExpenses()
        .flowOn(dispatcher)

    /**
     * Récupère une dépense par son ID
     */
    fun getExpenseById(id: Long) = expenseDao.getExpenseById(id)
        .flowOn(dispatcher)

    /**
     * Récupère les dépenses sur une période donnée
     */
    fun getExpensesBetweenDates(
        startDate: Date,
        endDate: Date
    ) = expenseDao.getExpensesBetweenDates(startDate, endDate)
        .flowOn(dispatcher)

    /**
     * Récupère les dépenses par catégorie
     */
    fun getExpensesByCategory(category: String) = expenseDao.getExpensesByCategory(category)
        .flowOn(dispatcher)

    /**
     * Récupère le total des dépenses sur une période
     */
    fun getTotalExpensesBetweenDates(
        startDate: Date,
        endDate: Date
    ) = expenseDao.getTotalExpensesBetweenDates(startDate, endDate)
        .map { it ?: 0.0 }
        .flowOn(dispatcher)

    /**
     * Récupère le total des dépenses par catégorie sur une période
     */
    fun getTotalExpensesByCategory(
        startDate: Date,
        endDate: Date
    ) = expenseDao.getTotalExpensesByCategory(startDate, endDate)
        .flowOn(dispatcher)

    /**
     * Récupère les dépenses récurrentes
     */
    fun getRecurringExpenses() = expenseDao.getRecurringExpenses()
        .flowOn(dispatcher)

    /**
     * Récupère les dépenses liées à une limite de dépenses
     */
    fun getExpensesBySpendingLimit(limitId: Long) = expenseDao.getExpensesBySpendingLimit(limitId)
        .flowOn(dispatcher)

    /**
     * Récupère le total des dépenses pour une limite
     */
    fun getTotalExpensesForSpendingLimit(
        limitId: Long,
        startDate: Date,
        endDate: Date
    ) = expenseDao.getTotalExpensesForSpendingLimit(limitId, startDate, endDate)
        .map { it ?: 0.0 }
        .flowOn(dispatcher)

    /**
     * Récupère toutes les catégories depuis la table CategoryEntity
     */
    fun getAllCategories(): Flow<List<String>> = categoryDao.getAll()
        .map { categories -> categories.map { it.name } }
        .flowOn(dispatcher)

    /**
     * Ajoute une nouvelle catégorie à la table CategoryEntity
     */
    suspend fun addCategory(categoryName: String) = withContext(dispatcher) {
        // Vérifie si la catégorie existe déjà pour éviter les doublons via la contrainte unique de la DB
        // L'insertion sera ignorée par Room si le nom existe déjà grâce à OnConflictStrategy.IGNORE
        categoryDao.insert(CategoryEntity(name = categoryName))
    }

    /**
     * Ajoute une nouvelle dépense
     */
    suspend fun addExpense(expense: Expense) = withContext(dispatcher) {
        expenseDao.insert(expense)
    }

    /**
     * Met à jour une dépense existante
     */
    suspend fun updateExpense(expense: Expense) = withContext(dispatcher) {
        expenseDao.update(expense)
    }

    /**
     * Supprime une dépense
     */
    suspend fun deleteExpense(expense: Expense) = withContext(dispatcher) {
        expenseDao.delete(expense)
    }

    /**
     * Supprime toutes les dépenses
     */
    suspend fun deleteAllExpenses() = withContext(dispatcher) {
        expenseDao.deleteAll()
    }

    /**
     * Supprime toutes les dépenses
     */
    suspend fun deleteAllData() = withContext(dispatcher) {
        expenseDao.deleteAll()
    }


    suspend fun getAllExpensesList(): List<Expense> = withContext(dispatcher) {
        expenseDao.getAllExpenses().first()
    }


    /**
     * Valide les champs d'une dépense
     */
    fun validateExpense(expense: Expense): ValidationResult {
        val errors = mutableListOf<String>()

        if (!expense.isValidAmount()) {
            errors.add("Le montant doit être supérieur à 0")
        }
        if (!expense.isValidDescription()) {
            errors.add("La description ne peut pas être vide")
        }
        if (!expense.isValidCategory()) {
            errors.add("La catégorie ne peut pas être vide")
        }
        if (!expense.isValidRecurrence()) {
            errors.add("La configuration de récurrence est invalide")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }

    /**
     * Calcule la prochaine date de génération pour une dépense récurrente.
     * Prend une date de base et une fréquence, et retourne la date de base + fréquence.
     */
    fun calculateNextGenerationDate(baseDate: Date, recurringFrequency: Int): Date {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = baseDate
        calendar.add(java.util.Calendar.DAY_OF_YEAR, recurringFrequency)
        return calendar.time
    }

    /**
     * Récupère les dépenses récurrentes dues (nextGenerationDate <= currentDate).
     */
    suspend fun getDueRecurringExpenses(currentDate: Date): List<Expense> = withContext(dispatcher) {
        expenseDao.getDueRecurringExpenses(currentDate)
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val errors: List<String>) : ValidationResult()
}
