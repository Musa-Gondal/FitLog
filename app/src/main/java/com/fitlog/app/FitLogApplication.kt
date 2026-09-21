package com.fitlog.app

import android.app.Application
import com.fitlog.app.notify.Notifications
import com.fitlog.app.notify.Reminders

class FitLogApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        Reminders.scheduleAll(this)
    }
}
