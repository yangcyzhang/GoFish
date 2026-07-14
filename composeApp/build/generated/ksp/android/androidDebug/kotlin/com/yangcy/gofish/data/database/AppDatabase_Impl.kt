package com.yangcy.gofish.`data`.database

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _catchLogDao: Lazy<CatchLogDao> = lazy {
    CatchLogDao_Impl(this)
  }


  private val _fishingSpotDao: Lazy<FishingSpotDao> = lazy {
    FishingSpotDao_Impl(this)
  }


  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2,
        "e4d7a8ae609fd9d222c53d926c8d5c7b", "3ddf9d3f44e96af3f88e99eda52e0f18") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `catch_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fishName` TEXT NOT NULL, `location` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `weatherCondition` TEXT NOT NULL, `temperature` TEXT NOT NULL, `weightKg` REAL NOT NULL, `lengthCm` REAL NOT NULL, `baitUsed` TEXT NOT NULL, `notes` TEXT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `fishing_spots` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `address` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `iconColor` TEXT NOT NULL, `iconStyle` TEXT NOT NULL, `arrivalTime` TEXT NOT NULL, `imageUrl` TEXT, `fishSpecies` TEXT NOT NULL, `bait` TEXT NOT NULL, `fee` TEXT NOT NULL, `notes` TEXT NOT NULL, `isSynced` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e4d7a8ae609fd9d222c53d926c8d5c7b')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `catch_logs`")
        connection.execSQL("DROP TABLE IF EXISTS `fishing_spots`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsCatchLogs: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCatchLogs.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCatchLogs.put("fishName", TableInfo.Column("fishName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCatchLogs.put("location", TableInfo.Column("location", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCatchLogs.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCatchLogs.put("weatherCondition", TableInfo.Column("weatherCondition", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCatchLogs.put("temperature", TableInfo.Column("temperature", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCatchLogs.put("weightKg", TableInfo.Column("weightKg", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCatchLogs.put("lengthCm", TableInfo.Column("lengthCm", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCatchLogs.put("baitUsed", TableInfo.Column("baitUsed", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCatchLogs.put("notes", TableInfo.Column("notes", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCatchLogs: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCatchLogs: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCatchLogs: TableInfo = TableInfo("catch_logs", _columnsCatchLogs,
            _foreignKeysCatchLogs, _indicesCatchLogs)
        val _existingCatchLogs: TableInfo = read(connection, "catch_logs")
        if (!_infoCatchLogs.equals(_existingCatchLogs)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |catch_logs(com.yangcy.gofish.data.model.CatchLog).
              | Expected:
              |""".trimMargin() + _infoCatchLogs + """
              |
              | Found:
              |""".trimMargin() + _existingCatchLogs)
        }
        val _columnsFishingSpots: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsFishingSpots.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFishingSpots.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFishingSpots.put("address", TableInfo.Column("address", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFishingSpots.put("latitude", TableInfo.Column("latitude", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFishingSpots.put("longitude", TableInfo.Column("longitude", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFishingSpots.put("iconColor", TableInfo.Column("iconColor", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFishingSpots.put("iconStyle", TableInfo.Column("iconStyle", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFishingSpots.put("arrivalTime", TableInfo.Column("arrivalTime", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFishingSpots.put("imageUrl", TableInfo.Column("imageUrl", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFishingSpots.put("fishSpecies", TableInfo.Column("fishSpecies", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFishingSpots.put("bait", TableInfo.Column("bait", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFishingSpots.put("fee", TableInfo.Column("fee", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFishingSpots.put("notes", TableInfo.Column("notes", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFishingSpots.put("isSynced", TableInfo.Column("isSynced", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysFishingSpots: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesFishingSpots: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoFishingSpots: TableInfo = TableInfo("fishing_spots", _columnsFishingSpots,
            _foreignKeysFishingSpots, _indicesFishingSpots)
        val _existingFishingSpots: TableInfo = read(connection, "fishing_spots")
        if (!_infoFishingSpots.equals(_existingFishingSpots)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |fishing_spots(com.yangcy.gofish.data.model.FishingSpot).
              | Expected:
              |""".trimMargin() + _infoFishingSpots + """
              |
              | Found:
              |""".trimMargin() + _existingFishingSpots)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "catch_logs", "fishing_spots")
  }

  public override fun clearAllTables() {
    super.performClear(false, "catch_logs", "fishing_spots")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(CatchLogDao::class, CatchLogDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(FishingSpotDao::class, FishingSpotDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun catchLogDao(): CatchLogDao = _catchLogDao.value

  public override fun fishingSpotDao(): FishingSpotDao = _fishingSpotDao.value
}
