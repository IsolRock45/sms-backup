package com.pavel.reminders

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pavel.reminders.ui.theme.RemindersTheme

/**
 * Demo Activity: lets you type a title + message and tap "Show reminder".
 * Real callers should invoke [ReminderNotifier.show] directly from
 * wherever the reminder is triggered (a `WorkManager` worker, an
 * `AlarmManager` callback, a button press, etc.).
 *
 * The Activity also handles the one place we *do* need a permission
 * dialog — `POST_NOTIFICATIONS` on Android 13+.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemindersTheme {
                ReminderDemoScreen()
            }
        }
    }
}

@Composable
private fun ReminderDemoScreen() {
    val context = LocalContext.current

    var title by rememberSaveable { mutableStateOf("Take a break") }
    var message by rememberSaveable { mutableStateOf("Stand up and stretch for 2 minutes.") }
    var lastId by remember { mutableStateOf<Int?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                lastId = ReminderNotifier.show(context, title, message)
            }
        },
    )

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Reminders demo",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Calls ReminderNotifier.show(context, title, message). " +
                    "Same call works from anywhere in the app — Worker, AlarmReceiver, ViewModel.",
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    val id = ReminderNotifier.show(context, title, message)
                    if (id != null) {
                        lastId = id
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Permission missing on Android 13+; ask for it.
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            ) {
                Text("Show reminder")
            }

            lastId?.let {
                Text(
                    text = "Last reminder id: $it (auto-cancels on tap).",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
