package com.fitlog.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

enum class Sex(val label: String) { MALE("Male"), FEMALE("Female") }

enum class Goal(val label: String, val kcalDelta: Int, val proteinPerKg: Double) {
    LOSE("Lose fat", -500, 2.0),
    MAINTAIN("Maintain / recomp", 0, 1.8),
    GAIN("Build muscle", 300, 1.8),
}

enum class ActivityLevel(val label: String, val factor: Double) {
    SEDENTARY("Sedentary – desk job, little exercise", 1.2),
    LIGHT("Light – 1–3 workouts / week", 1.375),
    MODERATE("Moderate – 3–5 workouts / week", 1.55),
    ACTIVE("Very active – 6–7 workouts / week", 1.725),
    ATHLETE("Athlete / physical job", 1.9),
}

data class Targets(val kcal: Int, val protein: Int, val carbs: Int, val fat: Int, val bmr: Int, val tdee: Int)

data class Profile(
    val onboarded: Boolean = false,
    val name: String = "",
    val sex: Sex = Sex.MALE,
    val age: Int = 25,
    val heightCm: Double = 172.0,
    val weightKg: Double = 75.0,
    val activity: ActivityLevel = ActivityLevel.MODERATE,
    val goal: Goal = Goal.MAINTAIN,
    val useLb: Boolean = false,
    val customKcal: Int = 0,        // 0 = automatic
    val waterGoalMl: Int = 3000,
    val stepGoal: Int = 8000,
    val restSeconds: Int = 90,
) {
    /** Mifflin-St Jeor BMR -> TDEE -> goal adjustment -> macro split. */
    fun targets(): Targets {
        val bmr = 10 * weightKg + 6.25 * heightCm - 5 * age + if (sex == Sex.MALE) 5 else -161
        val tdee = bmr * activity.factor
        val auto = ((tdee + goal.kcalDelta) / 10).roundToInt() * 10
        val floor = if (sex == Sex.MALE) 1500 else 1200
        val kcal = if (customKcal > 0) customKcal else auto.coerceAtLeast(floor)
        val protein = (weightKg * goal.proteinPerKg).roundToInt()
        val fat = (kcal * 0.25 / 9).roundToInt()
        val carbs = ((kcal - protein * 4 - fat * 9) / 4.0).roundToInt().coerceAtLeast(0)
        return Targets(kcal, protein, carbs, fat, bmr.roundToInt(), tdee.roundToInt())
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
            useLb = prefs.getBoolean("useLb", d.useLb),
            customKcal = prefs.getInt("customKcal", d.customKcal),
            waterGoalMl = prefs.getInt("waterGoalMl", d.waterGoalMl),
            stepGoal = prefs.getInt("stepGoal", d.stepGoal),
            restSeconds = prefs.getInt("restSeconds", d.restSeconds),
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
            .putBoolean("useLb", p.useLb)
            .putInt("customKcal", p.customKcal)
            .putInt("waterGoalMl", p.waterGoalMl)
            .putInt("stepGoal", p.stepGoal)
            .putInt("restSeconds", p.restSeconds)
            .apply()
        _profile.value = p
    }

    // Simple string slot used to persist an in-progress workout across app restarts.
    fun saveDraft(json: String?) {
        prefs.edit().putString("activeWorkout", json).apply()
    }

    fun loadDraft(): String? = prefs.getString("activeWorkout", null)
}
