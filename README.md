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
- Persists messages locally with Room (SQLite)
- Runtime permission flow for `RECEIVE_SMS` / `READ_SMS`
- Simple Compose UI listing the most recent backed-up messages

### `:reminders` — Personal Reminders

A small notification helper for a personal task management system.

- `ReminderNotifier.show(context, title, message)` is callable from
  anywhere — Activity, ViewModel, `WorkManager` worker, AlarmReceiver, etc.
- Heads-up display: notification channel `IMPORTANCE_HIGH` + builder
  `PRIORITY_HIGH` + `CATEGORY_REMINDER`
- Star vector drawable (`ic_reminder_star.xml`) used as the small icon
- Tapping opens the launcher Activity and auto-cancels the notification
- Runtime `POST_NOTIFICATIONS` permission flow for Android 13+

## Project layout

```
app/                            # :app  — SMS Backup
  src/main/java/com/pavel/smsbackup/
    MainActivity.kt
    SmsBackupApp.kt
    data/{SmsMessageEntity,SmsMessageDao,AppDatabase}.kt
    receiver/SmsReceiver.kt

reminders/                      # :reminders  — Personal Reminders
  src/main/java/com/pavel/reminders/
    MainActivity.kt
    RemindersApp.kt
    ReminderNotifier.kt          # show() / cancel() — call from anywhere
```

## Build

Open in Android Studio (Hedgehog or newer), let Gradle sync, then
*Run > app* or *Run > reminders*.

From the command line (requires the Android SDK and a `local.properties`
with `sdk.dir`):

```bash
./gradlew assembleDebug                  # all modules
./gradlew :app:installDebug              # SMS Backup
./gradlew :reminders:installDebug        # Personal Reminders
```

## Permissions

| Module        | Permission                          | How it's granted                                              |
|---------------|-------------------------------------|---------------------------------------------------------------|
| `:app`        | `RECEIVE_SMS`, `READ_SMS`           | Runtime dialog (`ActivityResultContracts`)                    |
| `:reminders`  | `POST_NOTIFICATIONS` (API 33+ only) | Runtime dialog (`ActivityResultContracts.RequestPermission`)  |

## Privacy / scope

Each module only persists data captured *after* its access is granted, on
the device it is installed on. Nothing is uploaded by this code. Sync to a
private server is intentionally left out of the initial scaffold — add an
outbound worker (e.g. `WorkManager` + Retrofit) and authenticate against
your own endpoint when ready.
