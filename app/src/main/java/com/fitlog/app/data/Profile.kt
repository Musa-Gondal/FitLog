package com.fitlog.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

enum class Sex(val label: String) { MALE("Male"), FEMALE("Female") }

/**
 * Goal with evidence-based defaults:
 * - Fat loss: 0.5–1.0 % of bodyweight per week preserves muscle best; protein ≈ 2.0–2.2 g/kg.
 * - Muscle gain: 0.25–0.5 % / week keeps fat gain low; protein ≈ 1.6–2.0 g/kg.
 */
enum class Goal(val label: String, val proteinPerKg: Double, val direction: Int, val defaultRate: Double, val rates: List<Double>) {
    LOSE("Lose fat", 2.1, -1, 0.5, listOf(0.25, 0.5, 0.75, 1.0)),
    MAINTAIN("Maintain / recomp", 1.8, 0, 0.0, listOf(0.0)),
    GAIN("Build muscle", 1.8, 1, 0.25, listOf(0.1, 0.25, 0.5)),
}

enum class ActivityLevel(val label: String, val factor: Double) {
    SEDENTARY("Sedentary – desk job, little exercise", 1.2),
    LIGHT("Light – 1–3 workouts / week", 1.375),
    MODERATE("Moderate – 3–5 workouts / week", 1.55),
    ACTIVE("Very active – 6–7 workouts / week", 1.725),
    ATHLETE("Athlete / physical job", 1.9),
}

data class Targets(
    val kcal: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val bmr: Int,
    val formulaTdee: Int,
    val tdee: Int,             // maintenance actually used (adaptive or formula)
    val adaptive: Boolean,     // true when tdee comes from your own data
    val dailyDelta: Int,       // kcal added/removed for the goal
    val weeklyChangeKg: Double, // planned change per week (negative = loss)
    val floored: Boolean,      // true if a safety minimum kicked in
)

/** Energy in 1 kg of body-mass change (mixed fat/lean tissue), commonly used ≈ 7700 kcal. */
const val KCAL_PER_KG = 7700.0

data class Profile(
    val onboarded: Boolean = false,
    val name: String = "",
    val sex: Sex = Sex.MALE,
    val age: Int = 25,
    val heightCm: Double = 172.0,
    val weightKg: Double = 75.0,
    val activity: ActivityLevel = ActivityLevel.MODERATE,
    val goal: Goal = Goal.MAINTAIN,
    val ratePct: Double = -1.0,      // % bodyweight per week; <0 = goal default
    val useLb: Boolean = false,
    val customKcal: Int = 0,         // 0 = automatic
    val waterGoalMl: Int = 3000,
    val stepGoal: Int = 8000,
    val restSeconds: Int = 90,
    val adaptiveEnabled: Boolean = true,
    val adaptiveTdee: Int = 0,       // 0 = not yet estimated
    val lastCheckIn: String = "",    // yyyy-MM-dd of last applied weekly check-in
    val keepAwake: Boolean = true,
    val barKg: Double = 20.0,
) {
    val rate: Double get() = if (goal == Goal.MAINTAIN) 0.0 else if (ratePct > 0) ratePct else goal.defaultRate

    val bmi: Double get() = weightKg / ((heightCm / 100) * (heightCm / 100))

    /** Protein is dosed on bodyweight, capped at the weight for BMI 25 when BMI > 27 (avoids overshooting). */
    val proteinRefKg: Double get() {
        val h = heightCm / 100
        return if (bmi > 27) 25 * h * h else weightKg
    }

    fun formulaBmr(): Double = 10 * weightKg + 6.25 * heightCm - 5 * age + (if (sex == Sex.MALE) 5 else -161)

    fun targets(): Targets {
        val bmr = formulaBmr()
        val formulaTdee = bmr * activity.factor
        val useAdaptive = adaptiveEnabled && adaptiveTdee > 0
        val tdee = if (useAdaptive) adaptiveTdee.toDouble() else formulaTdee
        val weeklyKg = goal.direction * rate / 100.0 * weightKg
        val delta = weeklyKg * KCAL_PER_KG / 7.0
        val floor = maxOf(if (sex == Sex.MALE) 1500.0 else 1200.0, bmr)
        var kcal = ((tdee + delta) / 10).roundToInt() * 10
        var floored = false
        if (customKcal > 0) {
            kcal = customKcal
        } else if (kcal < floor) {
            kcal = (floor / 10).roundToInt() * 10
            floored = true
        }
        val protein = (proteinRefKg * goal.proteinPerKg).roundToInt()
        val fat = maxOf(kcal * 0.25 / 9, weightKg * 0.6).roundToInt()
        val carbs = ((kcal - protein * 4 - fat * 9) / 4.0).roundToInt().coerceAtLeast(0)
        return Targets(
            kcal = kcal, protein = protein, carbs = carbs, fat = fat, bmr = bmr.roundToInt(),
            formulaTdee = formulaTdee.roundToInt(), tdee = tdee.roundToInt(), adaptive = useAdaptive,
            dailyDelta = if (customKcal > 0) kcal - tdee.roundToInt() else delta.roundToInt(),
            weeklyChangeKg = weeklyKg, floored = floored,
        )
    }
}

/** Unit helpers. Everything is stored in kg; converted only for display/input. */
object Units {
    const val LB_PER_KG = 2.2046226218
    fun toDisplay(kg: Double, lb: Boolean) = if (lb) kg * LB_PER_KG else kg
    fun fromDisplay(v: Double, lb: Boolean) = if (lb) v / LB_PER_KG else v
    fun label(lb: Boolean) = if (lb) "lb" else "kg"
    fun fmt(v: Double): String =
        if (v % 1.0 == 0.0) v.toLong().toString() else String.format(java.util.Locale.US, "%.1f", v)
    fun fmt2(v: Double): String = String.format(java.util.Locale.US, "%.2f", v).trimEnd('0').trimEnd('.')
    /** Format a kg value in the user's unit, e.g. "82.5 kg". */
    fun show(kg: Double, lb: Boolean): String = "${fmt(Math.round(toDisplay(kg, lb) * 10) / 10.0)} ${label(lb)}"
}

class ProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("profile", Context.MODE_PRIVATE)
    private val _profile = MutableStateFlow(load())
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    private fun load(): Profile {
        val d = Profile()
        return Profile(
            onboarded = prefs.getBoolean("onboarded", false),
            name = prefs.getString("name", d.name) ?: d.name,
            sex = runCatching { Sex.valueOf(prefs.getString("sex", d.sex.name)!!) }.getOrDefault(d.sex),
            age = prefs.getInt("age", d.age),
            heightCm = prefs.getFloat("heightCm", d.heightCm.toFloat()).toDouble(),
            weightKg = prefs.getFloat("weightKg", d.weightKg.toFloat()).toDouble(),
            activity = runCatching { ActivityLevel.valueOf(prefs.getString("activity", d.activity.name)!!) }.getOrDefault(d.activity),
            goal = runCatching { Goal.valueOf(prefs.getString("goal", d.goal.name)!!) }.getOrDefault(d.goal),
            ratePct = prefs.getFloat("ratePct", d.ratePct.toFloat()).toDouble(),
            useLb = prefs.getBoolean("useLb", d.useLb),
            customKcal = prefs.getInt("customKcal", d.customKcal),
            waterGoalMl = prefs.getInt("waterGoalMl", d.waterGoalMl),
            stepGoal = prefs.getInt("stepGoal", d.stepGoal),
            restSeconds = prefs.getInt("restSeconds", d.restSeconds),
            adaptiveEnabled = prefs.getBoolean("adaptiveEnabled", d.adaptiveEnabled),
            adaptiveTdee = prefs.getInt("adaptiveTdee", d.adaptiveTdee),
            lastCheckIn = prefs.getString("lastCheckIn", d.lastCheckIn) ?: "",
            keepAwake = prefs.getBoolean("keepAwake", d.keepAwake),
            barKg = prefs.getFloat("barKg", d.barKg.toFloat()).toDouble(),
        )
    }

    fun save(p: Profile) {
        prefs.edit()
            .putBoolean("onboarded", p.onboarded)
            .putString("name", p.name)
            .putString("sex", p.sex.name)
            .putInt("age", p.age)
            .putFloat("heightCm", p.heightCm.toFloat())
            .putFloat("weightKg", p.weightKg.toFloat())
            .putString("activity", p.activity.name)
            .putString("goal", p.goal.name)
            .putFloat("ratePct", p.ratePct.toFloat())
            .putBoolean("useLb", p.useLb)
            .putInt("customKcal", p.customKcal)
            .putInt("waterGoalMl", p.waterGoalMl)
            .putInt("stepGoal", p.stepGoal)
            .putInt("restSeconds", p.restSeconds)
            .putBoolean("adaptiveEnabled", p.adaptiveEnabled)
            .putInt("adaptiveTdee", p.adaptiveTdee)
            .putString("lastCheckIn", p.lastCheckIn)
            .putBoolean("keepAwake", p.keepAwake)
            .putFloat("barKg", p.barKg.toFloat())
            .apply()
        _profile.value = p
    }

    // Simple string slot used to persist an in-progress workout across app restarts.
    fun saveDraft(json: String?) {
        prefs.edit().putString("activeWorkout", json).apply()
    }

    fun loadDraft(): String? = prefs.getString("activeWorkout", null)

    /** Badge ids the user has already been shown (so new unlocks can be celebrated once). */
    fun seenBadges(): Set<String> = prefs.getStringSet("seenBadges", emptySet()) ?: emptySet()
    fun markBadgesSeen(ids: Set<String>) {
        prefs.edit().putStringSet("seenBadges", HashSet(ids)).apply()
    }

    fun badgeDates(): Map<String, String> =
        (prefs.getString("badgeDates", "") ?: "").split(';').filter { it.contains('=') }
            .associate { it.substringBefore('=') to it.substringAfter('=') }

    fun saveBadgeDates(m: Map<String, String>) {
        prefs.edit().putString("badgeDates", m.entries.joinToString(";") { "${it.key}=${it.value}" }).apply()
    }
}
