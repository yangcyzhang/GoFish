package com.yangcy.gofish.`data`.database

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.yangcy.gofish.`data`.model.FishingSpot
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
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
public class FishingSpotDao_Impl(
  __db: RoomDatabase,
) : FishingSpotDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfFishingSpot: EntityInsertAdapter<FishingSpot>

  private val __deleteAdapterOfFishingSpot: EntityDeleteOrUpdateAdapter<FishingSpot>
  init {
    this.__db = __db
    this.__insertAdapterOfFishingSpot = object : EntityInsertAdapter<FishingSpot>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `fishing_spots` (`id`,`name`,`address`,`latitude`,`longitude`,`iconColor`,`iconStyle`,`arrivalTime`,`imageUrl`,`fishSpecies`,`bait`,`fee`,`notes`,`isSynced`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: FishingSpot) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.address)
        statement.bindDouble(4, entity.latitude)
        statement.bindDouble(5, entity.longitude)
        statement.bindText(6, entity.iconColor)
        statement.bindText(7, entity.iconStyle)
        statement.bindText(8, entity.arrivalTime)
        val _tmpImageUrl: String? = entity.imageUrl
        if (_tmpImageUrl == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpImageUrl)
        }
        statement.bindText(10, entity.fishSpecies)
        statement.bindText(11, entity.bait)
        statement.bindText(12, entity.fee)
        statement.bindText(13, entity.notes)
        val _tmp: Int = if (entity.isSynced) 1 else 0
        statement.bindLong(14, _tmp.toLong())
      }
    }
    this.__deleteAdapterOfFishingSpot = object : EntityDeleteOrUpdateAdapter<FishingSpot>() {
      protected override fun createQuery(): String = "DELETE FROM `fishing_spots` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: FishingSpot) {
        statement.bindLong(1, entity.id.toLong())
      }
    }
  }

  public override suspend fun insertFishingSpot(spot: FishingSpot): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfFishingSpot.insert(_connection, spot)
  }

  public override suspend fun deleteFishingSpot(spot: FishingSpot): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfFishingSpot.handle(_connection, spot)
  }

  public override fun getAllFishingSpots(): Flow<List<FishingSpot>> {
    val _sql: String = "SELECT * FROM fishing_spots ORDER BY id DESC"
    return createFlow(__db, false, arrayOf("fishing_spots")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfAddress: Int = getColumnIndexOrThrow(_stmt, "address")
        val _columnIndexOfLatitude: Int = getColumnIndexOrThrow(_stmt, "latitude")
        val _columnIndexOfLongitude: Int = getColumnIndexOrThrow(_stmt, "longitude")
        val _columnIndexOfIconColor: Int = getColumnIndexOrThrow(_stmt, "iconColor")
        val _columnIndexOfIconStyle: Int = getColumnIndexOrThrow(_stmt, "iconStyle")
        val _columnIndexOfArrivalTime: Int = getColumnIndexOrThrow(_stmt, "arrivalTime")
        val _columnIndexOfImageUrl: Int = getColumnIndexOrThrow(_stmt, "imageUrl")
        val _columnIndexOfFishSpecies: Int = getColumnIndexOrThrow(_stmt, "fishSpecies")
        val _columnIndexOfBait: Int = getColumnIndexOrThrow(_stmt, "bait")
        val _columnIndexOfFee: Int = getColumnIndexOrThrow(_stmt, "fee")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfIsSynced: Int = getColumnIndexOrThrow(_stmt, "isSynced")
        val _result: MutableList<FishingSpot> = mutableListOf()
        while (_stmt.step()) {
          val _item: FishingSpot
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpAddress: String
          _tmpAddress = _stmt.getText(_columnIndexOfAddress)
          val _tmpLatitude: Double
          _tmpLatitude = _stmt.getDouble(_columnIndexOfLatitude)
          val _tmpLongitude: Double
          _tmpLongitude = _stmt.getDouble(_columnIndexOfLongitude)
          val _tmpIconColor: String
          _tmpIconColor = _stmt.getText(_columnIndexOfIconColor)
          val _tmpIconStyle: String
          _tmpIconStyle = _stmt.getText(_columnIndexOfIconStyle)
          val _tmpArrivalTime: String
          _tmpArrivalTime = _stmt.getText(_columnIndexOfArrivalTime)
          val _tmpImageUrl: String?
          if (_stmt.isNull(_columnIndexOfImageUrl)) {
            _tmpImageUrl = null
          } else {
            _tmpImageUrl = _stmt.getText(_columnIndexOfImageUrl)
          }
          val _tmpFishSpecies: String
          _tmpFishSpecies = _stmt.getText(_columnIndexOfFishSpecies)
          val _tmpBait: String
          _tmpBait = _stmt.getText(_columnIndexOfBait)
          val _tmpFee: String
          _tmpFee = _stmt.getText(_columnIndexOfFee)
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpIsSynced: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsSynced).toInt()
          _tmpIsSynced = _tmp != 0
          _item = FishingSpot(_tmpId,_tmpName,_tmpAddress,_tmpLatitude,_tmpLongitude,_tmpIconColor,_tmpIconStyle,_tmpArrivalTime,_tmpImageUrl,_tmpFishSpecies,_tmpBait,_tmpFee,_tmpNotes,_tmpIsSynced)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Int) {
    val _sql: String = "DELETE FROM fishing_spots WHERE id = ?"
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

  public override suspend fun updateSyncStatus(id: Int, synced: Boolean) {
    val _sql: String = "UPDATE fishing_spots SET isSynced = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (synced) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
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
