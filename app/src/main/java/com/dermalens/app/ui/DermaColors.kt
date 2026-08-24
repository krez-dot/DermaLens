package com.dermalens.app.ui.screens

import androidx.compose.ui.graphics.Color

val DermaGreen = Color(0xFF7C3AED)
val DermaGreenLight = Color(0xFFEDE9FE)
val DermaGreenDark = Color(0xFF6D28D9)

/** Hashes [password] with a fresh random salt. Stored format: "saltHex:hashHex". */
fun hashPassword(password: String): String {
    val saltBytes = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
    val salt = saltBytes.joinToString("") { "%02x".format(it) }
    return "$salt:${sha256Hex(salt + password)}"
}

/**
 * Verifies [password] against a [storedHash] produced by [hashPassword]. Also accepts the
 * legacy unsalted format (a bare SHA-256 hex digest, no ":") for accounts created before
 * salting was added, so existing logins keep working without a forced re-registration.
 */
fun verifyPassword(password: String, storedHash: String): Boolean {
    val separatorIndex = storedHash.indexOf(':')
    if (separatorIndex < 0) {
        // Legacy unsalted hash.
        return sha256Hex(password) == storedHash
    }
    val salt = storedHash.substring(0, separatorIndex)
    val expectedHash = storedHash.substring(separatorIndex + 1)
    return sha256Hex(salt + password) == expectedHash
}

private fun sha256Hex(value: String): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(value.toByteArray())
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
    const val KEY_CONTRIBUTE_DATA = "contribute_data"
    const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val KEY_IS_GUEST = "is_guest"
}