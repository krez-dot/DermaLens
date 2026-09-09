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
import com.google.android.gms.maps.MapsInitializer
import com.dermalens.app.navigation.DermaLensNavGraph
import com.dermalens.app.ui.AppSettings
import com.dermalens.app.ui.LocalAppSettings
import com.dermalens.app.ui.screens.DermaPrefs
import com.dermalens.app.ui.theme.DermaLensTheme
import com.dermalens.app.worker.ContributionUploadScheduler
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

        // Kicks off Maps SDK's internal renderer setup as early as possible so it's ready by
        // the time the user reaches Clinic Locator -- BitmapDescriptorFactory (used for custom
        // map markers) throws a NullPointerException if called before this has run at least once.
        MapsInitializer.initialize(this)

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

        // Re-arms the contribution upload worker on every launch, not just when the toggle is
        // flipped -- covers users who already had this enabled from before the upload pipeline
        // existed (enqueueUniquePeriodicWork with KEEP is a no-op if it's already scheduled).
        if (getSharedPreferences(DermaPrefs.PREFS_NAME, MODE_PRIVATE).getBoolean(DermaPrefs.KEY_CONTRIBUTE_DATA, false)) {
            ContributionUploadScheduler.scheduleUpload(this)
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