package com.yangcy.gofish.`data`.database

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.yangcy.gofish.`data`.model.CatchLog
import javax.`annotation`.processing.Generated
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class CatchLogDao_Impl(
  __db: RoomDatabase,
) : CatchLogDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCatchLog: EntityInsertAdapter<CatchLog>

  private val __deleteAdapterOfCatchLog: EntityDeleteOrUpdateAdapter<CatchLog>
  init {
    this.__db = __db
    this.__insertAdapterOfCatchLog = object : EntityInsertAdapter<CatchLog>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `catch_logs` (`id`,`fishName`,`location`,`timestamp`,`weatherCondition`,`temperature`,`weightKg`,`lengthCm`,`baitUsed`,`notes`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CatchLog) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.fishName)
        statement.bindText(3, entity.location)
        statement.bindLong(4, entity.timestamp)
        statement.bindText(5, entity.weatherCondition)
        statement.bindText(6, entity.temperature)
        statement.bindDouble(7, entity.weightKg)
        statement.bindDouble(8, entity.lengthCm)
        statement.bindText(9, entity.baitUsed)
        statement.bindText(10, entity.notes)
      }
    }
    this.__deleteAdapterOfCatchLog = object : EntityDeleteOrUpdateAdapter<CatchLog>() {
      protected override fun createQuery(): String = "DELETE FROM `catch_logs` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: CatchLog) {
        statement.bindLong(1, entity.id.toLong())
      }
    }
  }

  public override suspend fun insertCatchLog(log: CatchLog): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfCatchLog.insert(_connection, log)
  }

  public override suspend fun deleteCatchLog(log: CatchLog): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfCatchLog.handle(_connection, log)
  }

  public override fun getAllCatchLogs(): Flow<List<CatchLog>> {
    val _sql: String = "SELECT * FROM catch_logs ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("catch_logs")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfFishName: Int = getColumnIndexOrThrow(_stmt, "fishName")
        val _columnIndexOfLocation: Int = getColumnIndexOrThrow(_stmt, "location")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfWeatherCondition: Int = getColumnIndexOrThrow(_stmt, "weatherCondition")
        val _columnIndexOfTemperature: Int = getColumnIndexOrThrow(_stmt, "temperature")
        val _columnIndexOfWeightKg: Int = getColumnIndexOrThrow(_stmt, "weightKg")
        val _columnIndexOfLengthCm: Int = getColumnIndexOrThrow(_stmt, "lengthCm")
        val _columnIndexOfBaitUsed: Int = getColumnIndexOrThrow(_stmt, "baitUsed")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _result: MutableList<CatchLog> = mutableListOf()
        while (_stmt.step()) {
          val _item: CatchLog
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpFishName: String
          _tmpFishName = _stmt.getText(_columnIndexOfFishName)
          val _tmpLocation: String
          _tmpLocation = _stmt.getText(_columnIndexOfLocation)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpWeatherCondition: String
          _tmpWeatherCondition = _stmt.getText(_columnIndexOfWeatherCondition)
          val _tmpTemperature: String
          _tmpTemperature = _stmt.getText(_columnIndexOfTemperature)
          val _tmpWeightKg: Double
          _tmpWeightKg = _stmt.getDouble(_columnIndexOfWeightKg)
          val _tmpLengthCm: Double
          _tmpLengthCm = _stmt.getDouble(_columnIndexOfLengthCm)
          val _tmpBaitUsed: String
          _tmpBaitUsed = _stmt.getText(_columnIndexOfBaitUsed)
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          _item = CatchLog(_tmpId,_tmpFishName,_tmpLocation,_tmpTimestamp,_tmpWeatherCondition,_tmpTemperature,_tmpWeightKg,_tmpLengthCm,_tmpBaitUsed,_tmpNotes)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Int) {
    val _sql: String = "DELETE FROM catch_logs WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id.toLong())
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
