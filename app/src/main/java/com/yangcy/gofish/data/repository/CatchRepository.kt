package com.yangcy.gofish.data.repository

import com.yangcy.gofish.data.database.CatchLogDao
import com.yangcy.gofish.data.model.CatchLog
import kotlinx.coroutines.flow.Flow

class CatchRepository(private val catchLogDao: CatchLogDao) {
    val allCatches: Flow<List<CatchLog>> = catchLogDao.getAllCatchLogs()

    suspend fun insert(log: CatchLog) {
        catchLogDao.insertCatchLog(log)
    }

    suspend fun delete(log: CatchLog) {
        catchLogDao.deleteCatchLog(log)
    }

    suspend fun deleteById(id: Int) {
        catchLogDao.deleteById(id)
    }
}
