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

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) {
    companion object {
        private const val CHANNEL_LIMITS_ID = "spending_limits"
        private const val CHANNEL_SAVINGS_ID = "savings_reminders"
        private const val NOTIFICATION_LIMITS_GROUP = "group_limits"
        private const val NOTIFICATION_SAVINGS_GROUP = "group_savings"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val limitsChannel = NotificationChannel(
                CHANNEL_LIMITS_ID,
                context.getString(R.string.notification_channel_limits_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_limits_description)
            }

            val savingsChannel = NotificationChannel(
                CHANNEL_SAVINGS_ID,
                context.getString(R.string.notification_channel_savings_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_savings_description)
            }

            NotificationManagerCompat.from(context).apply {
                createNotificationChannels(listOf(limitsChannel, savingsChannel))
            }
        }
    }

    fun showLimitAlert(limit: SpendingLimit) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("screen", "limits")
            putExtra("category", limit.category)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            limit.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_LIMITS_ID)
            .setSmallIcon(R.drawable.ic_notification_warning)
            .setContentTitle(context.getString(R.string.notification_limit_exceeded_title))
            .setContentText(
                context.getString(
                    R.string.notification_limit_exceeded_text,
                    limit.category,
                    limit.getProgress().toInt()
                )
            )
            .setGroup(NOTIFICATION_LIMITS_GROUP)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(limit.id.toInt(), notification)
    }

    fun showSavingsReminder(project: SavingsProject) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("screen", "savings")
            putExtra("projectId", project.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            project.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SAVINGS_ID)
            .setSmallIcon(R.drawable.ic_notification_savings)
            .setContentTitle(context.getString(R.string.notification_savings_reminder_title))
            .setContentText(
                context.getString(
                    R.string.notification_savings_reminder_text,
                    project.title
                )
            )
            .setGroup(NOTIFICATION_SAVINGS_GROUP)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(project.id.toInt(), notification)
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
}
