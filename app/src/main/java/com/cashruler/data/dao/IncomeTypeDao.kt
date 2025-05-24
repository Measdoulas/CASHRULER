package com.cashruler.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cashruler.data.models.IncomeTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeTypeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(type: IncomeTypeEntity): Long

    @Query("SELECT * FROM income_types ORDER BY name ASC")
    fun getAll(): Flow<List<IncomeTypeEntity>>
    
    @Query("SELECT * FROM income_types WHERE name = :name LIMIT 1")
    suspend fun getTypeByName(name: String): IncomeTypeEntity?
}
