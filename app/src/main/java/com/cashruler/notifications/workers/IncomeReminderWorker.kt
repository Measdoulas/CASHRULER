package com.cashruler.notifications.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cashruler.data.repositories.IncomeRepository
import com.cashruler.notifications.NotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.coroutineScope

@HiltWorker
class IncomeReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val incomeRepository: IncomeRepository,
    private val notificationManager: NotificationManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        try {
            // Récupère les revenus récurrents
            val upcomingIncomes = incomeRepository.getUpcomingRecurringIncomes().first()

            // Traite chaque revenu récurrent
            upcomingIncomes.forEach { income ->
                val nextOccurrence = income.nextOccurrence
                if (nextOccurrence != null) {
                    val today = Calendar.getInstance()
                    val nextDate = Calendar.getInstance().apply { time = nextOccurrence }

                    // Calcule le nombre de jours jusqu'à la prochaine occurrence
                    val daysUntilNext = ((nextDate.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

                    // Envoie une notification si la prochaine occurrence est dans moins de 3 jours
                    if (daysUntilNext in 0..3) {
                        val message = when (daysUntilNext) {
                            0 -> "Revenu attendu aujourd'hui : ${income.description} (${income.amount} FCFA)"
                            1 -> "Revenu attendu demain : ${income.description} (${income.amount} FCFA)"
                            else -> "Revenu à venir dans $daysUntilNext jours : ${income.description} (${income.amount} FCFA)"
                        }

                        notificationManager.showIncomeReminder(
                            income.id.toInt(),
                            income.description,
                            message,
                            income.amount
                        )
                    }

                    // Si la date est passée, calcule la prochaine occurrence
                    if (daysUntilNext <= 0) {
                        val updatedIncome = income.copy(
                            nextOccurrence = incomeRepository.calculateNextOccurrence(income)
                        )
                        incomeRepository.updateIncome(updatedIncome)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("IncomeReminderWorker", "Erreur lors de la vérification des revenus récurrents", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "income_reminder_worker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val requestBuilder = PeriodicWorkRequestBuilder<IncomeReminderWorker>(
                1, TimeUnit.DAYS
            )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    requestBuilder.build()
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
