package com.cashruler.notifications.workers // Correct package based on existing workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cashruler.data.models.Expense
import com.cashruler.data.repositories.ExpenseRepository
import com.cashruler.data.repositories.SpendingLimitRepository // Injecte si la nouvelle dépense doit impacter les limites
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

@HiltWorker
class GenerateRecurringExpensesWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val expenseRepository: ExpenseRepository,
    private val spendingLimitRepository: SpendingLimitRepository // Optionnel, mais recommandé
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Worker started.")
        try {
            val today = Calendar.getInstance()
            // Réinitialise l'heure pour comparer uniquement les dates
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)
            val todayDate = today.time

            // Récupère toutes les dépenses récurrentes dont la nextGenerationDate est <= aujourd'hui
            val dueExpensesModels = expenseRepository.getDueRecurringExpenses(todayDate)
            Log.d(TAG, "Found ${dueExpensesModels.size} recurring expense models due.")

            for (model in dueExpensesModels) {
                if (model.recurringFrequency == null || model.recurringFrequency <= 0) {
                    Log.w(TAG, "Skipping model ${model.id} due to invalid recurringFrequency.")
                    continue
                }

                var nextGenDateForThisModel = model.nextGenerationDate ?: model.date // Date de la prochaine génération prévue

                // Boucle de rattrapage : génère les instances manquées
                while (nextGenDateForThisModel.before(todayDate) || nextGenDateForThisModel.equals(todayDate)) {
                    Log.d(TAG, "Generating instance for model ${model.id} for date $nextGenDateForThisModel")
                    
                    // Crée la nouvelle dépense générée
                    val newInstance = Expense(
                        // Ne pas copier l'ID, il sera auto-généré
                        amount = model.amount,
                        description = model.description,
                        category = model.category,
                        date = nextGenDateForThisModel, // Date de cette occurrence
                        isRecurring = false,           // L'instance générée n'est pas récurrente elle-même
                        recurringFrequency = null,
                        nextGenerationDate = null,     // Pas de prochaine génération pour l'instance
                        createdAt = Date(),            // Date de création de cette instance
                        updatedAt = Date(),
                        notes = model.notes,
                        attachmentUri = model.attachmentUri,
                        spendingLimitId = model.spendingLimitId // Reporter l'ID de la limite
                    )
                    
                    val newExpenseId = expenseRepository.addExpense(newInstance)
                    Log.d(TAG, "Generated new expense with ID $newExpenseId for model ${model.id}")

                    // Mettre à jour le spendingLimit si associé
                    newInstance.spendingLimitId?.let { limitId ->
                        spendingLimitRepository.addToSpentAmount(limitId, newInstance.amount)
                        Log.d(TAG, "Updated spending limit $limitId for new expense $newExpenseId")
                    }

                    // Prépare la prochaine date de génération pour CE MODÈLE
                    nextGenDateForThisModel = expenseRepository.calculateNextGenerationDate(
                        baseDate = nextGenDateForThisModel,
                        recurringFrequency = model.recurringFrequency!! // Non-nul et >0 vérifié au début du for
                    )
                }

                // Met à jour la dépense modèle avec sa nouvelle nextGenerationDate
                val updatedModel = model.copy(nextGenerationDate = nextGenDateForThisModel)
                expenseRepository.updateExpense(updatedModel)
                Log.d(TAG, "Updated model ${model.id} with new nextGenerationDate: $nextGenDateForThisModel")
            }

            Log.d(TAG, "Worker finished successfully.")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in GenerateRecurringExpensesWorker", e)
            return Result.retry() // Important pour que WorkManager réessaie en cas d'erreur
        }
    }

    companion object {
        private const val TAG = "GenerateRecurringExpensesWorker"
        const val WORK_NAME = "generate_recurring_expenses_worker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Pas besoin de réseau
                .setRequiresCharging(false) // Peut s'exécuter même si pas en charge
                .build()

            // Exécute une fois par jour
            val requestBuilder = PeriodicWorkRequestBuilder<GenerateRecurringExpensesWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.HOURS) // Délai initial pour éviter surcharge au démarrage de l'app
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // KEEP: si un travail est déjà planifié, ne rien faire.
                                                 // REPLACE: remplacerait le travail existant.
                requestBuilder.build()
            )
            Log.d(TAG, "Work scheduled.")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Work cancelled.")
        }
    }
}
