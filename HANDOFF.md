# DermaLens — Developer Handoff

Last updated: May 2026  
Branch: `master`  
Status: Feature-complete (except YOLOv11 inference)

---

## What's Done

### Auth & Session
- Login with email + SHA-256 hashed password
- Register with validation (name, email, password confirmation)
- Session persisted in `SharedPreferences` (`KEY_IS_LOGGED_IN`, `KEY_USER_EMAIL`)
- "Remember me" checkbox saves email for next login
- Auto-redirect to Home if already logged in (SplashScreen checks)

### Camera & Gallery
- CameraX with `PreviewView.ImplementationMode.COMPATIBLE` (TextureView — prevents SurfaceView Z-order bleed-through on EditProfile overlap)
- Gallery via `ActivityResultContracts.PickVisualMedia()` — no storage permission needed on Android 13+
- Selected gallery image previews inside the camera frame before "scan"
- `READ_EXTERNAL_STORAGE` has `maxSdkVersion="32"` in manifest; `READ_MEDIA_IMAGES` for API 33+

### Scan Result
- Detection result currently uses `mockDetectionResults.random()` — **this is the YOLOv11 placeholder**
- Image URI passed from CameraScreen via navigation argument (URL-encoded nullable string)
- Image displayed in the result banner using Coil `AsyncImage`
- "Save to History" button saves `ScanRecord` to Room DB with the logged-in user's ID
- If "Contribute to Research" is enabled, scan image is copied from the content URI to `filesDir/contributed_scans/` (permanent internal storage) before saving

### Progress Tracker
- Loads real scans from Room DB grouped by condition
- Timeline with trend indicators per scan entry (better/worse/stable)
- Delete scan: trash icon on each entry → confirmation dialog → `deleteScan(id)` → `refreshKey++` triggers re-fetch
- `LaunchedEffect(refreshKey)` pattern — also fires fresh on every navigation entry

### Care Guide
- 6 conditions: Acne Vulgaris, Atopic Dermatitis, Melasma, Tinea, Warts, Scabies
- Each has: Overview, Routine, Dos, Don'ts, Treatments
- Tab-based navigation between conditions

### Clinic Locator
- Real GPS location via `FusedLocationProviderClient`
- OSMDroid map with custom markers for clinics
- OSRM routing API draws a route polyline to selected clinic
- Hardcoded clinic list (real addresses in Tarlac area)

### Profile
- Loads real user data (name, email, scan stats) from Room DB
- Edit Profile: update name, email, or password (current password required for password change)
- Contribute to Research toggle: saves `KEY_CONTRIBUTE_DATA` to SharedPreferences
- Privacy Policy dialog: 8 sections, scrollable
- About DermaLens dialog: version + team names
- Accessibility: font size slider (4 steps: Small → XL), high contrast toggle
- Logout clears `KEY_IS_LOGGED_IN`

### High Contrast Mode
All screens respect `settings.highContrast`:
- Backgrounds: `Color.White` instead of `Color(0xFFF8F9FA)`
- Cards: light gray bg + 1dp black border, no elevation
- Dividers, text fields, switches all use high contrast variants

### Dialogs (all consistent)
Every `AlertDialog` in the app has:
```kotlin
containerColor = Color.White
titleContentColor = Color(0xFF111827)
textContentColor = Color(0xFF374151)
```
Dialogs: Logout, Privacy Policy (Profile), Privacy Policy (Register), About, Delete Scan

---

## What's Pending

### YOLOv11 TFLite Integration
The scaffold is in place and wired up — this is now just a "drop the model in" step, not a code-writing step.

**What's already done:**
- `ScanResultScreen.kt` uses `produceState` to run inference off the main thread and shows an "Analyzing your scan..." spinner while it resolves, instead of blocking on `mockDetectionResults.random()` directly
- `ml/YoloDetector.kt` has `runYoloInference(context, imageUri): DetectionResult?` — returns `null` (safe fallback to mock) if no model file is bundled yet, the image can't be read, or inference throws
- TFLite Gradle dependencies were already present in `build.gradle.kts`; added `androidResources { noCompress += "tflite" }` so the model file isn't corrupted by AAPT compression on mmap-load
- Output parsing in `YoloDetector.kt` auto-adapts to either a plain classifier `[1, numClasses]` shape or a YOLO detection-head shape (`[1, 4+numClasses, numBoxes]` or transposed) by taking the max per-class score across boxes — reasonable since this screen shows a whole-image diagnosis, not bounding boxes

**Steps to finish integration once your model is trained:**
1. Export YOLOv11 to `.tflite`
2. Drop it into `app/src/main/assets/model.tflite`
3. In `ml/YoloDetector.kt`, update the three `TODO`s at the top:
   - `MODEL_FILE_NAME` if you used a different filename
   - `INPUT_SIZE` to match your export's actual input resolution
   - `CLASS_LABELS` to match your training class order exactly (note the 9-class target mentioned in the Sprint 2 AI/ML note above — the placeholder only lists the original 6 Care Guide conditions)
4. Run a real scan and check `interpreter.getOutputTensor(0).shape()` (log it if unsure) against the assumptions in `bestClass()` — adjust if your export shape doesn't match either pattern

The `DetectionResult` data class (`ScanResultScreen.kt`) is unchanged:
```kotlin
data class DetectionResult(
    val condition: String,
    val confidence: Float,
    val severity: String,
    val description: String,
    val symptoms: List<String>,
    val recommendation: String,
    val color: Color
)
```

---

## Key Files Reference

| File | What it does |
|---|---|
| `DermaColors.kt` | Color constants, `DermaPrefs` keys, `hashPassword()` |
| `AppSettings.kt` | `AppSettings` data class with font/contrast helpers |
| `NavGraph.kt` | All routes; `ScanResult` takes optional `imageUri` nav argument |
| `DermaDatabase.kt` | Room DB v3, `fallbackToDestructiveMigration()` |
| `ScanRecord.kt` | Entity: `id, userId, condition, confidence, severity, notes, scanDate, imagePath, contributedForTraining` |
| `User.kt` | Entity: `userId, fullName, email, passwordHash, createdAt` |
| `ScanRecordDao.kt` | `insertScan`, `getScansByUser` (Flow), `getScansByUserOnce`, `deleteScan`, etc. |

## SharedPreferences Keys (`DermaPrefs`)

| Key | Type | Purpose |
|---|---|---|
| `KEY_IS_LOGGED_IN` | Boolean | Session gate on SplashScreen |
| `KEY_USER_EMAIL` | String | Used to look up user in DB |
| `KEY_REMEMBER_EMAIL` | String | Pre-fills email on LoginScreen |
| `KEY_FONT_SIZE` | Float | `0.75f` – `1.5f` font scale |
| `KEY_HIGH_CONTRAST` | Boolean | High contrast mode toggle |
| `KEY_CONTRIBUTE_DATA` | Boolean | Opt-in research data collection |

## DB Version History

| Version | Change |
|---|---|
| 1 | Initial schema |
| 2 | Added fields to User |
| 3 | Added `imagePath` + `contributedForTraining` to ScanRecord |

Uses `fallbackToDestructiveMigration()` — DB is dropped and recreated on version bump (fine for dev/capstone).

## Known Non-Issues (VS Code)

VS Code shows "Unresolved reference: androidx" on every import in Kotlin files. **These are fake.** The project builds fine in Android Studio after Gradle sync. Do not add workarounds for these errors.

---

*DermaLens — Tarlac State University Capstone 2026*
