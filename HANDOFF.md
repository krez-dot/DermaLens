# DermaLens — Developer Handoff

Last updated: August 2026
Branch: `master`
Status: Feature-complete. YOLOv11 inference pipeline is built and verified working end-to-end with a single-class test model — only the final merged multi-class model is still pending.

---

## What's Done

### Auth & Session
- Login with email + salted SHA-256 hashed password (`saltHex:hashHex` format, `verifyPassword()` in `DermaColors.kt`; backward-compatible with pre-salt accounts via a legacy unsalted-hash fallback path)
- Register with validation (name, email, password confirmation) + duplicate-email check with a `try/catch` safety net against the race
- Session persisted in `SharedPreferences` (`KEY_IS_LOGGED_IN`, `KEY_USER_EMAIL`)
- "Remember me" checkbox saves email for next login
- Auto-redirect to Home if already logged in (SplashScreen checks)

### Camera & Gallery
- CameraX with `PreviewView.ImplementationMode.COMPATIBLE` (TextureView — prevents SurfaceView Z-order bleed-through on EditProfile overlap)
- Real `ImageCapture` use case (1280x1280 target resolution, `CAPTURE_MODE_MINIMIZE_LATENCY`) — the shutter button actually takes a photo now; it used to just fake a 2-second delay and navigate with a null image URI
- Pinch-to-zoom on the live camera preview (real CameraX `setZoomRatio`, not just a decorative guide box)
- Gallery via `ActivityResultContracts.PickVisualMedia()` — no storage permission needed on Android 13+
- Selected gallery image is shown in full (`ContentScale.Fit`) with pan + pinch-to-zoom so the user can position it; on capture, the photo is cropped to exactly what's inside the guide frame (`cropGalleryImageToFrame()` in `CameraScreen.kt` — mirrors the display transform as an `android.graphics.Matrix`, inverts it, maps the guide box's screen corners back into source-bitmap pixel space)
- `READ_EXTERNAL_STORAGE` has `maxSdkVersion="32"` in manifest; `READ_MEDIA_IMAGES` for API 33+

### Scan Result
- Detection result defaults to `mockDetectionResults.random()` when no `.tflite` model is bundled (the normal case — model binaries are gitignored, see the YOLOv11 section below) via `runYoloInference()` in `ml/YoloDetector.kt`, which returns `null` on any failure so the caller falls back safely
- `ScanResultScreen.kt` uses `produceState` to run inference off the main thread, showing an "Analyzing your scan..." spinner while it resolves
- Image URI passed from CameraScreen via navigation argument (URL-encoded nullable string)
- Image displayed in the result banner using Coil `AsyncImage`
- "Save to History" button saves `ScanRecord` to Room DB with the logged-in user's ID
- If "Contribute to Research" is enabled, scan image is copied from the content URI to `filesDir/contributed_scans/` (permanent internal storage) before saving — nothing is uploaded anywhere yet (no upload code exists)

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
- Live results from OSM's Overpass API (`healthcare:speciality=dermatology` exact tag match, 15km radius) — no hardcoded clinic list anymore. Shows an explicit "No dermatology clinics found nearby" state if genuinely nothing's in range, instead of substituting fake data
- A regex-based name search (to also catch clinics with "skin"/"derma" in the name but not properly tagged) was tried and removed — it consistently timed out on Overpass's public instance (confirmed across several query shapes, 25-34s) and was silently contributing zero results while risking dragging down the reliable exact-tag query if bundled together
- OSMDroid map with custom markers for clinics
- OSRM routing API draws a route polyline to selected clinic

### Profile
- Loads real user data (name, email, scan stats) from Room DB
- Edit Profile: update name, email, or password (current password required for password change)
- Contribute to Research toggle: saves `KEY_CONTRIBUTE_DATA` to SharedPreferences
- Scan Reminders toggle: saves `KEY_NOTIFICATIONS_ENABLED` to SharedPreferences and actually calls `NotificationScheduler.scheduleDailyReminder()`/`cancelReminder()` — it used to be pure local UI state that reset to ON on every visit and didn't affect anything
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
**Verified working end-to-end on a physical device** with a real single-class Melasma test model — this is genuinely just a "drop the final model in" step now, not a code-writing or debugging step. Two real bugs got found and fixed along the way (both described below), so a from-scratch model should work without surprises.

**What's already done and tested:**
- `ScanResultScreen.kt` uses `produceState` to run inference off the main thread and shows an "Analyzing your scan..." spinner while it resolves, instead of blocking on `mockDetectionResults.random()` directly
- `ml/YoloDetector.kt` has `runYoloInference(context, imageUri): DetectionResult?` — returns `null` (safe fallback to mock) if no model file is bundled yet, the image can't be read, or inference throws
- TFLite Gradle dependencies were already present in `build.gradle.kts`; added `androidResources { noCompress += "tflite" }` so the model file isn't corrupted by AAPT compression on mmap-load
- Output parsing in `YoloDetector.kt` auto-adapts to either a plain classifier `[1, numClasses]` shape or a YOLO detection-head shape (`[1, 4+numClasses, numBoxes]` or transposed) by taking the max per-class score across boxes — reasonable since this screen shows a whole-image diagnosis, not bounding boxes
- **Input tensor layout is auto-detected**, not assumed. The tested model turned out to export as channels-first `[1, 3, 640, 640]` (NCHW) rather than the usual mobile/TFLite channels-last `[1, 640, 640, 3]` (NHWC) — its input tensor is even named `serving_default_args_0`, a generic SavedModel signature rather than Ultralytics' typical friendly naming, suggesting a slightly different export path was used. Feeding it NHWC-ordered pixel data crashed with `Cannot copy to a TensorFlowLite tensor... 4915200 bytes from a Java Buffer with 23040 bytes` — a real, confusing failure mode if you hit it blind. The code now checks which axis equals 3 and writes pixel data in whichever order (planar NCHW vs. interleaved NHWC) the model actually expects.
- **Input size is read from the model itself** (`interpreter.getInputTensor(0).shape()`), not hardcoded — no `INPUT_SIZE` constant to keep in sync anymore.
- **Preprocessing is a plain stretch-to-square resize, not letterboxed.** Both were tested against the real model: stretching gave ~40-78% confidence depending on image quality, letterboxing (aspect-preserving resize + gray padding, the more "textbook" YOLO approach) dropped confidence to ~9% on the same photo. That strongly suggests the training data was resized by stretching, not letterboxed — likely Roboflow's default "Resize: Stretch" preprocessing option (HANDOFF/README note a Kaggle/Roboflow dataset). **If the final multi-class model behaves differently, this is the first thing to re-test** — try both and compare confidence on a known-good photo.

**Steps to finish integration once the final merged model is ready:**
1. Export YOLOv11 to `.tflite` (`model.export(format='tflite', imgsz=<your training size>)` in the same Colab notebook you trained in)
2. Drop it into `app/src/main/assets/` — model files are gitignored (`*.tflite`), so this is a local-only step, share the file via Drive/Colab
3. In `ml/YoloDetector.kt`, update:
   - `MODEL_FILE_NAME` to match your filename
   - `CLASS_LABELS` to your final training class order exactly (note the 9-class target mentioned in the Sprint 2 AI/ML note in `README.md` — the current placeholder is just `["Melasma"]` for the single-class test)
4. Run a real scan and check logcat (tag `DermaLens`) for the `YOLO input=... output=...` line — confirms the layout auto-detection picked the right axes and shows the real output shape, useful to sanity-check against `bestClass()`'s assumptions if results look off

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
| `DermaColors.kt` | Color constants, `DermaPrefs` keys, `hashPassword()` / `verifyPassword()` (salted SHA-256) |
| `AppSettings.kt` | `AppSettings` data class with font/contrast helpers |
| `NavGraph.kt` | All routes; `ScanResult` takes optional `imageUri` nav argument |
| `DermaDatabase.kt` | Room DB v3, `fallbackToDestructiveMigration()` |
| `ScanRecord.kt` | Entity: `id, userId, condition, confidence, severity, notes, scanDate, imagePath, contributedForTraining` |
| `User.kt` | Entity: `userId, fullName, email, passwordHash, createdAt` |
| `ScanRecordDao.kt` | `insertScan`, `getScansByUser` (Flow), `getScansByUserOnce`, `deleteScan`, etc. |
| `ml/YoloDetector.kt` | `runYoloInference()` — model loading, layout-aware preprocessing, output parsing. See the YOLOv11 section above for what's tuned and why. |

## SharedPreferences Keys (`DermaPrefs`)

| Key | Type | Purpose |
|---|---|---|
| `KEY_IS_LOGGED_IN` | Boolean | Session gate on SplashScreen |
| `KEY_USER_EMAIL` | String | Used to look up user in DB |
| `KEY_REMEMBER_EMAIL` | String | Pre-fills email on LoginScreen |
| `KEY_FONT_SIZE` | Float | `0.75f` – `1.5f` font scale |
| `KEY_HIGH_CONTRAST` | Boolean | High contrast mode toggle |
| `KEY_CONTRIBUTE_DATA` | Boolean | Opt-in research data collection |
| `KEY_NOTIFICATIONS_ENABLED` | Boolean | Scan Reminders toggle, defaults true |

## DB Version History

| Version | Change |
|---|---|
| 1 | Initial schema |
| 2 | Added fields to User |
| 3 | Added `imagePath` + `contributedForTraining` to ScanRecord |

Uses `fallbackToDestructiveMigration()` — DB is dropped and recreated on version bump (fine for dev/capstone, but a real `Migration` should replace this before any schema change close to a demo — see `SECURITY_TESTING.md` SEC-F4). As of this sprint the database is also excluded from Android device/cloud backup (`backup_rules.xml`, `data_extraction_rules.xml`) so account data doesn't leave the device via `adb backup` or auto cloud-backup.

## Known Non-Issues (VS Code)

VS Code shows "Unresolved reference: androidx" on every import in Kotlin files. **These are fake.** The project builds fine in Android Studio after Gradle sync. Do not add workarounds for these errors.

---

*DermaLens — Tarlac State University Capstone 2026*
