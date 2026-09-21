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
import com.fitlog.app.data.AppDatabase
import com.fitlog.app.data.Backup
import com.fitlog.app.data.CustomExercise
import com.fitlog.app.data.CustomFood
import com.fitlog.app.data.DailyLog
import com.fitlog.app.data.DayTotals
import com.fitlog.app.data.ExerciseDef
import com.fitlog.app.data.ExerciseLibrary
import com.fitlog.app.data.FoodEntry
import com.fitlog.app.data.Measurement
import com.fitlog.app.data.Profile
import com.fitlog.app.data.ProfileStore
import com.fitlog.app.data.Routine
import com.fitlog.app.data.Units
import com.fitlog.app.data.WeightEntry
import com.fitlog.app.data.Workout
import com.fitlog.app.data.WorkoutSet
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// ---------- Active workout (in progress, not yet saved) ----------

data class ActiveSet(val weight: String = "", val reps: String = "", val done: Boolean = false)
data class ActiveExercise(val name: String, val sets: List<ActiveSet>)
data class ActiveWorkout(val name: String, val startMillis: Long, val exercises: List<ActiveExercise>)

/** A finished workout with its sets, for history screens. */
data class WorkoutSummary(val workout: Workout, val sets: List<WorkoutSet>) {
    val volumeKg: Double get() = sets.sumOf { it.weightKg * it.reps }
    val exercises: List<String> get() = sets.map { it.exercise }.distinct()
    val date: LocalDate get() = Instant.ofEpochMilli(workout.startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
}

data class PersonalRecord(val exercise: String, val bestKg: Double, val bestReps: Int, val best1rm: Double, val date: LocalDate)

/** Epley estimated one-rep max. */
fun e1rm(kg: Double, reps: Int): Double = if (reps <= 1) kg else kg * (1 + reps / 30.0)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val store = ProfileStore(app)

    val profile: StateFlow<Profile> = store.profile

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

    val last30: StateFlow<List<DayTotals>> = db.food().dailyTotals(LocalDate.now().minusDays(29).toString())
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val loggedFoodDates: StateFlow<List<String>> = db.food().loggedDates()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val weights: StateFlow<List<WeightEntry>> = db.body().weights()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val measurements: StateFlow<List<Measurement>> = db.body().measurements()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val routines: StateFlow<List<Routine>> = db.workout().routines()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val customExercises: StateFlow<List<CustomExercise>> = db.workout().customExercises()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allExercises: StateFlow<List<ExerciseDef>> = customExercises
        .map { custom -> (ExerciseLibrary.items + custom.map { ExerciseDef(it.name, it.muscle) }).sortedBy { it.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ExerciseLibrary.items)

    val history: StateFlow<List<WorkoutSummary>> = combine(db.workout().workouts(), db.workout().sets()) { ws, sets ->
        val byId = sets.groupBy { it.workoutId }
        ws.map { WorkoutSummary(it, byId[it.id].orEmpty()) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Best lift per exercise (by estimated 1RM). */
    val records: StateFlow<List<PersonalRecord>> = history.map { list ->
        val best = mutableMapOf<String, PersonalRecord>()
        list.forEach { s ->
            s.sets.filter { it.reps > 0 && it.weightKg > 0 }.forEach { set ->
                val est = e1rm(set.weightKg, set.reps)
                val cur = best[set.exercise]
                if (cur == null || est > cur.best1rm) {
                    best[set.exercise] = PersonalRecord(set.exercise, set.weightKg, set.reps, est, s.date)
                }
            }
        }
        best.values.sortedBy { it.exercise.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Sets from the most recent workout that included each exercise. */
    fun previousSets(exercise: String): List<WorkoutSet> =
        history.value.firstOrNull { s -> s.sets.any { it.exercise == exercise } }
            ?.sets?.filter { it.exercise == exercise }.orEmpty()

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
        db.workout().deleteWorkout(id)
    }

    /** Routine being created / edited (shared between editor and exercise picker). */
    val routineDraft = MutableStateFlow<Routine?>(null)

    // ---------- Active workout ----------
    private val _active = MutableStateFlow<ActiveWorkout?>(restoreDraft())
    val active: StateFlow<ActiveWorkout?> = _active.asStateFlow()

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

    private fun newExercise(name: String): ActiveExercise {
        val prev = previousSets(name)
        val lb = profile.value.useLb
        val sets = if (prev.isEmpty()) List(3) { ActiveSet() } else prev.map {
            ActiveSet(weight = Units.fmt(Units.toDisplay(it.weightKg, lb)), reps = it.reps.toString())
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

    fun addSet(ex: Int) = update { w ->
        w.copy(exercises = w.exercises.mapIndexed { i, e ->
            if (i != ex) e else e.copy(sets = e.sets + (e.sets.lastOrNull()?.copy(done = false) ?: ActiveSet()))
        })
    }

    fun removeSet(ex: Int, set: Int) = update { w ->
        w.copy(exercises = w.exercises.mapIndexed { i, e ->
            if (i != ex) e else e.copy(sets = e.sets.filterIndexed { j, _ -> j != set })
        })
    }

    fun editSet(ex: Int, set: Int, s: ActiveSet) {
        val wasDone = _active.value?.exercises?.getOrNull(ex)?.sets?.getOrNull(set)?.done ?: false
        update { w ->
            w.copy(exercises = w.exercises.mapIndexed { i, e ->
                if (i != ex) e else e.copy(sets = e.sets.mapIndexed { j, old -> if (j == set) s else old })
            })
        }
        if (s.done && !wasDone) startRest(profile.value.restSeconds)
    }

    fun discardWorkout() {
        _active.value = null
        store.saveDraft(null)
        stopRest()
    }

    /** Saves completed sets (done, or with reps entered). Returns false if nothing to save. */
    fun finishWorkout(notes: String = ""): Boolean {
        val w = _active.value ?: return false
        val lb = profile.value.useLb
        val sets = mutableListOf<WorkoutSet>()
        w.exercises.forEachIndexed { ei, e ->
            var so = 0
            e.sets.forEach { s ->
                val reps = s.reps.toIntOrNull() ?: 0
                val weight = s.weight.replace(',', '.').toDoubleOrNull() ?: 0.0
                if (s.done && reps > 0) {
                    sets += WorkoutSet(workoutId = 0, exercise = e.name, exerciseOrder = ei, setOrder = so++,
                        weightKg = Units.fromDisplay(weight, lb), reps = reps)
                }
            }
        }
        if (sets.isEmpty()) return false
        val workout = Workout(name = w.name.ifBlank { "Workout" }, startMillis = w.startMillis,
            endMillis = System.currentTimeMillis(), notes = notes)
        viewModelScope.launch {
            val id = db.workout().insertWorkout(workout)
            db.workout().insertSets(sets.map { it.copy(workoutId = id) })
        }
        discardWorkout()
        return true
    }

    // ---------- Rest timer ----------
    private val _rest = MutableStateFlow(0)
    val restRemaining: StateFlow<Int> = _rest.asStateFlow()
    private var restJob: Job? = null

    fun startRest(seconds: Int) {
        restJob?.cancel()
        _rest.value = seconds
        restJob = viewModelScope.launch {
            while (_rest.value > 0) {
                delay(1000)
                _rest.value = _rest.value - 1
            }
            vibrate()
        }
    }

    fun adjustRest(delta: Int) {
        if (_rest.value > 0) _rest.value = (_rest.value + delta).coerceAtLeast(1)
    }

    fun stopRest() { restJob?.cancel(); _rest.value = 0 }

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
            e.sets.forEach { s -> sets.put(JSONObject().put("w", s.weight).put("r", s.reps).put("d", s.done)) }
            ex.put(JSONObject().put("name", e.name).put("sets", sets))
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
            ActiveExercise(e.getString("name"), (0 until sa.length()).map { j ->
                val s = sa.getJSONObject(j)
                ActiveSet(s.optString("w"), s.optString("r"), s.optBoolean("d"))
            })
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
        store.save(Profile())
        onDone()
    }
}
