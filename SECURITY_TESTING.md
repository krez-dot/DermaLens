# DermaLens — Security Testing

Testing date: 2026-08-24
Scope: full app (Kotlin source, `AndroidManifest.xml`, resource XML, Room DB layer, network calls)
Methodology: static code review + manifest/configuration audit, cross-checked against the OWASP Mobile Application Security Verification Standard (MASVS) categories for local data storage, network communication, authentication, and platform interaction.

---

## Summary

| Severity | Count | Status |
|---|---|---|
| High | 0 | — |
| Medium | 2 | Open — recommendations below |
| Low | 1 | Open |
| Resolved this sprint | 5 | Fixed (see [Resolved Findings](#resolved-findings-this-sprint)) |

No high-severity or remotely-exploitable vulnerabilities were found. The two medium findings are both local-device platform-configuration gaps (Android manifest settings), not code-level bugs — both are one-line fixes.

---

## Test Cases

| ID | Test Case | Method | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|
| SEC-01 | SQL injection via login/register/edit-profile input | Reviewed all `UserDao`/`ScanRecordDao` queries | All queries use parameterized `@Query` bindings, no string concatenation | Confirmed — no raw SQL concatenation anywhere in the DAO layer | ✅ Pass |
| SEC-02 | Password storage | Reviewed `hashPassword()`/`verifyPassword()` in `DermaColors.kt` | Passwords not stored in plaintext | Salted SHA-256 (`saltHex:hashHex`), `SecureRandom` for salt generation | ✅ Pass |
| SEC-03 | Backward-compatible login after the salting change | Traced `verifyPassword()`'s legacy-hash branch | Existing accounts (pre-salt) should still log in without being locked out | Confirmed — falls back to unsalted comparison only for hashes with no `:` separator | ✅ Pass |
| SEC-04 | Network transport security | Grepped all network calls (`ClinicLocatorScreen.kt` — Overpass API, OSRM routing) | All external calls use HTTPS | Confirmed — no `http://` endpoints found anywhere in source | ✅ Pass |
| SEC-05 | Cleartext traffic policy | Checked `AndroidManifest.xml` | Cleartext traffic should be disabled since no endpoint needs it | `android:usesCleartextTraffic="true"` is set app-wide with no actual cleartext usage | ⚠️ Finding (Medium) — see SEC-F1 |
| SEC-06 | Local data backup exposure | Checked `allowBackup`, `backup_rules.xml`, `data_extraction_rules.xml` | Sensitive tables (users, password hashes) should be excluded from backup, or backup disabled | `allowBackup="true"` with both rule files left as unmodified boilerplate (no exclusions) | ⚠️ Finding (Medium) — see SEC-F2 |
| SEC-07 | Exported component surface | Checked all `<activity>`/`<service>`/`<receiver>`/`<provider>` entries | Only the launcher activity should be exported | Only `MainActivity` is exported (required for the launcher intent-filter); no other components declared | ✅ Pass |
| SEC-08 | File URI exposure (captured scan photos) | Reviewed `CameraScreen.kt`'s `Uri.fromFile()` usage | Captured photo URI shouldn't be exposed to other apps or allow path traversal | Filename is timestamp-based (no user input), URI is only used in-process for Compose Navigation, never passed via `Intent` to another app | ✅ Pass |
| SEC-09 | Crash-based DoS via malformed profile input | Tested blank name, blank email, duplicate email in Edit Profile / Register | App should show an error, not crash | Now guarded with validation + `try/catch` around `SQLiteConstraintException` (fixed this sprint — see below) | ✅ Pass |
| SEC-10 | Privacy Policy accuracy vs. actual implementation | Compared in-app Privacy Policy text against actual storage/upload behavior | Claims should match reality | Corrected this sprint — no longer claims "encrypted storage" or upload-time anonymization that don't exist | ✅ Pass |
| SEC-11 | Contributed research photos — network exposure | Traced the "Contribute to Research" toggle's data path end-to-end | Photos should not be transmitted anywhere without user awareness | Confirmed — photos are copied to internal app storage only (`filesDir/contributed_scans/`); no upload code exists anywhere in the app | ✅ Pass (low priority note — see SEC-F3) |
| SEC-12 | Destructive schema migration | Checked `DermaDatabase.kt` migration strategy | Should not silently destroy user data | Uses `fallbackToDestructiveMigration()` — any future schema bump wipes all accounts/scans with no warning | ⚠️ Finding (Low) — see SEC-F4 |

---

## Findings

### SEC-F1 (Medium) — Cleartext traffic allowed but unused
**File:** `app/src/main/AndroidManifest.xml:14`

`android:usesCleartextTraffic="true"` permits the app to make plaintext HTTP connections. Every network call actually made (Overpass API, OSRM routing) already uses HTTPS, so this flag currently grants attack surface with no functional benefit — it would allow a downgrade to plaintext if a future network call (or a compromised/misconfigured library) accidentally used `http://`, exposing that traffic to on-path interception (e.g. on public Wi-Fi).

**Fix:** Set `android:usesCleartextTraffic="false"` (or omit it — `false` is the default on API 28+, which this app already targets via `minSdk 26`/`targetSdk 35`, though `minSdk 26` means explicitly setting `false` is safer than relying on the default for API 26-27 devices).

### SEC-F2 (Medium) — App data is fully backup-eligible with no exclusions
**File:** `app/src/main/AndroidManifest.xml:15-17`, `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`

`android:allowBackup="true"` is set, and both the legacy backup rules (API <31) and the newer data extraction rules (API 31+) are left as unmodified boilerplate — every `<include>`/`<exclude>` is commented out, meaning **no data is excluded from backup**. Combined effect:

- On API 26-30 devices, `adb backup` (available with USB debugging enabled, no root required) can extract the entire app-private storage — including the Room database (names, emails, salted password hashes, scan history) — to an attacker's machine for offline analysis.
- On API 31+ devices, the same data is eligible for Android's automatic cloud backup to the user's Google account, which is usually desirable for UX but means the DB isn't cordoned off from a general-purpose backup channel a capstone panel or later contributor might not expect.

**Fix:** Either set `android:allowBackup="false"` (simplest, and reasonable for an app not intending to support device-transfer restore of local health data), or keep backup enabled but explicitly exclude the Room database file in both rule files:
```xml
<!-- backup_rules.xml -->
<full-backup-content>
    <exclude domain="database" path="." />
</full-backup-content>

<!-- data_extraction_rules.xml -->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="database" path="." />
    </cloud-backup>
    <device-transfer>
        <exclude domain="database" path="." />
    </device-transfer>
</data-extraction-rules>
```

### SEC-F3 (Low, informational) — "Anonymized" claim already corrected, but no anonymization exists
Already addressed in the Privacy Policy text (SEC-10), but worth restating for the record: contributed photos are copied byte-for-byte with no EXIF stripping. This has zero real-world impact today since nothing is uploaded (SEC-11), but should be implemented before any future Firebase/cloud upload feature ships, so the "opt-in, anonymized" framing stays true once photos actually leave the device.

### SEC-F4 (Low) — Silent destructive DB migration
**File:** `app/src/main/java/com/dermalens/app/data/db/DermaDatabase.kt:26`

`fallbackToDestructiveMigration()` means any future schema version bump drops and recreates every table with no warning to the user and no migration path — full account/scan data loss. Not an externally exploitable vulnerability, but a data-integrity risk worth fixing with a proper `Migration` object before more schema changes land, especially this close to a documentation/demo deadline where losing test data would be disruptive.

---

## Resolved Findings (this sprint)

The following were found and fixed earlier in this sprint, prior to this testing pass — included here for a complete record:

1. **Unsalted SHA-256 passwords** → salted SHA-256 with `SecureRandom`, backward-compatible verification for existing accounts (SEC-02, SEC-03)
2. **False "encrypted storage" claim in Privacy Policy** → corrected to describe actual (salted-hash, unencrypted local DB) storage (SEC-10)
3. **Blank-name crash in Profile screen** (`NoSuchElementException`) → input filtered safely, falls back to `"?"` avatar initial
4. **Unhandled duplicate-email crash in Edit Profile** (`SQLiteConstraintException`) → pre-check + validation + `try/catch`
5. **Unguarded duplicate-email race in Register** → `try/catch` added as defense in depth

---

## Recommendations (priority order)

1. Fix SEC-F1 and SEC-F2 — both are one-line/one-file manifest changes with no functional downside for this app.
2. Consider upgrading password hashing from salted SHA-256 to a purpose-built slow hash (bcrypt, Argon2, or PBKDF2 with a high iteration count) before any production/public release — SHA-256 is fast by design, making it comparatively cheap to brute-force even when salted, if the DB is ever extracted. Not urgent for a capstone defense, but worth a documentation note if asked about it.
3. Replace `fallbackToDestructiveMigration()` with a real `Migration` before the next schema change (SEC-F4).
4. If/when the Firebase upload feature for "Contribute to Research" is built, add EXIF stripping before upload so the "anonymized" claim in the Privacy Policy stays accurate (SEC-F3).
