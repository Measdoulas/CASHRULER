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
    const val EXPENSE_FORM = "expense_form"
    const val EXPENSE_DETAILS = "expense_details/{expenseId}"
    
    fun expenseDetails(expenseId: Long) = "expense_details/$expenseId"

    // Sous-écrans des revenus
    const val INCOME_FORM = "income_form"
    const val INCOME_DETAILS = "income_details/{incomeId}"
    
    fun incomeDetails(incomeId: Long) = "income_details/$incomeId"

    // Sous-écrans de l'épargne
    const val SAVINGS_FORM = "savings_form"
    const val SAVINGS_PROJECT = "savings_project/{projectId}"
    const val SAVINGS_TRANSACTION = "savings_transaction/{projectId}"
    
    fun savingsProject(projectId: Long) = "savings_project/$projectId"
    fun savingsTransaction(projectId: Long) = "savings_transaction/$projectId"

    // Écrans de paramètres
    const val BACKUP = "backup"
    const val NOTIFICATIONS = "notifications"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"
}
