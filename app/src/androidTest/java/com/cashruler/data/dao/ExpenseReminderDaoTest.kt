package com.cashruler.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cashruler.data.database.AppDatabase
import com.cashruler.data.database.Converters
import com.cashruler.data.models.CategoryEntity // Required for foreign key
import com.cashruler.data.models.ExpenseReminder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking // Using runBlocking for DAO tests is acceptable
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Date
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class ExpenseReminderDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var expenseReminderDao: ExpenseReminderDao
    private lateinit var categoryDao: CategoryDao // For setting up category for FK

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(Converters()) // Ensure converters are added
            // .allowMainThreadQueries() // For simplicity in tests, though runBlocking is preferred
            .build()
        expenseReminderDao = db.expenseReminderDao()
        categoryDao = db.categoryDao() // Initialize category DAO
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetById() = runBlocking {
        val reminder = ExpenseReminder(description = "Test Insert", reminderDate = Date())
        val id = expenseReminderDao.insert(reminder)
        
        val retrieved = expenseReminderDao.getById(id)
        assertNotNull(retrieved)
        assertEquals(id, retrieved?.id)
        assertEquals("Test Insert", retrieved?.description)
    }

    @Test
    @Throws(Exception::class)
    fun insertWithCategoryAndGetById() = runBlocking {
        // First, insert a category
        val category = CategoryEntity(name = "Groceries")
        val categoryId = categoryDao.insert(category)

        val reminder = ExpenseReminder(description = "Test With Category", reminderDate = Date(), categoryId = categoryId)
        val reminderId = expenseReminderDao.insert(reminder)
        
        val retrieved = expenseReminderDao.getById(reminderId)
        assertNotNull(retrieved)
        assertEquals(categoryId, retrieved?.categoryId)
    }


    @Test
    @Throws(Exception::class)
    fun updateAndGetById() = runBlocking {
        val reminder = ExpenseReminder(description = "Test Update", reminderDate = Date())
        val id = expenseReminderDao.insert(reminder)

        val retrieved = expenseReminderDao.getById(id)!!
        val updatedReminder = retrieved.copy(description = "Updated Description", amount = 100.0)
        expenseReminderDao.update(updatedReminder)

        val updatedRetrieved = expenseReminderDao.getById(id)
        assertNotNull(updatedRetrieved)
        assertEquals("Updated Description", updatedRetrieved?.description)
        assertEquals(100.0, updatedRetrieved?.amount, 0.0)
    }

    @Test
    @Throws(Exception::class)
    fun deleteAndGetById() = runBlocking {
        val reminder = ExpenseReminder(description = "Test Delete", reminderDate = Date())
        val id = expenseReminderDao.insert(reminder)
        
        assertNotNull(expenseReminderDao.getById(id)) // Ensure it's there

        val toDelete = expenseReminderDao.getById(id)!!
        expenseReminderDao.delete(toDelete)

        assertNull(expenseReminderDao.getById(id)) // Should be null after delete
    }

    @Test
    @Throws(Exception::class)
    fun getActiveReminders() = runBlocking {
        val activeReminder = ExpenseReminder(description = "Active", reminderDate = Date(), isActive = true)
        val inactiveReminder = ExpenseReminder(description = "Inactive", reminderDate = Date(), isActive = false)
        expenseReminderDao.insert(activeReminder)
        expenseReminderDao.insert(inactiveReminder)

        val activeList = expenseReminderDao.getActiveReminders().first()
        assertEquals(1, activeList.size)
        assertEquals("Active", activeList[0].description)
    }

    @Test
    @Throws(Exception::class)
    fun getActiveRemindersDue() = runBlocking {
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time

        val dueReminder = ExpenseReminder(description = "Due Today", reminderDate = today.time, isActive = true)
        val pastDueReminder = ExpenseReminder(description = "Due Yesterday", reminderDate = yesterday, isActive = true)
        val futureReminder = ExpenseReminder(description = "Due Tomorrow", reminderDate = tomorrow, isActive = true)
        val inactiveDueReminder = ExpenseReminder(description = "Inactive Due", reminderDate = today.time, isActive = false)

        expenseReminderDao.insert(dueReminder)
        expenseReminderDao.insert(pastDueReminder)
        expenseReminderDao.insert(futureReminder)
        expenseReminderDao.insert(inactiveDueReminder)

        // Get reminders due up to and including today
        val dueList = expenseReminderDao.getActiveRemindersDue(today.timeInMillis)
        
        assertEquals(2, dueList.size) // Should include "Due Today" and "Due Yesterday"
        assertTrue(dueList.any { it.description == "Due Today" })
        assertTrue(dueList.any { it.description == "Due Yesterday" })
        assertFalse(dueList.any { it.description == "Due Tomorrow" })
        assertFalse(dueList.any { it.description == "Inactive Due" })
    }

    @Test
    @Throws(Exception::class)
    fun setReminderActive() = runBlocking {
        val reminder = ExpenseReminder(description = "Set Active Test", reminderDate = Date(), isActive = true)
        val id = expenseReminderDao.insert(reminder)

        expenseReminderDao.setReminderActive(id, false, System.currentTimeMillis())
        var retrieved = expenseReminderDao.getById(id)
        assertNotNull(retrieved)
        assertFalse(retrieved!!.isActive)

        expenseReminderDao.setReminderActive(id, true, System.currentTimeMillis())
        retrieved = expenseReminderDao.getById(id)
        assertNotNull(retrieved)
        assertTrue(retrieved!!.isActive)
    }
}
