package com.yangcy.gofish.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.ConstructedBy
import androidx.room.RoomDatabaseConstructor
import com.yangcy.gofish.data.model.CatchLog
import com.yangcy.gofish.data.model.FishingSpot

@Database(
    entities = [CatchLog::class, FishingSpot::class], 
    version = 2
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catchLogDao(): CatchLogDao
    abstract fun fishingSpotDao(): FishingSpotDao
}

// The expect object should be implemented by the Room compiler
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
