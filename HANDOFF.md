# DermaLens — Developer Handoff

Last updated: 2026-08-28
Branch: `master` (all work committed and pushed, working tree clean)
Status: Firebase Auth live. Three of six target conditions have real trained/verified single-class models (Melasma, Atopic Dermatitis, Warts) — swapped in one at a time, not simultaneously. Multi-class merge still pending.

---

## Dev Environment Notes (read this first if starting fresh)

- **JAVA_HOME isn't set by default** in this shell. JDK 17 lives at `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot` — set `$env:JAVA_HOME` before running `.\gradlew.bat` anything, in PowerShell (not Git Bash — Gradle needs PowerShell/cmd here).
- **adb** is at `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`, **emulator** at `$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe`. AVD name: `DermaLensTest`. The emulator is not always running — check `adb devices` first, boot with `emulator.exe -avd DermaLensTest -WindowStyle Hidden` if empty, and poll `adb devices` until it shows `device` (takes 20-40s).
- **Git Bash mangles device-absolute paths** starting with `/sdcard/...` — double the leading slash (`//sdcard/...`) when passing paths to `adb push`/`pull`/`shell` through the Bash tool, or just use PowerShell for adb calls instead.
- **The Android system Photo Picker gets cluttered** by every screenshot taken during testing (they get indexed into the same "Recent" media view). Periodically `adb shell rm -f /sdcard/*.png` + `am force-stop com.android.providers.media.module` to reset it if the picker gets hard to navigate.
- Full team onboarding steps (Android Studio, `google-services.json`) are in `SETUP.md`.

---

## Auth — Now Firebase, Not Local

This changed substantially since the original local-hash implementation:

- **Firebase Authentication** (email/password) — real accounts, real email verification, real password reset, visible in the Firebase Console. See `FIREBASE_AUTH_PLAN.md` for the full rationale and manuscript impact (Scope & Limitations, IC1/IC2/SS1/SS3, Table 4, Figure 3).
- **Guest mode was added, then removed** (2026-08-24 → 2026-08-25) per the team's tech adviser. Registration is now required — no offline/no-account path. See "Guest Mode — Removed" in `FIREBASE_AUTH_PLAN.md`.
- **Email verification is enforced**, not just sent — an unverified account is routed to a "Verify Your Email" screen instead of Home, on both fresh Register and every subsequent Login, until Firebase reports `isEmailVerified == true`. See `VerifyEmailScreen` in `Screens.kt`.
- Password reset ("Forgot password?") **has been live-tested end-to-end** with a real inbox (not just implemented) — confirmed working.
- Email *change* is still NOT implemented — Edit Profile's email field is read-only. Needs Firebase's `verifyBeforeUpdateEmail()` flow. Open item, see README "Good First Issues."

---

## YOLOv11 — Three Conditions Trained & Verified, Swap-One-At-A-Time Workflow

**Important: only one `.tflite` model is bundled at a time.** `app/src/main/assets/best.tflite` gets overwritten each time a new condition's model is swapped in, and `CLASS_LABELS` in `ml/YoloDetector.kt` must be updated to match. There is no multi-class model yet — each condition was trained and verified independently.

**Currently bundled: Warts** (`CLASS_LABELS = listOf("Warts")`).

### Conditions trained and live-verified so far (via real app flow, real photos, not synthetic tests)
1. **Melasma** — verified ~68-78% confidence range across multiple real photos, correct multi-region bounding boxes (bilateral detection)
2. **Atopic Dermatitis** (Roboflow project named "eczema" — app's canonical name is "Atopic Dermatitis", matches `mockDetectionResults`/Care Guide; **must** set `CLASS_LABELS = listOf("Atopic Dermatitis")`, not `"Eczema"`, or `conditionTemplates[label]` lookup silently returns null) — verified 39.6% (below floor, correctly rejected) on a subtle photo, 59.0% (correctly identified) on a clearer one. Training log showed mAP50=0.480, P=0.554, R=0.456 — moderate accuracy, plateaued early (see below).
3. **Warts** — verified 70.8% (single wart cluster) and 64.8% (multiple wart clusters across 3 fingers, 4 boxes drawn correctly) on two different real photos.

### Remaining conditions to train (of the app's 6-condition set)
- **Acne Vulgaris**
- **Tinea**
- **Scabies**

### Real bugs found and fixed along the way (all committed, all live-tested)
- **No confidence floor existed.** A single-class model has no way to say "not skin" / "no condition" — it will confidently label a photo of a wall or wood grain with *some* score for its one class, because that's the only answer it can give. Fixed: `MIN_CONFIDENCE_PERCENT = 40f` in `YoloDetector.kt` — below this, the app returns a `lowConfidenceResult()` ("No Clear Condition Detected") instead of a fabricated diagnosis. Verified live with an unrelated billboard photo → correctly shows "No Clear Condition Detected," not a false label.
- **Severity badge was fabricated.** The "Mild/Moderate/Severe" badge on the Scan Result screen was mock data, never something the model actually predicted — removed from that screen's display (`DetectionResult.severity` and the badge UI are still used by Progress Tracker's improving/worsening/stable trend feature, which is real and was deliberately left alone — only the misleading display on the result screen itself was removed).
- Same two preprocessing gotchas as before still apply: **channels-first vs. channels-last auto-detection**, and **stretch-resize (not letterboxed) preprocessing** — see the code comments in `YoloDetector.kt`'s `preprocess()` and `bestClass()`.

### Confidence threshold (`BOX_CONFIDENCE_THRESHOLD = 0.25`, box-drawing only, doesn't affect the % shown)
A real experiment (not guessed) was run sweeping this value against one real inference pass — see `THRESHOLD_EXPERIMENT.md` for full methodology and results. Conclusion: keep 0.25, it sits safely below the point where real detections start getting dropped (~0.40).

### Training data quality issue found (relevant for future training runs)
Reviewed the actual training log for the Atopic Dermatitis run (79 epochs, early-stopped, mAP50 plateaued around 0.48 without smooth improvement — that pattern points to label/annotation inconsistency, not insufficient epochs). Two concrete causes were visually confirmed in the Roboflow project:
1. **Junk classes from mislabeling** — the Roboflow project has stray classes (`0`, `b`, `bb`, `bjnn`) alongside the real `Eczema` class, almost certainly typos during labeling that created new classes instead of using the existing one. Boxes under these never contribute to the real class's training.
2. **Inconsistent box tightness / wrong annotation tool** — some images were boxed edge-to-edge (whole image, uninformative), and at least 4 instances were annotated with the **Polygon** tool instead of **Bounding Box**, which Ultralytics silently drops entirely for a detection-format dataset (confirmed via the training log's `len(segments)=4, len(boxes)=768` warning).

**Recommendation for future training runs (any condition):** before training, check the Roboflow project's Classes panel for stray/junk classes and merge or delete them, and spot-check that all annotations use the Bounding Box tool with reasonably tight boxes around just the affected skin — not the whole image.

---

## What's Done (unchanged from before, still accurate)

### Camera & Gallery
- CameraX with `PreviewView.ImplementationMode.COMPATIBLE`, real `ImageCapture`, pinch-to-zoom, gallery picker with pan/pinch/crop-to-frame. See prior handoff detail — unchanged.

### Scan Result
- Real inference via `runYoloInference()`, falls back to `mockDetectionResults.random()` only if no model bundled or `imageUri` is null — never falls back on a low-confidence *real* result (that's the confidence-floor path instead, a genuine "no condition" result, not a random fake one).

### Progress Tracker
- Real scan history from Room DB, trend indicators based on `severity` (still real feature, untouched by the Scan Result severity-badge removal).

### Care Guide
- 6 conditions: Acne Vulgaris, Atopic Dermatitis, Melasma, Tinea, Warts, Scabies. Each has Overview/Routine/Dos/Don'ts/Treatments, plus (added by a teammate) a "Recommended OTC Product" card per condition and a "Consult a Doctor" card for Scabies specifically.
- **Known gap:** 3 of 5 recommended OTC products reference US-specific brands not confirmed available in PH pharmacies (Lotrimin AF, Compound W, and partially La Roche-Posay/The Ordinary) — flagged, not yet fixed. See README "Good First Issues."

### Clinic Locator
- Real GPS + OSM Overpass API, no hardcoded/fake clinic data.
- **Known gap:** "Open Now" badge defaults to `true` whenever real `opening_hours` data isn't available from OSM (same time the fallback "Contact clinic for hours" text shows) — this is misleading, a real clinic's open/closed status isn't actually known. User was going to research this themselves before a fix was implemented — check with them on where that landed. Fix would be defaulting to "Hours unknown" instead of `true`. See `isOpenNow()` in `ClinicLocatorScreen.kt`.
- Also: Overpass only matches the exact `healthcare:speciality=dermatology` tag — a regex name-search fallback was tried and reverted (times out on the public Overpass instance). Real dermatology clinics that aren't tagged that way in OSM won't show up, even though they're on Google Maps. No fix implemented — documented as a known data-source limitation.

### Profile
- Unchanged from before except: password change now goes through Firebase reauthentication (`EmailAuthProvider` + `reauthenticate()` + `updatePassword()`), not local hash comparison. Email field is read-only (see Auth section above).

---

## Key Files Reference

| File | What it does |
|---|---|
| `DermaColors.kt` | Color constants, `DermaPrefs` keys (no more `KEY_IS_GUEST` — removed) |
| `Screens.kt` | Login, Register, **VerifyEmailScreen** (new) |
| `NavGraph.kt` | All routes, includes `Screen.VerifyEmail` |
| `ml/YoloDetector.kt` | `runYoloInference()`, `CLASS_LABELS`, `MIN_CONFIDENCE_PERCENT`, `BOX_CONFIDENCE_THRESHOLD` — see YOLOv11 section above |
| `ScanResultScreen.kt` | `DetectionResult` now has `isLowConfidence: Boolean = false`; severity badge removed from display, field still used by Progress Tracker |
| `data/model/User.kt` | No more `isGuest` field (removed with guest mode) |
| `DermaDatabase.kt` | Room DB **v5** now (bumped for guest-mode field removal) |

## DB Version History

| Version | Change |
|---|---|
| 1 | Initial schema |
| 2 | Added fields to User |
| 3 | Added `imagePath` + `contributedForTraining` to ScanRecord |
| 4 | Added `firebaseUid`, `isGuest` to User (Firebase Auth) |
| 5 | Removed `isGuest` from User (guest mode removed) |

Still uses `fallbackToDestructiveMigration()` — acceptable for dev/capstone, wipes local data on every version bump.

## Known Non-Issues (VS Code)

VS Code shows "Unresolved reference: androidx" on every import in Kotlin files. **These are fake.** The project builds fine via Gradle (see Dev Environment Notes above for the JAVA_HOME gotcha) or in Android Studio.

## Reference Docs

- `FIREBASE_AUTH_PLAN.md` — Firebase Auth rationale, guest mode history, manuscript impact
- `THRESHOLD_EXPERIMENT.md` — confidence threshold experiment, full methodology and results
- `SETUP.md` — groupmate onboarding
- `README.md` — feature overview, "Good First Issues" list

---

*DermaLens — Tarlac State University Capstone 2026*
