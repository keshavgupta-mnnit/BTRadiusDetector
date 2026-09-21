package com.kglabs28.btradiusdetector.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertSettingsDao {
    @Query("SELECT * FROM alert_settings")
    fun observeAll(): Flow<List<AlertSettingsEntity>>

    @Query("SELECT * FROM alert_settings WHERE address = :address")
    suspend fun getByAddress(address: String): AlertSettingsEntity?

    @Upsert
    suspend fun upsert(entity: AlertSettingsEntity)

    @Delete
    suspend fun delete(entity: AlertSettingsEntity)
}