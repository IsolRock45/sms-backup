package com.pavel.inputlogger

import android.util.Log
import com.pavel.inputlogger.data.EventType
import com.pavel.inputlogger.data.InputEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Single point that receives `LoggedTextField` events and persists them.
 *
 * Scope: process-lifetime. The application class holds the DB; this object
 * just routes events onto a background coroutine so callers don't have to.
 *
 * The wrapper [com.pavel.inputlogger.LoggedTextField] is the only intended
 * caller — it's a developer-facing alternative to the View-system
 * `TextWatcher`. Other code shouldn't be calling these directly except in
 * tests.
 */
object InputLogger {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun logTextChanged(fieldId: String, oldText: String, newText: String) {
        if (oldText == newText) return // de-bounce idempotent recompositions
        log(
            fieldId = fieldId,
            type = EventType.TEXT_CHANGED,
            text = newText,
            charsDelta = newText.length - oldText.length,
        )
    }

    fun logFocusGained(fieldId: String, currentText: String) {
        log(
            fieldId = fieldId,
            type = EventType.FOCUS_GAINED,
            text = currentText,
            charsDelta = 0,
        )
    }

    fun logFocusLost(fieldId: String, currentText: String) {
        log(
            fieldId = fieldId,
            type = EventType.FOCUS_LOST,
            text = currentText,
            charsDelta = 0,
        )
    }

    fun logSubmit(fieldId: String, text: String) {
        log(
            fieldId = fieldId,
            type = EventType.SUBMIT,
            text = text,
            charsDelta = 0,
        )
    }

    private fun log(fieldId: String, type: EventType, text: String, charsDelta: Int) {
        val event = InputEvent(
            fieldId = fieldId,
            eventType = type.name,
            text = text,
            textLength = text.length,
            charsDelta = charsDelta,
            timestamp = System.currentTimeMillis(),
        )
        scope.launch {
            try {
                InputLoggerApp.get().database.inputEventDao().insert(event)
            } catch (t: Throwable) {
                Log.e(TAG, "failed to persist input event for $fieldId", t)
            }
        }
    }

    private const val TAG = "InputLogger"
}
