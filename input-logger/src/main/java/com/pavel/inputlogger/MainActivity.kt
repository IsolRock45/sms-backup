package com.pavel.inputlogger

import android.os.Bundle
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.pavel.inputlogger.data.InputEvent
import com.pavel.inputlogger.ui.theme.InputLoggerTheme
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
            InputLoggerTheme {
                InputLoggerScreen()
            }
        }
    }
}

@Composable
private fun InputLoggerScreen() {
    val context = LocalContext.current
    val app = remember { context.applicationContext as InputLoggerApp }
    val coroutineScope = rememberCoroutineScope()

    val events: Flow<List<InputEvent>> = remember {
        runCatching { app.database.inputEventDao().observeRecent() }
            .getOrElse { flowOf(emptyList()) }
    }
    val rows by events.collectAsState(initial = emptyList())

    var search by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var comment by rememberSaveable { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Input logger",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Logs only LoggedTextField fields below — own-app only, no AccessibilityService.",
                style = MaterialTheme.typography.bodyMedium,
            )

            LoggedTextField(
                fieldId = "demo.search",
                value = search,
                onValueChange = { search = it },
                label = "Search query",
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { InputLogger.logSubmit("demo.search", search) },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            LoggedTextField(
                fieldId = "demo.name",
                value = name,
                onValueChange = { name = it },
                label = "Name",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LoggedTextField(
                fieldId = "demo.comment",
                value = comment,
                onValueChange = { comment = it },
                label = "Comment",
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    InputLogger.logSubmit("demo.comment", comment)
                }) { Text("Submit comment") }
                OutlinedButton(onClick = {
                    coroutineScope.launch {
                        app.database.inputEventDao().clear()
                    }
                }) { Text("Clear log") }
            }

            Text(
                text = "Logged events (${rows.size}):",
                style = MaterialTheme.typography.titleMedium,
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(rows, key = { it.id }) { event ->
                    EventRow(event)
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: InputEvent) {
    val time = remember(event.timestamp) {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(event.timestamp))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = "${event.fieldId} • ${event.eventType} • Δ${event.charsDelta} • len=${event.textLength}",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
