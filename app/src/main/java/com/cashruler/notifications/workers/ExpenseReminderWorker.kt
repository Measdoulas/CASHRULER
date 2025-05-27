package com.cashruler.notifications.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cashruler.data.models.ExpenseReminder
import com.cashruler.data.repositories.ExpenseReminderRepositoryInterface
import com.cashruler.notifications.NotificationManager // Custom NotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date
import java.util.concurrent.TimeUnit

@HiltWorker
class ExpenseReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val expenseReminderRepository: ExpenseReminderRepositoryInterface,
    private val notificationManager: NotificationManager // Custom NotificationManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val dueReminders = expenseReminderRepository.getActiveRemindersDue(Date())

            dueReminders.forEach { reminder ->
                notificationManager.showExpenseReminder(reminder)

                if (!reminder.isRecurring) {
                    expenseReminderRepository.setReminderActive(reminder.id, false)
                }
                // For recurring reminders, the next reminder date would need to be calculated
                // and the reminder updated in the DB. This is not part of the current subtask.
            }
            Result.success()
        } catch (e: Exception) {
            // Log error or handle retry logic
            Result.failure()
        }
    }

    companion object {
        private const val WORKER_TAG = "expense_reminder_worker"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ExpenseReminderWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder()
                    // .setRequiresCharging(true) // Example constraint
                    .build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORKER_TAG,
                ExistingPeriodicWorkPolicy.KEEP, // Or REPLACE if new schedule should override old
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORKER_TAG)
        }
    }
}
