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

## Development Timeline
CP2 Development Plan — May – November 2026

| Sprint | Duration | Track | Sprint Goal | Key Deliverables | Technologies | Lead | Status | Priority |
|---|---|---|---|---|---|---|---|---|
| Sprint 1 | May 25 – Jun 7, 2026 | App Development | Project setup, user authentication (register/login/logout), initial UI scaffolding | Login & register screens; auth flow; GitHub repo with project structure | Android Studio, Kotlin, Jetpack Compose, Firebase Auth / Room DB | Chrisent Dayniel | ✅ Done | 🔴 Critical |
| Sprint 2 | Jun 8 – Jun 21, 2026 | App Dev + AI | CameraX real-time integration; start YOLOv11 model training on Google Colab | Working camera capture screen; initial YOLOv11 training pipeline; preliminary model weights | CameraX, Camera2 API, Python, Ultralytics YOLO, Kaggle/Roboflow dataset | Reynaldo | 🔄 In Progress | 🔴 Critical |
| Sprint 3 | Jun 22 – Jul 5, 2026 | AI + Integration | Fine-tune YOLOv11, convert to TFLite, integrate on-device inference into the app | Optimized .tflite model in APK; real-time detection screen with bounding box + confidence score | TensorFlow Lite, GPU/NNAPI delegates, Ultralytics YOLO export, Google Colab T4 | Mark Joseph | ⬜ Not started | 🔴 Critical |
| Sprint 4 | Jul 6 – Jul 19, 2026 | App Development | Detection result screen, skincare guidance content, Room DB for scan history | Complete result screen; skincare guide for all 6 conditions; working Room DB schema | Jetpack Compose, Room DB, SQLite, pre-built knowledge base JSON | Reicee Owen | ✅ Done | 🟠 High |
| Sprint 5 | Jul 20 – Aug 2, 2026 | App Development | Progress tracking dashboard, clinic locator via Google Maps, scan reminders | Progress tracker with charts; clinic locator with directions; WorkManager notifications | Google Maps SDK, Places API, WorkManager, MPAndroidChart / Compose Charts | Chrisent Dayniel | ✅ Done | 🟠 High |
| Sprint 6 | Aug 3 – Aug 16, 2026 | Testing | Full system integration, functional testing (FR1–FR11), performance testing, survey | Stable DermaLens APK; functional + performance test results; 100-respondent survey data | Android Profiler, Redmi Note 12 (Snapdragon 685, 6GB RAM), Likert scale questionnaire | All Members | ⬜ Not started | 🟠 High |
| Sprint 7 | Aug 17 – Aug 30, 2026 | Bug Fixing | Resolve bugs from testing; UI/UX polish; start Chapter 5 documentation | Refined APK; resolved bug report; Chapter 5 draft; updated methodology docs | Android Studio Debugger, Compose Previews | Mark Joseph | 🔄 In Progress | 🟡 Medium |
| Sprint 8 | Aug 31 – Sep 27, 2026 | Documentation | Final documentation, complete all chapters, defense preparation | Final capstone paper (all chapters); defense slides; submitted manuscript; archived APK | Google Docs / MS Word, PowerPoint / Canva | All Members | ⬜ Not started | 🟡 Medium |
| Post-Sprint | Oct – Nov 2026 | Wrap-up | Address panel feedback, finalize approved manuscript, archive project repository | Revised approved manuscript; archived repo; all submission requirements fulfilled | GitHub, Google Drive | All Members | ⬜ Not started | 🟢 Low |

> **AI/ML progress note (Sprint 2):** 4 of 9 skin condition classes annotated and trained as isolated single-class YOLOv11 models (Melasma, Eczema, Acne + 1 more). Multi-class merge pending resolution of annotation consistency issues before full 9-class training.

## Known Limitations
*As of now — to be updated as development progresses.*

Quick list of what's real vs. not real in the app right now.

| Feature | Is it real? |
|---|---|
| Clinic Locator | Real — location + live Overpass API results; shows an honest empty state if none found nearby |
| Skin Scan Results | Fake — random result every time |
| Progress Tracker | Fake — sample data only |

- **Clinic Locator** — Location detection works fine, and clinic results come from the OSM Overpass API. If no dermatology clinics are found within 15 km (like in Capas), the app now shows an explicit "No dermatology clinics found nearby" state instead of silently substituting fake ones.
- **Skin Scan Results** — Scanning skin doesn't actually analyze anything yet. It just randomly picks a result from a fixed list. This is the big one still needing a fix — it'll be real once the YOLOv11 model is plugged in.
- **Progress Tracker** — The progress/history charts shown are made-up sample data (fake conditions, fake dates, fake scores). It's not pulling real scan history yet.

**What IS working properly:** Login / Register / Logout, saving scans to the database (storage works, just not fed real results yet), Care Guide info pages.

## Team
- Mark Joseph Garcia
- Reynaldo Manio Jr.
- Reicee Owen Pastrana
- Chrisent Dayniel Tolentino

*Tarlac State University — Capstone 2026*
