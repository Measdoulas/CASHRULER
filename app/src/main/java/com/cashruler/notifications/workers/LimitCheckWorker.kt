package com.cashruler.notifications.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cashruler.data.repositories.SpendingLimitRepository
import com.cashruler.notifications.NotificationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LimitCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val spendingLimitRepository: SpendingLimitRepository,
    private val notificationService: NotificationService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val limitId = inputData.getLong("limitId", -1L)
        if (limitId == -1L) return Result.failure()

        return try {
            val limit = spendingLimitRepository.getLimitById(limitId)
            if (limit != null) {
                // Vérifier si la limite est dépassée ou proche du seuil
                val status = spendingLimitRepository.getLimitStatus(limit)
                if (status.requiresAttention()) {
                    notificationService.showLimitAlert(limit)
                }

                // Planifier la prochaine vérification
                if (spendingLimitRepository.isLimitActive(limit)) {
                    notificationService.scheduleLimitCheck(limitId)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

@HiltWorker
class SavingsReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val savingsRepository: com.cashruler.data.repositories.SavingsRepository,
    private val notificationService: NotificationService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val projectId = inputData.getLong("projectId", -1L)
        if (projectId == -1L) return Result.failure()

        return try {
            val project = savingsRepository.getProjectById(projectId).firstOrNull() // Utilise .firstOrNull() pour un Flow
            if (project != null) {
                // Vérifier si le projet est toujours actif et nécessite un rappel
                if (project.isActive && project.currentAmount < project.targetAmount) { // Utilise les champs directs
                    notificationService.showSavingsReminder(project)

                    // Replanifier le prochain rappel selon la fréquence du projet
                    project.savingFrequency?.let { intervalDays ->
                        if (intervalDays > 0) {
                            notificationService.scheduleSavingsReminder(projectId, intervalDays)
                        }
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
