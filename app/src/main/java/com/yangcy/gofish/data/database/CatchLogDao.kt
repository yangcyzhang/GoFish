package com.yangcy.gofish.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yangcy.gofish.data.model.CatchLog
import kotlinx.coroutines.flow.Flow

@Dao
interface CatchLogDao {
    @Query("SELECT * FROM catch_logs ORDER BY timestamp DESC")
    fun getAllCatchLogs(): Flow<List<CatchLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatchLog(log: CatchLog)

    @Delete
    suspend fun deleteCatchLog(log: CatchLog)

    @Query("DELETE FROM catch_logs WHERE id = :id")
    suspend fun deleteById(id: Int)
}
