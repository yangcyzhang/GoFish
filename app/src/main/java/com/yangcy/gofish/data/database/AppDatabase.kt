package com.yangcy.gofish.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yangcy.gofish.data.model.CatchLog
import com.yangcy.gofish.data.model.FishingSpot

@Database(
    entities = [CatchLog::class, FishingSpot::class], 
    version = AppDatabase.DATABASE_VERSION, 
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catchLogDao(): CatchLogDao
    abstract fun fishingSpotDao(): FishingSpotDao

    companion object {
        const val DATABASE_VERSION = 2

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 数据库迁移示例 (由 1 升到 2)
         * 保护措施：在未来的版本升级中，通过编写 Migration 确保用户已保存的钓点和日记不被清空。
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 此处编写 SQL 升级语句，例如：
                // db.execSQL("ALTER TABLE fishing_spots ADD COLUMN sync_status INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fish_guide_database"
                )
                // 升级保护核心配置：
                // 1. 显式配置 Migration 路径
                .addMigrations(MIGRATION_1_2)
                // 2. 如果没有提供对应的 Migration，则允许回退到破坏性迁移以防止崩溃（仅限开发环境建议）
                // 在生产环境下，如果对数据安全要求极高，应考虑移除此项，避免意外清空数据
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
