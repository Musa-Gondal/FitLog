@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.fitlog.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitlog.app.data.Units
import com.fitlog.app.goTab
import com.fitlog.app.ui.AppViewModel
import com.fitlog.app.ui.components.DateSwitcher
import com.fitlog.app.ui.components.HeroCard
import com.fitlog.app.ui.components.InsightRow
import com.fitlog.app.ui.components.MacroBar
import com.fitlog.app.ui.components.MiniRing
import com.fitlog.app.ui.components.NumberDialog
import com.fitlog.app.ui.components.Ring
import com.fitlog.app.ui.components.SectionCard
import com.fitlog.app.ui.components.StatTile
import com.fitlog.app.ui.components.Tag
import com.fitlog.app.ui.theme.CarbColor
import com.fitlog.app.ui.theme.FatColor
import com.fitlog.app.ui.theme.ProteinColor
import com.fitlog.app.ui.theme.WarnColor
import com.fitlog.app.ui.theme.WaterColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun TodayScreen(vm: AppViewModel, nav: NavHostController) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val date by vm.date.collectAsStateWithLifecycle()
    val entries by vm.foodEntries.collectAsStateWithLifecycle()
    val daily by vm.dailyLog.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val active by vm.active.collectAsStateWithLifecycle()
    val weights by vm.weights.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val insights by vm.insights.collectAsStateWithLifecycle()
    val checkInDue by vm.checkInDue.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    val t = profile.targets()
    val kcal = entries.sumOf { it.kcal }
    val protein = entries.sumOf { it.protein }
    val carbs = entries.sumOf { it.carbs }
    val fat = entries.sumOf { it.fat }
    val lb = profile.useLb

    var stepsDialog by remember { mutableStateOf(false) }
    var weightDialog by remember { mutableStateOf(false) }

    val workoutsToday = history.filter { it.date == date }
    val weekStart = LocalDate.now().minusDays(6)
    val workoutsThisWeek = history.count { !it.date.isBefore(weekStart) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val hour = LocalTime.now().hour
                val greet = when (hour) { in 4..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }
                Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")).uppercase(),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (profile.name.isNotBlank()) "$greet, ${profile.name}" else greet,
                    style = MaterialTheme.typography.headlineSmall)
            }
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalFireDepartment, null, tint = if (streak > 0) WarnColor else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("$streak", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        if (checkInDue) {
            Surface(
                shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth().clickable { vm.progressTab.value = 0; nav.goTab("progress") },
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EventAvailable, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Weekly check-in ready", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Review your week and update your targets", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Text("Open", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                }
            }
        }

        DateSwitcher(date) { vm.shiftDate(it) }

        // Hero: calories + macros
        HeroCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(progress = (kcal / t.kcal).toFloat(), color = MaterialTheme.colorScheme.primary, size = 142.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val left = t.kcal - kcal.toInt()
                            Text("${abs(left)}", style = MaterialTheme.typography.headlineMedium)
                            Text(if (left >= 0) "kcal left" else "kcal over", style = MaterialTheme.typography.labelMedium,
                                color = if (left >= 0) MaterialTheme.colorScheme.onSurfaceVariant else WarnColor)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatTile("Eaten", "${kcal.toInt()}")
                        StatTile("Target", "${t.kcal}", sub = if (t.adaptive) "adaptive" else null)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MiniRing("Protein", protein, t.protein, ProteinColor)
                    MiniRing("Carbs", carbs, t.carbs, CarbColor)
                    MiniRing("Fat", fat, t.fat, FatColor)
                }
            }
        }

        // Quick actions
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction(Icons.Filled.Restaurant, "Food", Modifier.weight(1f)) { nav.goTab("food") }
            QuickAction(Icons.Filled.LocalDrink, "+ Glass", Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.addWater(250)
            }
            QuickAction(Icons.Filled.FitnessCenter, if (active != null) "Resume" else "Train", Modifier.weight(1f)) {
                if (active != null) nav.navigate("active") else nav.goTab("workout")
            }
            QuickAction(Icons.Filled.MonitorWeight, "Weigh", Modifier.weight(1f)) { weightDialog = true }
        }

        // Coach
        if (insights.isNotEmpty()) {
            SectionCard(title = "Coach", action = {
                TextButton(onClick = { vm.progressTab.value = 0; nav.goTab("progress") }) { Text("See all") }
            }) {
                insights.take(3).forEach { InsightRow(it) }
            }
        }

        // Water
        SectionCard(title = "Water", action = { Text("${daily.waterMl} / ${profile.waterGoalMl} ml", style = MaterialTheme.typography.labelLarge) }) {
            val glasses = (profile.waterGoalMl / 250).coerceIn(4, 16)
            val filled = daily.waterMl / 250
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (0 until glasses).forEach { i ->
                    Icon(
                        if (i < filled) Icons.Filled.WaterDrop else Icons.Outlined.WaterDrop, null,
                        tint = if (i < filled) WaterColor else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(22.dp).clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.addWater((i + 1) * 250 - daily.waterMl)
                        },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.addWater(-250) }) { Text("−250") }
                Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.addWater(250) }) { Text("+ Glass") }
                FilledTonalButton(onClick = { vm.addWater(500) }) { Text("+ 500 ml") }
            }
        }

        // Steps + weight
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard(Modifier.weight(1f), title = "Steps", action = {
                Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null)
            }) {
                Text("${daily.steps}", style = MaterialTheme.typography.headlineSmall)
                MacroBar("", daily.steps.toDouble(), profile.stepGoal, MaterialTheme.colorScheme.secondary, "")
                TextButton(onClick = { stepsDialog = true }) { Text("Update") }
            }
            SectionCard(Modifier.weight(1f), title = "Weight", action = {
                Icon(Icons.Filled.MonitorWeight, contentDescription = null)
            }) {
                val latest = weights.lastOrNull()
                Text(latest?.let { Units.show(it.kg, lb) } ?: "—", style = MaterialTheme.typography.headlineSmall)
                val trend = remember(weights) { com.fitlog.app.data.Coach.weightTrend(weights).lastOrNull() }
                Text(trend?.let { "trend ${Units.show(it.trend, lb)}" } ?: "not logged yet",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (latest != null && latest.date == LocalDate.now().toString()) Tag("logged today", MaterialTheme.colorScheme.primary)
                TextButton(onClick = { weightDialog = true }) { Text("Log") }
            }
        }

        // Training
        SectionCard(title = "Training") {
            Row(Modifier.fillMaxWidth()) {
                StatTile("Workouts (7 days)", "$workoutsThisWeek", Modifier.weight(1f))
                StatTile("Day streak", "$streak", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            when {
                active != null -> Button(onClick = { nav.navigate("active") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue: ${active?.name}")
                }
                workoutsToday.isNotEmpty() -> workoutsToday.forEach {
                    Text("✓ ${it.workout.name} — ${it.working.size} sets, ${it.exercises.size} exercises",
                        style = MaterialTheme.typography.bodyMedium)
                }
                else -> FilledTonalButton(onClick = { nav.goTab("workout") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Start a workout")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (stepsDialog) {
        NumberDialog("Steps", if (daily.steps > 0) daily.steps.toString() else "", "Steps",
            onDismiss = { stepsDialog = false }, decimal = false) { vm.setSteps(it.toInt()) }
    }
    if (weightDialog) {
        NumberDialog("Today's weight", weights.lastOrNull()?.let { Units.fmt(Units.toDisplay(it.kg, lb)) } ?: "",
            "Weight (${Units.label(lb)})", onDismiss = { weightDialog = false }) {
            vm.logWeight(LocalDate.now(), Units.fromDisplay(it, lb))
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}
