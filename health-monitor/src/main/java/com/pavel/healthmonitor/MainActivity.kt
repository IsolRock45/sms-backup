package com.pavel.healthmonitor

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pavel.healthmonitor.data.HeartRateSample
import com.pavel.healthmonitor.service.HealthMonitorService
import com.pavel.healthmonitor.ui.theme.HealthMonitorTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Visible launcher Activity. The user MUST be able to open the app and
 * see their data — this is what separates a personal health monitor
 * from stalkerware. Don't drop the launcher intent filter.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthMonitorTheme {
                HealthMonitorScreen()
            }
        }
    }
}

@Composable
private fun HealthMonitorScreen() {
    val context = LocalContext.current
    val app = remember { context.applicationContext as HealthMonitorApp }
    val recent: Flow<List<HeartRateSample>> = remember {
        runCatching { app.database.heartRateDao().observeRecent() }
            .getOrElse { flowOf(emptyList()) }
    }
    val samples by recent.collectAsState(initial = emptyList())

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) HealthMonitorService.start(context)
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
                text = "Health Monitor",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Continuous foreground service. Visible icon, visible UI, visible " +
                    "ongoing notification — that's the contract. Tap Start to begin sampling " +
                    "(placeholder data until a real sensor source is wired in).",
                style = MaterialTheme.typography.bodyMedium,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            HealthMonitorService.start(context)
                        }
                    },
                ) {
                    Text("Start monitoring")
                }
                OutlinedButton(onClick = { HealthMonitorService.stop(context) }) {
                    Text("Stop")
                }
            }

            Text(
                text = "Recent samples (${samples.size}):",
                style = MaterialTheme.typography.titleMedium,
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(samples, key = { it.id }) { sample ->
                    SampleRow(sample)
                }
            }
        }
    }
}

@Composable
private fun SampleRow(sample: HeartRateSample) {
    val time = remember(sample.recordedAt) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(sample.recordedAt))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "$time  •  ${sample.bpm} bpm")
        Text(
            text = sample.source,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
