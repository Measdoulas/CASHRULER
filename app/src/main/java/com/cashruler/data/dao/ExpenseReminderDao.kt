package com.cashruler.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cashruler.data.models.ExpenseReminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ExpenseReminder): Long

    @Update
    suspend fun update(reminder: ExpenseReminder)

    @Delete
    suspend fun delete(reminder: ExpenseReminder)

    @Query("SELECT * FROM expense_reminders WHERE id = :id")
    suspend fun getById(id: Long): ExpenseReminder?

    @Query("SELECT * FROM expense_reminders WHERE isActive = 1 ORDER BY reminderDate ASC")
    fun getActiveReminders(): Flow<List<ExpenseReminder>>

    @Query("SELECT * FROM expense_reminders WHERE isActive = 1 AND reminderDate <= :date ORDER BY reminderDate ASC")
    suspend fun getActiveRemindersDue(date: Long): List<ExpenseReminder>

    @Query("UPDATE expense_reminders SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setReminderActive(id: Long, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())
}
