@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.fitlog.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitlog.app.data.Badge
import com.fitlog.app.data.Coach
import com.fitlog.app.data.ExerciseLibrary
import com.fitlog.app.data.Routine
import com.fitlog.app.data.Units
import com.fitlog.app.data.WorkoutSet
import com.fitlog.app.ui.ActiveExercise
import com.fitlog.app.ui.ActiveSet
import com.fitlog.app.ui.AppViewModel
import com.fitlog.app.ui.FinishSummary
import com.fitlog.app.ui.WorkoutSummary
import com.fitlog.app.ui.components.ConfirmDialog
import com.fitlog.app.ui.components.PlateCalculatorDialog
import com.fitlog.app.ui.components.ScreenScaffold
import com.fitlog.app.ui.components.SectionCard
import com.fitlog.app.ui.components.StatTile
import com.fitlog.app.ui.components.Tag
import com.fitlog.app.ui.components.badgeIcon
import com.fitlog.app.ui.components.pretty
import com.fitlog.app.ui.components.tierColor
import com.fitlog.app.ui.theme.Gold
import com.fitlog.app.ui.theme.GoodColor
import com.fitlog.app.ui.theme.InfoColor
import com.fitlog.app.ui.theme.WarnColor
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

fun formatDuration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

fun volumeText(kg: Double, lb: Boolean): String {
    val v = Units.toDisplay(kg, lb)
    return if (v >= 10000) "${String.format(java.util.Locale.US, "%.1f", v / 1000)}k ${Units.label(lb)}" else "${v.roundToInt()} ${Units.label(lb)}"
}

private val supersetColors = listOf(Color(0xFF4FC3F7), Color(0xFFFFB74D), Color(0xFFBA68C8), Color(0xFF81C784), Color(0xFFF06292))
private fun supersetColor(g: Int) = supersetColors[(g - 1).mod(supersetColors.size)]
private fun supersetLetter(g: Int) = ('A' + (g - 1).mod(26)).toString()

// ---------------- Workout tab ----------------

@Composable
fun WorkoutScreen(vm: AppViewModel, nav: NavHostController) {
    val active by vm.active.collectAsStateWithLifecycle()
    val routines by vm.routines.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf<Routine?>(null) }
    var replaceWith by remember { mutableStateOf<Pair<String, List<String>>?>(null) }

    fun start(name: String, exercises: List<String>) {
        if (active != null) replaceWith = name to exercises
        else { vm.startWorkout(name, exercises); nav.navigate("active") }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Workout", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            val a = active
            if (a != null) {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text("IN PROGRESS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(a.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        val done = a.exercises.sumOf { e -> e.sets.count { it.done } }
                        val total = a.exercises.sumOf { it.sets.size }
                        Text("$done / $total sets · ${a.exercises.size} exercises", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { nav.navigate("active") }, modifier = Modifier.fillMaxWidth()) { Text("Resume workout") }
                    }
                }
            } else {
                Button(onClick = { start("Workout", emptyList()) }, modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start empty workout", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        item {
            val monday = LocalDate.now().with(DayOfWeek.MONDAY)
            val week = history.filter { !it.date.isBefore(monday) }
            SectionCard(title = "This week") {
                Row(Modifier.fillMaxWidth()) {
                    StatTile("Workouts", "${week.size}", Modifier.weight(1f))
                    StatTile("Hard sets", "${week.sumOf { it.working.size }}", Modifier.weight(1f))
                    StatTile("Volume", volumeText(week.sumOf { it.volumeKg }, profile.useLb), Modifier.weight(1f))
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Routines", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    vm.routineDraft.value = Routine(name = "", exercises = "")
                    nav.navigate("routine")
                }) { Icon(Icons.Filled.Add, null); Text("New") }
            }
        }
        if (routines.isEmpty()) {
            item {
                SectionCard {
                    Text("Routines are workout templates, e.g. Push / Pull / Legs. Start one and the coach pre-fills your target weights and reps.",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(onClick = { vm.addStarterRoutines() }) { Text("Add starter routines (PPL + Full body)") }
                }
            }
        }
        items(routines, key = { "r${it.id}" }) { r ->
            val last = history.firstOrNull { it.workout.name == r.name }
            SectionCard(
                title = r.name,
                action = {
                    IconButton(onClick = { vm.routineDraft.value = r; nav.navigate("routine") }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { confirmDelete = r }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                },
            ) {
                Text(r.exerciseList().joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = { start(r.name, r.exerciseList()) }) {
                        Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Start")
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(last?.let { "Last: ${it.date.pretty()}" } ?: "Not done yet", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Text("History", style = MaterialTheme.typography.titleMedium) }
        if (history.isEmpty()) {
            item { Text("Finished workouts appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(history.take(60), key = { "h${it.workout.id}" }) { s ->
            HistoryCard(s, profile.useLb) { nav.navigate("history/${s.workout.id}") }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    confirmDelete?.let { r ->
        ConfirmDialog("Delete routine?", "\"${r.name}\" will be removed. Your workout history is not affected.",
            onDismiss = { confirmDelete = null }) { vm.deleteRoutine(r) }
    }
    replaceWith?.let { (name, ex) ->
        ConfirmDialog("Workout already in progress", "Discard the current workout and start \"$name\"?",
            confirm = "Discard & start", onDismiss = { replaceWith = null }) {
            vm.discardWorkout(); vm.startWorkout(name, ex); nav.navigate("active")
        }
    }
}

@Composable
private fun HistoryCard(s: WorkoutSummary, lb: Boolean, onClick: () -> Unit) {
    SectionCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.workout.name, style = MaterialTheme.typography.titleMedium)
                Text("${s.date.pretty()} · ${formatDuration(s.workout.endMillis - s.workout.startMillis)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${s.working.size} sets", style = MaterialTheme.typography.labelLarge)
                Text(volumeText(s.volumeKg, lb), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(s.exercises.joinToString(" · "), style = MaterialTheme.typography.bodySmall, maxLines = 2,
            overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------- Active workout ----------------

@Composable
fun ActiveWorkoutScreen(vm: AppViewModel, nav: NavHostController) {
    val active by vm.active.collectAsStateWithLifecycle()
    val rest by vm.restRemaining.collectAsStateWithLifecycle()
    val restTotal by vm.restTotal.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var finishDialog by remember { mutableStateOf(false) }
    var discardDialog by remember { mutableStateOf(false) }
    var renameDialog by remember { mutableStateOf(false) }
    var nothingDone by remember { mutableStateOf(false) }
    var plateFor by remember { mutableStateOf<String?>(null) }
    var infoFor by remember { mutableStateOf<String?>(null) }
    var setOptions by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val lb = profile.useLb
    val ctx = LocalContext.current

    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    LaunchedEffect(active == null) { if (active == null) nav.popBackStack() }

    // Keep the screen on while training.
    DisposableEffect(profile.keepAwake) {
        val window = (ctx as? Activity)?.window
        if (profile.keepAwake) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // Ask once for notification permission (rest-timer countdown on the lock screen).
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            val prefs = ctx.getSharedPreferences("ui", Context.MODE_PRIVATE)
            val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted && !prefs.getBoolean("askedNotif", false)) {
                prefs.edit().putBoolean("askedNotif", true).apply()
                permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val w = active ?: return
    val doneSets = w.exercises.sumOf { e -> e.sets.count { it.done } }
    val totalSets = w.exercises.sumOf { it.sets.size }

    ScreenScaffold(
        title = w.name,
        onBack = { nav.popBackStack() },
        actions = {
            Text(formatDuration(now - w.startMillis), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = { plateFor = "" }) { Icon(Icons.Filled.Calculate, "Plate calculator") }
            IconButton(onClick = { renameDialog = true }) { Icon(Icons.Filled.Edit, "Rename") }
            Button(onClick = { finishDialog = true }, contentPadding = PaddingValues(horizontal = 14.dp)) { Text("Finish") }
            Spacer(Modifier.width(8.dp))
        },
        bottomBar = {
            AnimatedVisibility(visible = rest > 0, enter = expandVertically(), exit = shrinkVertically()) {
                RestBar(rest, restTotal, onMinus = { vm.adjustRest(-15) }, onPlus = { vm.adjustRest(15) }, onSkip = { vm.stopRest() })
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            LinearProgressIndicator(
                progress = { if (totalSets > 0) doneSets / totalSets.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth().height(4.dp), gapSize = 0.dp, drawStopIndicator = {},
                strokeCap = StrokeCap.Butt,
            )
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (w.exercises.isEmpty()) {
                    item {
                        SectionCard {
                            Text("Add your first exercise to get going.", style = MaterialTheme.typography.bodyLarge)
                            Text("Tip: the coach pre-fills greyed-out targets. Just tick ✓ to log a set exactly as suggested, or type to change it.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                itemsIndexed(w.exercises, key = { i, e -> "$i-${e.name}" }) { ei, ex ->
                    val prev = remember(ex.name, history) {
                        Coach.sessionsFor(ex.name, history.map { it.date to it.sets }).firstOrNull()?.sets.orEmpty()
                    }
                    val suggestion = remember(ex.name, history, lb) { vm.suggestion(ex.name) }
                    ExerciseCard(
                        vm = vm, index = ei, ex = ex, exercises = w.exercises, prev = prev, suggestion = suggestion, lb = lb,
                        onInfo = { infoFor = ex.name },
                        onPlates = { plateFor = it },
                        onSetOptions = { si -> setOptions = ei to si },
                    )
                }
                item {
                    Button(onClick = { nav.navigate("pick/workout") }, modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add exercise")
                    }
                }
                item {
                    TextButton(onClick = { discardDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Discard workout", color = MaterialTheme.colorScheme.error)
                    }
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }

    if (finishDialog) {
        var notes by remember { mutableStateOf("") }
        val undone = totalSets - doneSets
        AlertDialog(
            onDismissRequest = { finishDialog = false },
            title = { Text("Finish workout?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$doneSets completed sets will be saved." + if (undone > 0) " $undone unticked set(s) will be ignored." else "")
                    OutlinedTextField(notes, { notes = it }, label = { Text("How did it feel? (optional)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    finishDialog = false
                    if (!vm.finishWorkout(notes.trim())) nothingDone = true
                }) { Text("Finish") }
            },
            dismissButton = { TextButton(onClick = { finishDialog = false }) { Text("Keep going") } },
        )
    }
    if (nothingDone) {
        AlertDialog(
            onDismissRequest = { nothingDone = false },
            title = { Text("No completed sets") },
            text = { Text("Tick ✓ on the sets you performed, then finish again.") },
            confirmButton = { TextButton(onClick = { nothingDone = false }) { Text("OK") } },
        )
    }
    if (discardDialog) {
        ConfirmDialog("Discard workout?", "Nothing from this session will be saved.", confirm = "Discard",
            onDismiss = { discardDialog = false }) { vm.discardWorkout() }
    }
    if (renameDialog) {
        var name by remember { mutableStateOf(w.name) }
        AlertDialog(
            onDismissRequest = { renameDialog = false },
            title = { Text("Workout name") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { vm.renameWorkout(name.trim().ifBlank { "Workout" }); renameDialog = false }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameDialog = false }) { Text("Cancel") } },
        )
    }
    plateFor?.let { PlateCalculatorDialog(it, lb, profile.barKg) { plateFor = null } }
    infoFor?.let { name -> ExerciseInfoDialog(vm, name) { infoFor = null } }
    setOptions?.let { (ei, si) ->
        val s = w.exercises.getOrNull(ei)?.sets?.getOrNull(si)
        if (s == null) setOptions = null
        else SetOptionsDialog(s, onDismiss = { setOptions = null },
            onChange = { vm.editSet(ei, si, it) },
            onDelete = { vm.removeSet(ei, si); setOptions = null })
    }
}

@Composable
private fun RestBar(rest: Int, total: Int, onMinus: () -> Unit, onPlus: () -> Unit, onSkip: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column {
            LinearProgressIndicator(
                progress = { if (total > 0) rest / total.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth().height(4.dp), gapSize = 0.dp, drawStopIndicator = {}, strokeCap = StrokeCap.Butt,
            )
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("REST", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(formatDuration(rest * 1000L), style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                OutlinedButton(onClick = onMinus) { Text("−15") }
                Spacer(Modifier.width(6.dp))
                OutlinedButton(onClick = onPlus) { Text("+15") }
                Spacer(Modifier.width(6.dp))
                Button(onClick = onSkip) { Text("Skip") }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    vm: AppViewModel,
    index: Int,
    ex: ActiveExercise,
    exercises: List<ActiveExercise>,
    prev: List<WorkoutSet>,
    suggestion: Coach.Suggestion,
    lb: Boolean,
    onInfo: () -> Unit,
    onPlates: (String) -> Unit,
    onSetOptions: (Int) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(ex.note.isNotBlank()) }
    var coachOpen by remember { mutableStateOf(false) }
    val def = remember(ex.name) { vm.lookup(ex.name) }
    val ssColor = if (ex.superset != 0) supersetColor(ex.superset) else Color.Transparent
    val linkedNext = ex.superset != 0 && exercises.getOrNull(index + 1)?.superset == ex.superset
    val prevWorking = prev

    Row(Modifier.fillMaxWidth().then(if (ex.superset != 0) Modifier.height(IntrinsicSize.Min) else Modifier)) {
        if (ex.superset != 0) {
            Box(Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(ssColor))
            Spacer(Modifier.width(6.dp))
        }
        SectionCard(Modifier.weight(1f)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable(onClick = onInfo)) {
                    Text(ex.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${def.muscle} · ${def.equipment.label}" + if (def.repHigh > 0) " · ${def.repLow}–${def.repHigh} reps" else "",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (ex.superset != 0) Tag("Superset ${supersetLetter(ex.superset)}", ssColor)
                    }
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("How to do it") }, leadingIcon = { Icon(Icons.Filled.Info, null) },
                            onClick = { menu = false; onInfo() })
                        DropdownMenuItem(text = { Text("Add warm-up sets") }, leadingIcon = { Icon(Icons.Filled.Whatshot, null) },
                            onClick = { menu = false; vm.addWarmups(index) })
                        DropdownMenuItem(text = { Text("Fill coach targets") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, null) },
                            onClick = { menu = false; vm.applySuggestion(index) })
                        if (index + 1 < exercises.size) {
                            DropdownMenuItem(
                                text = { Text(if (linkedNext) "Unlink superset" else "Superset with next") },
                                leadingIcon = { Icon(if (linkedNext) Icons.Filled.LinkOff else Icons.Filled.Link, null) },
                                onClick = { menu = false; vm.toggleSuperset(index) })
                        }
                        DropdownMenuItem(text = { Text(if (showNote) "Hide note" else "Add note") }, leadingIcon = { Icon(Icons.Filled.EditNote, null) },
                            onClick = { menu = false; showNote = !showNote })
                        DropdownMenuItem(text = { Text("Plate calculator") }, leadingIcon = { Icon(Icons.Filled.Calculate, null) },
                            onClick = {
                                menu = false
                                val s = ex.sets.firstOrNull { !it.done && it.type != "W" } ?: ex.sets.lastOrNull()
                                onPlates(s?.weight?.ifBlank { s.hintWeight } ?: "")
                            })
                        DropdownMenuItem(text = { Text("Move up") }, leadingIcon = { Icon(Icons.Filled.KeyboardArrowUp, null) },
                            onClick = { vm.moveExercise(index, -1); menu = false })
                        DropdownMenuItem(text = { Text("Move down") }, leadingIcon = { Icon(Icons.Filled.KeyboardArrowDown, null) },
                            onClick = { vm.moveExercise(index, 1); menu = false })
                        DropdownMenuItem(text = { Text("Remove exercise") }, leadingIcon = { Icon(Icons.Filled.Delete, null) },
                            onClick = { vm.removeExercise(index); menu = false })
                    }
                }
            }

            // Coach hint
            if (suggestion.kind != Coach.Kind.NONE) {
                val c = when (suggestion.kind) {
                    Coach.Kind.INCREASE -> GoodColor
                    Coach.Kind.DELOAD -> WarnColor
                    else -> InfoColor
                }
                Surface(
                    onClick = { coachOpen = !coachOpen },
                    shape = RoundedCornerShape(12.dp), color = c.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = c, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(suggestion.headline, style = MaterialTheme.typography.labelLarge, color = c, modifier = Modifier.weight(1f))
                            if (suggestion.plateau) Tag("Plateau", WarnColor)
                        }
                        AnimatedVisibility(coachOpen) {
                            Column {
                                Spacer(Modifier.height(4.dp))
                                Text(suggestion.detail, style = MaterialTheme.typography.bodySmall)
                                if (suggestion.weightKg != null || suggestion.reps != null) {
                                    TextButton(onClick = { vm.applySuggestion(index); coachOpen = false },
                                        contentPadding = PaddingValues(0.dp)) { Text("Fill all sets with this") }
                                }
                            }
                        }
                    }
                }
            }

            if (showNote) {
                OutlinedTextField(
                    ex.note, { vm.setExerciseNote(index, it) }, placeholder = { Text("Note (seat 4, grip, pain…)") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), textStyle = MaterialTheme.typography.bodySmall,
                )
            }

            // Column headers
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                HeaderCell("Set", Modifier.width(36.dp))
                HeaderCell("Previous", Modifier.width(70.dp))
                HeaderCell(if (def.isTimed && def.equipment.name == "CARDIO") "km/lvl" else Units.label(lb), Modifier.weight(1f))
                HeaderCell(if (def.isTimed) (if (def.equipment.name == "CARDIO") "min" else "sec") else "Reps", Modifier.weight(1f))
                HeaderCell("✓", Modifier.width(44.dp))
            }
            var workingIdx = 0
            ex.sets.forEachIndexed { si, s ->
                val label: String
                val p: WorkoutSet?
                if (s.type == "W") { label = "W"; p = null } else {
                    workingIdx++
                    label = when (s.type) { "D" -> "D"; "F" -> "F"; else -> "$workingIdx" }
                    p = prevWorking.getOrNull(workingIdx - 1)
                }
                SetRow(
                    label = label, set = s,
                    previous = p?.let { "${Units.fmt(Math.round(Units.toDisplay(it.weightKg, lb) * 10) / 10.0)}×${it.reps}" } ?: "—",
                    onCopyPrevious = {
                        if (p != null) vm.editSet(index, si, s.copy(
                            weight = Units.fmt(Math.round(Units.toDisplay(p.weightKg, lb) * 10) / 10.0), reps = p.reps.toString()))
                    },
                    onChange = { vm.editSet(index, si, it) },
                    onLabel = { onSetOptions(si) },
                )
            }
            Row {
                TextButton(onClick = { vm.addSet(index) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Add, null); Text("Add set")
                }
                if (ex.sets.none { it.type == "W" } && def.equipment.name != "CARDIO" && def.equipment.name != "BODYWEIGHT") {
                    TextButton(onClick = { vm.addWarmups(index) }) { Icon(Icons.Filled.Whatshot, null); Text("Warm-ups") }
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Text(text, modifier = modifier, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SetRow(
    label: String,
    set: ActiveSet,
    previous: String,
    onCopyPrevious: () -> Unit,
    onChange: (ActiveSet) -> Unit,
    onLabel: () -> Unit,
) {
    val focus = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val bg = if (set.done) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent
    val labelColor = when (set.type) {
        "W" -> WarnColor
        "D" -> InfoColor
        "F" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(12.dp)).background(bg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.width(36.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onLabel).padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, fontWeight = FontWeight.Bold, color = labelColor)
            if (set.rpe.isNotBlank()) Text("@${set.rpe}", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            previous,
            Modifier.width(70.dp).clickable(onClick = onCopyPrevious).padding(vertical = 8.dp),
            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
        )
        OutlinedTextField(
            value = set.weight,
            onValueChange = { onChange(set.copy(weight = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6))) },
            placeholder = { Text(set.hintWeight, Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            singleLine = true, modifier = Modifier.weight(1f).padding(horizontal = 3.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }),
            shape = RoundedCornerShape(12.dp),
        )
        OutlinedTextField(
            value = set.reps,
            onValueChange = { onChange(set.copy(reps = it.filter(Char::isDigit).take(4))) },
            placeholder = { Text(set.hintReps, Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            singleLine = true, modifier = Modifier.weight(1f).padding(horizontal = 3.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focus.clearFocus()
                if (!set.done) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onChange(set.copy(done = true)) }
            }),
            shape = RoundedCornerShape(12.dp),
        )
        Checkbox(
            checked = set.done,
            onCheckedChange = {
                if (it) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onChange(set.copy(done = it))
            },
            modifier = Modifier.width(44.dp),
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun SetOptionsDialog(set: ActiveSet, onDismiss: () -> Unit, onChange: (ActiveSet) -> Unit, onDelete: () -> Unit) {
    val types = listOf("N" to "Normal", "W" to "Warm-up", "D" to "Drop set", "F" to "To failure")
    val rpes = listOf("6", "7", "7.5", "8", "8.5", "9", "9.5", "10")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set options") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Type", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.forEach { (k, l) -> FilterChip(selected = set.type == k, onClick = { onChange(set.copy(type = k)) }, label = { Text(l) }) }
                }
                Text("RPE – how hard was it?", style = MaterialTheme.typography.labelLarge)
                Text("10 = no more reps possible · 9 = 1 left · 8 = 2 left · 7 = 3 left",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rpes.forEach { r ->
                        FilterChip(selected = set.rpe == r, onClick = { onChange(set.copy(rpe = if (set.rpe == r) "" else r)) },
                            label = { Text(r) })
                    }
                }
                Text("Warm-ups don't count toward volume, records or progression.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDelete) { Text("Delete set", color = MaterialTheme.colorScheme.error) } },
    )
}

@Composable
fun ExerciseInfoDialog(vm: AppViewModel, name: String, onDismiss: () -> Unit) {
    val def = remember(name) { vm.lookup(name) }
    val records by vm.records.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val lb = profile.useLb
    val pr = records.firstOrNull { it.exercise == name }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(def.muscle, MaterialTheme.colorScheme.primary)
                    Tag(def.equipment.label, InfoColor)
                    Tag(if (def.compound) "Compound" else "Isolation", MaterialTheme.colorScheme.tertiary)
                }
                if (def.secondary.isNotBlank()) Text("Also works: ${def.secondary}", style = MaterialTheme.typography.bodySmall)
                if (def.cues.isNotEmpty()) {
                    Text("Form cues", style = MaterialTheme.typography.labelLarge)
                    def.cues.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                } else {
                    Text("Custom exercise – add your own cues in the exercise note.", style = MaterialTheme.typography.bodySmall)
                }
                if (def.repHigh > 0) {
                    Text("Progression", style = MaterialTheme.typography.labelLarge)
                    val inc = def.incrementKg(lb)
                    Text(
                        "Work in ${def.repLow}–${def.repHigh} reps. When every set hits ${def.repHigh}" +
                            if (inc > 0) ", add ${Units.fmt(Math.round(Units.toDisplay(inc, lb) * 10) / 10.0)} ${Units.label(lb)}." else ", make it harder (load, tempo, pause).",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (pr != null) {
                    Text("Your best", style = MaterialTheme.typography.labelLarge)
                    Text("${Units.show(pr.bestKg, lb)} × ${pr.bestReps} on ${pr.date.pretty()} · est. 1RM ${Units.show(pr.best1rm, lb)}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

// ---------------- Post-workout & badge celebration ----------------

@Composable
fun FinishSummaryDialog(s: FinishSummary, lb: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.EmojiEvents, null, tint = Gold, modifier = Modifier.size(40.dp)) },
        title = { Text("Workout complete!", textAlign = TextAlign.Center) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(s.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Row(Modifier.fillMaxWidth()) {
                    StatTile("Time", formatDuration(s.durationMs), Modifier.weight(1f))
                    StatTile("Sets", "${s.sets}", Modifier.weight(1f))
                    StatTile("Volume", volumeText(s.volumeKg, lb), Modifier.weight(1f))
                }
                if (s.prs.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Gold.copy(alpha = 0.15f)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("🏆 ${s.prs.size} new personal record${if (s.prs.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.titleSmall, color = Gold)
                            s.prs.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                } else {
                    Text("Consistency beats intensity. See you next session!", style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Nice!") } },
    )
}

@Composable
fun NewBadgeDialog(badges: List<Badge>, onDismiss: () -> Unit) {
    val first = badges.first()
    val color = tierColor(first.tier)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(36.dp)).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(badgeIcon(first.icon), null, tint = color, modifier = Modifier.size(40.dp))
            }
        },
        title = { Text(if (badges.size == 1) "Badge unlocked!" else "${badges.size} badges unlocked!", textAlign = TextAlign.Center) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                badges.take(5).forEach { b ->
                    Text(b.title, style = MaterialTheme.typography.titleMedium, color = tierColor(b.tier))
                    Text(b.description, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
                if (badges.size > 5) Text("+${badges.size - 5} more on the Progress → Streaks tab")
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Awesome") } },
    )
}

// ---------------- Exercise picker ----------------

@Composable
fun ExercisePickerScreen(vm: AppViewModel, onBack: () -> Unit, onPicked: (List<String>) -> Unit) {
    val all by vm.allExercises.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val recent = remember { vm.recentExercises() }
    var muscle by remember { mutableStateOf<String?>(if (recent.isNotEmpty()) "Recent" else null) }
    var selected by remember { mutableStateOf(listOf<String>()) }
    var createDialog by remember { mutableStateOf(false) }
    var infoFor by remember { mutableStateOf<String?>(null) }

    val q = query.trim().lowercase()
    val list = when {
        q.isNotEmpty() -> all.filter { it.name.lowercase().contains(q) || it.muscle.lowercase().contains(q) }
        muscle == "Recent" -> recent.map { n -> all.firstOrNull { it.name == n } ?: vm.lookup(n) }
        muscle != null -> all.filter { it.muscle == muscle }
        else -> all
    }

    ScreenScaffold(
        title = "Add exercises",
        onBack = onBack,
        actions = { TextButton(onClick = { createDialog = true }) { Text("Custom") } },
        floatingActionButton = {
            if (selected.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onPicked(selected) },
                    icon = { Icon(Icons.Filled.Check, null) },
                    text = { Text("Add ${selected.size}") },
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                placeholder = { Text("Search exercises or muscles") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, "Clear") } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
            )
            LazyRow(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                if (recent.isNotEmpty()) item {
                    FilterChip(selected = muscle == "Recent", onClick = { muscle = if (muscle == "Recent") null else "Recent" }, label = { Text("Recent") })
                }
                item { FilterChip(selected = muscle == null, onClick = { muscle = null }, label = { Text("All") }) }
                items(ExerciseLibrary.muscles) { m ->
                    FilterChip(selected = muscle == m, onClick = { muscle = if (muscle == m) null else m }, label = { Text(m) })
                }
            }
            if (selected.isNotEmpty()) {
                Text("Selected: ${selected.joinToString()}", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp))
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                items(list) { ex ->
                    val isSel = ex.name in selected
                    ListItem(
                        headlineContent = { Text(ex.name) },
                        supportingContent = { Text("${ex.muscle} · ${ex.equipment.label}" + if (ex.custom) " · custom" else "") },
                        leadingContent = {
                            Checkbox(checked = isSel, onCheckedChange = {
                                selected = if (isSel) selected - ex.name else selected + ex.name
                            })
                        },
                        trailingContent = {
                            IconButton(onClick = { infoFor = ex.name }) { Icon(Icons.Filled.Info, "About ${ex.name}") }
                        },
                        colors = ListItemDefaults.colors(containerColor = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent),
                        modifier = Modifier.clickable { selected = if (isSel) selected - ex.name else selected + ex.name },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    if (createDialog) {
        var name by remember { mutableStateOf(query) }
        var m by remember { mutableStateOf(muscle?.takeIf { it != "Recent" } ?: "Chest") }
        AlertDialog(
            onDismissRequest = { createDialog = false },
            title = { Text("Custom exercise") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ExerciseLibrary.muscles.forEach { mm ->
                            FilterChip(selected = m == mm, onClick = { m = mm }, label = { Text(mm) })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = name.trim()
                    if (n.isNotEmpty()) {
                        if (all.none { it.name.equals(n, ignoreCase = true) }) vm.addCustomExercise(n, m)
                        selected = selected + n
                    }
                    createDialog = false
                }, enabled = name.isNotBlank()) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { createDialog = false }) { Text("Cancel") } },
        )
    }
    infoFor?.let { ExerciseInfoDialog(vm, it) { infoFor = null } }
}

// ---------------- Routine editor ----------------

@Composable
fun RoutineEditorScreen(vm: AppViewModel, nav: NavHostController) {
    val draft by vm.routineDraft.collectAsStateWithLifecycle()
    val d = draft ?: return
    val exercises = d.exerciseList()

    fun setList(l: List<String>) { vm.routineDraft.value = d.copy(exercises = l.joinToString("\n")) }

    ScreenScaffold(
        title = if (d.id == 0L) "New routine" else "Edit routine",
        onBack = { nav.popBackStack() },
        actions = {
            TextButton(
                onClick = {
                    vm.saveRoutine(d.copy(name = d.name.trim().ifBlank { "My routine" }))
                    nav.popBackStack()
                },
                enabled = exercises.isNotEmpty(),
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                OutlinedTextField(d.name, { vm.routineDraft.value = d.copy(name = it) }, label = { Text("Routine name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            itemsIndexed(exercises) { i, name ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}. $name", Modifier.weight(1f))
                        IconButton(onClick = {
                            if (i > 0) setList(exercises.toMutableList().also { val t = it[i]; it[i] = it[i - 1]; it[i - 1] = t })
                        }) { Icon(Icons.Filled.KeyboardArrowUp, "Up") }
                        IconButton(onClick = {
                            if (i < exercises.lastIndex) setList(exercises.toMutableList().also { val t = it[i]; it[i] = it[i + 1]; it[i + 1] = t })
                        }) { Icon(Icons.Filled.KeyboardArrowDown, "Down") }
                        IconButton(onClick = { setList(exercises.filterIndexed { j, _ -> j != i }) }) {
                            Icon(Icons.Filled.Close, "Remove")
                        }
                    }
                }
            }
            item {
                Button(onClick = { nav.navigate("pick/routine") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add exercises")
                }
            }
        }
    }
}

// ---------------- Workout detail ----------------

@Composable
fun WorkoutDetailScreen(vm: AppViewModel, nav: NavHostController, id: Long) {
    val history by vm.history.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val active by vm.active.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val prEvents by vm.prEvents.collectAsStateWithLifecycle()
    val s = history.firstOrNull { it.workout.id == id }
    var confirm by remember { mutableStateOf(false) }
    val lb = profile.useLb

    ScreenScaffold(
        title = s?.workout?.name ?: "Workout",
        onBack = { nav.popBackStack() },
        actions = { if (s != null) IconButton(onClick = { confirm = true }) { Icon(Icons.Filled.Delete, "Delete") } },
    ) { pad ->
        if (s == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { Text("Workout not found") }
            return@ScreenScaffold
        }
        val prsHere = prEvents.filter { it.first == s.date }.map { it.second }.toSet()
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard {
                    Text(s.date.pretty(), style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth()) {
                        StatTile("Duration", formatDuration(s.workout.endMillis - s.workout.startMillis), Modifier.weight(1f))
                        StatTile("Hard sets", "${s.working.size}", Modifier.weight(1f))
                        StatTile("Volume", volumeText(s.volumeKg, lb), Modifier.weight(1f))
                    }
                    if (s.workout.notes.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("“${s.workout.notes}”", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            items(s.exercises) { ex ->
                SectionCard(title = ex, action = { if (ex in prsHere) Tag("PR", Gold) }) {
                    notes.firstOrNull { it.workoutId == s.workout.id && it.exercise == ex }?.let {
                        Text(it.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                    }
                    var n = 0
                    s.sets.filter { it.exercise == ex }.forEach { set ->
                        val label = if (set.setType == "W") "Warm-up" else { n++; "Set $n" + when (set.setType) { "D" -> " (drop)"; "F" -> " (failure)"; else -> "" } }
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(label, Modifier.weight(1f), color = if (set.setType == "W") WarnColor else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${Units.show(set.weightKg, lb)} × ${set.reps}" + (set.rpe?.let { "  @${Units.fmt(it)}" } ?: ""),
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            item {
                FilledTonalButton(
                    onClick = {
                        if (active == null) {
                            vm.startWorkout(s.workout.name, s.exercises)
                            nav.navigate("active")
                        }
                    },
                    enabled = active == null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (active == null) "Repeat this workout" else "Finish your current workout first") }
            }
        }
    }

    if (confirm && s != null) {
        ConfirmDialog("Delete workout?", "This removes it from history and records.", onDismiss = { confirm = false }) {
            vm.deleteWorkout(s.workout.id)
            nav.popBackStack()
        }
    }
}

@Suppress("unused")
private fun daysAgo(d: LocalDate) = ChronoUnit.DAYS.between(d, LocalDate.now())
