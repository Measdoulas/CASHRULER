package com.cashruler.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.cashruler.R
import com.cashruler.data.models.SavingsProject
import com.cashruler.data.models.SpendingLimit
import com.cashruler.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

import com.cashruler.notifications.NotificationManager // Added import

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
    // Removed NotificationManagerCompat injection as it's not directly used for channel creation anymore
) {
    // Companion object and its constants removed
    // createNotificationChannels() method removed

    fun showLimitAlert(limit: SpendingLimit) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("screen", "limits") // Consider using Routes object if available
            putExtra("category", limit.category)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            limit.id.toInt(), // Ensure unique request code if multiple limits can notify
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationManager.ID_SPENDING_LIMITS) // Updated Channel ID
            .setSmallIcon(R.drawable.ic_notification_warning) // Ensure this drawable exists
            .setContentTitle(context.getString(R.string.notification_limit_exceeded_title))
            .setContentText(
                context.getString(
                    R.string.notification_limit_exceeded_text,
                    limit.category,
                    limit.getProgress().toInt() // Ensure getProgress() exists and returns Int or cast appropriately
                )
            )
            // .setGroup(NOTIFICATION_LIMITS_GROUP) // Grouping can be kept if desired
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Use system's NotificationManager to notify
        val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        systemNotificationManager.notify(limit.id.toInt(), notification)
    }

    fun showSavingsReminder(project: SavingsProject) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("screen", "savings") // Consider using Routes object
            putExtra("projectId", project.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            project.id.toInt(), // Ensure unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationManager.ID_SAVINGS_CONTRIBUTION_REMINDERS) // Updated Channel ID
            .setSmallIcon(R.drawable.ic_notification_savings) // Ensure this drawable exists
            .setContentTitle(context.getString(R.string.notification_savings_reminder_title))
            .setContentText(
                context.getString(
                    R.string.notification_savings_reminder_text,
                    project.title
                )
            )
            // .setGroup(NOTIFICATION_SAVINGS_GROUP) // Grouping can be kept
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        systemNotificationManager.notify(project.id.toInt(), notification)
    }

    fun scheduleLimitCheck(limitId: Long, intervalHours: Int = 24) {
        val checkLimitsWork = OneTimeWorkRequestBuilder<LimitCheckWorker>()
            .setInputData(workDataOf("limitId" to limitId))
            .setInitialDelay(intervalHours.toLong(), TimeUnit.HOURS)
            .build()

        workManager.enqueueUniqueWork(
            "limit_check_$limitId",
            ExistingWorkPolicy.REPLACE,
            checkLimitsWork
        )
    }

    fun scheduleSavingsReminder(projectId: Long, intervalDays: Int) {
        val savingsReminderWork = PeriodicWorkRequestBuilder<SavingsReminderWorker>(
            intervalDays.toLong(), TimeUnit.DAYS
        )
            .setInputData(workDataOf("projectId" to projectId))
            .build()

        workManager.enqueueUniquePeriodicWork(
            "savings_reminder_$projectId",
            ExistingPeriodicWorkPolicy.REPLACE,
            savingsReminderWork
        )
    }

    fun cancelLimitCheck(limitId: Long) {
        workManager.cancelUniqueWork("limit_check_$limitId")
    }

    fun cancelSavingsReminder(projectId: Long) {
        workManager.cancelUniqueWork("savings_reminder_$projectId")
    }

    fun scheduleExpenseReminders() {
        ExpenseReminderWorker.schedule(context)
    }

    fun cancelExpenseReminders() {
        ExpenseReminderWorker.cancel(context)
    }
}
