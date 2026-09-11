# DermaLens
An Android skin disease detection app built with Jetpack Compose. DermaLens lets users scan their skin, track conditions over time, and find nearby dermatology clinics — developed as a Capstone Project at Tarlac State University, 2026.

---

## Features
- **Skin Scan** — Capture via camera (pinch-to-zoom) or pick from gallery (pan + pinch-to-zoom to position, cropped to exactly what's in the guide frame before scanning); AI detects condition, severity, and confidence, with real bounding boxes (multi-region, NMS-filtered) drawn on the full result image
- **Scan History & Progress Tracker** — Timeline view per condition with trend indicators (improving / worsening / stable)
- **Condition Guidance** — Description, common symptoms, and recommendations shown directly on the scan result screen (this replaced the separate Care Guide screen — the content lives with the result it applies to instead of in a parallel browsable section)
- **Clinic Locator** — GPS-based Google Map with custom markers, dermatology clinics found via the Google Places API, and driving routes drawn from OSRM
- **Contribute to Research** — Opt-in, and it actually uploads: consented scans are sent over Wi-Fi to a Google Apps Script bridge that files them into per-condition folders in the project owner's own Google Drive, ready to fold into a future retraining run. Anonymous by construction — the filename is a random UUID plus the detected condition, with no account identifier anywhere in the request (see [Contribute to Research](#contribute-to-research-pipeline) below)
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
| Maps | Google Maps (`maps-compose`) + Google Places API (clinic search) + OSRM (routing only) |
| Location | `play-services-location` |
| AI Model | YOLOv11 TFLite — real **6-class merged model** trained and live-verified on-device (overall mAP50 0.557); auto-detects channels-first/-last tensor layout; expects stretch-to-square resize |
| Auth | Firebase Authentication (email/password) — registration required, no guest/offline path |
| Research uploads | Google Apps Script Web App → project owner's Google Drive, via WorkManager (`NetworkType.UNMETERED`) |

## Project Structure
```
app/src/main/java/
├── com/dermalens/app/
│   ├── data/
│   │   ├── db/          # Room database, DAOs
│   │   └── model/       # ScanRecord, User entities
│   ├── ml/
│   │   └── YoloDetector.kt   # TFLite inference, NMS, CLASS_LABELS, confidence floor
│   ├── navigation/      # NavGraph, Screen sealed class
│   └── ui/
│       ├── screens/     # All screen composables
│       │   ├── Screens.kt            # Login + Register + Privacy Policy
│       │   ├── HomeScreen.kt         # Home + bottom nav bar
│       │   ├── CameraScreen.kt       # Camera + gallery (+ scan sweep effect)
│       │   ├── ScanResultScreen.kt   # Detection result, condition content, save
│       │   ├── ProgressTrackerScreen.kt
│       │   ├── ClinicLocatorScreen.kt
│       │   └── ProfileScreen.kt      # Profile + EditProfile
│       ├── AppSettings.kt    # Font scale + high contrast state
│       └── DermaColors.kt    # Colors, DermaPrefs, animation helpers
└── worker/              # (package com.dermalens.app.worker — flat dir is intentional)
    ├── ScanReminderWorker.kt
    ├── ContributionUploadWorker.kt   # Research upload, Wi-Fi only
    └── NotificationScheduler.kt      # + ContributionUploadScheduler
```

Training/ML lives outside the app module:
```
training/
├── merge_and_train_multiclass.ipynb   # the real multi-class merge + train pipeline
├── retrain_yolo.ipynb                 # single-condition solo runs (sanity checks)
├── train_acne_subtypes.ipynb          # standalone acne subtype experiment
└── acne_subtypes_future/README.md     # why subtype splitting is parked
apps-script/ContributionUpload.gs      # the Drive upload endpoint (deploy as a Web App)
```

> The `.ipynb` files are **gitignored** — they carry pasted Roboflow API keys, so they're shared via Drive/Colab rather than committed (see `.gitignore`). Ask a team member for a copy. `acne_subtypes_future/README.md` and the Apps Script *are* committed.

## Building & Running
1. Clone the repo
2. Open in Android Studio (Hedgehog or later)
3. Let Gradle sync complete
4. Run on a device or emulator (API 26+)

> VS Code will show "Unresolved reference" errors on Compose/Room imports — these are fake and disappear after Gradle sync in Android Studio.

> **Want to help code?** See [SETUP.md](SETUP.md) for the full onboarding steps, including how to get `google-services.json` (needed to build — it's not in the repo, see below).

## Firebase Auth
Registered accounts (Login/Register) go through real Firebase Authentication — a new account is a real, Firebase-Console-visible user, gets a real verification email, and password reset goes through Firebase's real "Forgot password?" email flow. Registration is required to use the app — there is no guest/offline-account path. Everything else (scan history, profile stats) stays fully local in Room DB.

Email verification is enforced, not just sent: an unverified account is routed to a "Verify Your Email" screen (with Resend and Log Out options) instead of Home, on both fresh Register and every subsequent Login, until Firebase reports `isEmailVerified == true`. The `is_logged_in` session flag is only set once verified, so there's no way to reach the app's main features with an unconfirmed email.

> Guest mode was implemented and briefly live on 2026-08-24, then removed on 2026-08-25 per the team's tech adviser. See "Guest Mode — Removed" in `FIREBASE_AUTH_PLAN.md` for what changed and why, if you're wondering where it went.

Full rationale, the code-change list, and the capstone-manuscript impact (Scope & Limitations, IC1/IC2/SS1/SS3, Table 4, Figure 3, Privacy Policy text) are written up in [FIREBASE_AUTH_PLAN.md](FIREBASE_AUTH_PLAN.md) — read that before touching this area or updating the paper.

`app/google-services.json` is required to build but is **gitignored** (it's tied to the Firebase project). Ask Mark Joseph for a copy, or get added to the Firebase project and download your own from the Console.

**Live-verified so far:** Register (real Firebase account + verification email + local profile), Login (Firebase auth + resolves matching local profile), Logout (correctly signs out of Firebase too — this was a real bug, fixed), Change Password (Edit Profile → Account Security, reauthenticate + update, confirmed via a live "Saved!" success state), Verify Your Email (an unverified account is correctly blocked with "Still not verified" rather than let through, and Resend Email works — confirmed live with a real unverified test account).

## Contribute to Research pipeline
The Profile toggle isn't decorative — consented scans genuinely leave the device. How it works end to end:

1. User opts in (Profile → Contribute to Research, or the first-run prompt on Home).
2. On **Save to History**, if consent is on, the scan's image is copied into app-private storage and the row is flagged `contributedForTraining`.
3. A WorkManager job (`ContributionUploadWorker`, `NetworkType.UNMETERED`, ~12h period) POSTs pending images to a **Google Apps Script Web App**, which files each one into `DermaLens Contributions/<condition>/` in the script owner's own Google Drive. The row is then flagged `uploadedForTraining` so nothing uploads twice.

**Why Apps Script and not Firebase Storage:** Firebase/Cloud Storage now requires the project to be on the Blaze plan — the Spark free tier no longer includes Storage at all, and activating Blaze wanted a refundable prepayment we didn't want to spend on a student project. Apps Script runs under the owner's own Google account with no billing attached, and crucially it means **no real credential ships in the APK** — only a low-value shared secret, since the script itself holds the Drive permissions.

**Privacy properties, by construction rather than by promise:**
- The request contains only the image bytes, a random-UUID filename, and the detected condition. No user ID, email, device ID, or timestamp-of-account is sent.
- All contributions land in one shared per-condition folder tree — deliberately **not** per-user folders, since there's no user identifier to key them on and adding one would break the anonymity the consent dialog promises.
- Wi-Fi only, so it never quietly consumes someone's mobile data.

**Setup** (only needed once, by whoever owns the receiving Drive): deploy `apps-script/ContributionUpload.gs` as a Web App (Execute as: Me, Who has access: Anyone), set a `SHARED_SECRET` script property, then put the resulting `/exec` URL and the same secret into `local.properties` as `APPS_SCRIPT_URL` and `CONTRIBUTION_UPLOAD_SECRET`. Full step-by-step is in the comment header of the `.gs` file. Both values are read via `BuildConfig`, same pattern as `MAPS_API_KEY` — **never commit them**, this repo is public.

> If `APPS_SCRIPT_URL` is blank the worker exits cleanly as a no-op, so the app builds and runs fine without any of this configured.

## For Contributors — Known Gaps / Good First Issues
Not urgent, left for later. Good entry points if you want to help:
- **Email change isn't implemented.** Edit Profile can change your name but the email field is read-only. Firebase requires a `verifyBeforeUpdateEmail()` flow (sends a confirmation link to the *new* address) — deliberately scoped out of the initial Firebase pass, see the "Open Questions" section of `FIREBASE_AUTH_PLAN.md`.
- **"Forgot password?" email flow is implemented but not yet live-tested end-to-end** (i.e. actually clicking the reset link from a real inbox and confirming login with the new password works). Worth a pass.
- **Firebase BoM is pinned to `33.5.1`** in `app/build.gradle.kts` (see the comment there) because newer BoMs need Kotlin 2.3.0 and this project is pinned to Kotlin 2.0.21. Don't bump it without bumping Kotlin first, or the build breaks with a metadata-version mismatch.
- **`osmdroid` is a dead dependency.** `app/build.gradle.kts` still pulls in `org.osmdroid:osmdroid-android:6.1.18`, but it's imported in **zero** files since the Clinic Locator moved to Google Maps. Safe to delete — it's just APK weight.
- **The Places API cert SHA-1 is hardcoded**, in `ANDROID_CERT_SHA1` in `ClinicLocatorScreen.kt`. It's the SHA-1 of one specific debug keystore, so clinic search silently returns nothing for anyone building with a different one (and would need the release keystore's SHA-1 added for a signed build). Either add your own SHA-1 to the restricted key in Google Cloud Console and update the constant, or read it at runtime from the app's own signing info instead of hardcoding.

## Development Timeline
CP2 Development Plan — May – November 2026

| Sprint | Duration | Track | Sprint Goal | Key Deliverables | Technologies | Lead | Status | Priority |
|---|---|---|---|---|---|---|---|---|
| Sprint 1 | May 25 – Jun 7, 2026 | App Development | Project setup, user authentication (register/login/logout), initial UI scaffolding | Login & register screens; auth flow; GitHub repo with project structure | Android Studio, Kotlin, Jetpack Compose, Firebase Auth / Room DB | Chrisent Dayniel | ✅ Done | 🔴 Critical |
| Sprint 2 | Jun 8 – Jun 21, 2026 | App Dev + AI | CameraX real-time integration; start YOLOv11 model training on Google Colab | Working camera capture screen; initial YOLOv11 training pipeline; preliminary model weights | CameraX, Camera2 API, Python, Ultralytics YOLO, Kaggle/Roboflow dataset | Reynaldo | 🔄 In Progress | 🔴 Critical |
| Sprint 3 | Jun 22 – Jul 5, 2026 | AI + Integration | Fine-tune YOLOv11, convert to TFLite, integrate on-device inference into the app | Optimized .tflite model in APK; real-time detection screen with bounding box + confidence score | TensorFlow Lite, GPU/NNAPI delegates, Ultralytics YOLO export, Google Colab T4 | Mark Joseph | 🔄 In Progress | 🔴 Critical |
| Sprint 4 | Jul 6 – Jul 19, 2026 | App Development | Detection result screen, skincare guidance content, Room DB for scan history | Complete result screen; skincare guide for all 6 conditions; working Room DB schema | Jetpack Compose, Room DB, SQLite, pre-built knowledge base JSON | Reicee Owen | ✅ Done | 🟠 High |
| Sprint 5 | Jul 20 – Aug 2, 2026 | App Development | Progress tracking dashboard, clinic locator via Google Maps, scan reminders | Progress tracker with charts; clinic locator with directions; WorkManager notifications | Google Maps SDK, Places API, WorkManager, MPAndroidChart / Compose Charts | Chrisent Dayniel | ✅ Done | 🟠 High |
| Sprint 6 | Aug 3 – Aug 16, 2026 | Testing | Full system integration, functional testing (FR1–FR11), performance testing, survey | Stable DermaLens APK; functional + performance test results; 100-respondent survey data | Android Profiler, Likert scale questionnaire | All Members | ⬜ Not started | 🟠 High |
| Sprint 7 | Aug 17 – Aug 30, 2026 | Bug Fixing | Resolve bugs from testing; UI/UX polish; start Chapter 5 documentation | Refined APK; resolved bug report; Chapter 5 draft; updated methodology docs | Android Studio Debugger, Compose Previews | Mark Joseph | 🔄 In Progress | 🟡 Medium |
| Sprint 8 | Aug 31 – Sep 27, 2026 | Documentation | Final documentation, complete all chapters, defense preparation | Final capstone paper (all chapters); defense slides; submitted manuscript; archived APK | Google Docs / MS Word, PowerPoint / Canva | All Members | ⬜ Not started | 🟡 Medium |
| Post-Sprint | Oct – Nov 2026 | Wrap-up | Address panel feedback, finalize approved manuscript, archive project repository | Revised approved manuscript; archived repo; all submission requirements fulfilled | GitHub, Google Drive | All Members | ⬜ Not started | 🟢 Low |

> **AI/ML progress note (Sprint 2):** 4 of 9 skin condition classes annotated and trained as isolated single-class YOLOv11 models (Melasma, Eczema, Acne + 1 more). Multi-class merge pending resolution of annotation consistency issues before full 9-class training.
>
> **AI/ML progress note (Sprint 7):** The Melasma single-class model was exported to TFLite and tested end-to-end on a real device — correctly classified a real melasma photo at 77.7% confidence. This confirmed the app-side integration works and surfaced two real export details worth knowing for the other classes: (1) this export uses a channels-first `[1,3,H,W]` tensor layout rather than the usual channels-last `[1,H,W,3]` — the app now auto-detects either; (2) the model expects a plain stretch-to-square resize, not letterboxing — consistent with Roboflow's default "Resize: Stretch" preprocessing. See `HANDOFF.md` for the integration steps once the merged multi-class model is ready.
>
> **AI/ML progress note (Sprint 7, cont'd):** An audit against Chapter 4's FR7 ("visual overlay on the captured image") found `bestClass()` was discarding box coordinates entirely — fixed to extract real boxes, filtered with confidence thresholding + NMS (not just the single best box). Verified live on-device: correctly drew two separate boxes on a bilateral melasma photo (both cheeks), where the single-box version would have silently hidden the second affected region.
>
> **AI/ML progress note (Sprint 8) — the multi-class merge is done.** The 6-class merged model is trained, exported, bundled, and live-verified on-device. Per-class AP50: Eczema 0.643, Warts 0.641, Melasma 0.602, Acne Vulgaris 0.557, Tinea 0.525, Scabies 0.360 — **overall mAP50 0.557**. Three findings from getting there, each measured rather than assumed:
>
> 1. **Two source datasets were exported with the wrong preprocessing.** Melasma and Scabies used Roboflow's "Fit within" (letterbox) resize while the other four used "Stretch to" — mismatched geometry between classes in one merged dataset. Fixing both moved Scabies 0.297 → 0.360 and Melasma 0.572 → 0.602. Worth checking on *every* dataset version before training; it's silent otherwise.
> 2. **Scabies' weakness is annotation quality, not data volume.** The confusion matrix showed it isn't confused with other conditions at all — it's simply missed (62% predicted as background). Rendering the actual boxes found the cause: 34% of source images have a *single whole-image box* (90–100% of frame) over photos containing 6–15 discrete lesions, contradicting the other 66% that are correctly boxed per-lesion. The model can't learn a coherent lesion concept from both lessons at once. 114 affected images are identified and pending re-annotation.
> 3. **The confidence floor is now derived, not guessed.** `MIN_CONFIDENCE_PERCENT` was an arbitrary 40%; it's now 32%, taken from the trained model's own F1-confidence curve (F1 peaks at 0.55 at confidence 0.322).
>
> **Also measured and deliberately not shipped:** a `yolo11m` (medium) solo Melasma run scored **0.696 mAP50** vs. 0.602 for the same data on `yolo11s`, and correctly identified a real photo live — model capacity is a real lever for the smaller classes, but the full merge hasn't been retrained on it yet. Separately, a standalone 5-class acne subtype model (blackhead/whitehead/papula/pustula/nodules) scored only 0.234 mAP50 with blackhead recall at 1.5%, so subtype differentiation is parked rather than folded in — see `training/acne_subtypes_future/README.md` for the full reasoning.

## Known Limitations
*As of now — to be updated as development progresses.*

Quick list of what's real vs. not real in the app right now.

| Feature | Is it real? |
|---|---|
| Clinic Locator | Real — Google Map + live Google Places results + OSRM routes; shows an honest empty state if none found nearby |
| Skin Scan Results | Real, with a caveat — a trained 6-class model (mAP50 0.557) is live-verified on-device, but the `.tflite` is gitignored, so a **fresh clone falls back to random mock results** until you drop the model in `app/src/main/assets/` |
| Progress Tracker | Real — pulls actual scan history from Room DB, grouped by condition with trend indicators |
| Contribute to Research | Real — uploads to Drive via Apps Script, Wi-Fi only, anonymous; no-ops cleanly if unconfigured |

- **Clinic Locator** — Now on **Google Maps + Google Places**, not OpenStreetMap. The map itself is `maps-compose`'s `GoogleMap` with custom `BitmapDescriptorFactory` markers, clinics come from the Places API (`places:searchText`, query "dermatology clinic", 15 km location bias), and **OSRM is still used, but only for drawing driving routes** — that one piece stayed OSM-based. The earlier Overpass implementation (exact `healthcare:speciality=dermatology` tag matching) was replaced because it consistently timed out on Overpass's public instance and silently contributed zero results. Behaviour that carried over: if nothing is found within 15 km (like in Capas), you get an explicit "No dermatology clinics found nearby" state rather than fabricated filler; the screen checks real connectivity (`ConnectivityManager`) and shows a separate "No Internet Connection" state with Retry, so a dead network doesn't look identical to "no clinics nearby"; and clinic ratings still aren't displayed. (Note: unlike Overpass, the Places API *does* expose ratings — so showing them is now a possible feature rather than a data limitation.)
- **Clinic Locator needs `MAPS_API_KEY`** — the map and Places search both read it from the gitignored `local.properties` via `BuildConfig`/manifest placeholder. Without it the map renders blank and search returns nothing, which looks like a bug rather than missing config. Two related gotchas worth knowing before you touch `runPlacesTextSearch`: the key is restricted to this app's package + signing cert, and because the call is a raw `HttpURLConnection` rather than the Places SDK, it has to attach the `X-Android-Package` and `X-Android-Cert` headers itself or Google rejects it with `API_KEY_ANDROID_APP_BLOCKED`. The cert SHA-1 is currently **hardcoded** in `ClinicLocatorScreen.kt`, which means clinic search will fail for any contributor building with their own debug keystore until that value is updated (see Known Gaps).
- **Skin Scan Results** — A real trained 6-class model works and is live-verified on-device, but the `.tflite` is **gitignored** (large binaries belong in Drive/Colab, not git — see `.gitignore`), so a fresh clone falls back to `mockDetectionResults.random()` until you drop the model into `app/src/main/assets/best.tflite`. When the real model is present: correct classification, real multi-region bounding boxes (NMS-filtered, verified catching both cheeks on a bilateral melasma photo), and results below the 32% confidence floor are reported honestly as "No Clear Condition Detected" rather than shown as a confident-looking guess. **`CLASS_LABELS` in `ml/YoloDetector.kt` must match the training class order exactly** — the merge notebook prints the exact line to paste. See `HANDOFF.md`. The live camera captures via `ImageCapture` with pinch-to-zoom, and gallery-picked photos can be panned/zoomed and get cropped to exactly the guide frame before scanning.
- **Known model weaknesses, stated plainly** — Scabies is the weakest class (AP50 0.360) and misses roughly 60% of true cases; its dataset needs re-annotation (see the Sprint 8 note above). Acne is occasionally misread as Melasma on clean photos — a real confusion pair, since post-inflammatory hyperpigmentation from healed acne genuinely resembles melasma's brown patches. Individual scan confidence percentages vary widely photo-to-photo even for correct detections; that's expected and not a defect (mAP is the aggregate measure, a single scan's percentage is not).
- **Scanning guidance that came out of testing** — Don't over-zoom. Cropping tightly onto a single lesion measurably *lowered* confidence (28.6% → 17.4% on the same wart photo) because the training images are framed with the lesion in surrounding skin context, not filling the frame edge to edge.
- **Camera permission denial** — If a user permanently denies camera access ("Don't ask again"), the app detects this (`shouldShowRequestPermissionRationale`) and offers a real "Open Settings" button instead of retrying an in-app dialog Android will never show again.
- **Progress Tracker empty state** — Matches the paper's storyboard now (Figure 22 / ERR-05): "Start Your First Scan" CTA tied directly to the empty message, not a generic bottom button.
- **Progress Tracker** — Pulls real scan history from Room DB, grouped by condition with trend indicators. (Earlier versions of this doc incorrectly listed this as sample data — it wasn't.)
- **Scan Reminders** — The Profile toggle now actually persists and enables/disables the daily reminder worker; it previously reset to ON on every visit and didn't affect anything.

**What IS working properly:** Login / Register / Logout (passwords are salted + hashed, not stored in plain text), Edit Profile (with duplicate-email and blank-name validation), saving scans to the database, on-device 6-class detection with real bounding boxes, condition guidance content on the result screen, Progress Tracker, Scan Reminders, and Contribute to Research uploads. A security pass (`SECURITY_TESTING.md`) closed out the only two real findings found (unused cleartext traffic permission, DB not excluded from Android backup) — nothing high or medium severity remains open.

> **Fixed in Sprint 8:** "Save to History" could silently do nothing — if a Room schema bump wiped the `users` table while `SharedPreferences` still reported a logged-in session, the save looked up a profile row that no longer existed and gave up without any error, leaving the button visually unresponsive. It now self-heals by recreating the minimal profile row, same as the Login flow already did for fresh installs.

## Team
- Mark Joseph Garcia
- Reynaldo Manio Jr.
- Reicee Owen Pastrana
- Chrisent Dayniel Tolentino

*Tarlac State University — Capstone 2026*
