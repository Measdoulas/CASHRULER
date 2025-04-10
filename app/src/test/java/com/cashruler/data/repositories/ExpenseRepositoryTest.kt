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

    private lateinit var dao: ExpenseDao
    private lateinit var repository: ExpenseRepository

    @Before
    fun setup() {
        dao = mockk()
        repository = ExpenseRepository(dao)
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
        date: Date = Date()
    ) = Expense(
        id = id,
        amount = amount,
        title = title,
        category = category,
        date = date
    )
}
