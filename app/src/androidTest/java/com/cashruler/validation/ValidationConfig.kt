package com.cashruler.validation

/**
 * Configuration pour les tests de validation
 */
object ValidationConfig {
    // Paramètres de performance
    const val MAX_DB_OPERATION_TIME_MS = 100L
    const val MAX_CONCURRENT_OPERATIONS = 100
    const val MAX_MEMORY_USAGE_MB = 50

    // Paramètres de données
    const val MAX_DECIMAL_PLACES = 2
    const val MAX_TRANSACTION_AMOUNT = 1_000_000.0
    const val MIN_TRANSACTION_AMOUNT = 0.01

    // Paramètres de validation
    const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
    val VALID_CATEGORIES = setOf(
        "Alimentation",
        "Transport",
        "Logement",
        "Loisirs",
        "Santé",
        "Éducation",
        "Shopping",
        "Autres"
    )

    // Paramètres de sauvegarde
    const val MAX_BACKUP_SIZE_MB = 10
    const val MIN_BACKUP_INTERVAL_HOURS = 24

    // Paramètres de notification
    const val MIN_NOTIFICATION_INTERVAL_MINUTES = 15
    const val MAX_NOTIFICATIONS_PER_DAY = 10

    // Messages d'erreur
    object ErrorMessages {
        const val INVALID_AMOUNT = "Le montant doit être entre $MIN_TRANSACTION_AMOUNT et $MAX_TRANSACTION_AMOUNT"
        const val INVALID_CATEGORY = "La catégorie doit être l'une des catégories prédéfinies"
        const val INVALID_DATE = "La date doit être au format $DATE_FORMAT"
        const val PERFORMANCE_ERROR = "Performance en dehors des limites acceptables"
        const val BACKUP_ERROR = "Erreur lors de la sauvegarde/restauration"
        const val NOTIFICATION_ERROR = "Erreur lors de la gestion des notifications"
    }

    // Seuils de test
    object TestThresholds {
        const val DATABASE_OPERATIONS_SUCCESS_RATE = 0.99 // 99%
        const val UI_RESPONSE_TIME_MS = 16L // Pour 60 FPS
        const val ANIMATION_FRAME_DROP_THRESHOLD = 0.1 // Max 10% dropped frames
        const val MEMORY_LEAK_THRESHOLD_MB = 5
        const val CONCURRENT_OPERATIONS_SUCCESS_RATE = 0.95 // 95%
    }

    // Configuration de validation pour les différents types de données
    object DataValidation {
        val EXPENSE_RULES = ValidationRules(
            minAmount = MIN_TRANSACTION_AMOUNT,
            maxAmount = MAX_TRANSACTION_AMOUNT,
            requiredFields = setOf("title", "amount", "category", "date"),
            validCategories = VALID_CATEGORIES
        )

        val INCOME_RULES = ValidationRules(
            minAmount = MIN_TRANSACTION_AMOUNT,
            maxAmount = MAX_TRANSACTION_AMOUNT,
            requiredFields = setOf("name", "amount", "type", "date"),
            validCategories = setOf("Salaire", "Freelance", "Investissement", "Autre")
        )

        val SAVINGS_RULES = ValidationRules(
            minAmount = MIN_TRANSACTION_AMOUNT,
            maxAmount = MAX_TRANSACTION_AMOUNT,
            requiredFields = setOf("title", "targetAmount", "startDate", "deadline"),
            validCategories = emptySet()
        )
    }

    data class ValidationRules(
        val minAmount: Double,
        val maxAmount: Double,
        val requiredFields: Set<String>,
        val validCategories: Set<String>
    )
}
