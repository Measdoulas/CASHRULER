package com.cashruler

import androidx.multidex.MultiDexApplication
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.cashruler.notifications.NotificationService // Added
import com.cashruler.notifications.workers.GenerateRecurringExpensesWorker // Ajouté
import com.cashruler.notifications.workers.IncomeReminderWorker
import com.cashruler.notifications.workers.ExpenseReminderWorker // Added
import com.cashruler.notifications.workers.DailyLimitResetWorker // Added
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CashRulerApp : MultiDexApplication(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject // Added for NotificationService
    lateinit var notificationService: NotificationService

    @Inject // Added for NotificationManager to ensure init
    lateinit var notificationManager: com.cashruler.notifications.NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager // Reference to ensure Hilt initialization
        setupNotificationChannels()
        scheduleWorkers()
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }

    private fun setupNotificationChannels() {
        // This method is now empty as channel creation is centralized in NotificationManager.
        // It can be kept for future notification-related setup if needed.
    }

    private fun scheduleWorkers() {
        // Programme le worker pour les revenus récurrents
        IncomeReminderWorker.schedule(this)
        // Programme le worker pour la génération des dépenses récurrentes
        GenerateRecurringExpensesWorker.schedule(this)
        // Programme le worker pour les rappels de dépenses manuelles
        notificationService.scheduleExpenseReminders() // Changed to use injected service
        // Programme le worker pour la réinitialisation quotidienne des limites de dépenses
        DailyLimitResetWorker.schedule(this)
    }

    // Companion object with channel ID constants removed
}
