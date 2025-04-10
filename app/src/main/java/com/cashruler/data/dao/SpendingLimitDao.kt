package com.cashruler.data.dao

import androidx.room.*
import com.cashruler.data.models.SpendingLimit
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * DAO pour les opérations sur les limites de dépenses
 */
@Dao
interface SpendingLimitDao {
    /**
     * Insère une nouvelle limite de dépenses
     * @return l'ID de la limite créée
     */
    @Insert
    suspend fun insert(limit: SpendingLimit): Long

    /**
     * Met à jour une limite de dépenses existante
     */
    @Update
    suspend fun update(limit: SpendingLimit)

    /**
     * Supprime une limite de dépenses
     */
    @Delete
    suspend fun delete(limit: SpendingLimit)

    /**
     * Récupère une limite de dépenses par son ID
     */
    @Query("SELECT * FROM spending_limits WHERE id = :limitId")
    fun getLimitById(limitId: Long): Flow<SpendingLimit?>

    /**
     * Récupère toutes les limites de dépenses actives
     */
    @Query("SELECT * FROM spending_limits WHERE isActive = 1 ORDER BY category")
    fun getActiveLimits(): Flow<List<SpendingLimit>>

    /**
     * Récupère toutes les limites de dépenses
     */
    @Query("SELECT * FROM spending_limits ORDER BY isActive DESC, category")
    fun getAllLimits(): Flow<List<SpendingLimit>>

    /**
     * Met à jour le montant dépensé d'une limite
     */
    @Query("""
        UPDATE spending_limits 
        SET currentSpent = :amount,
            updatedAt = :date
        WHERE id = :limitId
    """)
    suspend fun updateSpentAmount(limitId: Long, amount: Double, date: Date = Date())

    /**
     * Ajoute un montant aux dépenses d'une limite
     */
    @Query("""
        UPDATE spending_limits 
        SET currentSpent = currentSpent + :amount,
            updatedAt = :date
        WHERE id = :limitId
    """)
    suspend fun addToSpentAmount(limitId: Long, amount: Double, date: Date = Date())

    /**
     * Change l'état actif/inactif d'une limite
     */
    @Query("UPDATE spending_limits SET isActive = :isActive, updatedAt = :date WHERE id = :limitId")
    suspend fun setLimitActive(limitId: Long, isActive: Boolean, date: Date = Date())

    /**
     * Récupère les limites de dépenses par catégorie
     */
    @Query("SELECT * FROM spending_limits WHERE category = :category AND isActive = 1")
    fun getLimitsByCategory(category: String): Flow<List<SpendingLimit>>

    /**
     * Récupère les limites de dépenses dépassées
     */
    @Query("SELECT * FROM spending_limits WHERE currentSpent > amount AND isActive = 1")
    fun getExceededLimits(): Flow<List<SpendingLimit>>

    /**
     * Récupère les limites de dépenses approchant leur seuil d'alerte
     */
    @Query("""
        SELECT * FROM spending_limits 
        WHERE isActive = 1
        AND enableNotifications = 1
        AND (currentSpent / amount) * 100 >= warningThreshold
        AND currentSpent <= amount
    """)
    fun getLimitsNearThreshold(): Flow<List<SpendingLimit>>

    /**
     * Réinitialise les montants dépensés des limites dont la période est terminée
     */
    @Query("""
        UPDATE spending_limits 
        SET currentSpent = 0,
            currentPeriodStart = :newPeriodStart,
            updatedAt = :date
        WHERE isActive = 1
        AND :date >= (currentPeriodStart + (periodInDays * 24 * 60 * 60 * 1000))
    """)
    suspend fun resetExpiredPeriods(newPeriodStart: Date = Date(), date: Date = Date())

    /**
     * Récupère les statistiques globales des limites
     */
    @Query("""
        SELECT COUNT(*) as totalLimits,
               SUM(CASE WHEN isActive = 1 THEN 1 ELSE 0 END) as activeLimits,
               SUM(CASE WHEN currentSpent > amount THEN 1 ELSE 0 END) as exceededLimits,
               SUM(currentSpent) as totalSpent,
               SUM(amount) as totalLimit,
               AVG(CASE WHEN amount > 0 THEN (currentSpent / amount) * 100 ELSE 0 END) as avgUsage
        FROM spending_limits
        WHERE isActive = 1
    """)
    fun getGlobalStatistics(): Flow<SpendingLimitStatistics>

    /**
     * Supprime toutes les limites de dépenses
     */
    @Query("DELETE FROM spending_limits")
    suspend fun deleteAll()
}

/**
 * Classe de données pour les statistiques globales
 */
data class SpendingLimitStatistics(
    val totalLimits: Int,
    val activeLimits: Int,
    val exceededLimits: Int,
    val totalSpent: Double,
    val totalLimit: Double,
    val avgUsage: Double
)
