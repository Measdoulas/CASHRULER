package com.cashruler.notifications.workers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.cashruler.data.models.Expense
import com.cashruler.data.repositories.ExpenseRepository
import com.cashruler.data.repositories.SpendingLimitRepository
import io.mockk.*
import kotlinx.coroutines.runBlocking // Utilise runBlocking pour les tests de Worker synchrones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

// Utilise RobolectricTestRunner si le contexte est nécessaire de manière plus complexe
// ou si des composants Android sont indirectement utilisés par les classes testées.
// Pour ce worker, si les dépendances sont bien mockées, un simple test JUnit pourrait suffire
// mais TestListenableWorkerBuilder fonctionne bien avec un contexte d'application.
@RunWith(RobolectricTestRunner::class)
class GenerateRecurringExpensesWorkerTest {

    private lateinit var context: Context
    private val expenseRepository: ExpenseRepository = mockk(relaxed = true)
    private val spendingLimitRepository: SpendingLimitRepository = mockk(relaxed = true)
    private lateinit var executor: Executor


    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor() // Pour TestListenableWorkerBuilder

        // Assure-toi que les mocks sont reset avant chaque test si nécessaire
        clearAllMocks() // ou clearMocks(expenseRepository, spendingLimitRepository)
        
        // Mock par défaut pour les appels qui ne sont pas le focus principal du test
        // ou qui doivent retourner une valeur pour éviter NPEs.
        coEvery { expenseRepository.addExpense(any()) } returns 1L // Simule un ID de retour
        coEvery { spendingLimitRepository.addToSpentAmount(any(), any()) } just Runs
        coEvery { expenseRepository.updateExpense(any()) } just Runs
    }

    private fun createWorker(): GenerateRecurringExpensesWorker {
        return TestListenableWorkerBuilder<GenerateRecurringExpensesWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return GenerateRecurringExpensesWorker(
                        appContext,
                        workerParameters,
                        expenseRepository,
                        spendingLimitRepository
                    )
                }
            })
            .build()
    }
    
    private fun createTestExpenseModel(
        id: Long,
        baseDate: Date,
        frequency: Int,
        isDue: Boolean, // True si nextGenerationDate doit être dans le passé/aujourd'hui
        spendingLimitId: Long? = null
    ): Expense {
        val calendar = Calendar.getInstance()
        calendar.time = baseDate // Utilise la date de base pour la dépense modèle elle-même
        
        val nextGenCalendar = Calendar.getInstance()
        nextGenCalendar.time = baseDate // Commence par la date de base pour calculer nextGenerationDate
        if (isDue) {
            // Si c'est "due", sa nextGenerationDate est dans le passé ou aujourd'hui
            // Par exemple, si la fréquence est de 7 jours, et elle était due il y a 2 jours
            // nextGenCalendar.add(Calendar.DAY_OF_YEAR, - (frequency - 5) ) // exemple
            nextGenCalendar.add(Calendar.DAY_OF_YEAR, -2) // Due il y a 2 jours
        } else {
            // Si ce n'est pas "due" pour ce test, sa nextGenerationDate est dans le futur
            nextGenCalendar.add(Calendar.DAY_OF_YEAR, frequency + 5)
        }

        return Expense(
            id = id,
            amount = 100.0,
            description = "Recurring Expense $id",
            category = "Test",
            date = baseDate, // Date de création/début du modèle
            isRecurring = true,
            recurringFrequency = frequency,
            nextGenerationDate = nextGenCalendar.time,
            spendingLimitId = spendingLimitId
        )
    }


    @Test
    fun `doWork nominal case - one due expense`() = runBlocking {
        val baseDateForModel = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.time // Semaine dernière
        val frequency = 7
        val dueExpenseModel = createTestExpenseModel(1L, baseDateForModel, frequency, isDue = true, spendingLimitId = 10L)

        val expectedNextGenDateAfterInstance = Calendar.getInstance().apply {
            time = dueExpenseModel.nextGenerationDate!! // La date à laquelle l'instance est générée
            add(Calendar.DAY_OF_YEAR, frequency)
        }.time
        
        coEvery { expenseRepository.getDueRecurringExpenses(any()) } returns listOf(dueExpenseModel)
        coEvery { expenseRepository.calculateNextGenerationDate(dueExpenseModel.nextGenerationDate!!, frequency) } returns expectedNextGenDateAfterInstance
        
        val capturedNewExpense = slot<Expense>()
        val capturedUpdatedModel = slot<Expense>()
        coEvery { expenseRepository.addExpense(capture(capturedNewExpense)) } returns 101L // new ID for generated instance

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { expenseRepository.addExpense(any()) }
        assertTrue(capturedNewExpense.isCaptured)
        assertEquals(dueExpenseModel.amount, capturedNewExpense.captured.amount, 0.01)
        assertEquals(dueExpenseModel.description, capturedNewExpense.captured.description)
        assertEquals(dueExpenseModel.nextGenerationDate, capturedNewExpense.captured.date) // Date de l'instance = nextGenDate du modèle
        assertFalse(capturedNewExpense.captured.isRecurring) // L'instance générée n'est pas récurrente
        assertNull(capturedNewExpense.captured.nextGenerationDate)
        
        coVerify(exactly = 1) { spendingLimitRepository.addToSpentAmount(10L, dueExpenseModel.amount) }
        
        coVerify(exactly = 1) { expenseRepository.updateExpense(capture(capturedUpdatedModel)) }
        assertTrue(capturedUpdatedModel.isCaptured)
        assertEquals(dueExpenseModel.id, capturedUpdatedModel.captured.id)
        assertEquals(expectedNextGenDateAfterInstance.time / 1000, capturedUpdatedModel.captured.nextGenerationDate!!.time / 1000)
    }

    @Test
    fun `doWork backfilling - expense 2 periods late`() = runBlocking {
        val frequency = 7
        val today = Calendar.getInstance()
        
        val modelNextGenDate = Calendar.getInstance().apply {
            time = today.time
            add(Calendar.DAY_OF_YEAR, -(2 * frequency) + 2) // Due il y a 2 périodes (moins 2 jours pour être sûr qu'elle est dans le passé)
        }.time
        
        val modelStartDate = Calendar.getInstance().apply { // La date de début originale du modèle
            time = modelNextGenDate
            add(Calendar.DAY_OF_YEAR, -frequency) // Suppose que la date du modèle est une période avant sa 1ère nextGenDate
        }.time

        val lateExpenseModel = createTestExpenseModel(1L, modelStartDate, frequency, isDue = true)
        // Surcharge nextGenerationDate pour le test de backfilling
        val correctedLateExpenseModel = lateExpenseModel.copy(nextGenerationDate = modelNextGenDate)

        val calculatedDates = mutableListOf<Date>()
        var tempDate = modelNextGenDate
        repeat(2) { // Devrait générer 2 instances
            calculatedDates.add(tempDate)
            tempDate = Calendar.getInstance().apply { time = tempDate; add(Calendar.DAY_OF_YEAR, frequency) }.time
        }
        // La date finale après les 2 générations pour la mise à jour du modèle
        val finalNextGenDateForModel = tempDate 

        coEvery { expenseRepository.getDueRecurringExpenses(any()) } returns listOf(correctedLateExpenseModel)
        // Simule les appels successifs à calculateNextGenerationDate
        val dateSlot = slot<Date>()
        coEvery { expenseRepository.calculateNextGenerationDate(capture(dateSlot), frequency) } answers {
            Calendar.getInstance().apply { time = dateSlot.captured; add(Calendar.DAY_OF_YEAR, frequency) }.time
        }
        
        val capturedNewExpenses = mutableListOf<Expense>()
        coEvery { expenseRepository.addExpense(capture(capturedNewExpenses)) } returnsMany listOf(101L, 102L)

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 2) { expenseRepository.addExpense(any()) }
        assertEquals(2, capturedNewExpenses.size)
        // Vérifie les dates des dépenses générées
        assertEquals(calculatedDates[0].time / 1000, capturedNewExpenses[0].date.time / 1000)
        assertEquals(calculatedDates[1].time / 1000, capturedNewExpenses[1].date.time / 1000)

        val capturedUpdatedModel = slot<Expense>()
        coVerify(exactly = 1) { expenseRepository.updateExpense(capture(capturedUpdatedModel)) }
        assertEquals(finalNextGenDateForModel.time / 1000, capturedUpdatedModel.captured.nextGenerationDate!!.time / 1000)
    }

    @Test
    fun `doWork no due expenses`() = runBlocking {
        coEvery { expenseRepository.getDueRecurringExpenses(any()) } returns emptyList()

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { expenseRepository.addExpense(any()) }
        coVerify(exactly = 0) { expenseRepository.updateExpense(any()) }
    }

    @Test
    fun `doWork handles repository error during addExpense`() = runBlocking {
        val baseDateForModel = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.time
        val dueExpenseModel = createTestExpenseModel(1L, baseDateForModel, 7, isDue = true)
        
        coEvery { expenseRepository.getDueRecurringExpenses(any()) } returns listOf(dueExpenseModel)
        coEvery { expenseRepository.addExpense(any()) } throws RuntimeException("DB error")

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `doWork skips model with invalid recurringFrequency`() = runBlocking {
        val model1 = createTestExpenseModel(1L, Date(), 7, isDue = true) // Valide
        val model2 = createTestExpenseModel(2L, Date(), 0, isDue = true) // Invalide (freq=0)
        val model3 = createTestExpenseModel(3L, Date(), -1, isDue = true) // Invalide (freq<0)
        
        coEvery { expenseRepository.getDueRecurringExpenses(any()) } returns listOf(model1, model2, model3)
        coEvery { expenseRepository.calculateNextGenerationDate(any(), 7) } answers { 
            Calendar.getInstance().apply { time = arg(0); add(Calendar.DAY_OF_YEAR, 7) }.time 
        }


        val worker = createWorker()
        worker.doWork()

        // addExpense et updateExpense ne devraient être appelés que pour model1
        coVerify(exactly = 1) { expenseRepository.addExpense(coWithArg {assertEquals(1L, it.id) }) } // Vérifie que c'est bien model1 (via son ID)
        coVerify(exactly = 1) { expenseRepository.updateExpense(coWithArg {assertEquals(1L, it.id) }) }
    }
}
