package com.cashruler.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "expense_reminders",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL // Or another action like NO_ACTION, RESTRICT
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class ExpenseReminder(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var description: String,
    var amount: Double? = null,
    var reminderDate: Date,
    var isRecurring: Boolean = false,
    var frequencyDays: Int? = null,
    var categoryId: Long? = null,
    var notes: String? = null,
    var isActive: Boolean = true,
    var createdAt: Date = Date(),
    var updatedAt: Date = Date()
)
