package com.pavel.smsbackup

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pavel.smsbackup.data.SmsMessageEntity
import com.pavel.smsbackup.ui.theme.SMSBackupTheme
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Single-screen UI:
 *  - Asks for RECEIVE_SMS / READ_SMS at runtime.
 *  - Shows the most recent backed-up messages from Room.
 *
 * The actual SMS capture is done by the manifest-registered receiver, so
 * messages keep being saved even when this Activity is not in the foreground.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SMSBackupTheme {
                val dao = remember { (application as SmsBackupApp).database.smsMessageDao() }
                val recent: Flow<List<SmsMessageEntity>> = remember {
                    dao.observeRecent(limit = 100)
                }
                BackupScreen(messagesFlow = recent)
            }
        }
    }
}

@Composable
private fun BackupScreen(messagesFlow: Flow<List<SmsMessageEntity>>) {
    val context = LocalContext.current

    var hasReceive by remember { mutableStateOf(context.hasPermission(Manifest.permission.RECEIVE_SMS)) }
    var hasRead by remember { mutableStateOf(context.hasPermission(Manifest.permission.READ_SMS)) }

    val launcher = rememberPermissionLauncher { granted ->
        hasReceive = granted[Manifest.permission.RECEIVE_SMS] == true
        hasRead = granted[Manifest.permission.READ_SMS] == true
    }

    val messages by messagesFlow.collectAsState(initial = emptyList())

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "SMS Backup",
                style = MaterialTheme.typography.headlineSmall,
            )

            PermissionStatus(hasReceive = hasReceive, hasRead = hasRead) {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS,
                    ),
                )
            }

            HorizontalDivider()

            Text(
                text = "Recent messages (${messages.size})",
                style = MaterialTheme.typography.titleMedium,
            )

            MessageList(messages = messages, contentPadding = PaddingValues(0.dp))
        }
    }
}

@Composable
private fun PermissionStatus(
    hasReceive: Boolean,
    hasRead: Boolean,
    onRequest: () -> Unit,
) {
    val ok = hasReceive && hasRead
    Text(
        text = if (ok) {
            "Permissions granted — incoming SMS will be backed up."
        } else {
            "RECEIVE_SMS / READ_SMS not granted. Tap the button to request them."
        },
        style = MaterialTheme.typography.bodyMedium,
    )
    if (!ok) {
        Spacer(Modifier.height(4.dp))
        Button(onClick = onRequest) {
            Text("Grant SMS permissions")
        }
    }
}

@Composable
private fun MessageList(
    messages: List<SmsMessageEntity>,
    contentPadding: PaddingValues,
) {
    if (messages.isEmpty()) {
        Text(
            text = "No messages yet. Send yourself a test SMS.",
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }
    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            Column {
                Text(
                    text = "${message.sender}  ·  ${formatTime(message.receivedAt)}",
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun rememberPermissionLauncher(
    onResult: (Map<String, Boolean>) -> Unit,
) = androidx.activity.compose.rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
    onResult = onResult,
)

private fun android.content.Context.hasPermission(name: String): Boolean =
    ContextCompat.checkSelfPermission(this, name) == PackageManager.PERMISSION_GRANTED

private fun formatTime(epochMillis: Long): String =
    DateFormat
        .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        .format(Date(epochMillis))

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun BackupScreenPreview() {
    SMSBackupTheme {
        BackupScreen(messagesFlow = flowOf(emptyList()))
    }
}
