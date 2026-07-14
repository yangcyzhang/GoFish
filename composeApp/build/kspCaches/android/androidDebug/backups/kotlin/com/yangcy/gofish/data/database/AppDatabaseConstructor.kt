package com.yangcy.gofish.`data`.database

import androidx.room.RoomDatabaseConstructor

public actual object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
  override fun initialize(): AppDatabase = com.yangcy.gofish.`data`.database.AppDatabase_Impl()
}
