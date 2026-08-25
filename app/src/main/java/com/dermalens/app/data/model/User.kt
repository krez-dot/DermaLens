package com.dermalens.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val userId: Int = 0,
    val fullName: String,
    val email: String,
    // Empty for Firebase-authenticated accounts (Firebase manages the real credential). Only
    // ever non-empty for legacy local-only accounts from before Firebase Auth was added.
    val passwordHash: String,
    val firebaseUid: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val notificationEnabled: Boolean = true

)