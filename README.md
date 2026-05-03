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

### `:health-monitor` — Continuous Vitals Monitor

A continuous health monitoring app structured the way personal-use
monitors are *supposed* to be structured: visible launcher icon, visible
ongoing notification, foreground service.

- Foreground `Service` with `foregroundServiceType="health"` (Android 14+)
  and `FOREGROUND_SERVICE_HEALTH` permission
- Persistent low-priority "Monitoring vitals" notification while running
- `START_STICKY` so the OS recreates the service after a low-memory kill
- `BootCompletedReceiver` re-starts the service after device reboot
  (Android 10+: only after the user has launched the app at least once)
- Runtime `POST_NOTIFICATIONS` permission flow for Android 13+
- Compose UI with Start / Stop buttons and a list of recent samples
- Room database (`health_monitor.db`) with one `HeartRateSample` per
  reading; current source is a placeholder ticker (phones don't ship
  `Sensor.TYPE_HEART_RATE`) tagged `source = "placeholder"` for trivial
  filtering once a real source — BLE GATT 0x2A37, Health Connect, or
  Wear OS data layer — is wired in.

## Project layout

```
app/                            # :app  — SMS Backup
  src/main/java/com/pavel/smsbackup/
    MainActivity.kt
    SmsBackupApp.kt
    data/{SmsMessageEntity,SmsMessageDao,AppDatabase}.kt
    receiver/SmsReceiver.kt

health-monitor/                 # :health-monitor  — Continuous Vitals Monitor
  src/main/java/com/pavel/healthmonitor/
    MainActivity.kt
    HealthMonitorApp.kt
    data/{HeartRateSample,HeartRateDao,HealthDatabase}.kt
    service/HealthMonitorService.kt
    receiver/BootCompletedReceiver.kt
```

## Build

Open in Android Studio (Hedgehog or newer), let Gradle sync, then
*Run > app* or *Run > health-monitor*.

From the command line (requires the Android SDK and a `local.properties`
with `sdk.dir`):

```bash
./gradlew assembleDebug                       # all modules
./gradlew :app:installDebug                   # SMS Backup
./gradlew :health-monitor:installDebug        # Health Monitor
```

## Permissions

| Module             | Permission                                                                 | How it's granted                                              |
|--------------------|----------------------------------------------------------------------------|---------------------------------------------------------------|
| `:app`             | `RECEIVE_SMS`, `READ_SMS`                                                  | Runtime dialog (`ActivityResultContracts`)                    |
| `:health-monitor`  | `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_HEALTH`, `RECEIVE_BOOT_COMPLETED` | Normal — granted at install                                   |
| `:health-monitor`  | `POST_NOTIFICATIONS` (API 33+ only)                                        | Runtime dialog (`ActivityResultContracts.RequestPermission`)  |
| `:health-monitor`  | `BODY_SENSORS`, `BODY_SENSORS_BACKGROUND`                                  | Runtime dialog when a real sensor source is wired in          |

## Privacy / scope

Each module persists data captured on the device it is installed on.
Nothing is uploaded by this code. Sync to a private server is intentionally
left out of the scaffold — add an outbound worker (e.g. `WorkManager` +
Retrofit) and authenticate against your own endpoint when ready.

`:health-monitor` deliberately keeps the launcher icon visible. A
continuous-monitoring app installed on the user's own device with no way
to open it would be the architecture of stalkerware, not a personal
tracker — keep the icon.
