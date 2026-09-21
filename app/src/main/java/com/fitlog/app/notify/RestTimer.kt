package com.fitlog.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Rest timer outside the app: a live countdown in the notification shade / lock screen,
 * plus an alarm that alerts you when rest is over if the app is in the background.
 */
object RestTimer {
    private const val NOTIF_COUNTDOWN = 201
    const val NOTIF_DONE = 202
    private const val REQ_ALARM = 203

    private fun alarmIntent(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, REQ_ALARM, Intent(ctx, RestDoneReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun start(ctx: Context, endAtMillis: Long, nextLabel: String?) {
        Notifications.cancel(ctx, NOTIF_DONE)
        val n = Notifications.builder(ctx, Notifications.CH_REST)
            .setContentTitle("Resting…")
            .setContentText(nextLabel?.let { "Next: $it" } ?: "Get ready for your next set")
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(endAtMillis)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setTimeoutAfter((endAtMillis - System.currentTimeMillis() + 1500).coerceAtLeast(1000))
            .setContentIntent(Notifications.openAppIntent(ctx, "active", NOTIF_COUNTDOWN))
            .build()
        Notifications.post(ctx, NOTIF_COUNTDOWN, n)

        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = alarmIntent(ctx)
        am.cancel(pi)
        val exactOk = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        runCatching {
            if (exactOk) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pi)
        }
    }

    fun cancel(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java)?.cancel(alarmIntent(ctx))
        Notifications.cancel(ctx, NOTIF_COUNTDOWN)
    }

    internal fun finished(ctx: Context) {
        Notifications.cancel(ctx, NOTIF_COUNTDOWN)
        if (AppState.foreground) return // the app itself vibrates
        val n = Notifications.builder(ctx, Notifications.CH_REST_DONE)
            .setContentTitle("Rest over – next set!")
            .setContentText("Tap to open your workout")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .setTimeoutAfter(60_000)
            .setContentIntent(Notifications.openAppIntent(ctx, "active", NOTIF_DONE))
            .build()
        Notifications.post(ctx, NOTIF_DONE, n)
    }
}

class RestDoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RestTimer.finished(context.applicationContext)
    }
}
