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
    // Empty for Firebase-authenticated accounts (Firebase manages the real credential) and for
    // guest accounts (no password at all). Only ever non-empty for legacy local-only accounts.
    val passwordHash: String,
    // Set for accounts registered through Firebase Auth; null for guest accounts.
    val firebaseUid: String? = null,
    val isGuest: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val notificationEnabled: Boolean = true

)