package com.cashruler.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.cashruler.R
import com.cashruler.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as SystemNotificationManager
    private val numberFormat = NumberFormat.getNumberInstance(Locale.FRANCE)

    companion object {
        const val ID_SPENDING_LIMITS = "spending_limits_channel"
        const val ID_SAVINGS_GOALS_ACHIEVED = "savings_goals_achieved_channel"
        const val ID_SAVINGS_CONTRIBUTION_REMINDERS = "savings_contribution_reminders_channel"
        const val ID_INCOME_REMINDERS = "income_reminders_channel"
        const val ID_EXPENSE_REMINDERS = "expense_reminders_channel"
        const val ID_BACKUP_STATUS = "backup_status_channel"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = mutableListOf<NotificationChannel>()

            channels.add(NotificationChannel(
                ID_SPENDING_LIMITS,
                "Alertes de Limites de Dépenses",
                SystemNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications pour les limites de dépenses approchant ou dépassées."
                enableVibration(true) // Keep vibration if desired
            })

            channels.add(NotificationChannel(
                ID_SAVINGS_GOALS_ACHIEVED,
                "Objectifs d'Épargne Atteints",
                SystemNotificationManager.IMPORTANCE_HIGH 
            ).apply {
                description = "Notifications lorsque vous atteignez un objectif d'épargne."
                enableVibration(true)
            })
            
            channels.add(NotificationChannel(
                ID_SAVINGS_CONTRIBUTION_REMINDERS,
                "Rappels de Contribution d'Épargne",
                SystemNotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Rappels pour effectuer des contributions régulières à vos projets d'épargne."
                enableVibration(true)
            })

            channels.add(NotificationChannel(
                ID_INCOME_REMINDERS,
                "Rappels de Revenus",
                SystemNotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications pour les revenus récurrents attendus."
                enableVibration(true)
            })

            channels.add(NotificationChannel(
                ID_EXPENSE_REMINDERS,
                "Rappels de Dépenses",
                SystemNotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications pour les dépenses manuelles récurrentes ou ponctuelles."
                enableVibration(true)
            })

            channels.add(NotificationChannel(
                ID_BACKUP_STATUS,
                "État de la Sauvegarde",
                SystemNotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications concernant l'état des sauvegardes de données."
                // No vibration for low importance by default, but can be enabled if needed
            })
            
            systemNotificationManager.createNotificationChannels(channels)
        }
    }

    fun showSpendingLimitWarning(
        notificationId: Int,
        category: String,
        message: String,
        currentAmount: Double,
        limitAmount: Double
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("category", category) // Consider navigating to specific limit/category screen
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId, // Ensure this ID is unique per notification instance
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progress = ((currentAmount / limitAmount) * 100).toInt()
        val notification = NotificationCompat.Builder(context, ID_SPENDING_LIMITS) // Updated Channel ID
            .setSmallIcon(R.drawable.ic_warning) // Ensure this drawable exists
            .setContentTitle("Alerte limite de dépenses")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "$message\n" +
                "Dépensé: ${numberFormat.format(currentAmount)} FCFA\n" +
                "Limite: ${numberFormat.format(limitAmount)} FCFA"
            ))
            .setProgress(100, progress, false)
            .setColor(ContextCompat.getColor(context, R.color.warning))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        systemNotificationManager.notify(notificationId, notification) // Use systemNotificationManager
    }

    fun showSavingsGoalAchieved(
        notificationId: Int, 
        projectTitle: String,
        targetAmount: Double
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Consider specific navigation route if available, e.g., to the project details
            putExtra("screen", "savings_project_details") 
            putExtra("projectId", notificationId.toLong()) 
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId, 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = "Félicitations ! Vous avez atteint votre objectif de ${numberFormat.format(targetAmount)} FCFA pour '$projectTitle'."

        val notification = NotificationCompat.Builder(context, ID_SAVINGS_GOALS_ACHIEVED) // Updated Channel ID
            .setSmallIcon(R.drawable.ic_savings_achieved) // Ensure this drawable exists
            .setContentTitle("Objectif d'épargne atteint !")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setColor(ContextCompat.getColor(context, R.color.success)) // Utilise une couleur de succès
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        systemNotificationManager.notify(notificationId, notification) // Use systemNotificationManager
    }

    // This method seems to be for general savings progress, might use ID_SAVINGS_CONTRIBUTION_REMINDERS
    // or if it's specifically for goal progress, a new channel could be considered.
    // For now, let's map it to ID_SAVINGS_CONTRIBUTION_REMINDERS as it's a form of reminder/update.
    fun showSavingsGoalProgress( 
        notificationId: Int,
        title: String,
        message: String,
        currentAmount: Double,
        targetAmount: Double
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("showSavings", true) // General navigation, consider more specific
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progress = ((currentAmount / targetAmount) * 100).toInt()
        val notification = NotificationCompat.Builder(context, ID_SAVINGS_CONTRIBUTION_REMINDERS) // Updated Channel ID
            .setSmallIcon(R.drawable.ic_savings) // Ensure this drawable exists
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "$message\n" +
                "Épargné: ${numberFormat.format(currentAmount)} FCFA\n" +
                "Objectif: ${numberFormat.format(targetAmount)} FCFA"
            ))
            .setProgress(100, progress, false)
            .setColor(ContextCompat.getColor(context, R.color.success))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        systemNotificationManager.notify(notificationId, notification) // Use systemNotificationManager
    }

    fun showIncomeReminder(
        notificationId: Int,
        title: String, // e.g., "Rappel de Revenu"
        message: String, // e.g., "Votre salaire mensuel de X FCFA est attendu."
        amount: Double // Could be part of message or used for structured data
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("showIncome", true) // General navigation
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ID_INCOME_REMINDERS) // Updated Channel ID
            .setSmallIcon(R.drawable.ic_income) // Ensure this drawable exists
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        systemNotificationManager.notify(notificationId, notification) // Use systemNotificationManager
    }

    fun showExpenseReminder(reminder: com.cashruler.data.models.ExpenseReminder) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Consider specific navigation route
            // putExtra("screen", Routes.EXPENSE_REMINDER_DETAILS) 
            // putExtra("reminderId", reminder.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(), 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        var contentText = reminder.description
        reminder.amount?.let {
            contentText += " - Montant: ${numberFormat.format(it)} FCFA"
        }

        val notification = NotificationCompat.Builder(context, ID_EXPENSE_REMINDERS) // Updated Channel ID
            .setSmallIcon(R.drawable.ic_calendar_clock) // Ensure this drawable exists
            .setContentTitle("Rappel de Dépense")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setColor(ContextCompat.getColor(context, R.color.primary)) 
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        systemNotificationManager.notify(reminder.id.toInt(), notification) // Use systemNotificationManager
    }
    
    // Placeholder for Backup Status Notification if needed by a worker
    fun showBackupStatusNotification(success: Boolean, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // putExtra("screen", Routes.BACKUP) // If navigation to backup screen is desired
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            ID_BACKUP_STATUS.hashCode(), // Unique ID for this type of notification
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (success) "Sauvegarde Réussie" else "Échec de la Sauvegarde"
        val smallIcon = if (success) R.drawable.ic_backup_success else R.drawable.ic_backup_failed // Ensure these drawables exist

        val notification = NotificationCompat.Builder(context, ID_BACKUP_STATUS)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(if (success) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        systemNotificationManager.notify(ID_BACKUP_STATUS.hashCode(), notification)
    }
}
