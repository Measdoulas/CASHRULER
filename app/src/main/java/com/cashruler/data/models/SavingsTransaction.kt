package com.cashruler.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "savings_transactions",
    foreignKeys = [
        ForeignKey(
            entity = SavingsProject::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE // If a project is deleted, its transactions are also deleted.
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class SavingsTransaction(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var projectId: Long,
    var amount: Double, // Positive for deposit, negative for withdrawal if using a single amount field
    var transactionDate: Date,
    var description: String? = null,
    var type: String, // "DEPOSIT" or "WITHDRAWAL"
    var createdAt: Date = Date()
) {
    companion object {
        const val TYPE_DEPOSIT = "DEPOSIT"
        const val TYPE_WITHDRAWAL = "WITHDRAWAL"
    }
}
