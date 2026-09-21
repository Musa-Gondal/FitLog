package com.fitlog.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fitlog.app.MainActivity
import com.fitlog.app.R

/** Tracks whether the app UI is visible (so background alerts don't double up with in-app ones). */
object AppState {
    @Volatile var foreground: Boolean = false
}

object Notifications {
    const val CH_REMINDERS = "reminders"
    const val CH_REST = "rest_timer"
    const val CH_REST_DONE = "rest_done"
    const val EXTRA_ROUTE = "route"

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CH_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Meal, water, weigh-in, workout and weekly check-in reminders"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_REST, "Rest timer countdown", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Live countdown between sets"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_REST_DONE, "Rest finished", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alert when your rest period is over"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
            }
        )
    }

    fun canPost(ctx: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    fun openAppIntent(ctx: Context, route: String, requestCode: Int): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ROUTE, route)
        }
        return PendingIntent.getActivity(ctx, requestCode, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun builder(ctx: Context, channel: String): NotificationCompat.Builder =
        NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_fitlog)
            .setColor(0xFFC6F432.toInt())
            .setAutoCancel(true)

    @android.annotation.SuppressLint("MissingPermission")
    fun post(ctx: Context, id: Int, n: android.app.Notification) {
        if (!canPost(ctx)) return
        runCatching { NotificationManagerCompat.from(ctx).notify(id, n) }
    }

    fun cancel(ctx: Context, id: Int) {
        NotificationManagerCompat.from(ctx).cancel(id)
    }
}
