package com.pavel.inputlogger

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged

/**
 * Compose equivalent of the View-system `TextWatcher` pattern, scoped to a
 * single field inside this app.
 *
 * Drop-in replacement for [androidx.compose.material3.OutlinedTextField] that
 * additionally records `TEXT_CHANGED`, `FOCUS_GAINED`, `FOCUS_LOST` and
 * (optionally) `SUBMIT` events to a Room table via [InputLogger].
 *
 * Privacy contract:
 * - Only fields explicitly wrapped with this Composable are logged. Plain
 *   `OutlinedTextField` / `TextField` instances elsewhere in the app are
 *   not affected.
 * - The captured data lives in `input_logger.db` and is never sent off-device
 *   by this code.
 *
 * Usage:
 * ```kotlin
 * var query by remember { mutableStateOf("") }
 * LoggedTextField(
 *     fieldId = "search.query",
 *     value = query,
 *     onValueChange = { query = it },
 *     label = "Search",
 * )
 * ```
 */
@Composable
fun LoggedTextField(
    fieldId: String,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    var hadFocus by remember(fieldId) { mutableStateOf(false) }
    var lastValue by remember(fieldId) { mutableStateOf(value) }

    LaunchedEffect(fieldId, value) {
        // Skip the first composition (lastValue == value) so we don't log
        // a "TEXT_CHANGED" for the initial render.
        if (value != lastValue) {
            InputLogger.logTextChanged(fieldId, lastValue, value)
            lastValue = value
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        modifier = modifier.onFocusChanged { focusState ->
            if (focusState.isFocused && !hadFocus) {
                hadFocus = true
                InputLogger.logFocusGained(fieldId, value)
            } else if (!focusState.isFocused && hadFocus) {
                hadFocus = false
                InputLogger.logFocusLost(fieldId, value)
            }
        },
    )
}
