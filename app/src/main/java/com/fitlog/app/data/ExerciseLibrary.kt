package com.fitlog.app.data

enum class Equipment(val label: String) {
    BARBELL("Barbell"), DUMBBELL("Dumbbell"), MACHINE("Machine"), CABLE("Cable"),
    BODYWEIGHT("Bodyweight"), KETTLEBELL("Kettlebell"), CARDIO("Cardio"),
}

/**
 * Exercise metadata used for coaching.
 * - repLow..repHigh: target rep range for double progression.
 * - lower: lower-body lift (bigger jumps in load).
 */
data class ExerciseDef(
    val name: String,
    val muscle: String,
    val secondary: String = "",
    val equipment: Equipment = Equipment.MACHINE,
    val compound: Boolean = false,
    val lower: Boolean = false,
    val cues: List<String> = emptyList(),
    val custom: Boolean = false,
) {
    val repLow: Int get() = when {
        equipment == Equipment.CARDIO -> 0
        muscle == "Core" -> 10
        equipment == Equipment.BODYWEIGHT -> 8
        compound -> 6
        else -> 10
    }
    val repHigh: Int get() = when {
        equipment == Equipment.CARDIO -> 0
        muscle == "Core" -> 20
        equipment == Equipment.BODYWEIGHT -> 15
        compound -> 10
        else -> 15
    }

    /** Smallest sensible load jump, in kg (lb users get 5/10 lb equivalents). */
    fun incrementKg(lb: Boolean): Double = when (equipment) {
        Equipment.BARBELL -> if (lower) (if (lb) 10 / Units.LB_PER_KG else 5.0) else (if (lb) 5 / Units.LB_PER_KG else 2.5)
        Equipment.DUMBBELL, Equipment.KETTLEBELL -> if (lb) 5 / Units.LB_PER_KG else 2.0
        Equipment.MACHINE, Equipment.CABLE -> if (lower) (if (lb) 10 / Units.LB_PER_KG else 5.0) else (if (lb) 5 / Units.LB_PER_KG else 2.5)
        Equipment.BODYWEIGHT, Equipment.CARDIO -> 0.0
    }

    val isTimed: Boolean get() = equipment == Equipment.CARDIO || name == "Plank"
}

object ExerciseLibrary {
    val muscles = listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs", "Glutes", "Core", "Cardio", "Full body")

    private val B = Equipment.BARBELL
    private val D = Equipment.DUMBBELL
    private val M = Equipment.MACHINE
    private val C = Equipment.CABLE
    private val BW = Equipment.BODYWEIGHT
    private val KB = Equipment.KETTLEBELL
    private val CAR = Equipment.CARDIO

    private fun x(name: String, muscle: String, secondary: String, eq: Equipment, compound: Boolean, lower: Boolean, vararg cues: String) =
        ExerciseDef(name, muscle, secondary, eq, compound, lower, cues.toList())

    val items: List<ExerciseDef> = listOf(
        // Chest
        x("Bench Press (Barbell)", "Chest", "Triceps, front delts", B, true, false,
            "Shoulder blades pinched and down, slight arch, feet planted.",
            "Lower to mid-chest with elbows ~45–70° from torso; press up and slightly back.",
            "Touch the chest lightly – no bouncing."),
        x("Bench Press (Dumbbell)", "Chest", "Triceps, front delts", D, true, false,
            "Kick the dumbbells up from your knees, then set your shoulder blades.",
            "Lower until the dumbbells are level with the chest, forearms vertical.",
            "Press up in a slight arc; don't clang the weights."),
        x("Incline Bench Press (Barbell)", "Chest", "Front delts, triceps", B, true, false,
            "Bench at 30–45°. Bar touches the upper chest, below the collarbone.",
            "Keep wrists stacked over elbows; drive feet into the floor."),
        x("Incline Bench Press (Dumbbell)", "Chest", "Front delts, triceps", D, true, false,
            "Bench at 30–45°. Lower with control to upper-chest level.",
            "Keep shoulder blades pinned; press without locking out hard."),
        x("Decline Bench Press", "Chest", "Triceps", B, true, false,
            "Hook legs securely; lower to the lower chest.",
            "Use a spotter – the bar path ends over your face."),
        x("Chest Fly (Dumbbell)", "Chest", "Front delts", D, false, false,
            "Soft bend in the elbows, fixed for the whole rep.",
            "Open until you feel a stretch across the chest, then hug a big tree."),
        x("Cable Crossover", "Chest", "Front delts", C, false, false,
            "Step forward in a split stance, slight forward lean.",
            "Bring hands together in front of the lower chest; squeeze 1 second."),
        x("Pec Deck / Machine Fly", "Chest", "Front delts", M, false, false,
            "Handles at chest height; keep shoulders down and back.",
            "Control the stretch – 2–3 seconds on the way back."),
        x("Chest Press (Machine)", "Chest", "Triceps, front delts", M, true, false,
            "Seat so the handles line up with mid-chest.",
            "Keep your back on the pad; don't let shoulders roll forward."),
        x("Push-up", "Chest", "Triceps, core", BW, true, false,
            "Hands slightly wider than shoulders, body in one straight line.",
            "Chest to a fist's height from the floor; elbows ~45°."),
        x("Chest Dip", "Chest", "Triceps, front delts", BW, true, false,
            "Lean the torso forward, elbows flared slightly.",
            "Lower until shoulders are just below elbows; stop if shoulders hurt."),

        // Back
        x("Deadlift", "Back", "Glutes, hamstrings, grip", B, true, true,
            "Bar over mid-foot, shins close; hinge and grip just outside the legs.",
            "Brace hard, flat back, push the floor away; lockout with glutes, not by leaning back.",
            "Lower with the same path – keep the bar touching your legs."),
        x("Pull-up", "Back", "Biceps, rear delts", BW, true, false,
            "Overhand grip slightly wider than shoulders; start from a dead hang.",
            "Pull elbows down to your ribs until the chin clears the bar."),
        x("Chin-up", "Back", "Biceps", BW, true, false,
            "Underhand shoulder-width grip.",
            "Chest up to the bar, full stretch at the bottom."),
        x("Lat Pulldown", "Back", "Biceps, rear delts", C, true, false,
            "Thighs locked under the pad, slight lean back.",
            "Pull the bar to the upper chest by driving elbows down; no swinging."),
        x("Barbell Row", "Back", "Biceps, rear delts, lower back", B, true, false,
            "Hinge to ~45°, flat back, bar hanging at arm's length.",
            "Row to the lower ribs/belly button; squeeze shoulder blades."),
        x("Dumbbell Row", "Back", "Biceps, rear delts", D, true, false,
            "One hand and knee on the bench, back flat.",
            "Pull the dumbbell toward your hip, not your armpit."),
        x("Seated Cable Row", "Back", "Biceps, rear delts", C, true, false,
            "Sit tall, slight knee bend; don't rock the torso.",
            "Pull the handle to your stomach and squeeze the shoulder blades."),
        x("T-Bar Row", "Back", "Biceps, rear delts", B, true, false,
            "Chest up, hinge at hips, neutral spine.",
            "Pull the handle to the chest; lower under control."),
        x("Face Pull", "Shoulders", "Rear delts, upper back", C, false, false,
            "Rope at face height, thumbs pointing back.",
            "Pull toward the eyes and spread the rope; finish with elbows high."),
        x("Straight-arm Pulldown", "Back", "Lats", C, false, false,
            "Slight hinge, arms nearly straight.",
            "Sweep the bar down to the thighs using the lats."),
        x("Back Extension", "Back", "Glutes, hamstrings", BW, false, false,
            "Hips on the pad edge; hinge at the hips.",
            "Rise to a straight line – don't hyperextend."),
        x("Shrug", "Back", "Traps", D, false, false,
            "Stand tall, arms straight.",
            "Lift shoulders straight up toward the ears, pause, lower slowly."),

        // Shoulders
        x("Overhead Press (Barbell)", "Shoulders", "Triceps, upper chest", B, true, false,
            "Grip just outside shoulders, squeeze glutes and brace.",
            "Press the bar straight up; move the head back then through once it passes the face."),
        x("Shoulder Press (Dumbbell)", "Shoulders", "Triceps", D, true, false,
            "Seated or standing, dumbbells at ear level.",
            "Press up and slightly in; don't arch the lower back."),
        x("Arnold Press", "Shoulders", "Triceps", D, true, false,
            "Start palms facing you at chin height.",
            "Rotate palms forward as you press overhead."),
        x("Lateral Raise", "Shoulders", "Traps", D, false, false,
            "Slight bend in elbows, lean a touch forward.",
            "Raise to shoulder height leading with the elbows; lower slowly."),
        x("Front Raise", "Shoulders", "Upper chest", D, false, false,
            "Raise to eye level with straight-ish arms; no swinging."),
        x("Rear Delt Fly", "Shoulders", "Upper back", D, false, false,
            "Hinge forward with a flat back.",
            "Raise arms out to the sides, leading with the pinkies."),
        x("Upright Row", "Shoulders", "Traps", B, false, false,
            "Grip shoulder-width or wider.",
            "Pull elbows up to shoulder height – no higher if it pinches."),
        x("Shoulder Press (Machine)", "Shoulders", "Triceps", M, true, false,
            "Handles start at shoulder level; back against the pad.",
            "Press without locking out harshly."),

        // Biceps
        x("Barbell Curl", "Biceps", "Forearms", B, false, false,
            "Elbows pinned at your sides.",
            "Curl up without swinging; lower in 2–3 seconds."),
        x("Dumbbell Curl", "Biceps", "Forearms", D, false, false,
            "Supinate (turn palms up) as you curl.",
            "Keep upper arms still."),
        x("Hammer Curl", "Biceps", "Brachialis, forearms", D, false, false,
            "Neutral grip (thumbs up) throughout.",
            "Curl toward the shoulder; don't let elbows drift forward."),
        x("Preacher Curl", "Biceps", "", M, false, false,
            "Armpits snug on the pad.",
            "Lower almost to full extension; don't bounce at the bottom."),
        x("Cable Curl", "Biceps", "Forearms", C, false, false,
            "Constant tension – stay a step away from the stack.",
            "Squeeze at the top for a second."),
        x("Incline Dumbbell Curl", "Biceps", "", D, false, false,
            "Bench at ~60°, arms hanging straight down.",
            "Curl without moving the elbows forward."),
        x("Concentration Curl", "Biceps", "", D, false, false,
            "Elbow braced against the inner thigh.",
            "Slow, full range; squeeze at the top."),

        // Triceps
        x("Tricep Pushdown", "Triceps", "", C, false, false,
            "Elbows tucked at your sides.",
            "Push down to full lockout, control the return."),
        x("Overhead Tricep Extension", "Triceps", "", C, false, false,
            "Elbows pointing forward, close to the head.",
            "Lower behind the head for a deep stretch, extend fully."),
        x("Skull Crusher", "Triceps", "", B, false, false,
            "Lower the bar to the forehead or just behind the head.",
            "Only the forearms move; elbows stay in."),
        x("Close-grip Bench Press", "Triceps", "Chest", B, true, false,
            "Grip about shoulder-width.",
            "Elbows tucked; touch the lower chest."),
        x("Tricep Dip", "Triceps", "Chest", BW, true, false,
            "Torso upright, elbows back.",
            "Lower to ~90° elbow bend and press up."),
        x("Tricep Kickback", "Triceps", "", D, false, false,
            "Hinge forward, upper arm parallel to the floor.",
            "Extend fully and squeeze."),

        // Legs
        x("Squat (Barbell)", "Legs", "Glutes, core", B, true, true,
            "Bar on upper traps, feet shoulder-width, toes slightly out.",
            "Brace, sit down between your heels, knees track over toes.",
            "Hit at least parallel; drive up keeping chest and hips rising together."),
        x("Front Squat", "Legs", "Glutes, core", B, true, true,
            "Bar on front delts, elbows high.",
            "Stay upright; squat deep."),
        x("Leg Press", "Legs", "Glutes", M, true, true,
            "Feet mid-platform, shoulder-width.",
            "Lower until knees are ~90° or deeper without the lower back lifting."),
        x("Hack Squat", "Legs", "Glutes", M, true, true,
            "Back flat on the pad, feet slightly forward.",
            "Control the descent; drive through the whole foot."),
        x("Romanian Deadlift", "Legs", "Hamstrings, glutes, lower back", B, true, true,
            "Soft knees, push hips back with a flat back.",
            "Lower until you feel a strong hamstring stretch (usually mid-shin), then drive hips forward."),
        x("Lunge", "Legs", "Glutes", D, true, true,
            "Long step, lower the back knee toward the floor.",
            "Front knee stays over the foot; push through the front heel."),
        x("Bulgarian Split Squat", "Legs", "Glutes", D, true, true,
            "Rear foot on a bench, front foot far enough forward.",
            "Drop straight down; slight forward lean targets glutes."),
        x("Leg Extension", "Legs", "Quads", M, false, true,
            "Knee joint lined up with the machine pivot.",
            "Extend fully, squeeze 1 second, lower slowly."),
        x("Leg Curl", "Legs", "Hamstrings", M, false, true,
            "Hips pressed into the pad.",
            "Curl fully and resist on the way down."),
        x("Goblet Squat", "Legs", "Glutes, core", D, true, true,
            "Hold a dumbbell at the chest, elbows inside the knees.",
            "Sit deep with an upright torso."),
        x("Standing Calf Raise", "Legs", "Calves", M, false, true,
            "Balls of the feet on the edge, full stretch at the bottom.",
            "Pause 1–2 seconds at the top and bottom."),
        x("Seated Calf Raise", "Legs", "Calves (soleus)", M, false, true,
            "Pad on lower thighs; full range, slow reps."),

        // Glutes
        x("Hip Thrust", "Glutes", "Hamstrings", B, true, true,
            "Upper back on a bench, bar over the hips (use a pad).",
            "Drive through heels, ribs down, lock out with glutes – shins vertical at the top."),
        x("Glute Bridge", "Glutes", "Hamstrings", BW, false, true,
            "Feet close to the hips, press through heels.",
            "Squeeze glutes hard at the top."),
        x("Cable Kickback", "Glutes", "", C, false, true,
            "Hinge slightly, kick the leg back without arching the lower back."),
        x("Hip Abduction (Machine)", "Glutes", "Glute medius", M, false, true,
            "Sit tall or lean forward slightly; push knees out with control."),

        // Core
        x("Plank", "Core", "Shoulders", BW, false, false,
            "Elbows under shoulders, body in a straight line.",
            "Squeeze glutes and brace. Log seconds in the reps column."),
        x("Crunch", "Core", "", BW, false, false,
            "Curl the ribs toward the pelvis; don't pull the neck."),
        x("Hanging Leg Raise", "Core", "Hip flexors, grip", BW, false, false,
            "Dead hang, no swinging.",
            "Raise knees or legs by curling the pelvis up."),
        x("Cable Crunch", "Core", "", C, false, false,
            "Kneel, rope by the head; crunch the ribs down toward the hips."),
        x("Russian Twist", "Core", "Obliques", BW, false, false,
            "Lean back ~45°, rotate from the torso, not the arms."),
        x("Ab Wheel Rollout", "Core", "Lats", BW, false, false,
            "Start on knees, brace hard; roll out only as far as you can keep a flat back."),
        x("Sit-up", "Core", "Hip flexors", BW, false, false,
            "Controlled up and down; anchor feet if needed."),

        // Cardio (reps column = minutes, weight column = km or level)
        x("Treadmill Run", "Cardio", "", CAR, false, false, "Log minutes as reps and distance (km) as weight."),
        x("Treadmill Walk (Incline)", "Cardio", "", CAR, false, false, "Log minutes as reps, incline % as weight."),
        x("Cycling", "Cardio", "", CAR, false, false, "Log minutes as reps and distance (km) as weight."),
        x("Elliptical", "Cardio", "", CAR, false, false, "Log minutes as reps."),
        x("Rowing Machine", "Cardio", "Back, legs", CAR, false, false, "Legs, then body, then arms. Log minutes as reps."),
        x("Jump Rope", "Cardio", "Calves", CAR, false, false, "Log minutes as reps."),
        x("Stair Climber", "Cardio", "Glutes", CAR, false, false, "Log minutes as reps; don't lean on the rails."),

        // Full body
        x("Clean and Press", "Full body", "Shoulders, legs, back", B, true, false,
            "Pull from the floor, catch at the shoulders, then press overhead.",
            "Learn with light weight first."),
        x("Kettlebell Swing", "Full body", "Glutes, hamstrings", KB, true, true,
            "It's a hip hinge, not a squat – snap the hips forward.",
            "Arms are just ropes; bell floats to chest height."),
        x("Burpee", "Full body", "", BW, true, false,
            "Squat, kick back to plank, push-up optional, jump up."),
        x("Farmer's Walk", "Full body", "Grip, traps, core", D, true, false,
            "Heavy dumbbells, tall posture, short quick steps. Log metres as reps."),
        x("Thruster", "Full body", "Legs, shoulders", B, true, true,
            "Front squat straight into an overhead press in one movement."),
    )

    private val byName = items.associateBy { it.name }

    /** Lookup with a safe fallback for custom / unknown exercises. */
    fun find(name: String, customMuscle: String? = null): ExerciseDef =
        byName[name] ?: ExerciseDef(name, customMuscle ?: "Other", equipment = Equipment.MACHINE, custom = true)

    /** Main muscle groups tracked for weekly volume. */
    val volumeMuscles = listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs", "Glutes", "Core")
}
