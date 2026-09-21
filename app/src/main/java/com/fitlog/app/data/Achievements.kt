package com.fitlog.app.data

import java.time.LocalDate
import kotlin.math.abs

data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,        // key mapped to an icon in the UI
    val tier: Int,           // 1 bronze, 2 silver, 3 gold
    val current: Double,
    val target: Double,
) {
    val unlocked: Boolean get() = current >= target
    val progress: Float get() = (current / target).coerceIn(0.0, 1.0).toFloat()
}

/** Per-day facts used for streaks, badges and the calendar heatmap. */
data class DayFacts(
    val date: LocalDate,
    val foodLogged: Boolean,
    val kcalOnTarget: Boolean,
    val proteinHit: Boolean,
    val waterHit: Boolean,
    val stepsHit: Boolean,
    val workout: Boolean,
    val volumeKg: Double,
) {
    /** 0–5 goals met, for the "overall" heatmap. */
    val score: Int get() = listOf(workout, kcalOnTarget, proteinHit, waterHit, stepsHit).count { it }
}

object Achievements {

    fun dayFacts(
        from: LocalDate,
        to: LocalDate,
        profile: Profile,
        totals: List<DayTotals>,
        daily: List<DailyLog>,
        workoutsByDate: Map<LocalDate, Double>,
    ): List<DayFacts> {
        val t = profile.targets()
        val tot = totals.associateBy { it.date }
        val dl = daily.associateBy { it.date }
        val out = mutableListOf<DayFacts>()
        var d = from
        while (!d.isAfter(to)) {
            val key = d.toString()
            val f = tot[key]
            val l = dl[key]
            out += DayFacts(
                date = d,
                foodLogged = f != null && f.kcal > 0,
                kcalOnTarget = f != null && f.kcal > 0 && abs(f.kcal - t.kcal) <= t.kcal * 0.1,
                proteinHit = f != null && f.protein >= t.protein * 0.9,
                waterHit = l != null && l.waterMl >= profile.waterGoalMl,
                stepsHit = l != null && profile.stepGoal > 0 && l.steps >= profile.stepGoal,
                workout = workoutsByDate.containsKey(d),
                volumeKg = workoutsByDate[d] ?: 0.0,
            )
            d = d.plusDays(1)
        }
        return out
    }

    private fun bestRun(days: List<DayFacts>, pred: (DayFacts) -> Boolean): Int {
        var best = 0
        var cur = 0
        days.forEach { if (pred(it)) { cur++; if (cur > best) best = cur } else cur = 0 }
        return best
    }

    /** Current streak of days with food or a workout logged (today counts if already logged, else from yesterday). */
    fun currentStreak(days: List<DayFacts>): Int {
        val active = days.filter { it.foodLogged || it.workout }.map { it.date }.toSet()
        var d = LocalDate.now()
        if (d !in active) d = d.minusDays(1)
        var n = 0
        while (d in active) { n++; d = d.minusDays(1) }
        return n
    }

    fun badges(
        profile: Profile,
        days: List<DayFacts>,                 // full history, oldest first
        workouts: List<Pair<LocalDate, List<WorkoutSet>>>,
        workoutStartHours: List<Int>,
        prCount: Int,
        weights: List<WeightEntry>,
    ): List<Badge> {
        val working = workouts.flatMap { it.second }.filter { it.isWorking }
        val volume = working.sumOf { it.weightKg * it.reps }
        val maxSet = working.maxOfOrNull { it.weightKg } ?: 0.0
        val bw = weights.maxByOrNull { it.date }?.kg ?: profile.weightKg
        val bestBench = working.filter { it.exercise.startsWith("Bench Press") && it.reps >= 1 }.maxOfOrNull { it.weightKg } ?: 0.0
        val bestDead = working.filter { it.exercise == "Deadlift" && it.reps >= 1 }.maxOfOrNull { it.weightKg } ?: 0.0
        val bestSquat = working.filter { it.exercise == "Squat (Barbell)" && it.reps >= 1 }.maxOfOrNull { it.weightKg } ?: 0.0
        val activeRun = bestRun(days) { it.foodLogged || it.workout }
        val nWorkouts = workouts.size.toDouble()
        val trend = Coach.weightTrend(weights)
        val moved = if (trend.size >= 2) (trend.last().trend - trend.first().trend) * profile.goal.direction else 0.0

        val list = mutableListOf(
            Badge("w1", "First Rep", "Finish your first workout", "dumbbell", 1, nWorkouts, 1.0),
            Badge("w10", "Getting Consistent", "Finish 10 workouts", "dumbbell", 1, nWorkouts, 10.0),
            Badge("w50", "Gym Regular", "Finish 50 workouts", "dumbbell", 2, nWorkouts, 50.0),
            Badge("w100", "Centurion", "Finish 100 workouts", "dumbbell", 3, nWorkouts, 100.0),
            Badge("s7", "Week Warrior", "Log something 7 days in a row", "fire", 1, activeRun.toDouble(), 7.0),
            Badge("s30", "Habit Formed", "Log something 30 days in a row", "fire", 2, activeRun.toDouble(), 30.0),
            Badge("s100", "Unstoppable", "Log something 100 days in a row", "fire", 3, activeRun.toDouble(), 100.0),
            Badge("p7", "Protein Pro", "Hit your protein target 7 days in a row", "egg", 2, bestRun(days) { it.proteinHit }.toDouble(), 7.0),
            Badge("k7", "Bullseye", "Calories within ±10 % of target 7 days in a row", "target", 2, bestRun(days) { it.kcalOnTarget }.toDouble(), 7.0),
            Badge("h7", "Hydrated", "Hit your water goal 7 days in a row", "water", 1, bestRun(days) { it.waterHit }.toDouble(), 7.0),
            Badge("st10", "Walker", "Hit your step goal on 10 days", "walk", 1, days.count { it.stepsHit }.toDouble(), 10.0),
            Badge("st50", "Road Runner", "Hit your step goal on 50 days", "walk", 2, days.count { it.stepsHit }.toDouble(), 50.0),
            Badge("pr1", "New Record", "Set your first personal record", "trophy", 1, prCount.toDouble(), 1.0),
            Badge("pr25", "Record Breaker", "Set 25 personal records", "trophy", 2, prCount.toDouble(), 25.0),
            Badge("pr100", "Legend", "Set 100 personal records", "trophy", 3, prCount.toDouble(), 100.0),
            Badge("c100", "100 kg Club", "Lift 100 kg in any working set", "medal", 2, maxSet, 100.0),
            Badge("bwb", "Bodyweight Bench", "Bench press your bodyweight", "medal", 2, bestBench, bw),
            Badge("bws", "1.5× Squat", "Squat 1.5× your bodyweight", "medal", 3, bestSquat, bw * 1.5),
            Badge("bwd", "Double-BW Deadlift", "Deadlift 2× your bodyweight", "medal", 3, bestDead, bw * 2),
            Badge("v10", "10 Tonnes", "Lift 10,000 kg total volume", "weight", 1, volume, 10_000.0),
            Badge("v100", "100 Tonnes", "Lift 100,000 kg total volume", "weight", 2, volume, 100_000.0),
            Badge("v1000", "Megaton", "Lift 1,000,000 kg total volume", "weight", 3, volume, 1_000_000.0),
            Badge("wi30", "Scale Regular", "Log 30 weigh-ins", "scale", 1, weights.size.toDouble(), 30.0),
            Badge("f30", "Food Logger", "Log food on 30 days", "food", 1, days.count { it.foodLogged }.toDouble(), 30.0),
            Badge("f100", "Nutrition Nerd", "Log food on 100 days", "food", 2, days.count { it.foodLogged }.toDouble(), 100.0),
            Badge("eb", "Early Bird", "Start a workout before 7 am", "sun", 1, if (workoutStartHours.any { it < 7 }) 1.0 else 0.0, 1.0),
            Badge("perfect", "Perfect Day", "Hit all 5 daily goals in one day", "star", 2, (days.maxOfOrNull { it.score } ?: 0).toDouble(), 5.0),
        )
        if (profile.goal != Goal.MAINTAIN) {
            val verb = if (profile.goal == Goal.LOSE) "Lose" else "Gain"
            list += Badge("g2", "First Milestone", "$verb 2 kg (trend weight)", "flag", 1, moved.coerceAtLeast(0.0), 2.0)
            list += Badge("g5", "Transformation", "$verb 5 kg (trend weight)", "flag", 2, moved.coerceAtLeast(0.0), 5.0)
            list += Badge("g10", "New You", "$verb 10 kg (trend weight)", "flag", 3, moved.coerceAtLeast(0.0), 10.0)
        }
        return list
    }
}
