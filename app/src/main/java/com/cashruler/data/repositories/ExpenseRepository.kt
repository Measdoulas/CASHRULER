package com.cashruler.data.repositories

import com.cashruler.data.dao.ExpenseDao
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
     * Récupère toutes les catégories distinctes utilisées
     */
    fun getAllCategories() = expenseDao.getAllCategories()
        .flowOn(dispatcher)

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
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val errors: List<String>) : ValidationResult()
}
