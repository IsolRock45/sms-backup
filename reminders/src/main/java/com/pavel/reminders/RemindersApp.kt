package com.pavel.reminders

import android.app.Application

/**
 * Application class. The notification channel is created lazily on first
 * `ReminderNotifier.show(...)` call so callers don't need to remember to
 * initialize anything.
 */
class RemindersApp : Application()
