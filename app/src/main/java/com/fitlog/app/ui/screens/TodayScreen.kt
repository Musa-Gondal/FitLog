@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.fitlog.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitlog.app.data.Units
import com.fitlog.app.goTab
import com.fitlog.app.ui.AppViewModel
import com.fitlog.app.ui.components.DateSwitcher
import com.fitlog.app.ui.components.MacroBar
import com.fitlog.app.ui.components.NumberDialog
import com.fitlog.app.ui.components.Ring
import com.fitlog.app.ui.components.SectionCard
import com.fitlog.app.ui.components.StatTile
import com.fitlog.app.ui.theme.CarbColor
import com.fitlog.app.ui.theme.FatColor
import com.fitlog.app.ui.theme.ProteinColor
import com.fitlog.app.ui.theme.WaterColor
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun TodayScreen(vm: AppViewModel, nav: NavHostController) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val date by vm.date.collectAsStateWithLifecycle()
    val entries by vm.foodEntries.collectAsStateWithLifecycle()
    val daily by vm.dailyLog.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val active by vm.active.collectAsStateWithLifecycle()
    val weights by vm.weights.collectAsStateWithLifecycle()
    val foodDates by vm.loggedFoodDates.collectAsStateWithLifecycle()

    val t = profile.targets()
    val kcal = entries.sumOf { it.kcal }
    val protein = entries.sumOf { it.protein }
    val carbs = entries.sumOf { it.carbs }
    val fat = entries.sumOf { it.fat }
    val lb = profile.useLb

    var stepsDialog by remember { mutableStateOf(false) }
    var weightDialog by remember { mutableStateOf(false) }

    val workoutsToday = history.filter { it.date == date }
    val streak = remember(foodDates, history) {
        val days = foodDates.toSet() + history.map { it.date.toString() }
        var d = LocalDate.now()
        if (d.toString() !in days) d = d.minusDays(1)
        var n = 0
        while (d.toString() in days) { n++; d = d.minusDays(1) }
        n
    }
    val weekStart = LocalDate.now().minusDays(6)
    val workoutsThisWeek = history.count { !it.date.isBefore(weekStart) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        val hour = LocalTime.now().hour
        val greet = when (hour) { in 4..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }
        Text(if (profile.name.isNotBlank()) "$greet, ${profile.name}" else greet,
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        DateSwitcher(date) { vm.shiftDate(it) }

        // Calories
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ring(progress = (kcal / t.kcal).toFloat(), color = MaterialTheme.colorScheme.primary) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val left = t.kcal - kcal.toInt()
                        Text("${kotlin.math.abs(left)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(if (left >= 0) "kcal left" else "kcal over", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    StatTile("Eaten", "${kcal.toInt()}")
                    StatTile("Target", "${t.kcal}")
                }
            }
            Spacer(Modifier.height(8.dp))
            MacroBar("Protein", protein, t.protein, ProteinColor)
            MacroBar("Carbs", carbs, t.carbs, CarbColor)
            MacroBar("Fat", fat, t.fat, FatColor)
            Spacer(Modifier.height(4.dp))
            FilledTonalButton(onClick = { nav.goTab("food") }, modifier = Modifier.fillMaxWidth()) { Text("Log food") }
        }

        // Water
        SectionCard(title = "Water", action = {
            Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = WaterColor)
        }) {
            MacroBar("${daily.waterMl / 250} glasses", daily.waterMl.toDouble(), profile.waterGoalMl, WaterColor, "ml")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.addWater(-250) }) { Text("−250") }
                Button(onClick = { vm.addWater(250) }) { Text("+ Glass") }
                Button(onClick = { vm.addWater(500) }) { Text("+ 500 ml") }
            }
        }

        // Steps + weight
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard(Modifier.weight(1f), title = "Steps", action = {
                Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null)
            }) {
                Text("${daily.steps}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("goal ${profile.stepGoal}", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { stepsDialog = true }) { Text("Update") }
            }
            SectionCard(Modifier.weight(1f), title = "Weight", action = {
                Icon(Icons.Filled.MonitorWeight, contentDescription = null)
            }) {
                val latest = weights.lastOrNull()
                Text(latest?.let { "${Units.fmt(Units.toDisplay(it.kg, lb))} ${Units.label(lb)}" } ?: "—",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(latest?.let { if (it.date == LocalDate.now().toString()) "logged today" else "last: ${it.date}" }
                    ?: "not logged yet", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { weightDialog = true }) { Text("Log") }
            }
        }

        // Training
        SectionCard(title = "Training", action = {
            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }) {
            Row(Modifier.fillMaxWidth()) {
                StatTile("Day streak", "$streak", Modifier.weight(1f))
                StatTile("Workouts (7 days)", "$workoutsThisWeek", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            when {
                active != null -> Button(onClick = { nav.navigate("active") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue: ${active?.name}")
                }
                workoutsToday.isNotEmpty() -> {
                    workoutsToday.forEach {
                        Text("✓ ${it.workout.name} — ${it.sets.size} sets, ${it.exercises.size} exercises",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                else -> FilledTonalButton(onClick = { nav.goTab("workout") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Start a workout")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (stepsDialog) {
        NumberDialog("Steps for ${date}", if (daily.steps > 0) daily.steps.toString() else "", "Steps",
            onDismiss = { stepsDialog = false }, decimal = false) { vm.setSteps(it.toInt()) }
    }
    if (weightDialog) {
        NumberDialog("Today's weight", weights.lastOrNull()?.let { Units.fmt(Units.toDisplay(it.kg, lb)) } ?: "",
            "Weight (${Units.label(lb)})", onDismiss = { weightDialog = false }) {
            vm.logWeight(LocalDate.now(), Units.fromDisplay(it, lb))
        }
    }
}
