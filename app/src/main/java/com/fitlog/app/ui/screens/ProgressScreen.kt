@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.fitlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitlog.app.data.Coach
import com.fitlog.app.data.DayFacts
import com.fitlog.app.data.Goal
import com.fitlog.app.data.Units
import com.fitlog.app.ui.AppViewModel
import com.fitlog.app.ui.components.BadgeTile
import com.fitlog.app.ui.components.BarChart
import com.fitlog.app.ui.components.HeatMode
import com.fitlog.app.ui.components.Heatmap
import com.fitlog.app.ui.components.InsightRow
import com.fitlog.app.ui.components.LineChart
import com.fitlog.app.ui.components.NumberDialog
import com.fitlog.app.ui.components.SectionCard
import com.fitlog.app.ui.components.StatTile
import com.fitlog.app.ui.components.Tag
import com.fitlog.app.ui.components.fmt1
import com.fitlog.app.ui.components.pretty
import com.fitlog.app.ui.e1rm
import com.fitlog.app.ui.theme.GoodColor
import com.fitlog.app.ui.theme.ProteinColor
import com.fitlog.app.ui.theme.WarnColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

private val shortFmt = DateTimeFormatter.ofPattern("d MMM")
private fun String.short(): String = runCatching { LocalDate.parse(this).format(shortFmt) }.getOrDefault(this)

val measurementTypes = listOf("Waist", "Chest", "Arms", "Hips", "Thighs", "Neck", "Body fat %")

@Composable
fun ProgressScreen(vm: AppViewModel, nav: NavHostController) {
    val tab by vm.progressTab.collectAsStateWithLifecycle()
    val checkInDue by vm.checkInDue.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        Text("Progress", style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp))
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp) {
            listOf("Coach", "Body", "Nutrition", "Strength", "Streaks").forEachIndexed { i, l ->
                Tab(selected = tab == i, onClick = { vm.progressTab.value = i },
                    text = { Text(if (i == 0 && checkInDue) "$l •" else l) })
            }
        }
        when (tab) {
            0 -> CoachTab(vm, nav)
            1 -> BodyTab(vm)
            2 -> NutritionTab(vm)
            3 -> StrengthTab(vm)
            else -> StreaksTab(vm)
        }
    }
}

// ---------------- Coach ----------------

@Composable
private fun CoachTab(vm: AppViewModel, nav: NavHostController) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val week by vm.thisWeek.collectAsStateWithLifecycle()
    val prevWeek by vm.lastWeek.collectAsStateWithLifecycle()
    val tdee by vm.tdeeEstimate.collectAsStateWithLifecycle()
    val insights by vm.insights.collectAsStateWithLifecycle()
    val due by vm.checkInDue.collectAsStateWithLifecycle()
    val t = profile.targets()
    val lb = profile.useLb

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            val w = week
            SectionCard(title = "Weekly check-in", action = { if (due) Tag("Due", WarnColor) else Tag("Done", GoodColor) }) {
                if (w == null) {
                    Text("Loading…")
                } else {
                    Text("${w.start.format(shortFmt)} – ${w.end.format(shortFmt)}", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    val p = prevWeek
                    CompareRow("Avg calories", w.avgKcal?.let { "$it" } ?: "—", p?.avgKcal?.let { "$it" }, "target ${t.kcal}")
                    CompareRow("Avg protein", w.avgProtein?.let { "$it g" } ?: "—", p?.avgProtein?.let { "$it g" }, "target ${t.protein} g")
                    CompareRow("Days logged", "${w.loggedDays}/7", p?.let { "${it.loggedDays}/7" }, null)
                    CompareRow("Protein target hit", "${w.proteinDaysHit} days", p?.let { "${it.proteinDaysHit} days" }, null)
                    CompareRow("Workouts", "${w.workouts}", p?.let { "${it.workouts}" }, null)
                    CompareRow("Hard sets", "${w.hardSets}", p?.let { "${it.hardSets}" }, null)
                    CompareRow("Trend weight", w.trendChangeKg?.let { signed(it, lb) } ?: "—", p?.trendChangeKg?.let { signed(it, lb) },
                        "plan ${signed(t.weeklyChangeKg, lb)}")
                    CompareRow("Avg steps", w.stepsAvg?.let { "$it" } ?: "—", p?.stepsAvg?.let { "$it" }, null)
                    CompareRow("Water goal hit", "${w.waterDaysHit} days", p?.let { "${it.waterDaysHit} days" }, null)
                    if (w.prs.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text("🏆 PRs: ${w.prs.joinToString()}", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(10.dp))
                    val est = tdee
                    val canApply = est != null && profile.adaptiveEnabled && profile.customKcal == 0 && abs(est.estimate - t.tdee) >= 50
                    if (canApply && est != null) {
                        val newTargets = profile.copy(adaptiveTdee = est.estimate).targets()
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text("Suggested update", style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Maintenance ${t.tdee} → ${est.estimate} kcal, so your daily target becomes ${newTargets.kcal} kcal " +
                                    "(P ${newTargets.protein} g · C ${newTargets.carbs} g · F ${newTargets.fat} g).",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.completeCheckIn(true) }) { Text("Apply & finish") }
                            OutlinedButton(onClick = { vm.completeCheckIn(false) }) { Text("Keep current") }
                        }
                    } else {
                        Button(onClick = { vm.completeCheckIn(false) }, enabled = due) { Text(if (due) "Complete check-in" else "Next check-in in a few days") }
                    }
                }
            }
        }
        item {
            SectionCard(title = "Maintenance calories") {
                val est = tdee
                Row(Modifier.fillMaxWidth()) {
                    StatTile("In use", "${t.tdee}", Modifier.weight(1f), sub = if (t.adaptive) "adaptive" else "formula")
                    StatTile("Formula", "${t.formulaTdee}", Modifier.weight(1f), sub = "Mifflin-St Jeor")
                    StatTile("Your data", est?.raw?.let { "$it" } ?: "—", Modifier.weight(1f),
                        sub = est?.let { "${(it.confidence * 100).roundToInt()}% confidence" } ?: "needs data")
                }
                Spacer(Modifier.height(8.dp))
                if (est == null) {
                    Text("To learn your real maintenance, log food on 10+ days and weigh in 5+ times over ~2–4 weeks. " +
                        "Then the app compares what you ate with how your trend weight moved (1 kg ≈ 7700 kcal).",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LinearProgressIndicator(
                        progress = { est.confidence.toFloat() }, modifier = Modifier.fillMaxWidth().height(6.dp),
                        strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Last 28 days: ate ~${est.avgIntake} kcal/day on ${est.loggedDays} logged days; trend changed " +
                        "${signed(est.kgPerWeek, lb)}/week (${est.weighIns} weigh-ins). " +
                        "${est.avgIntake} − (${Units.fmt2(est.kgPerWeek / 7)} kg/day × 7700) ≈ ${est.raw} kcal." +
                        (if (est.clamped) " This looked implausible (logging gaps?), so it was limited and weighted less." else "") +
                        " Blended with the formula by confidence → ${est.estimate} kcal.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (profile.adaptiveTdee > 0) {
                        TextButton(onClick = { vm.resetAdaptive() }, contentPadding = PaddingValues(0.dp)) { Text("Reset to formula") }
                    }
                }
            }
        }
        item {
            SectionCard(title = "Your plan") {
                val dir = when (profile.goal) { Goal.LOSE -> "lose"; Goal.GAIN -> "gain"; Goal.MAINTAIN -> "maintain" }
                Text(
                    if (profile.goal == Goal.MAINTAIN) "Maintain weight at ~${t.tdee} kcal/day."
                    else "$dir ${Units.fmt2(profile.rate)} % of bodyweight per week (${signed(t.weeklyChangeKg, lb)}/week) → " +
                        "${if (t.dailyDelta > 0) "+" else ""}${t.dailyDelta} kcal/day vs maintenance.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text("Protein ${profile.goal.proteinPerKg} g/kg" +
                    (if (profile.bmi > 27) " of reference weight (BMI 25)" else "") +
                    " = ${t.protein} g · fat ≥ 25 % of calories = ${t.fat} g · carbs fill the rest = ${t.carbs} g." +
                    if (t.floored) " Calories were raised to a safe minimum (never below BMR)." else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { nav.navigate("profile") }, contentPadding = PaddingValues(0.dp)) { Text("Change goal or rate") }
            }
        }
        item {
            SectionCard(title = "Coach insights") {
                if (insights.isEmpty()) Text("Log a few days to get personalised coaching.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                insights.forEach { InsightRow(it) }
            }
        }
        item {
            val w = week
            SectionCard(title = "Hard sets per muscle (7 days)") {
                Text("Growth sweet spot ≈ 10–20 sets per muscle per week. Secondary muscles count half.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                w?.muscleSets?.forEach { (m, v) -> MuscleBar(m, v) }
            }
        }
    }
}

private fun signed(kg: Double, lb: Boolean): String {
    val v = Units.toDisplay(kg, lb)
    return (if (v > 0.005) "+" else if (v < -0.005) "−" else "") + Units.fmt2(abs(v)) + " " + Units.label(lb)
}

@Composable
private fun CompareRow(label: String, value: String, previous: String?, note: String?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (note != null) Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (previous != null) Text("last wk $previous", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MuscleBar(muscle: String, sets: Double) {
    val color = when {
        sets < 10 -> WarnColor.copy(alpha = if (sets < 1) 0.4f else 1f)
        sets <= 20 -> GoodColor
        else -> Color(0xFFE57373)
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(muscle, Modifier.width(78.dp), style = MaterialTheme.typography.bodySmall)
        Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            Box(Modifier.fillMaxWidth((sets / 25).toFloat().coerceIn(0f, 1f)).height(14.dp).clip(RoundedCornerShape(7.dp)).background(color))
        }
        Text(fmt1(sets.toFloat()), Modifier.width(36.dp), style = MaterialTheme.typography.labelMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

// ---------------- Body ----------------

@Composable
private fun BodyTab(vm: AppViewModel) {
    val weights by vm.weights.collectAsStateWithLifecycle()
    val measurements by vm.measurements.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val lb = profile.useLb
    var weightDialog by remember { mutableStateOf(false) }
    var measureType by remember { mutableStateOf<String?>(null) }
    var showAll by remember { mutableStateOf(false) }
    var range by remember { mutableStateOf(90) }
    val trend = remember(weights) { Coach.weightTrend(weights) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard(title = "Body weight", action = {
                FilledTonalButton(onClick = { weightDialog = true }) { Text("Log") }
            }) {
                val latest = trend.lastOrNull()
                val slope = Coach.trendSlopePerDay(trend, 21)
                Row(Modifier.fillMaxWidth()) {
                    StatTile("Trend", latest?.let { Units.show(it.trend, lb) } ?: "—", Modifier.weight(1f),
                        sub = latest?.let { "scale ${Units.show(it.weight, lb)}" })
                    StatTile("Rate", slope?.let { signed(it * 7, lb) + "/wk" } ?: "—", Modifier.weight(1f),
                        sub = slope?.let { "${Units.fmt2(it * 7 / profile.weightKg * 100)} %/wk" })
                    val h = profile.heightCm / 100
                    StatTile("BMI", latest?.let { fmt1((it.trend / (h * h)).toFloat()) } ?: "—", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(30 to "1M", 90 to "3M", 180 to "6M", 3650 to "All").forEach { (d, l) ->
                        FilterChip(selected = range == d, onClick = { range = d }, label = { Text(l) })
                    }
                }
                val from = LocalDate.now().minusDays(range.toLong())
                val shown = trend.filter { !it.date.isBefore(from) }
                LineChart(
                    shown.map { it.date.format(shortFmt) to Units.toDisplay(it.weight, lb).toFloat() },
                    unit = " ${Units.label(lb)}",
                    overlay = shown.map { Units.toDisplay(it.trend, lb).toFloat() },
                )
                Text("Dots = daily scale weight · line = smoothed trend (what actually matters).",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (weights.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    val list = weights.reversed().let { if (showAll) it else it.take(5) }
                    list.forEach { w ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(w.date.short(), Modifier.weight(1f))
                            Text(Units.show(w.kg, lb), fontWeight = FontWeight.SemiBold)
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
                Text("Tap a measurement to log it (cm, body fat in %). Waist is the best fat-loss indicator.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

// ---------------- Nutrition ----------------

@Composable
private fun NutritionTab(vm: AppViewModel) {
    val totals by vm.allTotals.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val t = profile.targets()
    var days by remember { mutableStateOf(7) }
    val byDate = totals.associateBy { it.date }
    val range = (days - 1 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
    val logged = range.mapNotNull { byDate[it.toString()] }.filter { it.kcal > 0 }
    val labels = range.map {
        if (days <= 7) it.dayOfWeek.name.take(2).lowercase().replaceFirstChar(Char::uppercase)
        else if (it.dayOfMonth % 7 == 1) "${it.dayOfMonth}" else ""
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(7 to "7 days", 14 to "14 days", 30 to "30 days").forEach { (d, l) ->
                    FilterChip(selected = days == d, onClick = { days = d }, label = { Text(l) })
                }
            }
        }
        item {
            SectionCard(title = "Calories") {
                BarChart(range.map { (byDate[it.toString()]?.kcal ?: 0.0).toFloat() }, labels, target = t.kcal.toFloat())
                Spacer(Modifier.height(8.dp))
                Text("Dashed line = target (${t.kcal} kcal). Orange = more than 5 % over.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionCard(title = "Protein") {
                BarChart(range.map { (byDate[it.toString()]?.protein ?: 0.0).toFloat() }, labels, target = t.protein.toFloat(), color = ProteinColor)
                Spacer(Modifier.height(8.dp))
                Text("Target: ${t.protein} g/day", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionCard(title = "Averages (logged days)") {
                Row(Modifier.fillMaxWidth()) {
                    StatTile("Calories", if (logged.isEmpty()) "—" else "${logged.map { it.kcal }.average().roundToInt()}", Modifier.weight(1f))
                    StatTile("Protein", if (logged.isEmpty()) "—" else "${logged.map { it.protein }.average().roundToInt()} g", Modifier.weight(1f))
                    StatTile("Carbs", if (logged.isEmpty()) "—" else "${logged.map { it.carbs }.average().roundToInt()} g", Modifier.weight(1f))
                    StatTile("Fat", if (logged.isEmpty()) "—" else "${logged.map { it.fat }.average().roundToInt()} g", Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                val onTarget = logged.count { abs(it.kcal - t.kcal) <= t.kcal * 0.1 }
                val pHit = logged.count { it.protein >= t.protein * 0.9 }
                Text("Logged ${logged.size} of $days days · calories on target $onTarget days · protein hit $pHit days.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (logged.isNotEmpty()) {
                    val kc = logged.sumOf { it.kcal }.coerceAtLeast(1.0)
                    val pP = logged.sumOf { it.protein } * 4 / kc * 100
                    val cP = logged.sumOf { it.carbs } * 4 / kc * 100
                    val fP = logged.sumOf { it.fat } * 9 / kc * 100
                    Text("Calorie split: protein ${pP.roundToInt()} % · carbs ${cP.roundToInt()} % · fat ${fP.roundToInt()} %",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ---------------- Strength ----------------

@Composable
private fun StrengthTab(vm: AppViewModel) {
    val history by vm.history.collectAsStateWithLifecycle()
    val records by vm.records.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val lb = profile.useLb
    val trained = remember(history) {
        history.flatMap { it.exercises }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
    }
    var selected by remember { mutableStateOf<String?>(null) }
    val ex = selected ?: trained.firstOrNull()
    var menu by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            val thisMonday = LocalDate.now().with(DayOfWeek.MONDAY)
            val weeks = (7 downTo 0).map { thisMonday.minusWeeks(it.toLong()) }
            val counts = weeks.map { start -> history.count { !it.date.isBefore(start) && it.date.isBefore(start.plusWeeks(1)) }.toFloat() }
            val vols = weeks.map { start ->
                history.filter { !it.date.isBefore(start) && it.date.isBefore(start.plusWeeks(1)) }.sumOf { it.volumeKg }
            }
            SectionCard(title = "Workouts per week") {
                BarChart(counts, weeks.map { it.format(DateTimeFormatter.ofPattern("d/M")) }, target = null)
                Spacer(Modifier.height(6.dp))
                Text("Volume this week ${volumeText(vols.last(), lb)} · last week ${volumeText(vols[vols.size - 2], lb)}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    val sessions = Coach.sessionsFor(ex, history.map { it.date to it.sets }).reversed().takeLast(30)
                    val points = sessions.map { it.date.format(shortFmt) to Units.toDisplay(it.best1rm, lb).toFloat() }
                    Text("Estimated 1-rep max (Epley) per session", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LineChart(points, unit = " ${Units.label(lb)}")
                    val s = vm.suggestion(ex)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Next time: ${s.headline}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        if (s.plateau) Tag("Plateau", WarnColor)
                    }
                    Text(s.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val last = sessions.lastOrNull()
                    if (last != null) {
                        val daysAgo = ChronoUnit.DAYS.between(last.date, LocalDate.now())
                        Spacer(Modifier.height(4.dp))
                        Text("Last trained ${if (daysAgo == 0L) "today" else "$daysAgo days ago"}: " +
                            last.sets.joinToString(", ") { "${Units.fmt(Math.round(Units.toDisplay(it.weightKg, lb) * 10) / 10.0)}×${it.reps}" },
                            style = MaterialTheme.typography.bodySmall)
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
                            Text("${r.date.format(shortFmt)} · est. 1RM ${Units.show(r.best1rm, lb)}",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${Units.show(r.bestKg, lb)} × ${r.bestReps}", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Suppress("unused")
private fun unusedE1rm() = e1rm(0.0, 0)

// ---------------- Streaks & badges ----------------

@Composable
private fun StreaksTab(vm: AppViewModel) {
    val days by vm.dayFacts.collectAsStateWithLifecycle()
    val badges by vm.badges.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf(HeatMode.OVERALL) }
    var picked by remember { mutableStateOf<DayFacts?>(null) }
    var badgeInfo by remember { mutableStateOf<com.fitlog.app.data.Badge?>(null) }
    val dates = remember(badges) { vm.badgeDates() }

    val best = remember(days) {
        var b = 0; var c = 0
        days.forEach { if (it.foodLogged || it.workout) { c++; if (c > b) b = c } else c = 0 }
        b
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard {
                Row(Modifier.fillMaxWidth()) {
                    StatTile("Current streak", "$streak 🔥", Modifier.weight(1f))
                    StatTile("Best streak", "$best", Modifier.weight(1f))
                    StatTile("Badges", "${badges.count { it.unlocked }}/${badges.size}", Modifier.weight(1f))
                }
            }
        }
        item {
            SectionCard(title = "Consistency calendar") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HeatMode.entries.forEach { m -> FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.label) }) }
                }
                Spacer(Modifier.height(8.dp))
                Heatmap(days, mode) { picked = it }
                Spacer(Modifier.height(6.dp))
                val p = picked
                if (p != null) {
                    Text(p.date.pretty(), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Tag(if (p.workout) "Workout ✓" else "No workout", if (p.workout) GoodColor else MaterialTheme.colorScheme.outline)
                        Tag(if (p.kcalOnTarget) "Calories ✓" else if (p.foodLogged) "Calories off" else "Not logged",
                            if (p.kcalOnTarget) GoodColor else MaterialTheme.colorScheme.outline)
                        Tag(if (p.proteinHit) "Protein ✓" else "Protein ✗", if (p.proteinHit) GoodColor else MaterialTheme.colorScheme.outline)
                        Tag(if (p.waterHit) "Water ✓" else "Water ✗", if (p.waterHit) GoodColor else MaterialTheme.colorScheme.outline)
                        Tag(if (p.stepsHit) "Steps ✓" else "Steps ✗", if (p.stepsHit) GoodColor else MaterialTheme.colorScheme.outline)
                    }
                } else {
                    Text(
                        when (mode) {
                            HeatMode.OVERALL -> "Darker = more daily goals hit (workout, calories, protein, water, steps). Tap a day."
                            HeatMode.TRAINING -> "Darker = more training volume that day."
                            HeatMode.NUTRITION -> "Darkest = calories and protein both on target."
                        },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item { Text("Badges", style = MaterialTheme.typography.titleMedium) }
        val sorted = badges.sortedWith(compareByDescending<com.fitlog.app.data.Badge> { it.unlocked }.thenByDescending { it.progress })
        items(sorted.chunked(3)) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { b ->
                    BadgeTile(b, dates[b.id]?.short(), Modifier.weight(1f)) { badgeInfo = b }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }

    badgeInfo?.let { b ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { badgeInfo = null },
            title = { Text(b.title) },
            text = {
                Column {
                    Text(b.description)
                    Spacer(Modifier.height(8.dp))
                    Text(if (b.unlocked) "Unlocked${dates[b.id]?.let { " on ${it.short()}" } ?: ""}"
                        else "Progress: ${fmt1(b.current.toFloat())} / ${fmt1(b.target.toFloat())}",
                        style = MaterialTheme.typography.labelLarge)
                }
            },
            confirmButton = { TextButton(onClick = { badgeInfo = null }) { Text("OK") } },
        )
    }
}
