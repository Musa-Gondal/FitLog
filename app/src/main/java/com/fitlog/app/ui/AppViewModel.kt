package com.fitlog.app.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.app.data.Achievements
import com.fitlog.app.data.AppDatabase
import com.fitlog.app.data.Backup
import com.fitlog.app.data.Badge
import com.fitlog.app.data.Coach
import com.fitlog.app.data.CustomExercise
import com.fitlog.app.data.CustomFood
import com.fitlog.app.data.DailyLog
import com.fitlog.app.data.DayFacts
import com.fitlog.app.data.DayTotals
import com.fitlog.app.data.ExerciseDef
import com.fitlog.app.data.ExerciseLibrary
import com.fitlog.app.data.ExerciseNote
import com.fitlog.app.data.FoodEntry
import com.fitlog.app.data.Measurement
import com.fitlog.app.data.Profile
import com.fitlog.app.data.ProfileStore
import com.fitlog.app.data.Routine
import com.fitlog.app.data.Units
import com.fitlog.app.data.WeightEntry
import com.fitlog.app.data.Workout
import com.fitlog.app.data.WorkoutSet
import com.fitlog.app.data.isWorking
import com.fitlog.app.notify.AppState
import com.fitlog.app.notify.ReminderSettings
import com.fitlog.app.notify.Reminders
import com.fitlog.app.notify.RestTimer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// ---------- Active workout (in progress, not yet saved) ----------

/**
 * One set being logged. weight/reps are what the user typed; hintWeight/hintReps are the coach's
 * suggestion shown greyed-out – ticking ✓ on an empty set adopts the hint (one-tap logging).
 * type: N normal, W warm-up, D drop set, F failure.
 */
data class ActiveSet(
    val weight: String = "",
    val reps: String = "",
    val done: Boolean = false,
    val type: String = "N",
    val rpe: String = "",
    val hintWeight: String = "",
    val hintReps: String = "",
)

data class ActiveExercise(val name: String, val sets: List<ActiveSet>, val note: String = "", val superset: Int = 0)
data class ActiveWorkout(val name: String, val startMillis: Long, val exercises: List<ActiveExercise>)

/** A finished workout with its sets, for history screens. */
data class WorkoutSummary(val workout: Workout, val sets: List<WorkoutSet>) {
    val working: List<WorkoutSet> get() = sets.filter { it.isWorking }
    val volumeKg: Double get() = working.sumOf { it.weightKg * it.reps }
    val exercises: List<String> get() = sets.map { it.exercise }.distinct()
    val date: LocalDate get() = Instant.ofEpochMilli(workout.startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
}

data class PersonalRecord(val exercise: String, val bestKg: Double, val bestReps: Int, val best1rm: Double, val date: LocalDate)

/** Shown right after finishing a workout. */
data class FinishSummary(
    val name: String,
    val durationMs: Long,
    val sets: Int,
    val volumeKg: Double,
    val exercises: Int,
    val prs: List<String>,
)

/** Epley estimated one-rep max. */
fun e1rm(kg: Double, reps: Int): Double = Coach.e1rm(kg, reps)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val store = ProfileStore(app)

    val profile: StateFlow<Profile> = store.profile

    /** Screen requested by a notification tap. */
    val pendingRoute = MutableStateFlow<String?>(null)

    /** Selected tab on the Progress screen (0 = Coach). */
    val progressTab = MutableStateFlow(0)

    // ---------- Selected day (Today + Food screens) ----------
    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()
    fun setDate(d: LocalDate) { _date.value = if (d.isAfter(LocalDate.now())) LocalDate.now() else d }
    fun shiftDate(days: Long) = setDate(_date.value.plusDays(days))

    val foodEntries: StateFlow<List<FoodEntry>> = _date
        .flatMapLatest { db.food().entriesFor(it.toString()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val dailyLog: StateFlow<DailyLog> = _date
        .flatMapLatest { d -> db.body().daily(d.toString()).map { it ?: DailyLog(d.toString()) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DailyLog(LocalDate.now().toString()))

    val recentFoods: StateFlow<List<FoodEntry>> = db.food().recent()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val customFoods: StateFlow<List<CustomFood>> = db.food().customFoods()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allTotals: StateFlow<List<DayTotals>> = db.food().allDailyTotals()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Kept for screens that only need recent days. */
    val last30: StateFlow<List<DayTotals>> = allTotals
        .map { l -> val from = LocalDate.now().minusDays(29).toString(); l.filter { it.date >= from } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allDaily: StateFlow<List<DailyLog>> = db.body().allDailyLogs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val weights: StateFlow<List<WeightEntry>> = db.body().weights()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val measurements: StateFlow<List<Measurement>> = db.body().measurements()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val routines: StateFlow<List<Routine>> = db.workout().routines()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val customExercises: StateFlow<List<CustomExercise>> = db.workout().customExercises()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val notes: StateFlow<List<ExerciseNote>> = db.workout().notes()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allExercises: StateFlow<List<ExerciseDef>> = customExercises
        .map { custom ->
            (ExerciseLibrary.items + custom.map { ExerciseDef(it.name, it.muscle, custom = true) })
                .sortedBy { it.name.lowercase() }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ExerciseLibrary.items)

    fun lookup(name: String): ExerciseDef =
        ExerciseLibrary.items.firstOrNull { it.name == name }
            ?: ExerciseLibrary.find(name, customExercises.value.firstOrNull { it.name == name }?.muscle)

    val history: StateFlow<List<WorkoutSummary>> = combine(db.workout().workouts(), db.workout().sets()) { ws, sets ->
        val byId = sets.groupBy { it.workoutId }
        ws.map { WorkoutSummary(it, byId[it.id].orEmpty()) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** (date, sets) per workout, most recent first – the shape the coach works with. */
    private fun sessionsOf(h: List<WorkoutSummary>) = h.map { it.date to it.sets }

    /** Best lift per exercise (by estimated 1RM), warm-ups excluded. */
    val records: StateFlow<List<PersonalRecord>> = history.map { list ->
        val best = mutableMapOf<String, PersonalRecord>()
        list.forEach { s ->
            s.working.filter { it.reps > 0 && it.weightKg > 0 }.forEach { set ->
                val est = e1rm(set.weightKg, set.reps)
                val cur = best[set.exercise]
                if (cur == null || est > cur.best1rm) {
                    best[set.exercise] = PersonalRecord(set.exercise, set.weightKg, set.reps, est, s.date)
                }
            }
        }
        best.values.sortedBy { it.exercise.lowercase() }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val prEvents: StateFlow<List<Pair<LocalDate, String>>> = history
        .map { Coach.prEvents(sessionsOf(it).reversed()) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---------- Coaching ----------
    private val coachInputs: StateFlow<Coach.Inputs?> = combine(profile, allTotals, allDaily, weights, history) { p, t, d, w, h ->
        Coach.Inputs(
            profile = p, totals = t, daily = d, weights = w, sessions = sessionsOf(h),
            workoutDates = h.map { it.date }, prEvents = Coach.prEvents(sessionsOf(h).reversed()), lookup = ::lookup,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val tdeeEstimate: StateFlow<Coach.TdeeEstimate?> = combine(profile, allTotals, weights) { p, t, w ->
        Coach.estimateTdee(t, w, p.targets().formulaTdee)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val insights: StateFlow<List<Coach.Insight>> = combine(coachInputs, tdeeEstimate) { inp, tdee ->
        if (inp == null) emptyList() else Coach.insights(inp, tdee)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val thisWeek: StateFlow<Coach.Week?> = coachInputs.map { it?.let { inp -> Coach.week(inp) } }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val lastWeek: StateFlow<Coach.Week?> = coachInputs.map { it?.let { inp -> Coach.week(inp, inp.today.minusDays(7)) } }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Weekly check-in is due 7+ days after the last one (or once there's a week of data). */
    val checkInDue: StateFlow<Boolean> = combine(profile, allTotals, weights) { p, t, w ->
        val firstData = listOfNotNull(t.firstOrNull()?.date, w.firstOrNull()?.date).minOrNull()?.let { LocalDate.parse(it) }
        val last = p.lastCheckIn.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
        when {
            firstData == null -> false
            last == null -> ChronoUnit.DAYS.between(firstData, LocalDate.now()) >= 7
            else -> ChronoUnit.DAYS.between(last, LocalDate.now()) >= 7
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Apply the weekly check-in: adopt the adaptive maintenance estimate (if any) and mark done. */
    fun completeCheckIn(applyTdee: Boolean) {
        val est = tdeeEstimate.value
        val p = profile.value
        store.save(
            p.copy(
                adaptiveTdee = if (applyTdee && est != null) est.estimate else p.adaptiveTdee,
                lastCheckIn = LocalDate.now().toString(),
            )
        )
    }

    fun resetAdaptive() = store.save(profile.value.copy(adaptiveTdee = 0))

    /** Progression advice for an exercise based on its history. */
    fun suggestion(exercise: String): Coach.Suggestion =
        Coach.suggest(lookup(exercise), Coach.sessionsFor(exercise, sessionsOf(history.value)), profile.value.useLb)

    // ---------- Streaks, badges, heatmap ----------
    val dayFacts: StateFlow<List<DayFacts>> = combine(profile, allTotals, allDaily, history) { p, t, d, h ->
        val first = listOfNotNull(
            t.firstOrNull()?.date?.let { LocalDate.parse(it) },
            d.firstOrNull()?.date?.let { LocalDate.parse(it) },
            h.lastOrNull()?.date,
        ).minOrNull() ?: LocalDate.now()
        val from = minOf(first, LocalDate.now().minusDays(7 * 18L))
        val vol = h.groupBy { it.date }.mapValues { e -> e.value.sumOf { it.volumeKg } }
        Achievements.dayFacts(from, LocalDate.now(), p, t, d, vol)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val streak: StateFlow<Int> = dayFacts.map { Achievements.currentStreak(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val badges: StateFlow<List<Badge>> = combine(profile, dayFacts, history, prEvents, weights) { p, days, h, prs, w ->
        val zone = ZoneId.systemDefault()
        Achievements.badges(
            profile = p, days = days, workouts = sessionsOf(h),
            workoutStartHours = h.map { Instant.ofEpochMilli(it.workout.startMillis).atZone(zone).hour },
            prCount = prs.size, weights = w,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val seenVersion = MutableStateFlow(0)

    /** Newly unlocked badges not yet celebrated. */
    val newBadges: StateFlow<List<Badge>> = combine(badges, seenVersion) { b, _ ->
        val seen = store.seenBadges()
        b.filter { it.unlocked && it.id !in seen }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun badgeDates(): Map<String, String> = store.badgeDates()

    fun acknowledgeBadges() {
        val unlocked = badges.value.filter { it.unlocked }
        val dates = store.badgeDates().toMutableMap()
        unlocked.forEach { if (it.id !in dates) dates[it.id] = LocalDate.now().toString() }
        store.saveBadgeDates(dates)
        store.markBadgesSeen(unlocked.map { it.id }.toSet() + store.seenBadges())
        seenVersion.value++
    }

    // ---------- Reminders ----------
    private val _reminders = MutableStateFlow(Reminders.load(app))
    val reminders: StateFlow<ReminderSettings> = _reminders.asStateFlow()
    fun saveReminders(s: ReminderSettings) {
        _reminders.value = s
        viewModelScope.launch(Dispatchers.IO) { Reminders.save(getApplication<Application>(), s) }
    }

    // ---------- Profile ----------
    fun saveProfile(p: Profile) = store.save(p)

    // ---------- Food ----------
    fun addFood(meal: Int, name: String, serving: String, qty: Double, kcal: Double, p: Double, c: Double, f: Double) {
        val entry = FoodEntry(
            date = _date.value.toString(), meal = meal, name = name, servingLabel = serving, quantity = qty,
            kcal = kcal * qty, protein = p * qty, carbs = c * qty, fat = f * qty,
        )
        viewModelScope.launch { db.food().insert(entry) }
    }

    fun deleteFood(e: FoodEntry) = viewModelScope.launch { db.food().delete(e) }
    fun restoreFood(e: FoodEntry) = viewModelScope.launch { db.food().insert(e) }

    fun saveCustomFood(f: CustomFood) = viewModelScope.launch { db.food().insertCustom(f) }
    fun deleteCustomFood(f: CustomFood) = viewModelScope.launch { db.food().deleteCustom(f) }

    /** Copy all of yesterday's entries for a meal into the selected day. */
    fun copyMealFromPreviousDay(meal: Int) = viewModelScope.launch {
        val today = _date.value
        val prev = db.food().entriesOnce(today.minusDays(1).toString()).filter { it.meal == meal }
        db.food().insertAll(prev.map { it.copy(id = 0, date = today.toString()) })
    }

    // ---------- Water / steps ----------
    fun addWater(ml: Int) = viewModelScope.launch {
        val d = _date.value.toString()
        val cur = db.body().dailyOnce(d) ?: DailyLog(d)
        db.body().upsertDaily(cur.copy(waterMl = (cur.waterMl + ml).coerceAtLeast(0)))
    }

    fun setSteps(steps: Int) = viewModelScope.launch {
        val d = _date.value.toString()
        val cur = db.body().dailyOnce(d) ?: DailyLog(d)
        db.body().upsertDaily(cur.copy(steps = steps.coerceAtLeast(0)))
    }

    // ---------- Body ----------
    fun logWeight(date: LocalDate, kg: Double) = viewModelScope.launch {
        db.body().upsertWeight(WeightEntry(date.toString(), kg))
        // Keep calorie targets in sync with the most recent weigh-in.
        val latest = (weights.value.filter { it.date != date.toString() } + WeightEntry(date.toString(), kg))
            .maxByOrNull { it.date }
        if (latest != null && latest.date == date.toString()) store.save(profile.value.copy(weightKg = kg))
    }

    fun deleteWeight(w: WeightEntry) = viewModelScope.launch { db.body().deleteWeight(w) }

    fun addMeasurement(type: String, value: Double) = viewModelScope.launch {
        db.body().insertMeasurement(Measurement(date = LocalDate.now().toString(), type = type, value = value))
    }

    fun deleteMeasurement(m: Measurement) = viewModelScope.launch { db.body().deleteMeasurement(m) }

    // ---------- Routines & exercises ----------
    fun saveRoutine(r: Routine) = viewModelScope.launch { db.workout().upsertRoutine(r) }
    fun deleteRoutine(r: Routine) = viewModelScope.launch { db.workout().deleteRoutine(r) }

    /** A beginner-friendly Push / Pull / Legs split plus a full-body day. */
    fun addStarterRoutines() = viewModelScope.launch {
        listOf(
            Routine(name = "Push (chest, shoulders, triceps)", exercises = listOf(
                "Bench Press (Barbell)", "Incline Bench Press (Dumbbell)", "Shoulder Press (Dumbbell)",
                "Lateral Raise", "Tricep Pushdown", "Overhead Tricep Extension").joinToString("\n")),
            Routine(name = "Pull (back, biceps)", exercises = listOf(
                "Lat Pulldown", "Barbell Row", "Seated Cable Row", "Face Pull", "Barbell Curl", "Hammer Curl")
                .joinToString("\n")),
            Routine(name = "Legs", exercises = listOf(
                "Squat (Barbell)", "Romanian Deadlift", "Leg Press", "Leg Curl", "Leg Extension",
                "Standing Calf Raise").joinToString("\n")),
            Routine(name = "Full body", exercises = listOf(
                "Squat (Barbell)", "Bench Press (Barbell)", "Barbell Row", "Overhead Press (Barbell)",
                "Romanian Deadlift", "Plank").joinToString("\n")),
        ).forEach { db.workout().upsertRoutine(it) }
    }

    fun addCustomExercise(name: String, muscle: String) = viewModelScope.launch {
        db.workout().insertCustomExercise(CustomExercise(name = name.trim(), muscle = muscle))
    }

    fun deleteWorkout(id: Long) = viewModelScope.launch {
        db.workout().deleteSetsFor(id)
        db.workout().deleteNotesFor(id)
        db.workout().deleteWorkout(id)
    }

    /** Routine being created / edited (shared between editor and exercise picker). */
    val routineDraft = MutableStateFlow<Routine?>(null)

    /** Exercises used most recently, for the picker's "Recent" filter. */
    fun recentExercises(): List<String> = history.value.flatMap { it.exercises }.distinct().take(20)

    // ---------- Active workout ----------
    private val _active = MutableStateFlow(restoreDraft())
    val active: StateFlow<ActiveWorkout?> = _active.asStateFlow()

    private val _summary = MutableStateFlow<FinishSummary?>(null)
    val finishSummary: StateFlow<FinishSummary?> = _summary.asStateFlow()
    fun dismissSummary() { _summary.value = null }

    private fun update(block: (ActiveWorkout) -> ActiveWorkout) {
        val cur = _active.value ?: return
        val next = block(cur)
        _active.value = next
        store.saveDraft(toJson(next))
    }

    fun startWorkout(name: String, exercises: List<String>) {
        val w = ActiveWorkout(name, System.currentTimeMillis(), exercises.map { newExercise(it) })
        _active.value = w
        store.saveDraft(toJson(w))
    }

    private fun fmtW(kg: Double, lb: Boolean) = Units.fmt(Math.round(Units.toDisplay(kg, lb) * 10) / 10.0)

    /** New exercise pre-filled with the coach's suggestion as greyed-out hints. */
    private fun newExercise(name: String): ActiveExercise {
        val lb = profile.value.useLb
        val s = suggestion(name)
        val prev = Coach.sessionsFor(name, sessionsOf(history.value)).firstOrNull()?.sets.orEmpty()
        val count = s.sets.coerceIn(1, 6)
        val sets = List(count) { i ->
            val w = s.weightKg ?: prev.getOrNull(i)?.weightKg
            val r = s.reps ?: prev.getOrNull(i)?.reps
            ActiveSet(hintWeight = w?.let { fmtW(it, lb) } ?: "", hintReps = r?.toString() ?: "")
        }
        return ActiveExercise(name, sets)
    }

    fun renameWorkout(name: String) = update { it.copy(name = name) }
    fun addExercises(names: List<String>) = update { w -> w.copy(exercises = w.exercises + names.map { newExercise(it) }) }
    fun removeExercise(i: Int) = update { w -> w.copy(exercises = w.exercises.filterIndexed { idx, _ -> idx != i }) }
    fun moveExercise(i: Int, dir: Int) = update { w ->
        val j = i + dir
        if (j !in w.exercises.indices) w else {
            val l = w.exercises.toMutableList(); val t = l[i]; l[i] = l[j]; l[j] = t
            w.copy(exercises = l)
        }
    }

    fun setExerciseNote(ex: Int, note: String) = update { w ->
        w.copy(exercises = w.exercises.mapIndexed { i, e -> if (i == ex) e.copy(note = note) else e })
    }

    /** Link exercise [ex] with the next one as a superset (or unlink if already linked). */
    fun toggleSuperset(ex: Int) = update { w ->
        val list = w.exercises.toMutableList()
        if (ex !in list.indices || ex + 1 !in list.indices) return@update w
        val a = list[ex]
        val b = list[ex + 1]
        if (a.superset != 0 && a.superset == b.superset) {
            // Unlink b (and anything after it in the same group keeps b's group only if still adjacent).
            list[ex + 1] = b.copy(superset = 0)
            if (list.count { it.superset == a.superset } < 2) list[ex] = a.copy(superset = 0)
        } else {
            val group = when {
                a.superset != 0 -> a.superset
                b.superset != 0 -> b.superset
                else -> (list.maxOfOrNull { it.superset } ?: 0) + 1
            }
            list[ex] = a.copy(superset = group)
            list[ex + 1] = b.copy(superset = group)
        }
        w.copy(exercises = list)
    }

    fun addSet(ex: Int) = update { w ->
        w.copy(exercises = w.exercises.mapIndexed { i, e ->
            if (i != ex) e else {
                val last = e.sets.lastOrNull { it.type != "W" } ?: e.sets.lastOrNull()
                val newSet = if (last == null) ActiveSet() else ActiveSet(
                    hintWeight = last.weight.ifBlank { last.hintWeight },
                    hintReps = last.reps.ifBlank { last.hintReps },
                )
                e.copy(sets = e.sets + newSet)
            }
        })
    }

    fun removeSet(ex: Int, set: Int) = update { w ->
        w.copy(exercises = w.exercises.mapIndexed { i, e ->
            if (i != ex) e else e.copy(sets = e.sets.filterIndexed { j, _ -> j != set })
        })
    }

    /** Adds a warm-up ramp (bar, ~50 %, ~70 %, ~85 %) before the first working set. */
    fun addWarmups(ex: Int) = update { w ->
        val lb = profile.value.useLb
        w.copy(exercises = w.exercises.mapIndexed { i, e ->
            if (i != ex) e else {
                val first = e.sets.firstOrNull { it.type != "W" }
                val workDisp = (first?.let { f -> f.weight.ifBlank { f.hintWeight } } ?: "").replace(',', '.').toDoubleOrNull() ?: 0.0
                val ramp = Coach.warmups(Units.fromDisplay(workDisp, lb), lookup(e.name), profile.value.barKg, lb)
                if (ramp.isEmpty()) e else e.copy(
                    sets = ramp.map { (kg, reps) -> ActiveSet(weight = fmtW(kg, lb), reps = reps.toString(), type = "W") } +
                        e.sets.filter { it.type != "W" }
                )
            }
        })
    }

    /** Apply the coach's suggested weight/reps to every unfinished working set of an exercise. */
    fun applySuggestion(ex: Int) {
        val e = _active.value?.exercises?.getOrNull(ex) ?: return
        val s = suggestion(e.name)
        val lb = profile.value.useLb
        update { w ->
            w.copy(exercises = w.exercises.mapIndexed { i, x ->
                if (i != ex) x else x.copy(sets = x.sets.map { set ->
                    if (set.done || set.type == "W") set else set.copy(
                        weight = s.weightKg?.let { fmtW(it, lb) } ?: set.weight,
                        reps = s.reps?.toString() ?: set.reps,
                    )
                })
            })
        }
    }

    fun editSet(ex: Int, set: Int, s: ActiveSet) {
        val w0 = _active.value ?: return
        val wasDone = w0.exercises.getOrNull(ex)?.sets?.getOrNull(set)?.done ?: false
        // Ticking a set with empty fields adopts the greyed-out suggestion.
        val filled = if (s.done && !wasDone) s.copy(
            weight = s.weight.ifBlank { s.hintWeight },
            reps = s.reps.ifBlank { s.hintReps },
        ) else s
        update { w ->
            w.copy(exercises = w.exercises.mapIndexed { i, e ->
                if (i != ex) e else e.copy(sets = e.sets.mapIndexed { j, old -> if (j == set) filled else old })
            })
        }
        if (filled.done && !wasDone) {
            val exs = w0.exercises
            val cur = exs[ex]
            // In a superset, go straight to the next exercise; rest after the last one in the group.
            val nextInGroup = cur.superset != 0 && exs.getOrNull(ex + 1)?.superset == cur.superset
            if (!nextInGroup) {
                val rest = if (filled.type == "W") 45 else profile.value.restSeconds
                val nextLabel = nextUpLabel(ex, set)
                startRest(rest, nextLabel)
            }
        }
    }

    private fun nextUpLabel(ex: Int, set: Int): String? {
        val w = _active.value ?: return null
        val e = w.exercises.getOrNull(ex) ?: return null
        val groupStart = if (e.superset != 0) w.exercises.indexOfFirst { it.superset == e.superset } else ex
        val target = w.exercises.getOrNull(groupStart) ?: return null
        val nextSet = target.sets.indexOfFirst { !it.done }
        return if (nextSet >= 0) "${target.name} – set ${nextSet + 1}"
        else w.exercises.drop(ex + 1).firstOrNull { x -> x.sets.any { !it.done } }?.name
    }

    fun discardWorkout() {
        _active.value = null
        store.saveDraft(null)
        stopRest()
    }

    /** Saves completed sets (ticked and with reps). Returns false if nothing to save. */
    fun finishWorkout(notes: String = ""): Boolean {
        val w = _active.value ?: return false
        val lb = profile.value.useLb
        val sets = mutableListOf<WorkoutSet>()
        val exNotes = mutableListOf<Pair<String, String>>()
        w.exercises.forEachIndexed { ei, e ->
            var so = 0
            e.sets.forEach { s ->
                val reps = s.reps.toIntOrNull() ?: 0
                val weight = s.weight.replace(',', '.').toDoubleOrNull() ?: 0.0
                if (s.done && reps > 0) {
                    sets += WorkoutSet(
                        workoutId = 0, exercise = e.name, exerciseOrder = ei, setOrder = so++,
                        weightKg = Units.fromDisplay(weight, lb), reps = reps, setType = s.type,
                        rpe = s.rpe.replace(',', '.').toDoubleOrNull(),
                    )
                }
            }
            if (e.note.isNotBlank()) exNotes += e.name to e.note.trim()
        }
        if (sets.isEmpty()) return false

        // PRs = beat the best previous estimated 1RM for that exercise.
        val before = records.value.associateBy { it.exercise }
        val prs = sets.filter { it.isWorking && it.reps > 0 && it.weightKg > 0 }.groupBy { it.exercise }
            .filter { (ex, ss) ->
                val prev = before[ex] ?: return@filter false
                ss.maxOf { e1rm(it.weightKg, it.reps) } > prev.best1rm * 1.001
            }.keys.toList()
        val working = sets.filter { it.isWorking }
        val end = System.currentTimeMillis()
        _summary.value = FinishSummary(
            name = w.name.ifBlank { "Workout" }, durationMs = end - w.startMillis, sets = working.size,
            volumeKg = working.sumOf { it.weightKg * it.reps }, exercises = sets.map { it.exercise }.distinct().size, prs = prs,
        )
        val workout = Workout(name = w.name.ifBlank { "Workout" }, startMillis = w.startMillis, endMillis = end, notes = notes)
        viewModelScope.launch {
            val id = db.workout().insertWorkout(workout)
            db.workout().insertSets(sets.map { it.copy(workoutId = id) })
            db.workout().insertNotes(exNotes.map { ExerciseNote(workoutId = id, exercise = it.first, note = it.second) })
        }
        discardWorkout()
        return true
    }

    // ---------- Rest timer ----------
    private val _rest = MutableStateFlow(0)
    val restRemaining: StateFlow<Int> = _rest.asStateFlow()
    private val _restTotal = MutableStateFlow(0)
    val restTotal: StateFlow<Int> = _restTotal.asStateFlow()
    private var restJob: Job? = null
    private var restEndAt = 0L
    private var restNext: String? = null

    fun startRest(seconds: Int, nextLabel: String? = null) {
        restJob?.cancel()
        restNext = nextLabel
        restEndAt = System.currentTimeMillis() + seconds * 1000L
        _restTotal.value = seconds
        _rest.value = seconds
        RestTimer.start(getApplication<Application>(), restEndAt, nextLabel)
        restJob = viewModelScope.launch {
            while (true) {
                val left = ((restEndAt - System.currentTimeMillis() + 999) / 1000).toInt()
                _rest.value = left.coerceAtLeast(0)
                if (left <= 0) break
                delay(250)
            }
            if (AppState.foreground) vibrate()
        }
    }

    fun adjustRest(delta: Int) {
        if (_rest.value <= 0) return
        val left = ((restEndAt - System.currentTimeMillis()) / 1000).toInt() + delta
        startRest(left.coerceAtLeast(1), restNext)
        _restTotal.value = (_restTotal.value + delta).coerceAtLeast(1)
    }

    fun stopRest() {
        restJob?.cancel()
        _rest.value = 0
        RestTimer.cancel(getApplication<Application>())
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        val ctx = getApplication<Application>()
        val vib: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        runCatching { vib?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1)) }
    }

    // ---------- Draft persistence ----------
    private fun toJson(w: ActiveWorkout): String {
        val ex = JSONArray()
        w.exercises.forEach { e ->
            val sets = JSONArray()
            e.sets.forEach { s ->
                sets.put(JSONObject().put("w", s.weight).put("r", s.reps).put("d", s.done).put("t", s.type)
                    .put("rpe", s.rpe).put("hw", s.hintWeight).put("hr", s.hintReps))
            }
            ex.put(JSONObject().put("name", e.name).put("sets", sets).put("note", e.note).put("ss", e.superset))
        }
        return JSONObject().put("name", w.name).put("start", w.startMillis).put("ex", ex).toString()
    }

    private fun restoreDraft(): ActiveWorkout? = runCatching {
        val json = store.loadDraft() ?: return null
        val o = JSONObject(json)
        val arr = o.getJSONArray("ex")
        val exercises = (0 until arr.length()).map { i ->
            val e = arr.getJSONObject(i)
            val sa = e.getJSONArray("sets")
            ActiveExercise(
                name = e.getString("name"),
                sets = (0 until sa.length()).map { j ->
                    val s = sa.getJSONObject(j)
                    ActiveSet(
                        weight = s.optString("w"), reps = s.optString("r"), done = s.optBoolean("d"),
                        type = s.optString("t", "N").ifBlank { "N" }, rpe = s.optString("rpe"),
                        hintWeight = s.optString("hw"), hintReps = s.optString("hr"),
                    )
                },
                note = e.optString("note"),
                superset = e.optInt("ss", 0),
            )
        }
        ActiveWorkout(o.getString("name"), o.getLong("start"), exercises)
    }.getOrNull()

    // ---------- Backup ----------
    fun exportTo(uri: Uri, onDone: (String) -> Unit) = viewModelScope.launch {
        val msg = runCatching {
            val json = Backup.export(db, profile.value)
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
                    it.write(json.toByteArray())
                } ?: error("Could not open file")
            }
            "Backup saved"
        }.getOrElse { "Export failed: ${it.message}" }
        onDone(msg)
    }

    fun importFrom(uri: Uri, onDone: (String) -> Unit) = viewModelScope.launch {
        val msg = runCatching {
            val json = withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: error("Could not open file")
            }
            val p = Backup.import(db, json, profile.value)
            store.save(p)
            "Backup restored"
        }.getOrElse { "Import failed: ${it.message}" }
        onDone(msg)
    }

    fun resetAll(onDone: () -> Unit) = viewModelScope.launch {
        withContext(Dispatchers.IO) { db.clearAllTables() }
        discardWorkout()
        store.markBadgesSeen(emptySet())
        store.saveBadgeDates(emptyMap())
        store.save(Profile())
        onDone()
    }

    init {
        // Upgrading with existing history: mark already-earned badges as seen instead of celebrating them all at once.
        viewModelScope.launch {
            badges.collect { b ->
                if (b.isNotEmpty() && store.seenBadges().isEmpty() && store.badgeDates().isEmpty() && history.value.size > 5) {
                    acknowledgeBadges()
                }
            }
        }
    }
}
