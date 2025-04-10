package com.cashruler.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Modèle représentant un revenu
 */
@Entity(tableName = "incomes")
data class Income(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * Montant du revenu
     */
    val amount: Double,
    
    /**
     * Description du revenu
     */
    val description: String,
    
    /**
     * Type de revenu
     */
    val type: String,
    
    /**
     * Date du revenu
     */
    val date: Date,
    
    /**
     * Indique si le revenu est récurrent
     */
    val isRecurring: Boolean = false,
    
    /**
     * Fréquence de récurrence (en jours)
     * Null si le revenu n'est pas récurrent
     */
    val recurringFrequency: Int? = null,
    
    /**
     * Date de la prochaine occurrence
     * Uniquement pour les revenus récurrents
     */
    val nextOccurrence: Date? = null,
    
    /**
     * Date de création de l'entrée
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
     * Pièce jointe (URI de l'image/document)
     */
    val attachmentUri: String? = null,
    
    /**
     * Indique si le revenu est imposable
     */
    val isTaxable: Boolean = true,
    
    /**
     * Taux d'imposition applicable (en pourcentage)
     */
    val taxRate: Double? = null
) {
    /**
     * Vérifie si le montant est valide
     */
    fun isValidAmount(): Boolean = amount > 0
    
    /**
     * Vérifie si la description est valide
     */
    fun isValidDescription(): Boolean = description.isNotBlank()
    
    /**
     * Vérifie si le type est valide
     */
    fun isValidType(): Boolean = type.isNotBlank()
    
    /**
     * Vérifie si la configuration de récurrence est valide
     */
    fun isValidRecurrence(): Boolean = !isRecurring || (recurringFrequency != null && recurringFrequency > 0)
    
    /**
     * Vérifie si la configuration fiscale est valide
     */
    fun isValidTaxation(): Boolean = !isTaxable || (taxRate != null && taxRate >= 0 && taxRate <= 100)
    
    /**
     * Vérifie si l'entrée est valide dans son ensemble
     */
    fun isValid(): Boolean =
        isValidAmount() &&
        isValidDescription() &&
        isValidType() &&
        isValidRecurrence() &&
        isValidTaxation()
    
    /**
     * Calcule le montant net après impôts
     */
    fun getNetAmount(): Double {
        if (!isTaxable || taxRate == null) return amount
        return amount * (1 - taxRate / 100)
    }

    companion object {
        /**
         * Types de revenus par défaut
         */
        val DEFAULT_TYPES = listOf(
            "Salaire",
            "Freelance",
            "Investissement",
            "Location",
            "Prime",
            "Remboursement",
            "Autres"
        )

        /**
         * Crée une instance vide pour l'édition
         */
        fun empty() = Income(
            amount = 0.0,
            description = "",
            type = DEFAULT_TYPES.first(),
            date = Date()
        )
    }
}
