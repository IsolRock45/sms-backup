package com.pavel.tracker

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pavel.tracker.data.NotificationEntity
import com.pavel.tracker.service.NotificationCaptureService
import com.pavel.tracker.ui.theme.TrackerTheme
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Single-screen UI:
 *  - Shows whether the user has granted notification-listener access to
 *    this app, with a button to jump to the right Settings screen.
 *  - Lists the most recent captured notifications.
 *
 * Capture itself is handled by [NotificationCaptureService] for the entire
 * time access is granted — independently of whether this Activity is in
 * the foreground.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrackerTheme {
                val dao = remember { (application as TrackerApp).database.notificationDao() }
                val recent: Flow<List<NotificationEntity>> = remember {
                    dao.observeRecent(limit = 200)
                }
                TrackerScreen(messagesFlow = recent)
            }
        }
    }
}

@Composable
private fun TrackerScreen(messagesFlow: Flow<List<NotificationEntity>>) {
    val context = LocalContext.current

    var hasAccess by remember { mutableStateOf(context.hasNotificationAccess()) }

    // Re-check access whenever the Activity returns to the foreground —
    // typically right after the user toggled access in Settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                hasAccess = context.hasNotificationAccess()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForSettings()
    val notifications by messagesFlow.collectAsState(initial = emptyList())

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Notification Tracker",
                style = MaterialTheme.typography.headlineSmall,
            )

            AccessStatus(hasAccess = hasAccess) {
                launcher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }

            HorizontalDivider()

            Text(
                text = "Recent (${notifications.size})",
                style = MaterialTheme.typography.titleMedium,
            )

            NotificationList(notifications = notifications)
        }
    }
}

@Composable
private fun AccessStatus(hasAccess: Boolean, onRequest: () -> Unit) {
    Text(
        text = if (hasAccess) {
            "Notification access granted — capture is running."
        } else {
            "Notification access not granted. Tap below and toggle Tracker on in Settings."
        },
        style = MaterialTheme.typography.bodyMedium,
    )
    if (!hasAccess) {
        Button(onClick = onRequest) {
            Text("Open Notification Access settings")
        }
    }
}

@Composable
private fun NotificationList(notifications: List<NotificationEntity>) {
    if (notifications.isEmpty()) {
        Text(
            text = "Nothing captured yet. Toggle access on and wait for the next notification.",
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(notifications, key = { it.id }) { n ->
            Column {
                Text(
                    text = "${n.packageName}  ·  ${formatTime(n.postedAt)}",
                    style = MaterialTheme.typography.labelMedium,
                )
                if (!n.title.isNullOrBlank()) {
                    Text(
                        text = n.title,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                if (!n.text.isNullOrBlank()) {
                    Text(
                        text = n.text,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberLauncherForSettings() =
    androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { /* state is re-checked in ON_START */ },
    )

/**
 * The canonical "is my listener service enabled?" check. We compare against
 * a flat colon-separated list of `ComponentName` strings stored in Settings.
 * `NotificationManagerCompat.getEnabledListenerPackages` only checks at the
 * package level, which would falsely report `true` for any other listener
 * service in the same APK.
 */
private fun android.content.Context.hasNotificationAccess(): Boolean {
    val flat = Settings.Secure.getString(contentResolver, ENABLED_NOTIFICATION_LISTENERS) ?: return false
    val mine = ComponentName(this, NotificationCaptureService::class.java)
    return flat.split(':').any { entry ->
        ComponentName.unflattenFromString(entry) == mine
    }
}

private const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"

private fun formatTime(epochMillis: Long): String =
    DateFormat
        .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        .format(Date(epochMillis))

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun TrackerScreenPreview() {
    TrackerTheme {
        TrackerScreen(messagesFlow = flowOf(emptyList()))
    }
}
