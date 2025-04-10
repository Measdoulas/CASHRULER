package com.cashruler.ui.viewmodels

import com.cashruler.data.repositories.BackupRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    private lateinit var repository: BackupRepository
    private lateinit var viewModel: BackupViewModel
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        repository = mockk()
        viewModel = BackupViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        assertIs<BackupUiState.Idle>(viewModel.uiState.value)
    }

    @Test
    fun `loadBackups updates state to Success with backups list`() = runTest {
        // Given
        val backups = listOf(
            createBackupInfo("backup1.json", 1000L),
            createBackupInfo("backup2.json", 2000L)
        )
        coEvery { repository.listBackups() } returns backups

        // When
        viewModel.loadBackups()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertIs<BackupUiState.Success>(state)
        assertEquals(backups, state.backups)
    }

    @Test
    fun `createBackup shows loading and success states`() = runTest {
        // Given
        val backupPath = "/path/to/backup.json"
        coEvery { repository.exportData() } returns backupPath
        coEvery { repository.listBackups() } returns emptyList()

        // When
        viewModel.createBackup()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { repository.exportData() }
        coVerify { repository.listBackups() }
        assertIs<BackupUiState.Success>(viewModel.uiState.value)
    }

    @Test
    fun `createBackup shows error state when export fails`() = runTest {
        // Given
        coEvery { repository.exportData() } throws Exception("Export failed")

        // When
        viewModel.createBackup()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertIs<BackupUiState.Error>(state)
        assertEquals("Échec de la création de la sauvegarde", state.message)
    }

    @Test
    fun `restoreBackup shows loading and success states`() = runTest {
        // Given
        val backupPath = "/path/to/backup.json"
        coEvery { repository.importData(backupPath) } returns Unit
        coEvery { repository.listBackups() } returns emptyList()

        // When
        viewModel.restoreBackup(backupPath)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { repository.importData(backupPath) }
        coVerify { repository.listBackups() }
        assertIs<BackupUiState.Success>(viewModel.uiState.value)
    }

    @Test
    fun `deleteBackup removes backup and reloads list`() = runTest {
        // Given
        val backupPath = "/path/to/backup.json"
        coEvery { repository.listBackups() } returns emptyList()

        // When
        viewModel.deleteBackup(backupPath)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { repository.listBackups() }
        assertIs<BackupUiState.Success>(viewModel.uiState.value)
    }

    private fun createBackupInfo(
        filePath: String,
        timestamp: Long,
        version: Int = 1
    ) = BackupRepository.BackupInfo(
        file = java.io.File(filePath),
        timestamp = timestamp,
        version = version
    )
}
