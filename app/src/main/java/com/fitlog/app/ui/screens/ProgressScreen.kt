@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.fitlog.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.data.Units
import com.fitlog.app.ui.AppViewModel
import com.fitlog.app.ui.e1rm
import com.fitlog.app.ui.components.BarChart
import com.fitlog.app.ui.components.LineChart
import com.fitlog.app.ui.components.NumberDialog
import com.fitlog.app.ui.components.SectionCard
import com.fitlog.app.ui.components.StatTile
import com.fitlog.app.ui.components.fmt1
import com.fitlog.app.ui.theme.ProteinColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private val shortFmt = DateTimeFormatter.ofPattern("d MMM")
private fun String.short(): String = runCatching { LocalDate.parse(this).format(shortFmt) }.getOrDefault(this)

val measurementTypes = listOf("Waist", "Chest", "Arms", "Hips", "Thighs", "Neck", "Body fat %")

@Composable
fun ProgressScreen(vm: AppViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Text("Progress", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp))
        TabRow(selectedTabIndex = tab) {
            listOf("Body", "Nutrition", "Strength").forEachIndexed { i, l ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(l) })
            }
        }
        when (tab) {
            0 -> BodyTab(vm)
            1 -> NutritionTab(vm)
            else -> StrengthTab(vm)
        }
    }
}

@Composable
private fun BodyTab(vm: AppViewModel) {
    val weights by vm.weights.collectAsStateWithLifecycle()
    val measurements by vm.measurements.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val lb = profile.useLb
    var weightDialog by remember { mutableStateOf(false) }
    var measureType by remember { mutableStateOf<String?>(null) }
    var showAll by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard(title = "Body weight", action = {
                FilledTonalButton(onClick = { weightDialog = true }) { Text("Log") }
            }) {
                val latest = weights.lastOrNull()
                val monthAgo = LocalDate.now().minusDays(30).toString()
                val base = weights.firstOrNull { it.date >= monthAgo }
                Row(Modifier.fillMaxWidth()) {
                    StatTile("Current", latest?.let { "${Units.fmt(Units.toDisplay(it.kg, lb))} ${Units.label(lb)}" } ?: "—",
                        Modifier.weight(1f))
                    val change = if (latest != null && base != null) latest.kg - base.kg else null
                    StatTile("30-day change",
                        change?.let { (if (it > 0) "+" else "") + fmt1(Units.toDisplay(it, lb).toFloat()) + " " + Units.label(lb) } ?: "—",
                        Modifier.weight(1f))
                    val h = profile.heightCm / 100
                    StatTile("BMI", latest?.let { fmt1((it.kg / (h * h)).toFloat()) } ?: "—", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                LineChart(
                    weights.takeLast(60).map { it.date.short() to Units.toDisplay(it.kg, lb).toFloat() },
                    unit = " ${Units.label(lb)}",
                )
                if (weights.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    val shown = weights.reversed().let { if (showAll) it else it.take(5) }
                    shown.forEach { w ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(w.date.short(), Modifier.weight(1f))
                            Text("${Units.fmt(Units.toDisplay(w.kg, lb))} ${Units.label(lb)}", fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { vm.deleteWeight(w) }) { Icon(Icons.Filled.Close, "Delete") }
                        }
                    }
                    if (weights.size > 5) {
                        TextButton(onClick = { showAll = !showAll }) { Text(if (showAll) "Show less" else "Show all (${weights.size})") }
                    }
                }
            }
        }
        item {
            SectionCard(title = "Measurements") {
                Text("Tap a measurement to log it (cm, body fat in %).", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(measurementTypes) { t -> OutlinedButton(onClick = { measureType = t }) { Text(t) } }
                }
                Spacer(Modifier.height(8.dp))
                measurementTypes.forEach { type ->
                    val list = measurements.filter { it.type == type }  // newest first
                    if (list.isNotEmpty()) {
                        val latest = list.first()
                        val first = list.last()
                        val diff = latest.value - first.value
                        val unit = if (type == "Body fat %") "%" else " cm"
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(type, style = MaterialTheme.typography.bodyLarge)
                                Text("${list.size} entries · last ${latest.date.short()}", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${fmt1(latest.value.toFloat())}$unit", fontWeight = FontWeight.SemiBold)
                                if (list.size > 1) {
                                    Text((if (diff > 0) "+" else "") + fmt1(diff.toFloat()) + unit + " total",
                                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { vm.deleteMeasurement(latest) }) { Icon(Icons.Filled.Close, "Delete latest") }
                        }
                    }
                }
            }
        }
    }

    if (weightDialog) {
        NumberDialog("Log weight (today)", weights.lastOrNull()?.let { Units.fmt(Units.toDisplay(it.kg, lb)) } ?: "",
            "Weight (${Units.label(lb)})", onDismiss = { weightDialog = false }) {
            vm.logWeight(LocalDate.now(), Units.fromDisplay(it, lb))
        }
    }
    measureType?.let { t ->
        NumberDialog("Log $t", "", if (t == "Body fat %") "Percent" else "Centimetres", onDismiss = { measureType = null }) {
            vm.addMeasurement(t, it)
        }
    }
}

@Composable
private fun NutritionTab(vm: AppViewModel) {
    val last30 by vm.last30.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val daily by vm.dailyLog.collectAsStateWithLifecycle()
    val t = profile.targets()
    val byDate = last30.associateBy { it.date }
    val days7 = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
    val logged7 = days7.mapNotNull { byDate[it.toString()] }
    val logged30 = last30.filter { it.kcal > 0 }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard(title = "Calories – last 7 days") {
                BarChart(
                    values = days7.map { (byDate[it.toString()]?.kcal ?: 0.0).toFloat() },
                    labels = days7.map { it.dayOfWeek.name.take(2).lowercase().replaceFirstChar(Char::uppercase) },
                    target = t.kcal.toFloat(),
                )
                Spacer(Modifier.height(8.dp))
                Text("Dashed line = your target (${t.kcal} kcal). Orange bars are >5% over.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionCard(title = "Protein – last 7 days") {
                BarChart(
                    values = days7.map { (byDate[it.toString()]?.protein ?: 0.0).toFloat() },
                    labels = days7.map { it.dayOfWeek.name.take(2).lowercase().replaceFirstChar(Char::uppercase) },
                    target = null,
                    color = ProteinColor,
                )
                Spacer(Modifier.height(8.dp))
                Text("Target: ${t.protein} g/day", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionCard(title = "Averages (days with food logged)") {
                Row(Modifier.fillMaxWidth()) {
                    StatTile("7-day kcal", if (logged7.isEmpty()) "—" else "${logged7.map { it.kcal }.average().roundToInt()}", Modifier.weight(1f))
                    StatTile("7-day protein", if (logged7.isEmpty()) "—" else "${logged7.map { it.protein }.average().roundToInt()} g", Modifier.weight(1f))
                    StatTile("30-day kcal", if (logged30.isEmpty()) "—" else "${logged30.map { it.kcal }.average().roundToInt()}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Text("Logged ${logged30.size} of the last 30 days. Water today: ${daily.waterMl} ml.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionCard(title = "How your targets are set") {
                Text(
                    "BMR ${t.bmr} kcal (Mifflin-St Jeor) × activity = maintenance ≈ ${t.tdee} kcal. " +
                        "Goal \"${profile.goal.label}\" gives ${t.kcal} kcal. Protein ${profile.goal.proteinPerKg} g per kg bodyweight, " +
                        "fat 25% of calories, carbs the rest. If your weight doesn't move as expected after 2–3 weeks, " +
                        "adjust by 100–200 kcal in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun StrengthTab(vm: AppViewModel) {
    val history by vm.history.collectAsStateWithLifecycle()
    val records by vm.records.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val lb = profile.useLb
    val trained = remember(history) { history.flatMap { it.exercises }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key } }
    var selected by remember { mutableStateOf<String?>(null) }
    val ex = selected ?: trained.firstOrNull()
    var menu by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            // Workouts per week, last 8 weeks
            val thisMonday = LocalDate.now().with(DayOfWeek.MONDAY)
            val weeks = (7 downTo 0).map { thisMonday.minusWeeks(it.toLong()) }
            val counts = weeks.map { start -> history.count { !it.date.isBefore(start) && it.date.isBefore(start.plusWeeks(1)) }.toFloat() }
            SectionCard(title = "Workouts per week") {
                BarChart(counts, weeks.map { it.format(DateTimeFormatter.ofPattern("d/M")) }, target = null)
                Spacer(Modifier.height(6.dp))
                Text("Total workouts logged: ${history.size}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionCard(title = "Exercise progress") {
                if (ex == null) {
                    Text("Finish a workout to see strength trends.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Box {
                        OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(ex, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Filled.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            trained.forEach { name ->
                                DropdownMenuItem(text = { Text(name) }, onClick = { selected = name; menu = false })
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Best estimated 1RM per session, oldest first.
                    val points = history.reversed().mapNotNull { s ->
                        val best = s.sets.filter { it.exercise == ex && it.reps > 0 }.maxOfOrNull { e1rm(it.weightKg, it.reps) }
                        best?.let { s.date.format(shortFmt) to Units.toDisplay(it, lb).toFloat() }
                    }.takeLast(30)
                    Text("Estimated 1-rep max (Epley) per session", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LineChart(points, unit = " ${Units.label(lb)}")
                    val last = history.firstOrNull { s -> s.sets.any { it.exercise == ex } }
                    if (last != null) {
                        val daysAgo = ChronoUnit.DAYS.between(last.date, LocalDate.now())
                        Text("Last trained ${if (daysAgo == 0L) "today" else "$daysAgo days ago"}: " +
                            last.sets.filter { it.exercise == ex }.joinToString(", ") {
                                "${Units.fmt(Units.toDisplay(it.weightKg, lb))}×${it.reps}"
                            }, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            SectionCard(title = "Personal records") {
                if (records.isEmpty()) {
                    Text("Your best lifts will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                records.forEach { r ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.exercise, style = MaterialTheme.typography.bodyLarge)
                            Text("${r.date.format(shortFmt)} · est. 1RM ${Units.fmt(Math.round(Units.toDisplay(r.best1rm, lb) * 10) / 10.0)} ${Units.label(lb)}",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${Units.fmt(Units.toDisplay(r.bestKg, lb))} ${Units.label(lb)} × ${r.bestReps}", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
