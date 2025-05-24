package com.cashruler.navigation

object Routes {
    // Écrans principaux
    const val DASHBOARD = "dashboard"
    const val EXPENSES = "expenses"
    const val INCOME = "income"
    const val SAVINGS = "savings"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"

    // Sous-écrans des dépenses
    const val EXPENSE_FORM_NEW = "expense_form_new" // Pour créer une nouvelle dépense
    const val EXPENSE_DETAILS = "expense_details/{expenseId}" // Pour voir/éditer une dépense existante
    
    fun expenseDetails(expenseId: Long) = "expense_details/$expenseId"

    // Sous-écrans des revenus
    const val INCOME_FORM_NEW = "income_form_new" // Pour créer un nouveau revenu
    const val INCOME_DETAILS = "income_details/{incomeId}" // Pour voir/éditer un revenu existant
    
    fun incomeDetails(incomeId: Long) = "income_details/$incomeId"

    // Sous-écrans de l'épargne
    const val SAVINGS_FORM_NEW = "savings_form_new" // Pour créer un nouveau projet
    const val SAVINGS_FORM_EDIT = "savings_form_edit/{projectId}" // Pour éditer un projet existant
    const val SAVINGS_PROJECT_DETAILS = "savings_project_details/{projectId}" // Pour voir les détails d'un projet
    // SAVINGS_TRANSACTION et savingsTransaction sont supprimés

    fun savingsProjectDetails(projectId: Long) = "savings_project_details/$projectId"
    fun savingsFormEdit(projectId: Long) = "savings_form_edit/$projectId"
    // fun savingsTransaction(projectId: Long) = "savings_transaction/$projectId" // Supprimé

    // Écrans de paramètres
    const val BACKUP = "backup"
    const val NOTIFICATIONS = "notifications"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"
}
