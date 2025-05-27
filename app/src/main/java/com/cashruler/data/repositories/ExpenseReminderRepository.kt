package com.cashruler.data.repositories

import com.cashruler.data.dao.ExpenseReminderDao
import com.cashruler.data.models.ExpenseReminder
import com.cashruler.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

interface ExpenseReminderRepositoryInterface {
    suspend fun addReminder(reminder: ExpenseReminder): Long
    suspend fun updateReminder(reminder: ExpenseReminder)
    suspend fun deleteReminder(reminder: ExpenseReminder)
    suspend fun getReminderById(id: Long): ExpenseReminder?
    fun getActiveReminders(): Flow<List<ExpenseReminder>>
    suspend fun getActiveRemindersDue(date: Date): List<ExpenseReminder>
    suspend fun setReminderActive(id: Long, isActive: Boolean)
}

@Singleton
class ExpenseReminderRepository @Inject constructor(
    private val expenseReminderDao: ExpenseReminderDao,
    @IODispatcher private val dispatcher: CoroutineDispatcher
) : ExpenseReminderRepositoryInterface {

    override suspend fun addReminder(reminder: ExpenseReminder): Long = withContext(dispatcher) {
        return@withContext expenseReminderDao.insert(reminder)
    }

    override suspend fun updateReminder(reminder: ExpenseReminder) = withContext(dispatcher) {
        expenseReminderDao.update(reminder.copy(updatedAt = Date())) // Ensure updatedAt is set
    }

    override suspend fun deleteReminder(reminder: ExpenseReminder) = withContext(dispatcher) {
        expenseReminderDao.delete(reminder)
    }

    override suspend fun getReminderById(id: Long): ExpenseReminder? = withContext(dispatcher) {
        return@withContext expenseReminderDao.getById(id)
    }

    override fun getActiveReminders(): Flow<List<ExpenseReminder>> {
        // flowOn can be applied here if DAO doesn't already specify dispatcher,
        // but typically DAO methods returning Flow handle their own dispatching.
        // For consistency with other repositories, let's assume DAO handles it or apply it here.
        return expenseReminderDao.getActiveReminders() // .flowOn(dispatcher) if needed
    }

    override suspend fun getActiveRemindersDue(date: Date): List<ExpenseReminder> = withContext(dispatcher) {
        return@withContext expenseReminderDao.getActiveRemindersDue(date.time)
    }

    override suspend fun setReminderActive(id: Long, isActive: Boolean) = withContext(dispatcher) {
        // The DAO's setReminderActive has a default value for updatedAt (System.currentTimeMillis())
        expenseReminderDao.setReminderActive(id, isActive)
    }
}
