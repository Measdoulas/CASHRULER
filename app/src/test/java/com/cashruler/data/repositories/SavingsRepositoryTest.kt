package com.cashruler.data.repositories

import com.cashruler.data.dao.SavingsDao
import com.cashruler.data.models.SavingsProject
import com.cashruler.data.models.SavingsFrequency
import com.cashruler.data.models.SavingsTransaction
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

class SavingsRepositoryTest {

    private lateinit var dao: SavingsDao
    private lateinit var repository: SavingsRepository

    @Before
    fun setup() {
        dao = mockk()
        repository = SavingsRepository(dao)
    }

    @Test
    fun `getAllProjects returns projects from dao`() = runTest {
        // Given
        val projects = listOf(
            createProject(1),
            createProject(2)
        )
        coEvery { dao.getAllProjects() } returns flowOf(projects)

        // When
        val result = repository.getAllProjects()

        // Then
        result.collect { list ->
            assertEquals(projects, list)
        }
        coVerify { dao.getAllProjects() }
    }

    @Test
    fun `getProjectById returns project from dao`() = runTest {
        // Given
        val project = createProject(1)
        coEvery { dao.getProjectById(1) } returns flowOf(project)

        // When
        val result = repository.getProjectById(1)

        // Then
        result.collect { 
            assertNotNull(it)
            assertEquals(project, it)
        }
        coVerify { dao.getProjectById(1) }
    }

    @Test
    fun `addProject calls dao insert`() = runTest {
        // Given
        val project = createProject(1)
        coEvery { dao.insertProject(project) } returns 1

        // When
        repository.addProject(project)

        // Then
        coVerify { dao.insertProject(project) }
    }

    @Test
    fun `updateProject calls dao update`() = runTest {
        // Given
        val project = createProject(1)
        coEvery { dao.updateProject(project) } returns Unit

        // When
        repository.updateProject(project)

        // Then
        coVerify { dao.updateProject(project) }
    }

    @Test
    fun `deleteProject calls dao delete`() = runTest {
        // Given
        val project = createProject(1)
        coEvery { dao.deleteProject(project) } returns Unit

        // When
        repository.deleteProject(project)

        // Then
        coVerify { dao.deleteProject(project) }
    }

    @Test
    fun `getProjectTransactions returns transactions from dao`() = runTest {
        // Given
        val projectId = 1L
        val transactions = listOf(
            createTransaction(1, projectId),
            createTransaction(2, projectId)
        )
        coEvery { dao.getProjectTransactions(projectId) } returns flowOf(transactions)

        // When
        val result = repository.getProjectTransactions(projectId)

        // Then
        result.collect { list ->
            assertEquals(transactions, list)
        }
        coVerify { dao.getProjectTransactions(projectId) }
    }

    @Test
    fun `addTransaction calls dao insert and updates project amount`() = runTest {
        // Given
        val projectId = 1L
        val transaction = createTransaction(1, projectId)
        val project = createProject(projectId, currentAmount = 100.0)
        coEvery { dao.getProjectById(projectId) } returns flowOf(project)
        coEvery { dao.insertTransaction(transaction) } returns 1L
        coEvery { dao.updateProject(any()) } returns Unit

        // When
        repository.addTransaction(transaction)

        // Then
        coVerify { dao.insertTransaction(transaction) }
        coVerify { 
            dao.updateProject(match { 
                it.id == projectId && 
                it.currentAmount == project.currentAmount + transaction.amount 
            })
        }
    }

    @Test
    fun `getActiveProjects returns only active projects`() = runTest {
        // Given
        val projects = listOf(
            createProject(1, deadline = Date(System.currentTimeMillis() + 1000000)), // Active
            createProject(2, deadline = Date(System.currentTimeMillis() - 1000000))  // Inactive
        )
        coEvery { dao.getAllProjects() } returns flowOf(projects)

        // When
        val result = repository.getActiveProjects()

        // Then
        result.collect { list ->
            assertEquals(1, list.size)
            assertEquals(1L, list[0].id)
        }
    }

    private fun createProject(
        id: Long,
        title: String = "Test Project",
        targetAmount: Double = 1000.0,
        currentAmount: Double = 0.0,
        startDate: Date = Date(),
        deadline: Date = Date(System.currentTimeMillis() + 86400000), // +1 day
        frequency: SavingsFrequency = SavingsFrequency.MONTHLY
    ) = SavingsProject(
        id = id,
        title = title,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        startDate = startDate,
        deadline = deadline,
        frequency = frequency
    )

    private fun createTransaction(
        id: Long,
        projectId: Long,
        amount: Double = 100.0,
        date: Date = Date(),
        note: String? = null
    ) = SavingsTransaction(
        id = id,
        projectId = projectId,
        amount = amount,
        date = date,
        note = note
    )
}
