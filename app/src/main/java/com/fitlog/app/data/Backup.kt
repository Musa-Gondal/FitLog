package com.fitlog.app.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

/** Export / import all data as a single JSON document (uses Android's built-in org.json). */
object Backup {

    suspend fun export(db: AppDatabase, profile: Profile): String {
        val root = JSONObject()
        root.put("app", "FitLog")
        root.put("version", 1)
        root.put("profile", JSONObject().apply {
            put("name", profile.name); put("sex", profile.sex.name); put("age", profile.age)
            put("heightCm", profile.heightCm); put("weightKg", profile.weightKg)
            put("activity", profile.activity.name); put("goal", profile.goal.name)
            put("useLb", profile.useLb); put("customKcal", profile.customKcal)
            put("waterGoalMl", profile.waterGoalMl); put("stepGoal", profile.stepGoal)
            put("restSeconds", profile.restSeconds)
        })
        root.put("food", JSONArray().apply {
            db.food().all().forEach {
                put(JSONObject().put("date", it.date).put("meal", it.meal).put("name", it.name)
                    .put("serving", it.servingLabel).put("qty", it.quantity).put("kcal", it.kcal)
                    .put("p", it.protein).put("c", it.carbs).put("f", it.fat))
            }
        })
        root.put("customFoods", JSONArray().apply {
            db.food().allCustom().forEach {
                put(JSONObject().put("name", it.name).put("serving", it.serving).put("kcal", it.kcal)
                    .put("p", it.protein).put("c", it.carbs).put("f", it.fat))
            }
        })
        root.put("daily", JSONArray().apply {
            db.body().allDaily().forEach {
                put(JSONObject().put("date", it.date).put("water", it.waterMl).put("steps", it.steps))
            }
        })
        root.put("weights", JSONArray().apply {
            db.body().allWeights().forEach { put(JSONObject().put("date", it.date).put("kg", it.kg)) }
        })
        root.put("measurements", JSONArray().apply {
            db.body().allMeasurements().forEach {
                put(JSONObject().put("date", it.date).put("type", it.type).put("value", it.value))
            }
        })
        val setsByWorkout = db.workout().allSets().groupBy { it.workoutId }
        root.put("workouts", JSONArray().apply {
            db.workout().allWorkouts().forEach { w ->
                val sets = JSONArray()
                setsByWorkout[w.id].orEmpty().forEach {
                    sets.put(JSONObject().put("ex", it.exercise).put("eo", it.exerciseOrder)
                        .put("so", it.setOrder).put("kg", it.weightKg).put("reps", it.reps))
                }
                put(JSONObject().put("name", w.name).put("start", w.startMillis).put("end", w.endMillis)
                    .put("notes", w.notes).put("sets", sets))
            }
        })
        root.put("routines", JSONArray().apply {
            db.workout().allRoutines().forEach { put(JSONObject().put("name", it.name).put("exercises", it.exercises)) }
        })
        root.put("customExercises", JSONArray().apply {
            db.workout().allCustomExercises().forEach { put(JSONObject().put("name", it.name).put("muscle", it.muscle)) }
        })
        return root.toString(2)
    }

    private inline fun JSONArray.objects(block: (JSONObject) -> Unit) {
        for (i in 0 until length()) block(getJSONObject(i))
    }

    /** Replaces ALL current data with the backup. Returns the restored profile. */
    suspend fun import(db: AppDatabase, json: String, current: Profile): Profile {
        val root = JSONObject(json)
        require(root.optString("app") == "FitLog") { "Not a FitLog backup file" }

        val food = mutableListOf<FoodEntry>()
        root.optJSONArray("food")?.objects {
            food += FoodEntry(date = it.getString("date"), meal = it.getInt("meal"), name = it.getString("name"),
                servingLabel = it.optString("serving"), quantity = it.optDouble("qty", 1.0),
                kcal = it.optDouble("kcal", 0.0), protein = it.optDouble("p", 0.0),
                carbs = it.optDouble("c", 0.0), fat = it.optDouble("f", 0.0))
        }
        val custom = mutableListOf<CustomFood>()
        root.optJSONArray("customFoods")?.objects {
            custom += CustomFood(name = it.getString("name"), serving = it.optString("serving"),
                kcal = it.optDouble("kcal", 0.0), protein = it.optDouble("p", 0.0),
                carbs = it.optDouble("c", 0.0), fat = it.optDouble("f", 0.0))
        }
        val daily = mutableListOf<DailyLog>()
        root.optJSONArray("daily")?.objects {
            daily += DailyLog(it.getString("date"), it.optInt("water"), it.optInt("steps"))
        }
        val weights = mutableListOf<WeightEntry>()
        root.optJSONArray("weights")?.objects { weights += WeightEntry(it.getString("date"), it.getDouble("kg")) }
        val measurements = mutableListOf<Measurement>()
        root.optJSONArray("measurements")?.objects {
            measurements += Measurement(date = it.getString("date"), type = it.getString("type"), value = it.getDouble("value"))
        }
        val routines = mutableListOf<Routine>()
        root.optJSONArray("routines")?.objects { routines += Routine(name = it.getString("name"), exercises = it.getString("exercises")) }
        val customEx = mutableListOf<CustomExercise>()
        root.optJSONArray("customExercises")?.objects { customEx += CustomExercise(name = it.getString("name"), muscle = it.optString("muscle")) }

        db.withTransaction {
            db.food().clear(); db.food().clearCustom()
            db.body().clearDaily(); db.body().clearWeights(); db.body().clearMeasurements()
            db.workout().clearWorkouts(); db.workout().clearSets()
            db.workout().clearRoutines(); db.workout().clearCustomExercises()

            db.food().insertAll(food)
            db.food().insertCustomAll(custom)
            db.body().upsertDailyAll(daily)
            db.body().upsertWeightAll(weights)
            db.body().insertMeasurementAll(measurements)
            db.workout().insertRoutineAll(routines)
            db.workout().insertCustomExerciseAll(customEx)
            root.optJSONArray("workouts")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val w = arr.getJSONObject(i)
                    val id = db.workout().insertWorkout(
                        Workout(name = w.getString("name"), startMillis = w.getLong("start"),
                            endMillis = w.getLong("end"), notes = w.optString("notes"))
                    )
                    val sets = mutableListOf<WorkoutSet>()
                    w.optJSONArray("sets")?.objects {
                        sets += WorkoutSet(workoutId = id, exercise = it.getString("ex"), exerciseOrder = it.getInt("eo"),
                            setOrder = it.getInt("so"), weightKg = it.getDouble("kg"), reps = it.getInt("reps"))
                    }
                    db.workout().insertSets(sets)
                }
            }
        }

        val p = root.optJSONObject("profile") ?: return current
        return current.copy(
            onboarded = true,
            name = p.optString("name", current.name),
            sex = runCatching { Sex.valueOf(p.getString("sex")) }.getOrDefault(current.sex),
            age = p.optInt("age", current.age),
            heightCm = p.optDouble("heightCm", current.heightCm),
            weightKg = p.optDouble("weightKg", current.weightKg),
            activity = runCatching { ActivityLevel.valueOf(p.getString("activity")) }.getOrDefault(current.activity),
            goal = runCatching { Goal.valueOf(p.getString("goal")) }.getOrDefault(current.goal),
            useLb = p.optBoolean("useLb", current.useLb),
            customKcal = p.optInt("customKcal", current.customKcal),
            waterGoalMl = p.optInt("waterGoalMl", current.waterGoalMl),
            stepGoal = p.optInt("stepGoal", current.stepGoal),
            restSeconds = p.optInt("restSeconds", current.restSeconds),
        )
    }
}
