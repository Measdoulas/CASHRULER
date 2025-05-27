package com.cashruler.notifications.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cashruler.data.repositories.SpendingLimitRepositoryInterface
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyLimitResetWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val spendingLimitRepository: SpendingLimitRepositoryInterface
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "DailyLimitResetWorker started.")
            spendingLimitRepository.resetExpiredPeriods() // Uses default arguments for currentDate and newPeriodStart
            Log.d(TAG, "DailyLimitResetWorker completed successfully: Expired periods reset.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "DailyLimitResetWorker failed: ${e.message}", e)
            Result.failure() // Or Result.retry() if appropriate
        }
    }

    companion object {
        private const val TAG = "DailyLimitResetWorker"
        private const val WORKER_NAME = "daily_limit_reset_worker"

        private fun calculateInitialDelay(): Long {
            val currentTime = Calendar.getInstance()
            val dueTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 2) // 2 AM
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (currentTime.after(dueTime)) {
                dueTime.add(Calendar.DAY_OF_MONTH, 1) // If past 2 AM today, schedule for 2 AM tomorrow
            }
            return dueTime.timeInMillis - currentTime.timeInMillis
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyLimitResetWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().build()) // No specific constraints for now
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORKER_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "DailyLimitResetWorker scheduled to run with initial delay.")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORKER_NAME)
            Log.d(TAG, "DailyLimitResetWorker cancelled.")
        }
    }
}
