package com.yangcy.gofish.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yangcy.gofish.data.model.FishingSpot
import kotlinx.coroutines.flow.Flow

@Dao
interface FishingSpotDao {
    @Query("SELECT * FROM fishing_spots ORDER BY id DESC")
    fun getAllFishingSpots(): Flow<List<FishingSpot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFishingSpot(spot: FishingSpot)

    @Delete
    suspend fun deleteFishingSpot(spot: FishingSpot)

    @Query("DELETE FROM fishing_spots WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE fishing_spots SET isSynced = :synced WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, synced: Boolean)
}
