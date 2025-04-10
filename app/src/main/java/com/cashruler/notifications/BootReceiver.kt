package com.cashruler.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cashruler.data.repositories.SavingsRepository
import com.cashruler.data.repositories.SpendingLimitRepository
import com.cashruler.data.repositories.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var spendingLimitRepository: SpendingLimitRepository

    @Inject
    lateinit var savingsRepository: SavingsRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var notificationManager: NotificationManager

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            coroutineScope.launch {
                restoreNotifications()
            }
        }
    }

    private suspend fun restoreNotifications() {
        // Vérifier si les notifications sont activées
        if (!settingsRepository.getLimitsNotificationsEnabled() && 
            !settingsRepository.getSavingsNotificationsEnabled()) {
            return
        }

        // Restaurer les notifications des limites de dépenses
        if (settingsRepository.getLimitsNotificationsEnabled()) {
            val limits = spendingLimitRepository.getAllActiveLimits()
            limits.forEach { limit ->
                notificationManager.setupLimitAlerts(limit)
            }
        }

        // Restaurer les notifications des projets d'épargne
        if (settingsRepository.getSavingsNotificationsEnabled()) {
            val projects = savingsRepository.getActiveProjects()
            projects.forEach { project ->
                notificationManager.setupSavingsReminders(project)
            }
        }
    }
}

/**
 * Extension du SpendingLimitRepository pour obtenir uniquement les limites actives
 */
suspend fun SpendingLimitRepository.getAllActiveLimits() = getAllLimitsList().filter { limit ->
    when (limit.frequency) {
        SpendingLimitFrequency.DAILY -> true // Toujours actif
        SpendingLimitFrequency.WEEKLY -> {
            // Vérifier si la limite est dans la semaine en cours
            val now = System.currentTimeMillis()
            val limitStart = limit.startDate.time
            val weekInMillis = 7 * 24 * 60 * 60 * 1000L
            (now - limitStart) < weekInMillis
        }
        SpendingLimitFrequency.MONTHLY -> {
            // Vérifier si la limite est dans le mois en cours
            val now = java.util.Calendar.getInstance()
            val limitStart = java.util.Calendar.getInstance().apply {
                time = limit.startDate
            }
            now.get(java.util.Calendar.YEAR) == limitStart.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.MONTH) == limitStart.get(java.util.Calendar.MONTH)
        }
    }
}

/**
 * Extension du SavingsRepository pour obtenir uniquement les projets actifs
 */
suspend fun SavingsRepository.getActiveProjects() = getAllProjectsList().filter { project ->
    project.isActive() && project.currentAmount < project.targetAmount
}
