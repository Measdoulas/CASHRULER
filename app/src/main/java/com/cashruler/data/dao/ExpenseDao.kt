package com.cashruler.data.dao

import androidx.room.*
import com.cashruler.data.models.Expense
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * DAO pour les opérations sur les dépenses
 */
@Dao
interface ExpenseDao {
    /**
     * Insère une nouvelle dépense
     * @return l'ID de la dépense créée
     */
    @Insert
    suspend fun insert(expense: Expense): Long

    /**
     * Met à jour une dépense existante
     */
    @Update
    suspend fun update(expense: Expense)

    /**
     * Supprime une dépense
     */
    @Delete
    suspend fun delete(expense: Expense)

    /**
     * Récupère une dépense par son ID
     */
    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    fun getExpenseById(expenseId: Long): Flow<Expense?>

    /**
     * Récupère toutes les dépenses, triées par date décroissante
     */
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    /**
     * Récupère les dépenses sur une période donnée
     */
    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesBetweenDates(startDate: Date, endDate: Date): Flow<List<Expense>>

    /**
     * Récupère les dépenses par catégorie
     */
    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC")
    fun getExpensesByCategory(category: String): Flow<List<Expense>>

    /**
     * Récupère le total des dépenses sur une période
     */
    @Query("SELECT SUM(amount) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    fun getTotalExpensesBetweenDates(startDate: Date, endDate: Date): Flow<Double?>

    /**
     * Récupère le total des dépenses par catégorie sur une période
     */
    @Query("""
        SELECT category, SUM(amount) as total
        FROM expenses
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getTotalExpensesByCategory(startDate: Date, endDate: Date): Flow<Map<String, Double>>

    /**
     * Récupère les dépenses récurrentes
     */
    @Query("SELECT * FROM expenses WHERE isRecurring = 1 ORDER BY date DESC")
    fun getRecurringExpenses(): Flow<List<Expense>>

    /**
     * Récupère les dépenses liées à une limite de dépenses
     */
    @Query("SELECT * FROM expenses WHERE spendingLimitId = :limitId ORDER BY date DESC")
    fun getExpensesBySpendingLimit(limitId: Long): Flow<List<Expense>>

    /**
     * Récupère le total des dépenses pour une limite de dépenses
     */
    @Query("""
        SELECT SUM(amount)
        FROM expenses
        WHERE spendingLimitId = :limitId
        AND date BETWEEN :startDate AND :endDate
    """)
    fun getTotalExpensesForSpendingLimit(
        limitId: Long,
        startDate: Date,
        endDate: Date
    ): Flow<Double?>

    /**
     * Supprime toutes les dépenses
     */
    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    /**
     * Récupère le nombre total de dépenses
     */
    @Query("SELECT COUNT(*) FROM expenses")
    fun getExpenseCount(): Flow<Int>

    /**
     * Récupère toutes les catégories distinctes utilisées
     */
    @Query("SELECT DISTINCT category FROM expenses ORDER BY category")
    fun getAllCategories(): Flow<List<String>>

    /**
     * Récupère les dépenses récurrentes dont la prochaine date de rappel est avant ou égale à untilDate.
     */
    @Query("SELECT * FROM expenses WHERE isRecurring = 1 AND nextReminderDate IS NOT NULL AND nextReminderDate <= :untilDate")
    fun getUpcomingRecurringExpensesForReminderList(untilDate: Date): List<Expense>
}
