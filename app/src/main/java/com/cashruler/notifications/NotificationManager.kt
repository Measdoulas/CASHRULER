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
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val numberFormat = NumberFormat.getNumberInstance(Locale.FRANCE)

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Canal pour les limites de dépenses
            val spendingLimitChannel = NotificationChannel(
                CHANNEL_SPENDING_LIMIT,
                "Limites de dépenses",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications concernant les limites de dépenses"
                enableVibration(true)
            }

            // Canal pour les rappels de revenus
            val incomeReminderChannel = NotificationChannel(
                CHANNEL_INCOME_REMINDER,
                "Rappels de revenus",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Rappels pour les revenus récurrents"
                enableVibration(true)
            }

            // Canal pour les objectifs d'épargne
            val savingsGoalChannel = NotificationChannel(
                CHANNEL_SAVINGS_GOAL,
                "Objectifs d'épargne",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications concernant les objectifs d'épargne"
                enableVibration(true)
            }

            // Canal pour les rappels de dépenses (supprimé)
            // val expenseReminderChannel = NotificationChannel(...)

            notificationManager.createNotificationChannels(
                listOf(spendingLimitChannel, incomeReminderChannel, savingsGoalChannel) // expenseReminderChannel supprimé
            )
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
            putExtra("category", category)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progress = ((currentAmount / limitAmount) * 100).toInt()
        val notification = NotificationCompat.Builder(context, CHANNEL_SPENDING_LIMIT)
            .setSmallIcon(R.drawable.ic_warning)
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

        notificationManager.notify(notificationId, notification)
    }

    fun showSavingsGoalAchieved(
        notificationId: Int, // Peut être basé sur projectId.toInt()
        projectTitle: String,
        targetAmount: Double
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("screen", "savings_project_details") // Route pour naviguer vers les détails du projet
            putExtra("projectId", notificationId.toLong()) // Utilise notificationId comme projectId
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId, // Utilise un ID unique pour la notification
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = "Félicitations ! Vous avez atteint votre objectif de ${numberFormat.format(targetAmount)} FCFA pour '$projectTitle'."

        val notification = NotificationCompat.Builder(context, CHANNEL_SAVINGS_GOAL) // Utilise le canal existant
            .setSmallIcon(R.drawable.ic_savings_achieved) // Tu devras ajouter une icône appropriée ic_savings_achieved.xml
            .setContentTitle("Objectif d'épargne atteint !")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setColor(ContextCompat.getColor(context, R.color.success)) // Utilise une couleur de succès
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showIncomeReminder(
        notificationId: Int,
        title: String,
        message: String,
        amount: Double
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("showIncome", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_INCOME_REMINDER)
            .setSmallIcon(R.drawable.ic_income)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showSavingsGoalProgress(
        notificationId: Int,
        title: String,
        message: String,
        currentAmount: Double,
        targetAmount: Double
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("showSavings", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progress = ((currentAmount / targetAmount) * 100).toInt()
        val notification = NotificationCompat.Builder(context, CHANNEL_SAVINGS_GOAL)
            .setSmallIcon(R.drawable.ic_savings)
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

        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_SPENDING_LIMIT = "spending_limit"
        const val CHANNEL_INCOME_REMINDER = "income_reminder"
        const val CHANNEL_SAVINGS_GOAL = "savings_goal"
        // const val CHANNEL_EXPENSE_REMINDER = "expense_reminder" // Supprimé
    }

    // La méthode showExpenseReminder(...) est supprimée.
}
