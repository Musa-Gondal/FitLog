package com.fitlog.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_entries WHERE date = :date ORDER BY meal, id")
    fun entriesFor(date: String): Flow<List<FoodEntry>>

    @Query(
        "SELECT date, SUM(kcal) AS kcal, SUM(protein) AS protein, SUM(carbs) AS carbs, SUM(fat) AS fat " +
            "FROM food_entries WHERE date >= :from GROUP BY date ORDER BY date"
    )
    fun dailyTotals(from: String): Flow<List<DayTotals>>

    @Query("SELECT * FROM food_entries WHERE id IN (SELECT MAX(id) FROM food_entries GROUP BY name) ORDER BY id DESC LIMIT 25")
    fun recent(): Flow<List<FoodEntry>>

    @Query("SELECT DISTINCT date FROM food_entries ORDER BY date DESC LIMIT 500")
    fun loggedDates(): Flow<List<String>>

    @Query("SELECT * FROM food_entries WHERE date = :date")
    suspend fun entriesOnce(date: String): List<FoodEntry>

    @Insert
    suspend fun insert(entry: FoodEntry): Long

    @Insert
    suspend fun insertAll(entries: List<FoodEntry>)

    @Delete
    suspend fun delete(entry: FoodEntry)

    @Query("SELECT * FROM food_entries")
    suspend fun all(): List<FoodEntry>

    @Query("DELETE FROM food_entries")
    suspend fun clear()

    // Custom foods
    @Query("SELECT * FROM custom_foods ORDER BY name COLLATE NOCASE")
    fun customFoods(): Flow<List<CustomFood>>

    @Insert
    suspend fun insertCustom(food: CustomFood): Long

    @Insert
    suspend fun insertCustomAll(foods: List<CustomFood>)

    @Delete
    suspend fun deleteCustom(food: CustomFood)

    @Query("SELECT * FROM custom_foods")
    suspend fun allCustom(): List<CustomFood>

    @Query("DELETE FROM custom_foods")
    suspend fun clearCustom()
}

@Dao
interface BodyDao {
    @Query("SELECT * FROM daily_logs WHERE date = :date")
    fun daily(date: String): Flow<DailyLog?>

    @Query("SELECT * FROM daily_logs WHERE date = :date")
    suspend fun dailyOnce(date: String): DailyLog?

    @Query("SELECT * FROM daily_logs WHERE date >= :from ORDER BY date")
    fun dailyRange(from: String): Flow<List<DailyLog>>

    @Upsert
    suspend fun upsertDaily(log: DailyLog)

    @Upsert
    suspend fun upsertDailyAll(logs: List<DailyLog>)

    @Query("SELECT * FROM daily_logs")
    suspend fun allDaily(): List<DailyLog>

    @Query("DELETE FROM daily_logs")
    suspend fun clearDaily()

    @Query("SELECT * FROM weights ORDER BY date")
    fun weights(): Flow<List<WeightEntry>>

    @Upsert
    suspend fun upsertWeight(w: WeightEntry)

    @Upsert
    suspend fun upsertWeightAll(w: List<WeightEntry>)

    @Delete
    suspend fun deleteWeight(w: WeightEntry)

    @Query("SELECT * FROM weights")
    suspend fun allWeights(): List<WeightEntry>

    @Query("DELETE FROM weights")
    suspend fun clearWeights()

    @Query("SELECT * FROM measurements ORDER BY date DESC, id DESC")
    fun measurements(): Flow<List<Measurement>>

    @Insert
    suspend fun insertMeasurement(m: Measurement)

    @Insert
    suspend fun insertMeasurementAll(m: List<Measurement>)

    @Delete
    suspend fun deleteMeasurement(m: Measurement)

    @Query("SELECT * FROM measurements")
    suspend fun allMeasurements(): List<Measurement>

    @Query("DELETE FROM measurements")
    suspend fun clearMeasurements()
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY startMillis DESC")
    fun workouts(): Flow<List<Workout>>

    @Query("SELECT * FROM workout_sets ORDER BY workoutId, exerciseOrder, setOrder")
    fun sets(): Flow<List<WorkoutSet>>

    @Insert
    suspend fun insertWorkout(w: Workout): Long

    @Insert
    suspend fun insertWorkoutAll(w: List<Workout>)

    @Insert
    suspend fun insertSets(sets: List<WorkoutSet>)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    @Query("DELETE FROM workout_sets WHERE workoutId = :id")
    suspend fun deleteSetsFor(id: Long)

    @Query("SELECT * FROM workouts")
    suspend fun allWorkouts(): List<Workout>

    @Query("SELECT * FROM workout_sets")
    suspend fun allSets(): List<WorkoutSet>

    @Query("DELETE FROM workouts")
    suspend fun clearWorkouts()

    @Query("DELETE FROM workout_sets")
    suspend fun clearSets()

    // Routines
    @Query("SELECT * FROM routines ORDER BY name COLLATE NOCASE")
    fun routines(): Flow<List<Routine>>

    @Upsert
    suspend fun upsertRoutine(r: Routine)

    @Insert
    suspend fun insertRoutineAll(r: List<Routine>)

    @Delete
    suspend fun deleteRoutine(r: Routine)

    @Query("SELECT * FROM routines")
    suspend fun allRoutines(): List<Routine>

    @Query("DELETE FROM routines")
    suspend fun clearRoutines()

    // Custom exercises
    @Query("SELECT * FROM custom_exercises ORDER BY name COLLATE NOCASE")
    fun customExercises(): Flow<List<CustomExercise>>

    @Insert
    suspend fun insertCustomExercise(e: CustomExercise)

    @Insert
    suspend fun insertCustomExerciseAll(e: List<CustomExercise>)

    @Delete
    suspend fun deleteCustomExercise(e: CustomExercise)

    @Query("SELECT * FROM custom_exercises")
    suspend fun allCustomExercises(): List<CustomExercise>

    @Query("DELETE FROM custom_exercises")
    suspend fun clearCustomExercises()
}

@Database(
    entities = [
        FoodEntry::class, CustomFood::class, DailyLog::class, WeightEntry::class,
        Measurement::class, Workout::class, WorkoutSet::class, Routine::class, CustomExercise::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun food(): FoodDao
    abstract fun body(): BodyDao
    abstract fun workout(): WorkoutDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "fitlog.db")
                .build().also { instance = it }
        }
    }
}
