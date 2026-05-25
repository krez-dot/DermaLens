package com.dermalens.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_records")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val condition: String,
    val confidence: Float,
    val severity: String,
    val notes: String = "",
    val scanDate: Long = System.currentTimeMillis(),
    val imagePath: String = "",
    val contributedForTraining: Boolean = false
)