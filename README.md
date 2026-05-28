# DermaLens

An Android skin disease detection app built with Jetpack Compose. DermaLens lets users scan their skin, track conditions over time, and find nearby dermatology clinics — developed as a Capstone Project at Tarlac State University, 2026.

---

## Features

- **Skin Scan** — Capture via camera or pick from gallery; AI detects condition, severity, and confidence
- **Scan History & Progress Tracker** — Timeline view per condition with trend indicators (improving / worsening / stable)
- **Care Guide** — Detailed skincare routines, dos/don'ts, and treatment options for 6 common skin conditions
- **Clinic Locator** — GPS-based map (OpenStreetMap + OSRM routing) to find nearby dermatology clinics
- **Contribute to Research** — Opt-in feature to anonymously share scan data for model retraining (Filipino skin tone focus)
- **Accessibility** — Font size slider, high contrast mode across all screens
- **Privacy Policy** — Full in-app privacy policy dialog

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation |
| Database | Room (SQLite) |
| Camera | CameraX (`PreviewView.ImplementationMode.COMPATIBLE`) |
| Gallery | `ActivityResultContracts.PickVisualMedia` (Android 13+) |
| Image loading | Coil (`AsyncImage`) |
| Maps | OSMDroid + OSRM routing API |
| AI Model | YOLOv11 TFLite *(pending integration)* |
| Auth | SHA-256 password hashing + SharedPreferences session |

## Project Structure

```
app/src/main/java/com/dermalens/app/
├── data/
│   ├── db/          # Room database, DAOs
│   └── model/       # ScanRecord, User entities
├── navigation/      # NavGraph, Screen sealed class
└── ui/
    ├── screens/     # All screen composables
    │   ├── Screens.kt            # Login + Register
    │   ├── HomeScreen.kt         # Home + bottom nav bar
    │   ├── CameraScreen.kt       # Camera + gallery
    │   ├── ScanResultScreen.kt   # Detection result + save
    │   ├── ProgressTrackerScreen.kt
    │   ├── CareGuideScreen.kt
    │   ├── ClinicLocatorScreen.kt
    │   └── ProfileScreen.kt      # Profile + EditProfile
    ├── AppSettings.kt    # Font scale + high contrast state
    └── DermaColors.kt    # Colors, DermaPrefs, hashPassword
```

## Building & Running

1. Clone the repo
2. Open in Android Studio (Hedgehog or later)
3. Let Gradle sync complete
4. Run on a device or emulator (API 26+)

> VS Code will show "Unresolved reference" errors on Compose/Room imports — these are fake and disappear after Gradle sync in Android Studio.

## Pending

- [ ] YOLOv11 TFLite model integration (replaces `mockDetectionResults.random()` in `ScanResultScreen.kt:76`)
- [ ] Model training with Filipino skin tone dataset (Fitzpatrick III–IV)

## Team

- Mark Joseph Garcia
- Reynaldo Manio Jr.
- Reicee Owen Pastrana
- Chrisent Dayniel Tolentino

*Tarlac State University — Capstone 2026*
