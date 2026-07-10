package com.yangcy.gofish.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fishing_spots")
data class FishingSpot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val iconColor: String, // hex string like "#00ADB5" or "#FF5252"
    val iconStyle: String, // "pin", "fish", "anchor", "flag", "star"
    val arrivalTime: String,
    val imageUrl: String? = null,
    val fishSpecies: String,
    val bait: String,
    val fee: String,
    val notes: String,
    val isSynced: Boolean = false
)
