package com.fitlog.app.data

data class ExerciseDef(val name: String, val muscle: String)

object ExerciseLibrary {
    val muscles = listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs", "Glutes", "Core", "Cardio", "Full body")

    private fun e(muscle: String, vararg names: String) = names.map { ExerciseDef(it, muscle) }

    val items: List<ExerciseDef> =
        e("Chest", "Bench Press (Barbell)", "Bench Press (Dumbbell)", "Incline Bench Press (Barbell)",
            "Incline Bench Press (Dumbbell)", "Decline Bench Press", "Chest Fly (Dumbbell)", "Cable Crossover",
            "Pec Deck / Machine Fly", "Chest Press (Machine)", "Push-up", "Chest Dip") +
        e("Back", "Deadlift", "Pull-up", "Chin-up", "Lat Pulldown", "Barbell Row", "Dumbbell Row",
            "Seated Cable Row", "T-Bar Row", "Face Pull", "Straight-arm Pulldown", "Back Extension", "Shrug") +
        e("Shoulders", "Overhead Press (Barbell)", "Shoulder Press (Dumbbell)", "Arnold Press",
            "Lateral Raise", "Front Raise", "Rear Delt Fly", "Upright Row", "Shoulder Press (Machine)") +
        e("Biceps", "Barbell Curl", "Dumbbell Curl", "Hammer Curl", "Preacher Curl", "Cable Curl",
            "Incline Dumbbell Curl", "Concentration Curl") +
        e("Triceps", "Tricep Pushdown", "Overhead Tricep Extension", "Skull Crusher", "Close-grip Bench Press",
            "Tricep Dip", "Tricep Kickback") +
        e("Legs", "Squat (Barbell)", "Front Squat", "Leg Press", "Hack Squat", "Romanian Deadlift",
            "Lunge", "Bulgarian Split Squat", "Leg Extension", "Leg Curl", "Goblet Squat",
            "Standing Calf Raise", "Seated Calf Raise") +
        e("Glutes", "Hip Thrust", "Glute Bridge", "Cable Kickback", "Hip Abduction (Machine)") +
        e("Core", "Plank", "Crunch", "Hanging Leg Raise", "Cable Crunch", "Russian Twist", "Ab Wheel Rollout",
            "Sit-up") +
        e("Cardio", "Treadmill Run", "Treadmill Walk (Incline)", "Cycling", "Elliptical", "Rowing Machine",
            "Jump Rope", "Stair Climber") +
        e("Full body", "Clean and Press", "Kettlebell Swing", "Burpee", "Farmer's Walk", "Thruster")
}
