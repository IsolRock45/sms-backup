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

### `:input-logger` — Own-App Input Logger

A Compose-native equivalent of `TextWatcher`, scoped to fields in *this
module's own UI* (not other apps). For local mobile-usability research
where the subject is also the device owner.

- `LoggedTextField(fieldId, value, onValueChange, label)` — drop-in
  replacement for `OutlinedTextField` that emits `TEXT_CHANGED`,
  `FOCUS_GAINED`, `FOCUS_LOST`, and `SUBMIT` events
- `InputLogger` singleton routes events onto a service-scoped `Dispatchers.IO`
  coroutine and persists them in a Room database (`input_logger.db`)
- One `InputEvent` row per event with field id, event type, current text,
  text length, and `charsDelta` (positive = insertion, negative = deletion)
- No system-level permissions. No AccessibilityService. The `LoggedTextField`
  wrapper is the *only* way for content to enter the table — plain
  `TextField`s elsewhere in the app are not affected.

## Project layout

```
app/                            # :app  — SMS Backup
  src/main/java/com/pavel/smsbackup/
    MainActivity.kt
    SmsBackupApp.kt
    data/{SmsMessageEntity,SmsMessageDao,AppDatabase}.kt
    receiver/SmsReceiver.kt

input-logger/                   # :input-logger  — Own-App Input Logger
  src/main/java/com/pavel/inputlogger/
    MainActivity.kt
    InputLoggerApp.kt
    InputLogger.kt              # singleton routing events to Room
    LoggedTextField.kt          # Compose wrapper that emits events
    data/{InputEvent,InputEventDao,InputDatabase}.kt
```

## Build

```bash
./gradlew assembleDebug                       # all modules
./gradlew :app:installDebug                   # SMS Backup
./gradlew :input-logger:installDebug          # Input Logger
```

## Permissions

| Module           | Permission                          | How it's granted                                              |
|------------------|-------------------------------------|---------------------------------------------------------------|
| `:app`           | `RECEIVE_SMS`, `READ_SMS`           | Runtime dialog (`ActivityResultContracts`)                    |
| `:input-logger`  | None                                | Logs only fields it owns inside its own UI                    |

## Privacy / scope

Each module persists data captured on the device it is installed on.
Nothing is uploaded by this code.

`:input-logger` deliberately does NOT use AccessibilityService, IME, or any
other system-wide capture mechanism. It logs only fields explicitly wrapped
with `LoggedTextField` inside this module's own Activity — appropriate for
self-experiments and consented usability research, not for capturing input
from other apps.
