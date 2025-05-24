package com.cashruler.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "income_types", indices = [Index(value = ["name"], unique = true)])
data class IncomeTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
