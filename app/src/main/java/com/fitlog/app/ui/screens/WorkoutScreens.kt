@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.fitlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitlog.app.data.ExerciseLibrary
import com.fitlog.app.data.Routine
import com.fitlog.app.data.Units
import com.fitlog.app.ui.ActiveSet
import com.fitlog.app.ui.AppViewModel
import com.fitlog.app.ui.WorkoutSummary
import com.fitlog.app.ui.components.ConfirmDialog
import com.fitlog.app.ui.components.ScreenScaffold
import com.fitlog.app.ui.components.SectionCard
import com.fitlog.app.ui.components.StatTile
import com.fitlog.app.ui.components.pretty
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
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
    return if (v >= 10000) "${(v / 1000).roundToInt()}k ${Units.label(lb)}" else "${v.roundToInt()} ${Units.label(lb)}"
}

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
            Text("Workout", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            val a = active
            if (a != null) {
                SectionCard(title = "In progress") {
                    Text(a.name, style = MaterialTheme.typography.titleMedium)
                    Text("${a.exercises.size} exercises · started ${formatDuration(System.currentTimeMillis() - a.startMillis)} ago",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { nav.navigate("active") }, modifier = Modifier.fillMaxWidth()) { Text("Resume workout") }
                }
            } else {
                Button(onClick = { start("Workout", emptyList()) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start empty workout")
                }
            }
        }
        item {
            // This week at a glance
            val monday = LocalDate.now().with(DayOfWeek.MONDAY)
            val week = history.filter { !it.date.isBefore(monday) }
            SectionCard(title = "This week") {
                Row(Modifier.fillMaxWidth()) {
                    StatTile("Workouts", "${week.size}", Modifier.weight(1f))
                    StatTile("Sets", "${week.sumOf { it.sets.size }}", Modifier.weight(1f))
                    StatTile("Volume", volumeText(week.sumOf { it.volumeKg }, profile.useLb), Modifier.weight(1f))
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Routines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    vm.routineDraft.value = Routine(name = "", exercises = "")
                    nav.navigate("routine")
                }) { Icon(Icons.Filled.Add, null); Text("New") }
            }
        }
        if (routines.isEmpty()) {
            item {
                SectionCard {
                    Text("Routines are workout templates, e.g. Push / Pull / Legs. Start one and your last weights are pre-filled.",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(onClick = { vm.addStarterRoutines() }) { Text("Add starter routines (PPL + Full body)") }
                }
            }
        }
        items(routines, key = { "r${it.id}" }) { r ->
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
                FilledTonalButton(onClick = { start(r.name, r.exerciseList()) }) {
                    Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Start")
                }
            }
        }
        item {
            Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
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
                Text("${s.sets.size} sets", style = MaterialTheme.typography.labelLarge)
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
    val profile by vm.profile.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var finishDialog by remember { mutableStateOf(false) }
    var discardDialog by remember { mutableStateOf(false) }
    var renameDialog by remember { mutableStateOf(false) }
    var nothingDone by remember { mutableStateOf(false) }
    val lb = profile.useLb

    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    LaunchedEffect(active == null) { if (active == null) nav.popBackStack() }

    val w = active ?: return

    ScreenScaffold(
        title = w.name,
        onBack = { nav.popBackStack() },
        actions = {
            Text(formatDuration(now - w.startMillis), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = { renameDialog = true }) { Icon(Icons.Filled.Edit, "Rename") }
            TextButton(onClick = { finishDialog = true }) { Text("Finish", fontWeight = FontWeight.Bold) }
        },
        bottomBar = {
            if (rest > 0) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Rest", style = MaterialTheme.typography.labelMedium)
                            Text(formatDuration(rest * 1000L), style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { vm.adjustRest(-15) }) { Text("−15") }
                        Spacer(Modifier.width(6.dp))
                        OutlinedButton(onClick = { vm.adjustRest(15) }) { Text("+15") }
                        Spacer(Modifier.width(6.dp))
                        Button(onClick = { vm.stopRest() }) { Text("Skip") }
                    }
                }
            }
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (w.exercises.isEmpty()) {
                item {
                    Text("Add your first exercise to get going.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            itemsIndexed(w.exercises, key = { i, e -> "$i-${e.name}" }) { ei, ex ->
                val prev = remember(ex.name, history) { vm.previousSets(ex.name) }
                var menu by remember { mutableStateOf(false) }
                SectionCard(
                    title = ex.name,
                    action = {
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Options") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("Move up") }, leadingIcon = { Icon(Icons.Filled.KeyboardArrowUp, null) },
                                    onClick = { vm.moveExercise(ei, -1); menu = false })
                                DropdownMenuItem(text = { Text("Move down") }, leadingIcon = { Icon(Icons.Filled.KeyboardArrowDown, null) },
                                    onClick = { vm.moveExercise(ei, 1); menu = false })
                                DropdownMenuItem(text = { Text("Remove exercise") }, leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                    onClick = { vm.removeExercise(ei); menu = false })
                            }
                        }
                    },
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        HeaderCell("Set", Modifier.width(32.dp))
                        HeaderCell("Previous", Modifier.width(72.dp))
                        HeaderCell(Units.label(lb), Modifier.weight(1f))
                        HeaderCell("Reps", Modifier.weight(1f))
                        HeaderCell("✓", Modifier.width(48.dp))
                        Spacer(Modifier.width(36.dp))
                    }
                    ex.sets.forEachIndexed { si, s ->
                        val p = prev.getOrNull(si)
                        SetRow(
                            index = si, set = s,
                            previous = p?.let { "${Units.fmt(Units.toDisplay(it.weightKg, lb))}×${it.reps}" } ?: "—",
                            onChange = { vm.editSet(ei, si, it) },
                            onRemove = { vm.removeSet(ei, si) },
                        )
                    }
                    TextButton(onClick = { vm.addSet(ei) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, null); Text("Add set")
                    }
                }
            }
            item {
                Button(onClick = { nav.navigate("pick/workout") }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add exercise")
                }
            }
            item {
                TextButton(onClick = { discardDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Discard workout", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (finishDialog) {
        var notes by remember { mutableStateOf("") }
        val done = w.exercises.sumOf { e -> e.sets.count { it.done } }
        AlertDialog(
            onDismissRequest = { finishDialog = false },
            title = { Text("Finish workout?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$done completed sets will be saved. Unticked sets are ignored.")
                    OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") })
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
            text = { Text("Tick ✓ on the sets you performed (with reps entered), then finish again.") },
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
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Text(text, modifier = modifier, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SetRow(index: Int, set: ActiveSet, previous: String, onChange: (ActiveSet) -> Unit, onRemove: () -> Unit) {
    val bg = if (set.done) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).background(bg, RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${index + 1}", Modifier.width(32.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
        Text(previous, Modifier.width(72.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        OutlinedTextField(
            value = set.weight, onValueChange = { onChange(set.copy(weight = it.filter { c -> c.isDigit() || c == '.' || c == ',' })) },
            singleLine = true, modifier = Modifier.weight(1f).padding(horizontal = 3.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        OutlinedTextField(
            value = set.reps, onValueChange = { onChange(set.copy(reps = it.filter(Char::isDigit).take(3))) },
            singleLine = true, modifier = Modifier.weight(1f).padding(horizontal = 3.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Checkbox(checked = set.done, onCheckedChange = { onChange(set.copy(done = it)) }, modifier = Modifier.width(48.dp))
        IconButton(onClick = onRemove, modifier = Modifier.width(36.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Remove set", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------- Exercise picker ----------------

@Composable
fun ExercisePickerScreen(vm: AppViewModel, onBack: () -> Unit, onPicked: (List<String>) -> Unit) {
    val all by vm.allExercises.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf(listOf<String>()) }
    var createDialog by remember { mutableStateOf(false) }

    val q = query.trim().lowercase()
    val list = all.filter { (muscle == null || it.muscle == muscle) && (q.isEmpty() || it.name.lowercase().contains(q)) }

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
                placeholder = { Text("Search exercises") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LazyRow(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                item { FilterChip(selected = muscle == null, onClick = { muscle = null }, label = { Text("All") }) }
                items(ExerciseLibrary.muscles) { m ->
                    FilterChip(selected = muscle == m, onClick = { muscle = if (muscle == m) null else m }, label = { Text(m) })
                }
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                items(list) { ex ->
                    val isSel = ex.name in selected
                    ListItem(
                        headlineContent = { Text(ex.name) },
                        supportingContent = { Text(ex.muscle) },
                        trailingContent = {
                            Checkbox(checked = isSel, onCheckedChange = {
                                selected = if (isSel) selected - ex.name else selected + ex.name
                            })
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { selected = if (isSel) selected - ex.name else selected + ex.name },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    if (createDialog) {
        var name by remember { mutableStateOf(query) }
        var m by remember { mutableStateOf(muscle ?: "Chest") }
        AlertDialog(
            onDismissRequest = { createDialog = false },
            title = { Text("Custom exercise") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ExerciseLibrary.muscles) { mm ->
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
                        StatTile("Sets", "${s.sets.size}", Modifier.weight(1f))
                        StatTile("Volume", volumeText(s.volumeKg, lb), Modifier.weight(1f))
                    }
                    if (s.workout.notes.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(s.workout.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            items(s.exercises) { ex ->
                SectionCard(title = ex) {
                    s.sets.filter { it.exercise == ex }.forEachIndexed { i, set ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text("Set ${i + 1}", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${Units.fmt(Units.toDisplay(set.weightKg, lb))} ${Units.label(lb)} × ${set.reps}",
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
