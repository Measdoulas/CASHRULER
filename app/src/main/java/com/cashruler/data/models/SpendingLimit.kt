package com.cashruler.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Modèle représentant une limite de dépenses
 */
@Entity(tableName = "spending_limits")
data class SpendingLimit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * Catégorie concernée par la limite
     */
    val category: String,
    
    /**
     * Montant limite
     */
    val amount: Double,
    
    /**
     * Période de la limite en jours
     * (ex: 30 pour mensuel, 7 pour hebdomadaire)
     */
    val periodInDays: Int,
    
    /**
     * Date de début de la période courante
     */
    val currentPeriodStart: Date = Date(),
    
    /**
     * Montant déjà dépensé dans la période courante
     */
    val currentSpent: Double = 0.0,
    
    /**
     * Indique si des notifications doivent être envoyées
     */
    val enableNotifications: Boolean = true,
    
    /**
     * Seuil d'alerte en pourcentage (0-100)
     */
    val warningThreshold: Int = 80,
    
    /**
     * Date de création de la limite
     */
    val createdAt: Date = Date(),
    
    /**
     * Date de dernière modification
     */
    val updatedAt: Date = Date(),
    
    /**
     * Notes additionnelles
     */
    val notes: String? = null,
    
    /**
     * Indique si la limite est active
     */
    val isActive: Boolean = true
) {
    /**
     * Vérifie si la catégorie est valide
     */
    fun isValidCategory(): Boolean = category.isNotBlank()
    
    /**
     * Vérifie si le montant est valide
     */
    fun isValidAmount(): Boolean = amount > 0
    
    /**
     * Vérifie si la période est valide
     */
    fun isValidPeriod(): Boolean = periodInDays > 0
    
    /**
     * Vérifie si le seuil d'alerte est valide
     */
    fun isValidWarningThreshold(): Boolean = warningThreshold in 0..100
    
    /**
     * Vérifie si la limite est valide dans son ensemble
     */
    fun isValid(): Boolean =
        isValidCategory() &&
        isValidAmount() &&
        isValidPeriod() &&
        isValidWarningThreshold()
    
    /**
     * Calcule le pourcentage déjà consommé
     */
    fun getUsagePercentage(): Float = (currentSpent / amount * 100).toFloat()
    
    /**
     * Vérifie si le seuil d'alerte est atteint
     */
    fun isWarningThresholdReached(): Boolean = getUsagePercentage() >= warningThreshold
    
    /**
     * Vérifie si la limite est dépassée
     */
    fun isLimitExceeded(): Boolean = currentSpent > amount
    
    /**
     * Calcule le montant restant disponible
     */
    fun getRemainingAmount(): Double = (amount - currentSpent).coerceAtLeast(0.0)
    
    /**
     * Calcule le nombre de jours restants dans la période courante
     */
    fun getRemainingDays(): Int {
        val periodEnd = Date(currentPeriodStart.time + periodInDays * 24 * 60 * 60 * 1000)
        return ((periodEnd.time - Date().time) / (1000 * 60 * 60 * 24)).toInt()
    }

    companion object {
        /**
         * Périodes prédéfinies
         */
        const val PERIOD_DAILY = 1
        const val PERIOD_WEEKLY = 7
        const val PERIOD_MONTHLY = 30
        const val PERIOD_YEARLY = 365

        /**
         * Seuils d'alerte prédéfinis
         */
        val PRESET_THRESHOLDS = listOf(50, 75, 80, 90)

        /**
         * Crée une instance vide pour l'édition
         */
        fun empty() = SpendingLimit(
            category = "",
            amount = 0.0,
            periodInDays = PERIOD_MONTHLY
        )
    }
}
