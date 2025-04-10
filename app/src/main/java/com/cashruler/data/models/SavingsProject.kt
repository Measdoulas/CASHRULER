package com.cashruler.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Modèle représentant un projet d'épargne
 */
@Entity(tableName = "savings_projects")
data class SavingsProject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * Titre du projet d'épargne
     */
    val title: String,
    
    /**
     * Description du projet
     */
    val description: String,
    
    /**
     * Objectif de montant à atteindre
     */
    val targetAmount: Double,
    
    /**
     * Montant actuel épargné
     */
    val currentAmount: Double = 0.0,
    
    /**
     * Date de début du projet
     */
    val startDate: Date = Date(),
    
    /**
     * Date cible pour atteindre l'objectif
     * Null si pas de date limite
     */
    val targetDate: Date? = null,
    
    /**
     * Montant à épargner périodiquement
     */
    val periodicAmount: Double? = null,
    
    /**
     * Fréquence d'épargne en jours
     * (ex: 30 pour mensuel, 7 pour hebdomadaire)
     */
    val savingFrequency: Int? = null,
    
    /**
     * Indique si le projet est actif
     */
    val isActive: Boolean = true,
    
    /**
     * Date de création du projet
     */
    val createdAt: Date = Date(),
    
    /**
     * Date de dernière modification
     */
    val updatedAt: Date = Date(),
    
    /**
     * Icône du projet (nom de ressource)
     */
    val icon: String? = null,
    
    /**
     * Notes additionnelles
     */
    val notes: String? = null,
    
    /**
     * Priorité du projet (plus le chiffre est bas, plus la priorité est haute)
     */
    val priority: Int = 0
) {
    /**
     * Vérifie si le titre est valide
     */
    fun isValidTitle(): Boolean = title.isNotBlank()
    
    /**
     * Vérifie si la description est valide
     */
    fun isValidDescription(): Boolean = description.isNotBlank()
    
    /**
     * Vérifie si le montant cible est valide
     */
    fun isValidTargetAmount(): Boolean = targetAmount > 0
    
    /**
     * Vérifie si le montant actuel est valide
     */
    fun isValidCurrentAmount(): Boolean = currentAmount >= 0 && currentAmount <= targetAmount
    
    /**
     * Vérifie si la configuration d'épargne périodique est valide
     */
    fun isValidPeriodicSaving(): Boolean {
        if (periodicAmount == null && savingFrequency == null) return true
        return (periodicAmount != null && periodicAmount > 0) &&
               (savingFrequency != null && savingFrequency > 0)
    }
    
    /**
     * Vérifie si les dates sont valides
     */
    fun isValidDates(): Boolean {
        if (targetDate == null) return true
        return targetDate.after(startDate)
    }
    
    /**
     * Vérifie si le projet est valide dans son ensemble
     */
    fun isValid(): Boolean =
        isValidTitle() &&
        isValidDescription() &&
        isValidTargetAmount() &&
        isValidCurrentAmount() &&
        isValidPeriodicSaving() &&
        isValidDates()
    
    /**
     * Calcule le pourcentage de progression
     */
    fun getProgress(): Float = (currentAmount / targetAmount).toFloat()
    
    /**
     * Calcule le montant restant à épargner
     */
    fun getRemainingAmount(): Double = targetAmount - currentAmount
    
    /**
     * Calcule le temps restant en jours
     * Retourne null si pas de date cible
     */
    fun getRemainingDays(): Int? {
        if (targetDate == null) return null
        return ((targetDate.time - Date().time) / (1000 * 60 * 60 * 24)).toInt()
    }

    companion object {
        /**
         * Icônes disponibles pour les projets
         */
        val AVAILABLE_ICONS = listOf(
            "ic_home",
            "ic_car",
            "ic_vacation",
            "ic_education",
            "ic_shopping",
            "ic_health",
            "ic_gift",
            "ic_other"
        )

        /**
         * Crée une instance vide pour l'édition
         */
        fun empty() = SavingsProject(
            title = "",
            description = "",
            targetAmount = 0.0
        )
    }
}
