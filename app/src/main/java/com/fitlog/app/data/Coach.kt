package com.fitlog.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The coaching engine. Pure functions over logged data – no Android dependencies,
 * so every number shown in the app can be traced back to a formula here.
 */
object Coach {

    // ------------------------------------------------------------------
    // 1. Weight trend (removes day-to-day water/food noise)
    // ------------------------------------------------------------------

    data class TrendPoint(val date: LocalDate, val weight: Double, val trend: Double)

    /**
     * Exponentially smoothed trend weight (10 % per day, as in "The Hacker's Diet").
     * Gaps between weigh-ins are handled by compounding the smoothing factor over the gap.
     */
    fun weightTrend(weights: List<WeightEntry>): List<TrendPoint> {
        val sorted = weights.sortedBy { it.date }
        if (sorted.isEmpty()) return emptyList()
        val out = ArrayList<TrendPoint>(sorted.size)
        var prevDate = LocalDate.parse(sorted.first().date)
        var trend = sorted.first().kg
        sorted.forEachIndexed { i, w ->
            val d = LocalDate.parse(w.date)
            if (i > 0) {
                val gap = ChronoUnit.DAYS.between(prevDate, d).coerceAtLeast(1)
                val alpha = 1 - 0.9.pow(gap.toDouble())
                trend += alpha * (w.kg - trend)
            }
            out += TrendPoint(d, w.kg, trend)
            prevDate = d
        }
        return out
    }

    /** Least-squares slope of trend weight, in kg per day, over the last [days] days. */
    fun trendSlopePerDay(trend: List<TrendPoint>, days: Long, today: LocalDate = LocalDate.now()): Double? {
        val from = today.minusDays(days)
        val pts = trend.filter { it.date.isAfter(from) }
        if (pts.size < 4) return null
        val span = ChronoUnit.DAYS.between(pts.first().date, pts.last().date)
        if (span < 7) return null
        val xs = pts.map { ChronoUnit.DAYS.between(pts.first().date, it.date).toDouble() }
        val ys = pts.map { it.trend }
        val mx = xs.average()
        val my = ys.average()
        var num = 0.0
        var den = 0.0
        for (i in xs.indices) { num += (xs[i] - mx) * (ys[i] - my); den += (xs[i] - mx) * (xs[i] - mx) }
        return if (den == 0.0) null else num / den
    }

    // ------------------------------------------------------------------
    // 2. Adaptive maintenance calories (energy balance on YOUR data)
    // ------------------------------------------------------------------

    data class TdeeEstimate(
        val estimate: Int,          // blended maintenance recommendation
        val raw: Int,               // pure data estimate (intake − stored energy)
        val formula: Int,           // Mifflin-St Jeor × activity
        val avgIntake: Int,
        val kgPerWeek: Double,
        val loggedDays: Int,
        val weighIns: Int,
        val confidence: Double,     // 0..1
        val clamped: Boolean,       // raw estimate was implausible and got limited
    )

    /**
     * Maintenance = average intake − (trend weight change × 7700 kcal/kg).
     * Uses the last 28 complete days. Days logged below 50 % of the formula estimate are treated as
     * incomplete logs and skipped. The result is blended with the formula according to confidence.
     */
    fun estimateTdee(
        totals: List<DayTotals>,
        weights: List<WeightEntry>,
        formulaTdee: Int,
        today: LocalDate = LocalDate.now(),
    ): TdeeEstimate? {
        val window = 28L
        val start = today.minusDays(window)
        val end = today.minusDays(1) // today is still in progress
        val days = totals.filter {
            val d = LocalDate.parse(it.date)
            !d.isBefore(start) && !d.isAfter(end) && it.kcal >= formulaTdee * 0.5
        }
        val wIn = weights.filter { val d = LocalDate.parse(it.date); !d.isBefore(start) && !d.isAfter(today) }
        if (days.size < 10 || wIn.size < 5) return null
        val trend = weightTrend(weights)
        val slope = trendSlopePerDay(trend, window, today) ?: return null
        val avgIntake = days.map { it.kcal }.average()
        var raw = avgIntake - slope * KCAL_PER_KG
        val lo = formulaTdee * 0.65
        val hi = formulaTdee * 1.35
        val clamped = raw < lo || raw > hi
        raw = raw.coerceIn(lo, hi)
        val confidence = (minOf(1.0, days.size / 21.0) * minOf(1.0, wIn.size / 12.0)).let { if (clamped) it * 0.5 else it }
        val blended = formulaTdee * (1 - confidence) + raw * confidence
        return TdeeEstimate(
            estimate = (blended / 10).roundToInt() * 10,
            raw = raw.roundToInt(),
            formula = formulaTdee,
            avgIntake = avgIntake.roundToInt(),
            kgPerWeek = slope * 7,
            loggedDays = days.size,
            weighIns = wIn.size,
            confidence = confidence,
            clamped = clamped,
        )
    }

    // ------------------------------------------------------------------
    // 3. Progressive overload (double progression) + plateau detection
    // ------------------------------------------------------------------

    enum class Kind { FIRST, INCREASE, ADD_REPS, HOLD, DELOAD, NONE }

    data class Suggestion(
        val kind: Kind,
        val weightKg: Double?,
        val reps: Int?,
        val sets: Int,
        val headline: String,
        val detail: String,
        val plateau: Boolean = false,
    )

    /** One past session of an exercise: its working sets (warm-ups removed). */
    data class Session(val date: LocalDate, val sets: List<WorkoutSet>) {
        val topKg: Double get() = sets.maxOfOrNull { it.weightKg } ?: 0.0
        val best1rm: Double get() = sets.maxOfOrNull { e1rm(it.weightKg, it.reps) } ?: 0.0
    }

    fun e1rm(kg: Double, reps: Int): Double = if (reps <= 1) kg else kg * (1 + reps / 30.0)

    private fun roundTo(v: Double, step: Double) = if (step <= 0) v else Math.round(v / step) * step

    /**
     * Double progression:
     *  - every working set at the top weight reached the top of the rep range → add the smallest load jump;
     *  - otherwise keep the weight and add a rep to sets below the top of the range;
     *  - two sessions in a row below the bottom of the range → reduce load ~10 %.
     * Plateau = no estimated-1RM improvement across the last 3 sessions compared with the best before them.
     */
    fun suggest(def: ExerciseDef, sessions: List<Session>, lb: Boolean): Suggestion {
        if (def.equipment == Equipment.CARDIO) {
            val last = sessions.firstOrNull()
            return Suggestion(Kind.NONE, last?.topKg, last?.sets?.maxOfOrNull { it.reps }, last?.sets?.size ?: 1,
                "Cardio", "Try to beat last time by a minute or a little distance.")
        }
        val last = sessions.firstOrNull()
            ?: return Suggestion(Kind.FIRST, null, def.repLow + (def.repHigh - def.repLow) / 2, 3,
                "First time",
                "Pick a weight you can lift for ${def.repLow}–${def.repHigh} reps with about 2 reps left in the tank. Log it, and next time I'll tell you when to go up.")

        // Respect the user's style: heavy compound work done mostly at ≤5 reps uses a 3–6 strength range.
        val recentReps = sessions.take(3).flatMap { ss -> ss.sets.map { it.reps } }.sorted()
        val medianReps = recentReps.getOrElse(recentReps.size / 2) { def.repLow }
        val strength = def.compound && def.equipment != Equipment.BODYWEIGHT && medianReps in 1..5
        val low = if (strength) 3 else def.repLow
        val high = if (strength) 6 else def.repHigh

        val setCount = last.sets.size.coerceIn(1, 6)
        val step = def.incrementKg(lb)
        val displayStep = if (lb) Units.toDisplay(step, true) else step
        val bodyweight = last.topKg <= 0.0 || step == 0.0
        val top = last.topKg
        val topSets = last.sets.filter { it.weightKg >= top - 0.01 }
        val minReps = topSets.minOf { it.reps }
        val allAtTop = topSets.all { it.reps >= high }
        val rpeVals = topSets.mapNotNull { it.rpe }
        val tooHard = rpeVals.isNotEmpty() && rpeVals.average() >= 9.5

        val plateau = sessions.size >= 4 && run {
            val recentBest = sessions.take(3).maxOf { it.best1rm }
            val before = sessions.drop(3).take(5).maxOf { it.best1rm }
            recentBest <= before * 1.005
        }
        val plateauText = " You've stalled for 3 sessions: consider a deload week (−10 % load, same reps), " +
            "then build back up – and check sleep and protein."

        if (bodyweight) {
            val target = (minReps + 1).coerceAtMost(high)
            return if (allAtTop) Suggestion(Kind.INCREASE, top, low, setCount, "Make it harder",
                "You hit $high+ reps on every set. Add load (backpack / belt / dumbbell) or a slower tempo.",
                plateau)
            else Suggestion(Kind.ADD_REPS, top, target, setCount, "Aim for $target reps",
                "Beat last time by one rep per set until you reach $high.", plateau)
        }

        // Two consecutive sessions under the range → too heavy.
        val prev = sessions.getOrNull(1)
        val underNow = minReps < low
        val underPrev = prev != null && prev.topKg >= top - 0.01 && prev.sets.filter { it.weightKg >= top - 0.01 }.minOf { it.reps } < low
        val noProgress = prev != null && last.best1rm <= prev.best1rm * 1.005
        if (underNow && underPrev && noProgress) {
            val w = Units.fromDisplay(roundTo(Units.toDisplay(top * 0.9, lb), displayStep), lb)
            return Suggestion(Kind.DELOAD, w, high, setCount, "Drop to ${Units.show(w, lb)}",
                "Two sessions below $low reps. Reduce ~10 % and rebuild to $high reps with good form.", plateau)
        }

        if (allAtTop && !tooHard) {
            val w = Units.fromDisplay(roundTo(Units.toDisplay(top, lb) + displayStep, displayStep), lb)
            val aim = if (strength) 4 else low
            return Suggestion(Kind.INCREASE, w, aim, setCount, "Go up to ${Units.show(w, lb)}",
                "All sets hit $high reps last time – add ${Units.fmt(displayStep)} ${Units.label(lb)} and aim for $aim+ reps." +
                    if (plateau) plateauText else "", plateau)
        }

        if (tooHard && allAtTop) {
            return Suggestion(Kind.HOLD, top, high, setCount, "Repeat ${Units.show(top, lb)}",
                "You hit the reps but at RPE ~10. Own this weight once more before adding load.", plateau)
        }

        val target = (minReps + 1).coerceIn(low, high)
        return Suggestion(Kind.ADD_REPS, top, target, setCount, "Stay at ${Units.show(top, lb)}, aim $target+ reps",
            "Add a rep to each set until every set reaches $high, then increase the weight." +
                if (plateau) plateauText else "", plateau)
    }

    /** Working sets grouped into sessions, most recent first. */
    fun sessionsFor(exercise: String, history: List<Pair<LocalDate, List<WorkoutSet>>>): List<Session> =
        history.mapNotNull { (date, sets) ->
            val s = sets.filter { it.exercise == exercise && it.isWorking && it.setType != "D" && it.reps > 0 }
            if (s.isEmpty()) null else Session(date, s)
        }

    /** Warm-up ramp for a working weight: bar/empty, ~50 %, ~70 %, ~85 %. */
    fun warmups(workKg: Double, def: ExerciseDef, barKg: Double, lb: Boolean): List<Pair<Double, Int>> {
        if (workKg <= 0) return emptyList()
        val step = def.incrementKg(lb).takeIf { it > 0 } ?: 2.5
        val disp = Units.toDisplay(step, lb)
        fun r(kg: Double) = Units.fromDisplay(roundTo(Units.toDisplay(kg, lb), disp), lb)
        val out = mutableListOf<Pair<Double, Int>>()
        val start = if (def.equipment == Equipment.BARBELL) barKg else 0.0
        if (def.equipment == Equipment.BARBELL && workKg > barKg * 1.5) out += barKg to 10
        listOf(0.5 to 8, 0.7 to 5, 0.85 to 3).forEach { (pct, reps) ->
            val w = r(workKg * pct)
            if (w > start && w < workKg && out.none { abs(it.first - w) < 0.01 }) out += w to reps
        }
        return out
    }

    // ------------------------------------------------------------------
    // 4. Weekly volume per muscle (hard sets)
    // ------------------------------------------------------------------

    /**
     * Hard sets per muscle in [from, to]. A set counts 1 for the primary muscle and 0.5 for listed
     * secondary muscle groups. Evidence suggests ~10–20 hard sets / muscle / week for growth.
     */
    fun muscleSets(
        sessions: List<Pair<LocalDate, List<WorkoutSet>>>,
        from: LocalDate, to: LocalDate,
        lookup: (String) -> ExerciseDef,
    ): Map<String, Double> {
        val out = linkedMapOf<String, Double>()
        ExerciseLibrary.volumeMuscles.forEach { out[it] = 0.0 }
        sessions.filter { !it.first.isBefore(from) && !it.first.isAfter(to) }.forEach { (_, sets) ->
            sets.filter { it.isWorking }.forEach { s ->
                val def = lookup(s.exercise)
                val primary = if (def.muscle == "Full body") "Legs" else def.muscle
                if (primary in out) out[primary] = out.getValue(primary) + 1
                ExerciseLibrary.volumeMuscles.filter { it != primary && def.secondary.contains(it, ignoreCase = true) }
                    .forEach { out[it] = out.getValue(it) + 0.5 }
                if (def.secondary.contains("hamstring", true) || def.secondary.contains("quads", true)) {
                    if (primary != "Legs") out["Legs"] = out.getValue("Legs") + 0.5
                }
                if (def.secondary.contains("delt", true) && primary != "Shoulders") out["Shoulders"] = out.getValue("Shoulders") + 0.5
                if (def.secondary.contains("lats", true) && primary != "Back") out["Back"] = out.getValue("Back") + 0.5
            }
        }
        return out
    }

    // ------------------------------------------------------------------
    // 5. Insights / weekly check-in
    // ------------------------------------------------------------------

    enum class Level { GOOD, INFO, WARN }

    data class Insight(val level: Level, val title: String, val detail: String, val action: String? = null)

    data class Week(
        val start: LocalDate,
        val end: LocalDate,
        val loggedDays: Int,
        val avgKcal: Int?,
        val avgProtein: Int?,
        val proteinDaysHit: Int,
        val kcalDaysOnTarget: Int,
        val workouts: Int,
        val hardSets: Int,
        val volumeKg: Double,
        val muscleSets: Map<String, Double>,
        val waterDaysHit: Int,
        val stepsAvg: Int?,
        val trendChangeKg: Double?,
        val prs: List<String>,
    )

    data class Inputs(
        val profile: Profile,
        val totals: List<DayTotals>,
        val daily: List<DailyLog>,
        val weights: List<WeightEntry>,
        val sessions: List<Pair<LocalDate, List<WorkoutSet>>>, // most recent first
        val workoutDates: List<LocalDate>,
        val prEvents: List<Pair<LocalDate, String>>,
        val lookup: (String) -> ExerciseDef,
        val today: LocalDate = LocalDate.now(),
    )

    fun week(inp: Inputs, endInclusive: LocalDate = inp.today): Week {
        val from = endInclusive.minusDays(6)
        val t = inp.profile.targets()
        fun inRange(d: LocalDate) = !d.isBefore(from) && !d.isAfter(endInclusive)
        val food = inp.totals.filter { inRange(LocalDate.parse(it.date)) && it.kcal > 0 }
        val daily = inp.daily.filter { inRange(LocalDate.parse(it.date)) }
        val sess = inp.sessions.filter { inRange(it.first) }
        val trend = weightTrend(inp.weights)
        val tStart = trend.lastOrNull { !it.date.isAfter(from) } ?: trend.firstOrNull { inRange(it.date) }
        val tEnd = trend.lastOrNull { !it.date.isAfter(endInclusive) }
        val working = sess.flatMap { it.second }.filter { it.isWorking }
        return Week(
            start = from, end = endInclusive,
            loggedDays = food.size,
            avgKcal = food.takeIf { it.isNotEmpty() }?.map { it.kcal }?.average()?.roundToInt(),
            avgProtein = food.takeIf { it.isNotEmpty() }?.map { it.protein }?.average()?.roundToInt(),
            proteinDaysHit = food.count { it.protein >= t.protein * 0.9 },
            kcalDaysOnTarget = food.count { abs(it.kcal - t.kcal) <= t.kcal * 0.1 },
            workouts = inp.workoutDates.count { inRange(it) },
            hardSets = working.size,
            volumeKg = working.sumOf { it.weightKg * it.reps },
            muscleSets = muscleSets(inp.sessions, from, endInclusive, inp.lookup),
            waterDaysHit = daily.count { it.waterMl >= inp.profile.waterGoalMl },
            stepsAvg = daily.filter { it.steps > 0 }.takeIf { it.isNotEmpty() }?.map { it.steps }?.average()?.roundToInt(),
            trendChangeKg = if (tStart != null && tEnd != null && tStart.date != tEnd.date) tEnd.trend - tStart.trend else null,
            prs = inp.prEvents.filter { inRange(it.first) }.map { it.second }.distinct(),
        )
    }

    /** Prioritised, specific coaching messages. */
    fun insights(inp: Inputs, tdee: TdeeEstimate?): List<Insight> {
        val p = inp.profile
        val t = p.targets()
        val lb = p.useLb
        val out = mutableListOf<Insight>()
        val wk = week(inp)
        val trend = weightTrend(inp.weights)
        val slope21 = trendSlopePerDay(trend, 21, inp.today)

        // --- Rate of weight change vs goal -------------------------------------------
        if (slope21 != null && trend.size >= 6) {
            val ratePctWk = slope21 * 7 / p.weightKg * 100
            val kgWk = slope21 * 7
            val actual = "${if (kgWk >= 0) "+" else "−"}${Units.fmt2(abs(Units.toDisplay(kgWk, lb)))} ${Units.label(lb)}/week"
            when (p.goal) {
                Goal.LOSE -> when {
                    ratePctWk < -maxOf(1.0, p.rate * 1.6) -> out += Insight(Level.WARN, "Losing too fast ($actual)",
                        "Faster than ~1 % bodyweight a week risks muscle loss and fatigue. Add ~${adjustKcal(ratePctWk, -p.rate, p)} kcal/day.",
                        "Increase target")
                    ratePctWk > -0.1 -> out += Insight(Level.WARN, "Weight isn't dropping ($actual)",
                        "Over 3 weeks your trend is flat. Check logging accuracy (oil, chai, snacks) or cut ~${adjustKcal(ratePctWk, -p.rate, p)} kcal/day." +
                            if (p.adaptiveEnabled) " The adaptive target will also correct this at your weekly check-in." else "",
                        "Review target")
                    else -> out += Insight(Level.GOOD, "On track: $actual", "Right in the healthy fat-loss range for your goal.")
                }
                Goal.GAIN -> when {
                    ratePctWk > maxOf(0.6, p.rate * 2) -> out += Insight(Level.WARN, "Gaining fast ($actual)",
                        "Above ~0.5 % a week, most extra gain is fat. Trim ~${adjustKcal(ratePctWk, p.rate, p)} kcal/day.",
                        "Reduce target")
                    ratePctWk < 0.05 -> out += Insight(Level.WARN, "Not gaining ($actual)",
                        "Muscle growth needs a small surplus. Add ~${adjustKcal(ratePctWk, p.rate, p)} kcal/day (e.g. a glass of milk + banana).",
                        "Increase target")
                    else -> out += Insight(Level.GOOD, "Lean bulk on track: $actual", "Slow, steady gain – ideal for building muscle.")
                }
                Goal.MAINTAIN -> if (abs(ratePctWk) > 0.3) out += Insight(Level.INFO, "Weight drifting ($actual)",
                    "For maintenance, aim for less than ±0.25 % a week. Adjust ~${adjustKcal(ratePctWk, 0.0, p)} kcal/day.")
                else out += Insight(Level.GOOD, "Weight stable ($actual)", "Maintenance is working.")
            }
        }

        // --- Adaptive maintenance --------------------------------------------------
        if (tdee != null && tdee.confidence >= 0.4 && p.adaptiveEnabled && p.customKcal == 0) {
            val current = t.tdee
            val diff = tdee.estimate - current
            if (abs(diff) >= 100) {
                out += Insight(Level.INFO, "Your real maintenance ≈ ${tdee.estimate} kcal",
                    "From ${tdee.loggedDays} logged days and ${tdee.weighIns} weigh-ins: you ate ~${tdee.avgIntake} kcal/day while your trend " +
                        "moved ${Units.fmt2(Units.toDisplay(tdee.kgPerWeek, lb))} ${Units.label(lb)}/week. " +
                        "That's ${if (diff > 0) "$diff kcal higher" else "${-diff} kcal lower"} than the current estimate.",
                    "Apply at check-in")
            }
        }

        // --- Protein ---------------------------------------------------------------
        if (wk.loggedDays >= 3) {
            if (wk.proteinDaysHit < wk.loggedDays * 0.6) {
                val gap = (t.protein - (wk.avgProtein ?: 0)).coerceAtLeast(0)
                out += Insight(Level.WARN, "Protein short on ${wk.loggedDays - wk.proteinDaysHit} of ${wk.loggedDays} days",
                    "You average ${wk.avgProtein} g vs ${t.protein} g target (−$gap g). Easy fixes: 1 scoop whey (24 g), " +
                        "150 g chicken breast (46 g), 3 eggs (19 g), 200 g dahi (~7–20 g).")
            } else {
                out += Insight(Level.GOOD, "Protein on point", "Target hit on ${wk.proteinDaysHit} of ${wk.loggedDays} logged days.")
            }
            val avg = wk.avgKcal ?: 0
            if (avg > t.kcal * 1.1) out += Insight(Level.WARN, "Calories ${avg - t.kcal} over target on average",
                "Your 7-day average is $avg kcal vs ${t.kcal}. Watch liquid calories (chai with sugar, shakes, soft drinks) and cooking oil.")
            else if (avg in 1 until (t.kcal * 0.85).toInt() && p.goal != Goal.LOSE) out += Insight(Level.WARN, "Under-eating",
                "Average $avg kcal vs ${t.kcal}. Under-eating stalls strength and muscle gain.")
        } else {
            out += Insight(Level.INFO, "Log food at least 4–5 days a week",
                "Coaching accuracy depends on it – even rough logs beat none. Use Quick add when unsure.")
        }

        // --- Training frequency & balance -----------------------------------------------
        val lastWorkout = inp.workoutDates.maxOrNull()
        if (lastWorkout == null) {
            out += Insight(Level.INFO, "Start your first workout", "Pick a starter routine on the Workout tab.")
        } else {
            val gap = ChronoUnit.DAYS.between(lastWorkout, inp.today)
            if (gap >= 5) out += Insight(Level.INFO, "$gap days since your last workout", "A short session today keeps the habit alive.")
            if (wk.workouts >= 2) {
                val low = wk.muscleSets.filter { it.key in listOf("Chest", "Back", "Legs", "Shoulders") && it.value < 4 }.keys
                if (low.isNotEmpty()) out += Insight(Level.INFO, "Under-trained this week: ${low.joinToString()}",
                    "Aim for ~10–20 hard sets per muscle per week, spread over 2 sessions.")
                val high = wk.muscleSets.filter { it.value > 25 }.keys
                if (high.isNotEmpty()) out += Insight(Level.INFO, "Very high volume: ${high.joinToString()}",
                    "Over ~25 weekly sets rarely adds growth but adds fatigue. Consider trimming.")
            }
        }
        if (wk.prs.isNotEmpty()) out += Insight(Level.GOOD, "${wk.prs.size} new PR${if (wk.prs.size > 1) "s" else ""} this week",
            wk.prs.take(4).joinToString())

        // --- Weigh-ins / hydration ---------------------------------------------------------
        val lastWeigh = inp.weights.maxOfOrNull { it.date }?.let { LocalDate.parse(it) }
        if (lastWeigh == null) out += Insight(Level.INFO, "Weigh in to unlock trend coaching",
            "Weigh every morning after the toilet, before food. The app smooths out daily swings.")
        else if (ChronoUnit.DAYS.between(lastWeigh, inp.today) >= 4) out += Insight(Level.INFO,
            "No weigh-in for ${ChronoUnit.DAYS.between(lastWeigh, inp.today)} days", "Daily weigh-ins make the trend and calorie adjustments far more accurate.")
        val waterDays = inp.daily.count { LocalDate.parse(it.date).isAfter(inp.today.minusDays(7)) && it.waterMl >= p.waterGoalMl }
        if (waterDays < 3 && wk.loggedDays >= 3) out += Insight(Level.INFO, "Hydration: goal hit $waterDays/7 days",
            "Keep a bottle at your desk; aim for pale-yellow urine. Drink extra on training days.")

        return out.sortedBy { when (it.level) { Level.WARN -> 0; Level.INFO -> 1; Level.GOOD -> 2 } }
    }

    /** Daily kcal adjustment needed to move from the current rate to the goal rate. */
    private fun adjustKcal(currentPctWk: Double, goalPctWk: Double, p: Profile): Int {
        val diffKgWk = (goalPctWk - currentPctWk) / 100 * p.weightKg
        return (abs(diffKgWk * KCAL_PER_KG / 7) / 10).roundToInt() * 10
    }

    /** PR events: dates where a session beat all previous estimated 1RMs for that exercise (first session excluded). */
    fun prEvents(sessionsOldestFirst: List<Pair<LocalDate, List<WorkoutSet>>>): List<Pair<LocalDate, String>> {
        val best = mutableMapOf<String, Double>()
        val out = mutableListOf<Pair<LocalDate, String>>()
        sessionsOldestFirst.forEach { (date, sets) ->
            sets.filter { it.isWorking && it.reps > 0 && it.weightKg > 0 }.groupBy { it.exercise }.forEach { (ex, ss) ->
                val b = ss.maxOf { e1rm(it.weightKg, it.reps) }
                val prev = best[ex]
                if (prev != null && b > prev * 1.001) out += date to ex
                if (prev == null || b > prev) best[ex] = b
            }
        }
        return out
    }
}
