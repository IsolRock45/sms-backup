package com.pavel.foregroundapptracker

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pavel.foregroundapptracker.data.AppUsageAggregate
import com.pavel.foregroundapptracker.ui.theme.ForegroundAppTrackerTheme
import com.pavel.foregroundapptracker.worker.UsageSyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ForegroundAppTrackerTheme {
                TrackerScreen()
            }
        }
    }
}

@Composable
private fun TrackerScreen() {
    val context = LocalContext.current
    val app = remember { context.applicationContext as ForegroundAppTrackerApp }
    val coroutineScope = rememberCoroutineScope()

    var hasAccess by remember { mutableStateOf(UsageAccess.isGranted(context)) }
    var lastSyncResult by remember { mutableStateOf<String?>(null) }

    // Re-check Usage Access on every ON_START — the user toggles it in
    // Settings (separate Activity), so we can't get a result callback.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                hasAccess = UsageAccess.isGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val sinceMillis = remember { System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 }
    val aggregates: Flow<List<AppUsageAggregate>> = remember {
        runCatching { app.database.appUsageDao().observeAggregates(sinceMillis) }
            .getOrElse { flowOf(emptyList()) }
    }
    val rows by aggregates.collectAsState(initial = emptyList())

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Foreground app tracker",
                style = MaterialTheme.typography.headlineSmall,
            )
            if (!hasAccess) {
                Text(
                    text = "Usage Access is not granted. Tap below and enable \"Foreground App Tracker\" in the system list.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                    )
                }) { Text("Open Usage Access settings") }
            } else {
                Text(
                    text = "Usage Access granted.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        coroutineScope.launch {
                            val sync = UsageStatsSync.create(context)
                            if (sync == null) {
                                lastSyncResult = "UsageStatsManager unavailable"
                                return@launch
                            }
                            val now = System.currentTimeMillis()
                            val begin = now - UsageStatsSync.DEFAULT_LOOKBACK_MILLIS
                            runCatching { sync.sync(begin, now) }
                                .onSuccess { lastSyncResult = "Sync inserted $it sessions" }
                                .onFailure { lastSyncResult = "Sync failed: ${it.message}" }
                        }
                    }) { Text("Sync now (last 7 days)") }
                    OutlinedButton(onClick = {
                        UsageSyncWorker.ensureScheduled(context)
                        lastSyncResult = "Periodic sync scheduled (every 15 min)"
                    }) { Text("Schedule periodic") }
                    OutlinedButton(onClick = {
                        coroutineScope.launch {
                            app.database.appUsageDao().clear()
                            lastSyncResult = "Cleared"
                        }
                    }) { Text("Clear") }
                }
                lastSyncResult?.let {
                    Text(text = it, style = MaterialTheme.typography.labelMedium)
                }
            }

            HorizontalDivider()
            Text(
                text = "Apps (last 7 days, ${rows.size}):",
                style = MaterialTheme.typography.titleMedium,
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(rows, key = { it.packageName }) { aggregate ->
                    AggregateRow(aggregate)
                }
            }
        }
    }
}

@Composable
private fun AggregateRow(row: AppUsageAggregate) {
    val timeFmt = remember(row.lastUsedAt) {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(row.lastUsedAt))
    }
    val totalMin = row.totalDurationMillis / 60_000
    Column {
        Text(
            text = row.appLabel ?: row.packageName,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "${row.packageName} • ${row.sessionCount} sessions • ${totalMin} min total • last $timeFmt",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
