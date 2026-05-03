# Personal Trackers

A multi-module Android Studio project hosting personal-use, on-device data
capture tools. Each tracker is its own installable APK, but they share a
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
- Persists messages locally with Room (SQLite)
- Runtime permission flow for `RECEIVE_SMS` / `READ_SMS`
- Simple Compose UI listing the most recent backed-up messages

### `:tracker` — Notification Tracker

A personal productivity tool that captures every "interesting" notification
posted on the device while access is granted, for offline usage analysis.

- `NotificationListenerService` declared with the
  `BIND_NOTIFICATION_LISTENER_SERVICE` system permission
- Extracts package name, title, text, post timestamp + on-device capture
  timestamp
- Filters out group summaries, foreground-service stickies and ongoing
  notifications so the table represents *events*, not device state
- Persists rows to its own Room database (`tracker.db`)
- Compose UI with a button that jumps straight to
  `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` and re-checks access
  whenever the Activity returns to the foreground

## Project layout

```
app/                            # :app  — SMS Backup
  src/main/java/com/pavel/smsbackup/
    MainActivity.kt
    SmsBackupApp.kt
    data/{SmsMessageEntity,SmsMessageDao,AppDatabase}.kt
    receiver/SmsReceiver.kt

tracker/                        # :tracker  — Notification Tracker
  src/main/java/com/pavel/tracker/
    MainActivity.kt
    TrackerApp.kt
    data/{NotificationEntity,NotificationDao,TrackerDatabase}.kt
    service/NotificationCaptureService.kt
```

## Build

Open in Android Studio (Hedgehog or newer), let Gradle sync, then
*Run > app* or *Run > tracker*.

From the command line (requires the Android SDK and a `local.properties`
with `sdk.dir`):

```bash
./gradlew assembleDebug                  # both modules
./gradlew :app:installDebug              # SMS Backup
./gradlew :tracker:installDebug          # Notification Tracker
```

## Permissions

| Module      | Permission                                    | How it's granted                                              |
|-------------|-----------------------------------------------|---------------------------------------------------------------|
| `:app`      | `RECEIVE_SMS`, `READ_SMS`                     | Runtime dialog (`ActivityResultContracts`)                    |
| `:tracker`  | `BIND_NOTIFICATION_LISTENER_SERVICE` (system) | User toggle in *Settings → Notifications → Device & app notifications* |

The notification listener is **not** a runtime permission — Android requires
the user to flip a switch in Settings. The Tracker UI has a button that
launches that screen directly via `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`.

## Privacy / scope

Both modules only persist data captured *after* their access is granted, on
the device they are installed on. Nothing is uploaded by this code. Sync to
a private server is intentionally left out of the initial scaffold — add an
outbound worker (e.g. `WorkManager` + Retrofit) and authenticate against
your own endpoint when ready.

The notification listener captures content from *all* apps it can see on the
device, including private messages from third parties. Use it only on your
own device for your own usage analysis. Do not publish, redistribute, or
ship the captured database off-device without consent.
