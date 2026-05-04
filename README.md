# Personal Trackers

A multi-module Android Studio project hosting personal-use, on-device data
capture tools. Each module is its own installable APK, but they share a
single Gradle build, version catalog and CI workflow.

## Modules

### `:app` — SMS Backup

A minimal personal app that captures incoming SMS messages and stores them
locally in a Room (SQLite) database. Intended as a backup of *your own*
incoming messages on *your own* device, before being synced to *your own*
private server.

- `BroadcastReceiver` for `android.provider.Telephony.SMS_RECEIVED`
- Extracts sender phone number, message body (multi-part messages joined),
  and receive timestamp
- Persists messages locally with Room
- Runtime permission flow for `RECEIVE_SMS` / `READ_SMS`
- Compose UI listing the most recent backed-up messages

### `:foreground-app-tracker` — UsageStatsManager session log

Aggregates which apps are in the foreground and for how long, by walking
the events stream from `UsageStatsManager.queryEvents()`. Sessions are
persisted locally as `(packageName, startedAt, endedAt, durationMillis)`
rows — no AccessibilityService, no content capture from other apps.

- `UsageStatsSync` walks `ACTIVITY_RESUMED` / `ACTIVITY_PAUSED` /
  `ACTIVITY_STOPPED` and emits one closed `AppUsageSession` per pair
- App labels are resolved at sync time via `PackageManager`
- Periodic `UsageSyncWorker` (15-min cadence, 30-min window) runs via
  WorkManager so the table grows even when the user doesn't open the app
- "Sync now (last 7 days)" button does a one-shot catch-up sync; "Clear"
  drops every row
- Aggregate query groups sessions by package and orders by total duration

`PACKAGE_USAGE_STATS` is a system-controlled appop — the user must enable
"Foreground App Tracker" inside *Settings → Apps → Special access →
Usage access*. The activity has a button that jumps directly there.

## Project layout

```
app/                            # :app  — SMS Backup
  src/main/java/com/pavel/smsbackup/
    MainActivity.kt
    SmsBackupApp.kt
    data/{SmsMessageEntity,SmsMessageDao,AppDatabase}.kt
    receiver/SmsReceiver.kt

foreground-app-tracker/         # :foreground-app-tracker  — UsageStatsManager
  src/main/java/com/pavel/foregroundapptracker/
    MainActivity.kt
    ForegroundAppTrackerApp.kt
    UsageAccess.kt              # PACKAGE_USAGE_STATS appop check
    UsageStatsSync.kt           # queryEvents → sessions → Room
    worker/UsageSyncWorker.kt   # WorkManager periodic sync
    data/{AppUsageSession,AppUsageDao,TrackerDatabase}.kt
```

## Build

```bash
./gradlew assembleDebug                              # all modules
./gradlew :app:installDebug                          # SMS Backup
./gradlew :foreground-app-tracker:installDebug       # Foreground App Tracker
```

## Permissions

| Module                     | Permission             | How it's granted                                                   |
|----------------------------|------------------------|--------------------------------------------------------------------|
| `:app`                     | `RECEIVE_SMS`, `READ_SMS` | Runtime dialog (`ActivityResultContracts`)                      |
| `:foreground-app-tracker`  | `PACKAGE_USAGE_STATS`  | User toggles "Usage access" in Settings (in-app deep link button)  |

## Privacy / scope

Each module persists data captured on the device it is installed on.
Nothing is uploaded by this code.

`:foreground-app-tracker` only stores app metadata (package name, label,
start/end timestamps, duration). No window content, no input, no URLs.
