package com.yangcy.gofish.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yangcy.gofish.data.model.CatchLog
import com.yangcy.gofish.data.model.FishingSpot

@Database(entities = [CatchLog::class, FishingSpot::class], version = AppDatabase.DATABASE_VERSION, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catchLogDao(): CatchLogDao
    abstract fun fishingSpotDao(): FishingSpotDao

    companion object {
        // 当前数据库版本号，后续若修改实体结构（Entity），只需在此将版本号+1
        // 并根据需要添加对应的 Migration 升级逻辑以保留用户本地数据
        const val DATABASE_VERSION = 2

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fish_guide_database"
                )
                // ⚠️ 当前开发阶段默认开启破坏性迁移（会清除本地旧数据），方便快速调试。
                // 线上版本若需保留用户钓鱼记录和点位，请移除下行，并使用 .addMigrations(MIGRATION_X_Y) 升级。
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
