package com.yangcy.gofish.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catch_logs")
data class CatchLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fishName: String,
    val location: String,
    val timestamp: Long = System.currentTimeMillis(),
    val weatherCondition: String = "晴朗",
    val temperature: String = "25°C",
    val weightKg: Double = 0.5, // kilograms
    val lengthCm: Double = 20.0, // centimeters
    val baitUsed: String = "红虫",
    val notes: String = "在荷花池草边钓获，拉力十足！"
)
