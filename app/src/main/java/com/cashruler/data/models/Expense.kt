package com.cashruler.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Modèle représentant une dépense
 */
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * Montant de la dépense
     */
    val amount: Double,
    
    /**
     * Description de la dépense
     */
    val description: String,
    
    /**
     * Catégorie de la dépense
     */
    val category: String,
    
    /**
     * Date de la dépense
     */
    val date: Date,
    
    /**
     * Indique si la dépense est récurrente
     */
    val isRecurring: Boolean = false,
    
    /**
     * Fréquence de récurrence (en jours)
     * Null si la dépense n'est pas récurrente
     */
    val recurringFrequency: Int? = null,
    val nextGenerationDate: Date? = null, // Champ renommé
    
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
     * ID de la limite de dépense associée
     */
    val spendingLimitId: Long? = null
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
     * Vérifie si la catégorie est valide
     */
    fun isValidCategory(): Boolean = category.isNotBlank()
    
    /**
     * Vérifie si la configuration de récurrence est valide
     */
    fun isValidRecurrence(): Boolean = !isRecurring || (recurringFrequency != null && recurringFrequency > 0)
    
    /**
     * Vérifie si l'entrée est valide dans son ensemble
     */
    fun isValid(): Boolean =
        isValidAmount() &&
        isValidDescription() &&
        isValidCategory() &&
        isValidRecurrence()

    companion object {
        /**
         * Catégories de dépenses par défaut
         */
        val DEFAULT_CATEGORIES = listOf(
            "Alimentation",
            "Transport",
            "Logement",
            "Loisirs",
            "Santé",
            "Éducation",
            "Shopping",
            "Autres"
        )

        /**
         * Crée une instance vide pour l'édition
         */
        fun empty() = Expense(
            amount = 0.0,
            description = "",
            category = DEFAULT_CATEGORIES.first(),
            date = Date()
        )
    }
}
