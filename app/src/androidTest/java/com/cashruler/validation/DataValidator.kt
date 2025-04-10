package com.cashruler.validation

import com.cashruler.data.models.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Validateur des données de l'application
 */
class DataValidator {

    private val dateFormat = SimpleDateFormat(ValidationConfig.DATE_FORMAT, Locale.getDefault())

    fun validateExpense(expense: Expense): ValidationResult {
        val rules = ValidationConfig.DataValidation.EXPENSE_RULES
        val errors = mutableListOf<String>()

        // Validation du montant
        if (expense.amount !in rules.minAmount..rules.maxAmount) {
            errors.add(ValidationConfig.ErrorMessages.INVALID_AMOUNT)
        }

        // Validation de la catégorie
        if (expense.category !in rules.validCategories) {
            errors.add(ValidationConfig.ErrorMessages.INVALID_CATEGORY)
        }

        // Validation des champs requis
        if (expense.title.isBlank()) {
            errors.add("Le titre est requis")
        }

        // Validation de la date
        if (!isValidDate(expense.date)) {
            errors.add(ValidationConfig.ErrorMessages.INVALID_DATE)
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    fun validateIncome(income: Income): ValidationResult {
        val rules = ValidationConfig.DataValidation.INCOME_RULES
        val errors = mutableListOf<String>()

        // Validation du montant
        if (income.amount !in rules.minAmount..rules.maxAmount) {
            errors.add(ValidationConfig.ErrorMessages.INVALID_AMOUNT)
        }

        // Validation du type
        if (income.type !in rules.validCategories) {
            errors.add("Type de revenu invalide")
        }

        // Validation des champs requis
        if (income.name.isBlank()) {
            errors.add("Le nom est requis")
        }

        // Validation de la date
        if (!isValidDate(income.date)) {
            errors.add(ValidationConfig.ErrorMessages.INVALID_DATE)
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    fun validateSavingsProject(project: SavingsProject): ValidationResult {
        val rules = ValidationConfig.DataValidation.SAVINGS_RULES
        val errors = mutableListOf<String>()

        // Validation des montants
        if (project.targetAmount !in rules.minAmount..rules.maxAmount) {
            errors.add("Montant cible invalide")
        }
        if (project.currentAmount > project.targetAmount) {
            errors.add("Le montant actuel ne peut pas dépasser le montant cible")
        }

        // Validation des dates
        if (!isValidDate(project.startDate)) {
            errors.add("Date de début invalide")
        }
        if (!isValidDate(project.deadline)) {
            errors.add("Date limite invalide")
        }
        if (project.deadline.before(project.startDate)) {
            errors.add("La date limite doit être postérieure à la date de début")
        }

        // Validation du titre
        if (project.title.isBlank()) {
            errors.add("Le titre est requis")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    fun validateSpendingLimit(limit: SpendingLimit): ValidationResult {
        val errors = mutableListOf<String>()

        // Validation du montant
        if (limit.amount <= 0) {
            errors.add("Le montant de la limite doit être positif")
        }

        // Validation de la catégorie
        if (limit.category !in ValidationConfig.VALID_CATEGORIES) {
            errors.add(ValidationConfig.ErrorMessages.INVALID_CATEGORY)
        }

        // Validation de la date
        if (!isValidDate(limit.startDate)) {
            errors.add(ValidationConfig.ErrorMessages.INVALID_DATE)
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    fun validateTransaction(transaction: SavingsTransaction): ValidationResult {
        val errors = mutableListOf<String>()

        // Validation du montant
        if (transaction.amount <= 0) {
            errors.add("Le montant de la transaction doit être positif")
        }

        // Validation de la date
        if (!isValidDate(transaction.date)) {
            errors.add(ValidationConfig.ErrorMessages.INVALID_DATE)
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    private fun isValidDate(date: Date): Boolean {
        return try {
            dateFormat.format(date)
            date.time in 0..System.currentTimeMillis()
        } catch (e: Exception) {
            false
        }
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList()
    )

    companion object {
        fun validateDecimals(amount: Double): Boolean {
            val decimals = (amount * 100).toInt() % 100
            return decimals.toString().length <= ValidationConfig.MAX_DECIMAL_PLACES
        }

        fun validateDateRange(startDate: Date, endDate: Date): Boolean {
            return startDate.before(endDate) && 
                   startDate.time >= 0 && 
                   endDate.time <= System.currentTimeMillis()
        }
    }
}
