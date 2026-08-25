# DermaLens
An Android skin disease detection app built with Jetpack Compose. DermaLens lets users scan their skin, track conditions over time, and find nearby dermatology clinics — developed as a Capstone Project at Tarlac State University, 2026.

---

## Features
- **Skin Scan** — Capture via camera (pinch-to-zoom) or pick from gallery (pan + pinch-to-zoom to position, cropped to exactly what's in the guide frame before scanning); AI detects condition, severity, and confidence, with real bounding boxes (multi-region, NMS-filtered) drawn on the full result image
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
| AI Model | YOLOv11 TFLite — pipeline verified working end-to-end on-device with a single-class test model; auto-detects channels-first/-last tensor layout; final multi-class model still pending |
| Auth | Firebase Authentication (email/password) — registration required, no guest/offline path |

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

> **Want to help code?** See [SETUP.md](SETUP.md) for the full onboarding steps, including how to get `google-services.json` (needed to build — it's not in the repo, see below).

## Firebase Auth
Registered accounts (Login/Register) go through real Firebase Authentication — a new account is a real, Firebase-Console-visible user, gets a real verification email, and password reset goes through Firebase's real "Forgot password?" email flow. Registration is required to use the app — there is no guest/offline-account path. Everything else (scan history, profile stats) stays fully local in Room DB.

> Guest mode was implemented and briefly live on 2026-08-24, then removed on 2026-08-25 per the team's tech adviser. See "Guest Mode — Removed" in `FIREBASE_AUTH_PLAN.md` for what changed and why, if you're wondering where it went.

Full rationale, the code-change list, and the capstone-manuscript impact (Scope & Limitations, IC1/IC2/SS1/SS3, Table 4, Figure 3, Privacy Policy text) are written up in [FIREBASE_AUTH_PLAN.md](FIREBASE_AUTH_PLAN.md) — read that before touching this area or updating the paper.

`app/google-services.json` is required to build but is **gitignored** (it's tied to the Firebase project). Ask Mark Joseph for a copy, or get added to the Firebase project and download your own from the Console.

**Live-verified so far:** Register (real Firebase account + verification email + local profile), Login (Firebase auth + resolves matching local profile), Logout (correctly signs out of Firebase too — this was a real bug, fixed), Change Password (Edit Profile → Account Security, reauthenticate + update, confirmed via a live "Saved!" success state).

## For Contributors — Known Gaps / Good First Issues
Not urgent, left for later. Good entry points if you want to help:
- **Email change isn't implemented.** Edit Profile can change your name but the email field is read-only. Firebase requires a `verifyBeforeUpdateEmail()` flow (sends a confirmation link to the *new* address) — deliberately scoped out of the initial Firebase pass, see the "Open Questions" section of `FIREBASE_AUTH_PLAN.md`.
- **"Forgot password?" email flow is implemented but not yet live-tested end-to-end** (i.e. actually clicking the reset link from a real inbox and confirming login with the new password works). Worth a pass.
- **Firebase BoM is pinned to `33.5.1`** in `app/build.gradle.kts` (see the comment there) because newer BoMs need Kotlin 2.3.0 and this project is pinned to Kotlin 2.0.21. Don't bump it without bumping Kotlin first, or the build breaks with a metadata-version mismatch.

## Development Timeline
CP2 Development Plan — May – November 2026

| Sprint | Duration | Track | Sprint Goal | Key Deliverables | Technologies | Lead | Status | Priority |
|---|---|---|---|---|---|---|---|---|
| Sprint 1 | May 25 – Jun 7, 2026 | App Development | Project setup, user authentication (register/login/logout), initial UI scaffolding | Login & register screens; auth flow; GitHub repo with project structure | Android Studio, Kotlin, Jetpack Compose, Firebase Auth / Room DB | Chrisent Dayniel | ✅ Done | 🔴 Critical |
| Sprint 2 | Jun 8 – Jun 21, 2026 | App Dev + AI | CameraX real-time integration; start YOLOv11 model training on Google Colab | Working camera capture screen; initial YOLOv11 training pipeline; preliminary model weights | CameraX, Camera2 API, Python, Ultralytics YOLO, Kaggle/Roboflow dataset | Reynaldo | 🔄 In Progress | 🔴 Critical |
| Sprint 3 | Jun 22 – Jul 5, 2026 | AI + Integration | Fine-tune YOLOv11, convert to TFLite, integrate on-device inference into the app | Optimized .tflite model in APK; real-time detection screen with bounding box + confidence score | TensorFlow Lite, GPU/NNAPI delegates, Ultralytics YOLO export, Google Colab T4 | Mark Joseph | 🔄 In Progress | 🔴 Critical |
| Sprint 4 | Jul 6 – Jul 19, 2026 | App Development | Detection result screen, skincare guidance content, Room DB for scan history | Complete result screen; skincare guide for all 6 conditions; working Room DB schema | Jetpack Compose, Room DB, SQLite, pre-built knowledge base JSON | Reicee Owen | ✅ Done | 🟠 High |
| Sprint 5 | Jul 20 – Aug 2, 2026 | App Development | Progress tracking dashboard, clinic locator via Google Maps, scan reminders | Progress tracker with charts; clinic locator with directions; WorkManager notifications | Google Maps SDK, Places API, WorkManager, MPAndroidChart / Compose Charts | Chrisent Dayniel | ✅ Done | 🟠 High |
| Sprint 6 | Aug 3 – Aug 16, 2026 | Testing | Full system integration, functional testing (FR1–FR11), performance testing, survey | Stable DermaLens APK; functional + performance test results; 100-respondent survey data | Android Profiler, Redmi Note 12 (Snapdragon 685, 6GB RAM), Likert scale questionnaire | All Members | ⬜ Not started | 🟠 High |
| Sprint 7 | Aug 17 – Aug 30, 2026 | Bug Fixing | Resolve bugs from testing; UI/UX polish; start Chapter 5 documentation | Refined APK; resolved bug report; Chapter 5 draft; updated methodology docs | Android Studio Debugger, Compose Previews | Mark Joseph | 🔄 In Progress | 🟡 Medium |
| Sprint 8 | Aug 31 – Sep 27, 2026 | Documentation | Final documentation, complete all chapters, defense preparation | Final capstone paper (all chapters); defense slides; submitted manuscript; archived APK | Google Docs / MS Word, PowerPoint / Canva | All Members | ⬜ Not started | 🟡 Medium |
| Post-Sprint | Oct – Nov 2026 | Wrap-up | Address panel feedback, finalize approved manuscript, archive project repository | Revised approved manuscript; archived repo; all submission requirements fulfilled | GitHub, Google Drive | All Members | ⬜ Not started | 🟢 Low |

> **AI/ML progress note (Sprint 2):** 4 of 9 skin condition classes annotated and trained as isolated single-class YOLOv11 models (Melasma, Eczema, Acne + 1 more). Multi-class merge pending resolution of annotation consistency issues before full 9-class training.
>
> **AI/ML progress note (Sprint 7):** The Melasma single-class model was exported to TFLite and tested end-to-end on a real device — correctly classified a real melasma photo at 77.7% confidence. This confirmed the app-side integration works and surfaced two real export details worth knowing for the other classes: (1) this export uses a channels-first `[1,3,H,W]` tensor layout rather than the usual channels-last `[1,H,W,3]` — the app now auto-detects either; (2) the model expects a plain stretch-to-square resize, not letterboxing — consistent with Roboflow's default "Resize: Stretch" preprocessing. See `HANDOFF.md` for the integration steps once the merged multi-class model is ready.
>
> **AI/ML progress note (Sprint 7, cont'd):** An audit against Chapter 4's FR7 ("visual overlay on the captured image") found `bestClass()` was discarding box coordinates entirely — fixed to extract real boxes, filtered with confidence thresholding + NMS (not just the single best box). Verified live on-device: correctly drew two separate boxes on a bilateral melasma photo (both cheeks), where the single-box version would have silently hidden the second affected region.

## Known Limitations
*As of now — to be updated as development progresses.*

Quick list of what's real vs. not real in the app right now.

| Feature | Is it real? |
|---|---|
| Clinic Locator | Real — location + live Overpass API results; shows an honest empty state if none found nearby |
| Skin Scan Results | Fake by default (random result) since no model ships in the repo yet — but the real pipeline is built and verified working locally with a single-class test model |
| Progress Tracker | Real — pulls actual scan history from Room DB, grouped by condition with trend indicators |

- **Clinic Locator** — Location detection works fine, and clinic results come from the OSM Overpass API (exact `healthcare:speciality=dermatology` tag match only — a regex name search was tried and removed after confirming it consistently times out on Overpass's public instance regardless of query shape, silently contributing zero results). If no dermatology clinics are found within 15 km (like in Capas), the app shows an explicit "No dermatology clinics found nearby" state instead of silently substituting fake ones. Clinic "ratings" were removed entirely rather than kept as a fabricated 4.5-star placeholder — OSM/Overpass has no ratings data. The screen also checks real connectivity (`ConnectivityManager`) and shows a distinct "No Internet Connection" state with Retry, instead of a dead network looking identical to "no clinics nearby."
- **Skin Scan Results** — Ships with `mockDetectionResults.random()` as the default, since no `.tflite` model is committed to the repo (large binaries belong in Drive/Colab, not git — see `.gitignore`). The real inference path (`ml/YoloDetector.kt`) is fully implemented and was verified end-to-end on a physical device with a real single-class Melasma model: correct classification, sensible confidence (77.7% on a clean clinical photo), and real multi-region bounding boxes (NMS-filtered, not just a single best box — verified catching both cheeks on a bilateral melasma photo, not just one). Drop a `.tflite` into `app/src/main/assets/` and update the class list to go live — see `HANDOFF.md`. The live camera now actually captures a photo via `ImageCapture` with pinch-to-zoom (previously it only faked a delay and never took a picture), and gallery-picked photos can be panned/zoomed and get cropped to exactly the guide frame before scanning.
- **Camera permission denial** — If a user permanently denies camera access ("Don't ask again"), the app detects this (`shouldShowRequestPermissionRationale`) and offers a real "Open Settings" button instead of retrying an in-app dialog Android will never show again.
- **Progress Tracker empty state** — Matches the paper's storyboard now (Figure 22 / ERR-05): "Start Your First Scan" CTA tied directly to the empty message, not a generic bottom button.
- **Progress Tracker** — Pulls real scan history from Room DB, grouped by condition with trend indicators. (Earlier versions of this doc incorrectly listed this as sample data — it wasn't.)
- **Scan Reminders** — The Profile toggle now actually persists and enables/disables the daily reminder worker; it previously reset to ON on every visit and didn't affect anything.

**What IS working properly:** Login / Register / Logout (passwords are salted + hashed, not stored in plain text), Edit Profile (with duplicate-email and blank-name validation), saving scans to the database (storage works, just not fed real results yet by default), Care Guide info pages, Progress Tracker, Scan Reminders. A security pass (`SECURITY_TESTING.md`) closed out the only two real findings found (unused cleartext traffic permission, DB not excluded from Android backup) — nothing high or medium severity remains open.

## Team
- Mark Joseph Garcia
- Reynaldo Manio Jr.
- Reicee Owen Pastrana
- Chrisent Dayniel Tolentino

*Tarlac State University — Capstone 2026*
