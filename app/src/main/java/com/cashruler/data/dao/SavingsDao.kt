package com.cashruler.data.dao

import androidx.room.*
import com.cashruler.data.models.SavingsProject
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * DAO pour les opérations sur les projets d'épargne
 */
@Dao
interface SavingsDao {
    /**
     * Insère un nouveau projet d'épargne
     * @return l'ID du projet créé
     */
    @Insert
    suspend fun insert(project: SavingsProject): Long

    /**
     * Met à jour un projet d'épargne existant
     */
    @Update
    suspend fun update(project: SavingsProject)

    /**
     * Supprime un projet d'épargne
     */
    @Delete
    suspend fun delete(project: SavingsProject)

    /**
     * Récupère un projet d'épargne par son ID
     */
    @Query("SELECT * FROM savings_projects WHERE id = :projectId")
    fun getProjectById(projectId: Long): Flow<SavingsProject?>

    /**
     * Récupère tous les projets d'épargne actifs
     */
    @Query("SELECT * FROM savings_projects WHERE isActive = 1 ORDER BY priority, createdAt DESC")
    fun getActiveProjects(): Flow<List<SavingsProject>>

    /**
     * Récupère tous les projets d'épargne
     */
    @Query("SELECT * FROM savings_projects ORDER BY isActive DESC, priority, createdAt DESC")
    fun getAllProjects(): Flow<List<SavingsProject>>

    /**
     * Met à jour le montant actuel d'un projet
     */
    @Query("UPDATE savings_projects SET currentAmount = :amount, updatedAt = :date WHERE id = :projectId")
    suspend fun updateProjectAmount(projectId: Long, amount: Double, date: Date = Date())

    /**
     * Ajoute un montant au montant actuel d'un projet
     */
    @Query("""
        UPDATE savings_projects 
        SET currentAmount = currentAmount + :amount,
            updatedAt = :date
        WHERE id = :projectId
    """)
    suspend fun addToProjectAmount(projectId: Long, amount: Double, date: Date = Date())

    /**
     * Retire un montant du montant actuel d'un projet
     */
    @Query("""
        UPDATE savings_projects 
        SET currentAmount = MAX(0, currentAmount - :amount),
            updatedAt = :date
        WHERE id = :projectId
    """)
    suspend fun subtractFromProjectAmount(projectId: Long, amount: Double, date: Date = Date())

    /**
     * Change l'état actif/inactif d'un projet
     */
    @Query("UPDATE savings_projects SET isActive = :isActive, updatedAt = :date WHERE id = :projectId")
    suspend fun setProjectActive(projectId: Long, isActive: Boolean, date: Date = Date())

    /**
     * Récupère le total épargné sur tous les projets actifs
     */
    @Query("SELECT SUM(currentAmount) FROM savings_projects WHERE isActive = 1")
    fun getTotalSavedAmount(): Flow<Double?>

    /**
     * Récupère le total des objectifs pour tous les projets actifs
     */
    @Query("SELECT SUM(targetAmount) FROM savings_projects WHERE isActive = 1")
    fun getTotalTargetAmount(): Flow<Double?>

    /**
     * Récupère les projets dont la date cible approche
     */
    @Query("""
        SELECT * FROM savings_projects 
        WHERE isActive = 1 
        AND targetDate IS NOT NULL 
        AND targetDate > :currentDate
        AND targetDate <= :futureDate
        ORDER BY targetDate
    """)
    fun getUpcomingDeadlines(
        currentDate: Date = Date(),
        futureDate: Date
    ): Flow<List<SavingsProject>>

    /**
     * Récupère les projets en retard
     */
    @Query("""
        SELECT * FROM savings_projects 
        WHERE isActive = 1 
        AND targetDate IS NOT NULL 
        AND targetDate < :currentDate
        AND currentAmount < targetAmount
        ORDER BY targetDate
    """)
    fun getOverdueProjects(currentDate: Date = Date()): Flow<List<SavingsProject>>

    /**
     * Récupère les projets terminés
     */
    @Query("""
        SELECT * FROM savings_projects 
        WHERE currentAmount >= targetAmount
        OR (targetDate IS NOT NULL AND targetDate < :currentDate)
        ORDER BY updatedAt DESC
    """)
    fun getCompletedProjects(currentDate: Date = Date()): Flow<List<SavingsProject>>

    /**
     * Récupère les statistiques globales des projets
     */
    @Query("""
        SELECT COUNT(*) as totalProjects,
               SUM(CASE WHEN isActive = 1 THEN 1 ELSE 0 END) as activeProjects,
               SUM(currentAmount) as totalSaved,
               SUM(targetAmount) as totalTarget,
               AVG(CASE WHEN currentAmount > 0 THEN (currentAmount / targetAmount) * 100 ELSE 0 END) as avgProgress
        FROM savings_projects
    """)
    fun getGlobalStatistics(): Flow<SavingsStatistics>

    /**
     * Supprime tous les projets
     */
    @Query("DELETE FROM savings_projects")
    suspend fun deleteAll()
}

/**
 * Classe de données pour les statistiques globales
 */
data class SavingsStatistics(
    val totalProjects: Int,
    val activeProjects: Int,
    val totalSaved: Double,
    val totalTarget: Double,
    val avgProgress: Double
)
