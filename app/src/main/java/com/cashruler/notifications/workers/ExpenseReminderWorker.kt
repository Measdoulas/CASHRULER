package com.cashruler.notifications.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cashruler.data.models.Expense
import com.cashruler.data.repositories.ExpenseRepository
import com.cashruler.notifications.NotificationManager // Assure-toi que c'est le bon NotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

@HiltWorker
class ExpenseReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val expenseRepository: ExpenseRepository,
    private val notificationManager: NotificationManager // Le tien, pas android.app.NotificationManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            Log.d("ExpenseReminderWorker", "Worker started")
            val today = Calendar.getInstance()
            // Crée une date pour "dans 3 jours" pour récupérer les dépenses pertinentes
            val reminderWindowEndDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 3)
            }.time

            val upcomingExpenses = expenseRepository.getUpcomingRecurringExpensesForReminderList(reminderWindowEndDate)
            Log.d("ExpenseReminderWorker", "Found ${upcomingExpenses.size} expenses for reminder.")

            upcomingExpenses.forEach { expense ->
                expense.nextReminderDate?.let { nextDate ->
                    val daysUntilNext = ((nextDate.time - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

                    if (daysUntilNext in 0..3) { // Rappel si c'est aujourd'hui ou dans les 3 prochains jours
                        val message = when (daysUntilNext) {
                            0 -> "Dépense récurrente prévue aujourd'hui : ${expense.description}"
                            1 -> "Dépense récurrente prévue demain : ${expense.description}"
                            else -> "Dépense récurrente prévue dans $daysUntilNext jours : ${expense.description}"
                        }
                        // Tu auras besoin d'une nouvelle méthode dans NotificationManager
                        notificationManager.showExpenseReminder(
                            expenseId = expense.id.toInt(),
                            title = "Rappel de Dépense",
                            message = message,
                            amount = expense.amount
                        )
                        Log.d("ExpenseReminderWorker", "Notification sent for expense: ${expense.id}")

                        // Recalcule et met à jour pour le prochain rappel
                        val nextReminder = expenseRepository.calculateNextReminderDate(
                            expense.copy(date = nextDate) // Important: base le calcul sur la date de rappel actuelle
                        )
                        val updatedExpense = expense.copy(nextReminderDate = nextReminder)
                        expenseRepository.updateExpense(updatedExpense)
                        Log.d("ExpenseReminderWorker", "Updated next reminder date for expense: ${expense.id} to $nextReminder")
                    }
                }
            }
            Log.d("ExpenseReminderWorker", "Worker finished successfully")
            return Result.success()
        } catch (e: Exception) {
            Log.e("ExpenseReminderWorker", "Error in ExpenseReminderWorker", e)
            return Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "expense_reminder_worker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val requestBuilder = PeriodicWorkRequestBuilder<ExpenseReminderWorker>(1, TimeUnit.DAYS) // Exécute une fois par jour
                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.MINUTES) // Délai initial pour éviter surcharge au démarrage
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Ou REPLACE si tu veux forcer la replanification
                requestBuilder.build()
            )
            Log.d("ExpenseReminderWorker", "Work scheduled")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d("ExpenseReminderWorker", "Work cancelled")
        }
    }
}
