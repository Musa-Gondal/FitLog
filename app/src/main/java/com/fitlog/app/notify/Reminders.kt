package com.fitlog.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.fitlog.app.data.AppDatabase
import com.fitlog.app.data.DailyLog
import com.fitlog.app.data.ProfileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** All reminder preferences. Times are minutes after midnight. */
data class ReminderSettings(
    val meals: Boolean = false,
    val breakfast: Int = 9 * 60,
    val lunch: Int = 14 * 60,
    val dinner: Int = 20 * 60 + 30,
    val water: Boolean = false,
    val waterStart: Int = 10 * 60,
    val waterEnd: Int = 22 * 60,
    val waterEvery: Int = 120,
    val weighIn: Boolean = false,
    val weighInTime: Int = 8 * 60,
    val workout: Boolean = false,
    val workoutDays: Set<Int> = setOf(1, 3, 5),   // DayOfWeek values, Monday = 1
    val workoutTime: Int = 18 * 60,
    val checkIn: Boolean = false,
    val checkInDay: Int = 7,                       // Sunday
    val checkInTime: Int = 10 * 60,
) {
    val anyEnabled: Boolean get() = meals || water || weighIn || workout || checkIn
}

fun Int.hhmm(): String {
    val h = this / 60
    val m = this % 60
    val ampm = if (h < 12) "AM" else "PM"
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "%d:%02d %s".format(h12, m, ampm)
}

enum class ReminderType(val code: Int) { BREAKFAST(101), LUNCH(102), DINNER(103), WATER(104), WEIGH_IN(105), WORKOUT(106), CHECK_IN(107) }

object Reminders {
    private const val PREFS = "reminders"
    private const val ACTION_FIRE = "com.fitlog.app.REMINDER"
    const val ACTION_ADD_WATER = "com.fitlog.app.ADD_WATER"
    private const val EXTRA_TYPE = "type"

    fun load(ctx: Context): ReminderSettings {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val d = ReminderSettings()
        return ReminderSettings(
            meals = p.getBoolean("meals", d.meals),
            breakfast = p.getInt("breakfast", d.breakfast),
            lunch = p.getInt("lunch", d.lunch),
            dinner = p.getInt("dinner", d.dinner),
            water = p.getBoolean("water", d.water),
            waterStart = p.getInt("waterStart", d.waterStart),
            waterEnd = p.getInt("waterEnd", d.waterEnd),
            waterEvery = p.getInt("waterEvery", d.waterEvery),
            weighIn = p.getBoolean("weighIn", d.weighIn),
            weighInTime = p.getInt("weighInTime", d.weighInTime),
            workout = p.getBoolean("workout", d.workout),
            workoutDays = (p.getString("workoutDays", null)?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet()) ?: d.workoutDays,
            workoutTime = p.getInt("workoutTime", d.workoutTime),
            checkIn = p.getBoolean("checkIn", d.checkIn),
            checkInDay = p.getInt("checkInDay", d.checkInDay),
            checkInTime = p.getInt("checkInTime", d.checkInTime),
        )
    }

    fun save(ctx: Context, s: ReminderSettings) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("meals", s.meals).putInt("breakfast", s.breakfast).putInt("lunch", s.lunch).putInt("dinner", s.dinner)
            .putBoolean("water", s.water).putInt("waterStart", s.waterStart).putInt("waterEnd", s.waterEnd)
            .putInt("waterEvery", s.waterEvery)
            .putBoolean("weighIn", s.weighIn).putInt("weighInTime", s.weighInTime)
            .putBoolean("workout", s.workout).putString("workoutDays", s.workoutDays.sorted().joinToString(","))
            .putInt("workoutTime", s.workoutTime)
            .putBoolean("checkIn", s.checkIn).putInt("checkInDay", s.checkInDay).putInt("checkInTime", s.checkInTime)
            .apply()
        scheduleAll(ctx)
    }

    private fun pending(ctx: Context, type: ReminderType): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java).setAction(ACTION_FIRE).putExtra(EXTRA_TYPE, type.name)
        return PendingIntent.getBroadcast(ctx, type.code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun scheduleAll(ctx: Context) {
        ReminderType.entries.forEach { schedule(ctx, it) }
    }

    fun schedule(ctx: Context, type: ReminderType) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = pending(ctx, type)
        val next = nextTrigger(load(ctx), type, LocalDateTime.now())
        am.cancel(pi)
        if (next != null) {
            val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            // Inexact but Doze-friendly; reminders don't need second precision.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    /** Next time a reminder should fire, or null if disabled. */
    fun nextTrigger(s: ReminderSettings, type: ReminderType, now: LocalDateTime): LocalDateTime? {
        fun nextDaily(min: Int): LocalDateTime {
            val today = now.toLocalDate().atTime(LocalTime.of(min / 60, min % 60))
            return if (today.isAfter(now.plusSeconds(30))) today else today.plusDays(1)
        }
        return when (type) {
            ReminderType.BREAKFAST -> if (s.meals) nextDaily(s.breakfast) else null
            ReminderType.LUNCH -> if (s.meals) nextDaily(s.lunch) else null
            ReminderType.DINNER -> if (s.meals) nextDaily(s.dinner) else null
            ReminderType.WEIGH_IN -> if (s.weighIn) nextDaily(s.weighInTime) else null
            ReminderType.WATER -> if (!s.water) null else {
                val every = s.waterEvery.coerceAtLeast(30)
                var candidate: LocalDateTime? = null
                for (dayOffset in 0L..1L) {
                    val day = now.toLocalDate().plusDays(dayOffset)
                    var m = s.waterStart
                    while (m <= s.waterEnd) {
                        val t = day.atTime(LocalTime.of(m / 60, m % 60))
                        if (t.isAfter(now.plusSeconds(30))) { candidate = t; break }
                        m += every
                    }
                    if (candidate != null) break
                }
                candidate
            }
            ReminderType.WORKOUT -> if (!s.workout || s.workoutDays.isEmpty()) null else {
                (0L..7L).map { now.toLocalDate().plusDays(it).atTime(LocalTime.of(s.workoutTime / 60, s.workoutTime % 60)) }
                    .firstOrNull { it.isAfter(now.plusSeconds(30)) && it.dayOfWeek.value in s.workoutDays }
            }
            ReminderType.CHECK_IN -> if (!s.checkIn) null else {
                (0L..7L).map { now.toLocalDate().plusDays(it).atTime(LocalTime.of(s.checkInTime / 60, s.checkInTime % 60)) }
                    .firstOrNull { it.isAfter(now.plusSeconds(30)) && it.dayOfWeek == DayOfWeek.of(s.checkInDay) }
            }
        }
    }

    internal fun typeOf(intent: Intent): ReminderType? =
        intent.getStringExtra(EXTRA_TYPE)?.let { n -> ReminderType.entries.firstOrNull { it.name == n } }
}

/** Fires a reminder, but only if it's still relevant (e.g. meal not yet logged), then schedules the next one. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ctx = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == Reminders.ACTION_ADD_WATER) {
                    val db = AppDatabase.get(ctx)
                    val d = LocalDate.now().toString()
                    val cur = db.body().dailyOnce(d) ?: DailyLog(d)
                    db.body().upsertDaily(cur.copy(waterMl = cur.waterMl + 250))
                    Notifications.cancel(ctx, ReminderType.WATER.code)
                } else {
                    val type = Reminders.typeOf(intent)
                    if (type != null) {
                        fire(ctx, type)
                        Reminders.schedule(ctx, type)
                    }
                }
            } catch (e: Throwable) {
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun fire(ctx: Context, type: ReminderType) {
        val db = AppDatabase.get(ctx)
        val profile = ProfileStore(ctx).profile.value
        if (!profile.onboarded) return
        val today = LocalDate.now().toString()
        val food = db.food().entriesOnce(today)
        val t = profile.targets()
        val eaten = food.sumOf { it.kcal }.toInt()

        fun meal(idx: Int, name: String): Pair<String, String>? =
            if (food.any { it.meal == idx }) null
            else "Log your $name" to "${(t.kcal - eaten).coerceAtLeast(0)} kcal and ${(t.protein - food.sumOf { it.protein }.toInt()).coerceAtLeast(0)} g protein left today."

        val content: Pair<String, String>? = when (type) {
            ReminderType.BREAKFAST -> meal(0, "breakfast")
            ReminderType.LUNCH -> meal(1, "lunch")
            ReminderType.DINNER -> meal(2, "dinner")
            ReminderType.WATER -> {
                val water = db.body().dailyOnce(today)?.waterMl ?: 0
                if (water >= profile.waterGoalMl) null
                else "Time for a glass of water" to "$water / ${profile.waterGoalMl} ml so far today."
            }
            ReminderType.WEIGH_IN -> {
                if (db.body().allWeights().any { it.date == today }) null
                else "Morning weigh-in" to "Weigh after the toilet, before eating – it keeps your trend and calorie targets accurate."
            }
            ReminderType.WORKOUT -> {
                val zone = ZoneId.systemDefault()
                val trainedToday = db.workout().allWorkouts().any {
                    Instant.ofEpochMilli(it.startMillis).atZone(zone).toLocalDate().toString() == today
                }
                if (trainedToday || ProfileStore(ctx).loadDraft() != null) null
                else "Workout day 💪" to "Your plan says train today. Open FitLog and start your routine."
            }
            ReminderType.CHECK_IN -> "Weekly check-in ready" to "See your week, your real maintenance calories and next week's targets."
        }
        content ?: return

        val route = when (type) {
            ReminderType.BREAKFAST, ReminderType.LUNCH, ReminderType.DINNER -> "food"
            ReminderType.WORKOUT -> "workout"
            ReminderType.CHECK_IN -> "coach"
            else -> "today"
        }
        val b = Notifications.builder(ctx, Notifications.CH_REMINDERS)
            .setContentTitle(content.first)
            .setContentText(content.second)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.second))
            .setContentIntent(Notifications.openAppIntent(ctx, route, type.code))
        if (type == ReminderType.WATER) {
            val add = Intent(ctx, ReminderReceiver::class.java).setAction(Reminders.ACTION_ADD_WATER)
            val pi = PendingIntent.getBroadcast(ctx, 999, add, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            b.addAction(0, "+250 ml", pi)
        }
        Notifications.post(ctx, type.code, b.build())
    }
}

/** Re-arms alarms after reboot, app update or time-zone change (Android clears them). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.scheduleAll(context.applicationContext)
    }
}
