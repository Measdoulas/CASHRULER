package com.cashruler.data.repositories

import com.cashruler.data.dao.ExpenseDao
import com.cashruler.data.models.Expense
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ExpenseRepositoryTest {

    private lateinit var expenseDao: ExpenseDao // Renommé pour clarté
    private lateinit var categoryDao: com.cashruler.data.dao.CategoryDao // Ajouté
    private lateinit var repository: ExpenseRepository
    private val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher() // Ajouté pour withContext

    @Before
    fun setup() {
        expenseDao = mockk()
        categoryDao = mockk(relaxed = true) // Relaxed mock pour CategoryDao car non central à ces tests
        repository = ExpenseRepository(expenseDao, categoryDao, testDispatcher) // Modifié
    }

    @Test
    fun `getAllExpenses returns expenses from dao`() = runTest {
        // Given
        val expenses = listOf(
            createExpense(1),
            createExpense(2)
        )
        coEvery { dao.getAllExpenses() } returns flowOf(expenses)

        // When
        val result = repository.getAllExpenses()

        // Then
        result.collect { list ->
            assertEquals(expenses, list)
        }
        coVerify { dao.getAllExpenses() }
    }

    @Test
    fun `getExpenseById returns expense from dao`() = runTest {
        // Given
        val expense = createExpense(1)
        coEvery { dao.getExpenseById(1) } returns flowOf(expense)

        // When
        val result = repository.getExpenseById(1)

        // Then
        result.collect { 
            assertNotNull(it)
            assertEquals(expense, it)
        }
        coVerify { dao.getExpenseById(1) }
    }

    @Test
    fun `addExpense calls dao insert`() = runTest {
        // Given
        val expense = createExpense(1)
        coEvery { dao.insert(expense) } returns 1

        // When
        repository.addExpense(expense)

        // Then
        coVerify { dao.insert(expense) }
    }

    @Test
    fun `updateExpense calls dao update`() = runTest {
        // Given
        val expense = createExpense(1)
        coEvery { dao.update(expense) } returns Unit

        // When
        repository.updateExpense(expense)

        // Then
        coVerify { dao.update(expense) }
    }

    @Test
    fun `deleteExpense calls dao delete`() = runTest {
        // Given
        val expense = createExpense(1)
        coEvery { dao.delete(expense) } returns Unit

        // When
        repository.deleteExpense(expense)

        // Then
        coVerify { dao.delete(expense) }
    }

    @Test
    fun `getExpensesByPeriod returns filtered expenses`() = runTest {
        // Given
        val start = Date(1000)
        val end = Date(2000)
        val expenses = listOf(
            createExpense(1, date = Date(1500)), // Should be included
            createExpense(2, date = Date(500)),  // Should be excluded
            createExpense(3, date = Date(2500))  // Should be excluded
        )
        coEvery { dao.getExpensesBetweenDates(start, end) } returns flowOf(expenses.filter { 
            it.date.time in start.time..end.time 
        })

        // When
        val result = repository.getExpensesByPeriod(start, end)

        // Then
        result.collect { list ->
            assertEquals(1, list.size)
            assertEquals(1L, list[0].id)
        }
        coVerify { dao.getExpensesBetweenDates(start, end) }
    }

    @Test
    fun `getExpensesByCategory returns filtered expenses`() = runTest {
        // Given
        val category = "Food"
        val expenses = listOf(
            createExpense(1, category = "Food"),
            createExpense(2, category = "Transport")
        )
        coEvery { dao.getExpensesByCategory(category) } returns flowOf(
            expenses.filter { it.category == category }
        )

        // When
        val result = repository.getExpensesByCategory(category)

        // Then
        result.collect { list ->
            assertEquals(1, list.size)
            assertEquals(category, list[0].category)
        }
        coVerify { dao.getExpensesByCategory(category) }
    }

    private fun createExpense(
        id: Long,
        amount: Double = 100.0,
        title: String = "Test Expense",
        category: String = "Food",
        date: Date = Date(),
        isRecurring: Boolean = false,
        recurringFrequency: Int? = null,
        nextGenerationDate: Date? = null
    ) = Expense(
        id = id,
        amount = amount,
        description = title, // Note: Le modèle Expense utilise 'description' et non 'title'
        category = category,
        date = date,
        isRecurring = isRecurring,
        recurringFrequency = recurringFrequency,
        nextGenerationDate = nextGenerationDate
    )

    // Nouveaux tests pour calculateNextGenerationDate
    @Test
    fun `calculateNextGenerationDate returns correct future date`() {
        val calendar = java.util.Calendar.getInstance()
        val baseDate = calendar.time
        val frequency = 7 // 7 jours

        calendar.add(java.util.Calendar.DAY_OF_YEAR, frequency)
        val expectedDate = calendar.time

        val result = repository.calculateNextGenerationDate(baseDate, frequency)
        assertEquals(expectedDate.time / 1000, result.time / 1000) // Compare en secondes pour éviter diff millisecondes
    }
    
    @Test
    fun `calculateNextGenerationDate handles month and year changes`() {
        val calendar = java.util.Calendar.getInstance()
        // Met la date au 28 Décembre 2023
        calendar.set(2023, java.util.Calendar.DECEMBER, 28)
        val baseDate = calendar.time
        val frequency = 5 // 5 jours

        // La date attendue est le 2 Janvier 2024
        val expectedCalendar = java.util.Calendar.getInstance()
        expectedCalendar.set(2024, java.util.Calendar.JANUARY, 2)
        val expectedDate = expectedCalendar.time
        
        val result = repository.calculateNextGenerationDate(baseDate, frequency)
        
        val resultCal = java.util.Calendar.getInstance().apply { time = result }
        val expectedCal = java.util.Calendar.getInstance().apply { time = expectedDate }

        assertEquals(expectedCal.get(java.util.Calendar.YEAR), resultCal.get(java.util.Calendar.YEAR))
        assertEquals(expectedCal.get(java.util.Calendar.MONTH), resultCal.get(java.util.Calendar.MONTH))
        assertEquals(expectedCal.get(java.util.Calendar.DAY_OF_MONTH), resultCal.get(java.util.Calendar.DAY_OF_MONTH))
    }

    // Nouveaux tests pour getDueRecurringExpenses
    @Test
    fun `getDueRecurringExpenses returns correct expenses from dao`() = runTest {
        val calendar = java.util.Calendar.getInstance()
        val currentDate = calendar.time
        
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -1) // Hier
        val pastDate = calendar.time
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 2) // Demain (basé sur hier + 2 = demain par rapport à aujourd'hui)
        val futureDate = calendar.time

        val dueExpense = createExpense(1, isRecurring = true, nextGenerationDate = pastDate)
        val dueTodayExpense = createExpense(2, isRecurring = true, nextGenerationDate = currentDate)
        val futureExpense = createExpense(3, isRecurring = true, nextGenerationDate = futureDate)
        val nonRecurringExpense = createExpense(4, isRecurring = false, nextGenerationDate = pastDate)
        val nullDateExpense = createExpense(5, isRecurring = true, nextGenerationDate = null)

        val expectedExpenses = listOf(dueExpense, dueTodayExpense)
        
        // Mock la réponse du DAO
        coEvery { expenseDao.getDueRecurringExpenses(currentDate) } returns expectedExpenses

        // Appelle la méthode du repository
        val result = repository.getDueRecurringExpenses(currentDate)

        // Vérifie le résultat
        assertEquals(expectedExpenses.size, result.size)
        assertEquals(expectedExpenses, result)
        coVerify { expenseDao.getDueRecurringExpenses(currentDate) }
    }

    @Test
    fun `getDueRecurringExpenses returns empty list when no expenses are due`() = runTest {
        val currentDate = Date()
        coEvery { expenseDao.getDueRecurringExpenses(currentDate) } returns emptyList()

        val result = repository.getDueRecurringExpenses(currentDate)

        assertTrue(result.isEmpty())
        coVerify { expenseDao.getDueRecurringExpenses(currentDate) }
    }
}
