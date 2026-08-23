package com.dermalens.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.dermalens.app.navigation.DermaLensNavGraph
import com.dermalens.app.ui.AppSettings
import com.dermalens.app.ui.LocalAppSettings
import com.dermalens.app.ui.screens.DermaPrefs
import com.dermalens.app.ui.theme.DermaLensTheme
import com.dermalens.app.worker.NotificationScheduler

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && remindersEnabled()) {
            NotificationScheduler.scheduleDailyReminder(this)
        }
    }

    private fun remindersEnabled(): Boolean =
        getSharedPreferences(DermaPrefs.PREFS_NAME, MODE_PRIVATE)
            .getBoolean(DermaPrefs.KEY_NOTIFICATIONS_ENABLED, true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else if (remindersEnabled()) {
                NotificationScheduler.scheduleDailyReminder(this)
            }
        } else if (remindersEnabled()) {
            NotificationScheduler.scheduleDailyReminder(this)
        }

        setContent {
            val prefs = remember { getSharedPreferences(DermaPrefs.PREFS_NAME, MODE_PRIVATE) }

            var fontScale by remember { mutableStateOf(prefs.getFloat(DermaPrefs.KEY_FONT_SIZE, 1.0f)) }
            var highContrast by remember { mutableStateOf(prefs.getBoolean(DermaPrefs.KEY_HIGH_CONTRAST, false)) }

            DisposableEffect(Unit) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        DermaPrefs.KEY_FONT_SIZE -> fontScale = prefs.getFloat(DermaPrefs.KEY_FONT_SIZE, 1.0f)
                        DermaPrefs.KEY_HIGH_CONTRAST -> highContrast = prefs.getBoolean(DermaPrefs.KEY_HIGH_CONTRAST, false)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            val appSettings = AppSettings(fontScale = fontScale, highContrast = highContrast)

            CompositionLocalProvider(LocalAppSettings provides appSettings) {
                DermaLensTheme {
                    DermaLensNavGraph()
                }
            }
        }
    }
}