package com.dermalens.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.dermalens.app.navigation.DermaLensNavGraph
import com.dermalens.app.ui.AppSettings
import com.dermalens.app.ui.LocalAppSettings
import com.dermalens.app.ui.screens.DermaPrefs
import com.dermalens.app.ui.theme.DermaLensTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = remember { getSharedPreferences(DermaPrefs.PREFS_NAME, MODE_PRIVATE) }

            var fontScale by remember { mutableStateOf(prefs.getFloat(DermaPrefs.KEY_FONT_SIZE, 1.0f)) }
            var highContrast by remember { mutableStateOf(prefs.getBoolean(DermaPrefs.KEY_HIGH_CONTRAST, false)) }

            // Listen for preference changes
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