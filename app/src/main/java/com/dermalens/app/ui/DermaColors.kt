package com.dermalens.app.ui.screens

import androidx.compose.ui.graphics.Color

val DermaGreen = Color(0xFF7C3AED)
val DermaGreenLight = Color(0xFFEDE9FE)
val DermaGreenDark = Color(0xFF6D28D9)

fun hashPassword(password: String): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(password.toByteArray())
    return hashBytes.joinToString("") { "%02x".format(it) }
}

object DermaPrefs {
    const val PREFS_NAME = "dermalens_prefs"
    const val KEY_REMEMBER_EMAIL = "remember_email"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    const val KEY_USER_EMAIL = "user_email"
    const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
    const val KEY_FONT_SIZE = "font_size"
    const val KEY_HIGH_CONTRAST = "high_contrast"
}