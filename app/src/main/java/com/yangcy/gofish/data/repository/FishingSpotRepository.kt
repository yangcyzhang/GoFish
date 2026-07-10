package com.yangcy.gofish.data.repository

import com.yangcy.gofish.data.database.FishingSpotDao
import com.yangcy.gofish.data.model.FishingSpot
import kotlinx.coroutines.flow.Flow

class FishingSpotRepository(private val fishingSpotDao: FishingSpotDao) {
    val allFishingSpots: Flow<List<FishingSpot>> = fishingSpotDao.getAllFishingSpots()

    suspend fun insert(spot: FishingSpot) {
        fishingSpotDao.insertFishingSpot(spot)
    }

    suspend fun delete(spot: FishingSpot) {
        fishingSpotDao.deleteFishingSpot(spot)
    }

    suspend fun deleteById(id: Int) {
        fishingSpotDao.deleteById(id)
    }

    suspend fun updateSyncStatus(id: Int, synced: Boolean) {
        fishingSpotDao.updateSyncStatus(id, synced)
    }
}
