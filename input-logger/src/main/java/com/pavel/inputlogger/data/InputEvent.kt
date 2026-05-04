package com.pavel.inputlogger.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per logged event from a [com.pavel.inputlogger.LoggedTextField].
 *
 * `text` stores the **current** value of the field at the moment of the
 * event. For `TEXT_CHANGED` events, the previous value is reconstructable
 * from the immediately preceding event for the same `fieldId`, so we don't
 * duplicate it.
 */
@Entity(
    tableName = "input_events",
    indices = [
        Index(value = ["fieldId"]),
        Index(value = ["timestamp"]),
    ],
)
data class InputEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Stable id assigned by the developer when wrapping the field. */
    val fieldId: String,
    /** One of [EventType] entries, stored as a String to keep Room schema simple. */
    val eventType: String,
    /** Current text value of the field at the moment of the event. */
    val text: String,
    /** `text.length` denormalized for cheap aggregate queries. */
    val textLength: Int,
    /**
     * `newText.length - oldText.length`. Positive for insertions, negative for
     * deletions (incl. backspace), zero for paste-replacements with same length
     * or focus events. Useful for keystroke-cadence analysis.
     */
    val charsDelta: Int,
    /** Wall-clock time of the event. */
    val timestamp: Long,
)

/** Closed set of event kinds the wrapper emits. */
enum class EventType {
    TEXT_CHANGED,
    FOCUS_GAINED,
    FOCUS_LOST,
    SUBMIT,
    ;

    companion object {
        fun from(name: String): EventType? = values().firstOrNull { it.name == name }
    }
}
