# Firebase Hybrid Auth — Integration Plan

Status: **Scoped, not yet implemented.** Blocked on prerequisite steps below (need a real
Firebase project before any code can be written and tested).

Decided by the team on 2026-08-24: add Firebase Authentication (hybrid model, see below) plus a
guest mode, to solve two real gaps found during Sprint 7 testing:

1. **No real email verification.** Local-only validation can only check an email's *shape*
   (`local@domain.tld`), never whether the address is real or owned by the registrant — that
   requires actually sending mail, which needs a backend.
2. **No real "how many users" visibility.** All accounts currently live in a Room DB local to
   whichever single device runs the app, so there's no way to see a genuine cross-device user
   count without a server.

Firebase Authentication solves both without requiring a full backend rewrite.

---

## What "Hybrid" Means

**Firebase Auth handles identity only** — email/password accounts, real email verification,
and the authoritative user count (visible directly in the Firebase Console, no extra code).

**Everything else stays exactly as it is today:**
- Scan history, condition tracking, progress data → still Room DB, still fully local, still
  works offline.
- Care Guide, Skincare Guidance → unchanged, still bundled locally.
- Clinic Locator → unchanged, still OpenStreetMap/Overpass, still the only feature requiring
  internet.
- **Guest mode** → bypasses Firebase entirely. A guest gets a local-only Room DB profile with
  no cloud account at all — this is what keeps the app usable fully offline for anyone who
  doesn't want to create a real account.

So after this change, the app has two account types:
- **Registered account** (Firebase Auth) — email verified, persists in the cloud (so it *could*
  survive an app reinstall/device change if that's ever wanted later), counted in the Firebase
  Console.
- **Guest account** (local Room DB only) — no email, no verification, no cross-device anything,
  same as how every account works today.

---

## What Actually Changes in Code

| File | Change |
|---|---|
| `Screens.kt` (Login/Register) | Register calls Firebase's `createUserWithEmailAndPassword()` + `sendEmailVerification()` instead of writing directly to Room's `users` table. Login calls `signInWithEmailAndPassword()` instead of the local hash check. |
| `data/model/User.kt` / Room `users` table | Keeps existing role for guest profiles. For registered accounts, becomes a local *profile* record (name, prefs) keyed by the Firebase UID rather than self-managing email/password. |
| `DermaColors.kt` (`DermaPrefs`) | Session tracking updates to distinguish "Firebase session" vs. "guest session." |
| `MainActivity.kt` | Splash/auth-check logic needs to check Firebase's current-user state for registered accounts, Room DB flag for guests. |
| `ProfileScreen.kt` (Edit Profile) | Email changes for registered accounts now go through Firebase (`updateEmail()`, re-verification), not just a local Room update + uniqueness check. |
| New: a "Continue as Guest" entry point on the Login screen | Skips Firebase entirely, creates a local Room profile directly. |

Nothing in `ScanResultScreen.kt`, `ClinicLocatorScreen.kt`, `ProgressTrackerScreen.kt`,
`CareGuideScreen.kt`, or `YoloDetector.kt` needs to change — none of them touch auth.

---

## Prerequisite Steps (only the team can do these — blocking)

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
   using a Google account (doesn't need to be a paid/billing-enabled one for this — Spark/free
   plan covers email/password Authentication).
2. In the Firebase Console: **Authentication → Sign-in method → enable Email/Password.**
3. Register the Android app in the Firebase project using the app's package name
   (`com.dermalens.app` — check `app/build.gradle.kts` `applicationId` to confirm exact value).
4. Download the generated `google-services.json` file and hand it over (or drop it directly into
   `app/google-services.json` in the repo — **note:** this file contains project identifiers,
   not secrets in the traditional sense, but it's still project-specific config, not something
   to commit if the team wants to keep the Firebase project itself private; check with the team
   before pushing it to a public repo).

Once that file exists, the actual Gradle setup (Firebase BoM, Auth SDK, Google Services Gradle
plugin) and the code changes above can be written and tested for real, the same way every other
fix this sprint was verified live rather than shipped blind.

---

## Manuscript / Paper Impact

This reverses part of an already-approved Form 12 revision, so it needs to be handled
deliberately in the documentation, not silently. Specific sections that need updating:

| Section | Current text (approved) | What needs to change |
|---|---|---|
| **Scope and Limitations** (Ch. 1) | States the system "eliminates server-side scripting" — added specifically per Form 12 panel feedback | Needs rewording: server-side involvement is now limited *only* to authentication (identity + email verification) for registered accounts; guest mode remains fully local/offline. Frame this as a deliberate, scoped exception, not walking back the original claim. |
| **IC1** (Design and Implementation Constraints, Ch. 4) | "The system shall require user authentication before accessing any feature of the application." | Needs to explicitly allow the guest path: authentication is required *unless* the user chooses to continue as a guest, in which case functionality is scoped to on-device features only (no cloud sync, no clinic-locator account features if any are ever tied to identity). |
| **IC2** (Design and Implementation Constraints, Ch. 4) | "The system shall store all user data, scan history, and detection results in a local Room DB database residing on the user's Android device." | Still true for scan history/detection results. Needs a carve-out: *account identity* (email, verification status) for registered users is managed by Firebase Authentication, not Room DB. |
| **SS1** (Non-Functional Requirements — Safety and Security) | Describes local authentication/local profile protection | Needs a line distinguishing the two account types and what Firebase actually stores (email + hashed credentials, managed by Google's infrastructure, not DermaLens's own servers) vs. what stays local. |
| **SS3** (Non-Functional Requirements — Safety and Security) | "The application will not send any user-captured skin images or personal data to external servers... because all YOLOv11 model inference operates completely on-device" | Still fully true and unaffected — this is specifically about image/inference data, not auth. Worth a sentence clarifying the two are separate to avoid a panelist conflating them. |
| **Table 4** (Software Requirements, Ch. 3) | Lists Room DB, no auth-backend entry | Add a row: Firebase Authentication — email/password identity management and verification for registered accounts. |
| **Figure 3** (System Architecture Overview, Ch. 3) | No external auth service shown | Add Firebase Authentication as a new external entity, with a clear "guest mode bypasses this entirely" note so the diagram doesn't imply every user hits the cloud. |
| **In-app Privacy Policy text** (`ProfileScreen.kt`) | Currently states data is stored locally | Needs an added disclosure: registered-account emails and verification status are held by Firebase (Google's infrastructure); everything else (scans, profile details, guest accounts) stays on-device. |

None of this needs to happen before implementation starts, per the team's call — but it should
land before Sprint 8 documentation work locks in, so the manuscript and the shipped app don't
quietly diverge the way the confidence-threshold table almost did.

---

## Open Questions for the Team

- Does "guest mode" need its own explicit mention in the FR table (FR1-11), or is it covered
  implicitly under existing login/register FRs? Worth checking with your adviser given IC1 is
  already being revised anyway.
- Should guest-created data ever be *upgradeable* to a real account later (e.g., "sign up to
  save your guest history to the cloud")? Not required for the stated goals (verification +
  user count), but worth deciding now rather than after guest mode ships, since it changes the
  data model (whether guest Room records need a migration path to a Firebase UID later).
