package com.cashruler

import androidx.multidex.MultiDexApplication
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.cashruler.notifications.workers.IncomeReminderWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CashRulerApp : MultiDexApplication(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Canal pour les limites de dépenses
            NotificationChannel(
                SPENDING_LIMIT_CHANNEL_ID,
                "Limites de dépenses",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications pour les limites de dépenses atteintes"
                notificationManager.createNotificationChannel(this)
            }

            // Canal pour les rappels d'épargne
            NotificationChannel(
                SAVINGS_REMINDER_CHANNEL_ID,
                "Rappels d'épargne",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Rappels pour vos objectifs d'épargne"
                notificationManager.createNotificationChannel(this)
            }

            // Canal pour les revenus récurrents
            NotificationChannel(
                INCOME_REMINDER_CHANNEL_ID,
                "Rappels de revenus",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Rappels pour les revenus récurrents à venir"
                enableVibration(true)
                notificationManager.createNotificationChannel(this)
            }

            // Canal pour les sauvegardes
            NotificationChannel(
                BACKUP_CHANNEL_ID,
                "Sauvegardes",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications liées aux sauvegardes de données"
                notificationManager.createNotificationChannel(this)
            }
        }
    }

    private fun scheduleWorkers() {
        // Programme le worker pour les revenus récurrents
        IncomeReminderWorker.schedule(this)
    }

    companion object {
        const val SPENDING_LIMIT_CHANNEL_ID = "spending_limit_channel"
        const val SAVINGS_REMINDER_CHANNEL_ID = "savings_reminder_channel"
        const val INCOME_REMINDER_CHANNEL_ID = "income_reminder_channel"
        const val BACKUP_CHANNEL_ID = "backup_channel"
    }
}
