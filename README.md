# SMS Backup

A minimal personal Android app that captures incoming SMS messages and stores
them locally in a Room (SQLite) database. Intended as a backup of *your own*
incoming messages on *your own* device, before being synced to *your own*
private server.

## Features

- `BroadcastReceiver` for `android.provider.Telephony.SMS_RECEIVED`
- Extracts sender phone number, message body (multi-part messages joined), and
  receive timestamp
- Persists messages locally with Room (SQLite)
- Runtime permission flow for `RECEIVE_SMS` and `READ_SMS`
- Simple Jetpack Compose UI listing the most recent backed-up messages

## Project layout

```
app/
  src/main/
    AndroidManifest.xml
    java/com/pavel/smsbackup/
      MainActivity.kt           # Compose UI + runtime permission flow
      SmsBackupApp.kt           # Application class, exposes the database
      data/
        SmsMessageEntity.kt     # @Entity row stored in SQLite
        SmsMessageDao.kt        # Insert / query helpers
        AppDatabase.kt          # Room database + singleton accessor
      receiver/
        SmsReceiver.kt          # BroadcastReceiver for SMS_RECEIVED
```

## Build

Open the project in Android Studio (Hedgehog or newer), let Gradle sync, then
*Run > app*.

From the command line (requires the Android SDK and `local.properties` with
`sdk.dir`):

```bash
./gradlew :app:assembleDebug
```

Install onto a connected device or emulator:

```bash
./gradlew :app:installDebug
```

## Permissions

The app requests `RECEIVE_SMS` (and `READ_SMS` so initial state can be read)
at runtime. Both must be granted for the receiver to actually deliver new
messages — Android silently drops `SMS_RECEIVED` broadcasts to apps without
the permission.

## Privacy / scope

The app only persists messages received *after* permission is granted, on the
device it is installed on. Nothing is uploaded by this code. Sync to a private
server is intentionally left out of this initial scaffold; add an outbound
worker (e.g. `WorkManager` + Retrofit) and authenticate against your own
endpoint when you are ready.

Do not publish this app or use it to capture messages you do not own.
