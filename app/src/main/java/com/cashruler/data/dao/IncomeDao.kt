package com.cashruler.data.dao

import androidx.room.*
import com.cashruler.data.models.Income
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * DAO pour les opérations sur les revenus
 */
@Dao
interface IncomeDao {
    /**
     * Insère un nouveau revenu
     * @return l'ID du revenu créé
     */
    @Insert
    suspend fun insert(income: Income): Long

    /**
     * Met à jour un revenu existant
     */
    @Update
    suspend fun update(income: Income)

    /**
     * Supprime un revenu
     */
    @Delete
    suspend fun delete(income: Income)

    /**
     * Récupère un revenu par son ID
     */
    @Query("SELECT * FROM incomes WHERE id = :incomeId")
    fun getIncomeById(incomeId: Long): Flow<Income?>

    /**
     * Récupère tous les revenus, triés par date décroissante
     */
    @Query("SELECT * FROM incomes ORDER BY date DESC")
    fun getAllIncomes(): Flow<List<Income>>

    /**
     * Récupère les revenus sur une période donnée
     */
    @Query("SELECT * FROM incomes WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getIncomesBetweenDates(startDate: Date, endDate: Date): Flow<List<Income>>

    /**
     * Récupère les revenus par type
     */
    @Query("SELECT * FROM incomes WHERE type = :type ORDER BY date DESC")
    fun getIncomesByType(type: String): Flow<List<Income>>

    /**
     * Récupère le total des revenus sur une période
     */
    @Query("SELECT SUM(amount) FROM incomes WHERE date BETWEEN :startDate AND :endDate")
    fun getTotalIncomesBetweenDates(startDate: Date, endDate: Date): Flow<Double?>

    /**
     * Récupère le total des revenus nets (après impôts) sur une période
     */
    @Query("""
        SELECT SUM(
            CASE 
                WHEN isTaxable = 1 AND taxRate IS NOT NULL 
                THEN amount * (1 - taxRate / 100)
                ELSE amount
            END
        )
        FROM incomes 
        WHERE date BETWEEN :startDate AND :endDate
    """)
    fun getTotalNetIncomeBetweenDates(startDate: Date, endDate: Date): Flow<Double?>

    /**
     * Récupère le total des revenus par type sur une période
     */
    @Query("""
        SELECT type, SUM(amount) as total
        FROM incomes
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY type
        ORDER BY total DESC
    """)
    fun getTotalIncomesByType(startDate: Date, endDate: Date): Flow<Map<String, Double>>

    /**
     * Récupère les revenus récurrents
     */
    @Query("SELECT * FROM incomes WHERE isRecurring = 1 ORDER BY date DESC")
    fun getRecurringIncomes(): Flow<List<Income>>

    /**
     * Récupère les revenus imposables
     */
    @Query("SELECT * FROM incomes WHERE isTaxable = 1 ORDER BY date DESC")
    fun getTaxableIncomes(): Flow<List<Income>>

    /**
     * Calcule le total des impôts sur une période
     */
    @Query("""
        SELECT SUM(
            CASE 
                WHEN isTaxable = 1 AND taxRate IS NOT NULL 
                THEN amount * (taxRate / 100)
                ELSE 0
            END
        )
        FROM incomes 
        WHERE date BETWEEN :startDate AND :endDate
    """)
    fun getTotalTaxesBetweenDates(startDate: Date, endDate: Date): Flow<Double?>

    /**
     * Supprime tous les revenus
     */
    @Query("DELETE FROM incomes")
    suspend fun deleteAll()

    /**
     * Récupère le nombre total de revenus
     */
    @Query("SELECT COUNT(*) FROM incomes")
    fun getIncomeCount(): Flow<Int>

    /**
     * Récupère tous les types distincts utilisés
     */
    @Query("SELECT DISTINCT type FROM incomes ORDER BY type")
    fun getAllTypes(): Flow<List<String>>

    /**
     * Récupère les revenus avec la prochaine occurrence prévue
     */
    @Query("""
        SELECT * FROM incomes 
        WHERE isRecurring = 1 
        AND nextOccurrence IS NOT NULL 
        AND nextOccurrence > :currentDate
        ORDER BY nextOccurrence
    """)
    fun getUpcomingRecurringIncomes(currentDate: Date = Date()): Flow<List<Income>>
}
