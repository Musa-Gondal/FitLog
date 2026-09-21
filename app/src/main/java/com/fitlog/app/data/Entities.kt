package com.fitlog.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One logged food item. Nutrition values are TOTALS for the quantity eaten. */
@Entity(tableName = "food_entries", indices = [Index("date")])
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,          // yyyy-MM-dd
    val meal: Int,             // index into Meals.names
    val name: String,
    val servingLabel: String,  // e.g. "1 roti (40 g)"
    val quantity: Double,      // number of servings
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)

/** A food the user created. Nutrition is per ONE serving. */
@Entity(tableName = "custom_foods")
data class CustomFood(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val serving: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)

/** Water and steps for a day. */
@Entity(tableName = "daily_logs")
data class DailyLog(
    @PrimaryKey val date: String,
    val waterMl: Int = 0,
    val steps: Int = 0,
)

/** Body weight, one value per day (always stored in kg). */
@Entity(tableName = "weights")
data class WeightEntry(
    @PrimaryKey val date: String,
    val kg: Double,
)

/** Body measurement: type is e.g. "Waist"; value in cm (or % for body fat). */
@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val type: String,
    val value: Double,
)

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startMillis: Long,
    val endMillis: Long,
    val notes: String = "",
)

/** A completed set. Weight always stored in kg. */
@Entity(tableName = "workout_sets", indices = [Index("workoutId"), Index("exercise")])
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exercise: String,
    val exerciseOrder: Int,
    val setOrder: Int,
    val weightKg: Double,
    val reps: Int,
)

/** Workout template. exercises = names separated by '\n'. */
@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val exercises: String,
) {
    fun exerciseList(): List<String> = exercises.split('\n').filter { it.isNotBlank() }
}

@Entity(tableName = "custom_exercises")
data class CustomExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscle: String,
)

/** Result row for per-day nutrition totals. */
data class DayTotals(
    val date: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)

object Meals {
    val names = listOf("Breakfast", "Lunch", "Dinner", "Snacks")
}
