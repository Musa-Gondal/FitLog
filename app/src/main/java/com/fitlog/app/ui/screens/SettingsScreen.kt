@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.fitlog.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitlog.app.data.Units
import com.fitlog.app.notify.ReminderSettings
import com.fitlog.app.notify.hhmm
import com.fitlog.app.ui.AppViewModel
import com.fitlog.app.ui.components.ConfirmDialog
import com.fitlog.app.ui.components.NumberDialog
import com.fitlog.app.ui.components.SectionCard
import com.fitlog.app.ui.components.pickTime
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private enum class Edit { KCAL, WATER, STEPS, REST, BAR }

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavHostController) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val rem by vm.reminders.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val t = profile.targets()
    var editing by remember { mutableStateOf<Edit?>(null) }
    var confirmImport by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var pendingReminder by remember { mutableStateOf<ReminderSettings?>(null) }

    val toast: (String) -> Unit = { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show() }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportTo(uri, toast)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importFrom(uri, toast)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingReminder?.let { vm.saveReminders(it) }
        pendingReminder = null
        if (!granted) toast("Notifications are blocked – enable them in Android settings to get reminders.")
    }

    /** Save reminder settings, asking for notification permission the first time one is switched on. */
    fun updateReminders(s: ReminderSettings) {
        val needs = Build.VERSION.SDK_INT >= 33 && s.anyEnabled &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needs) {
            pendingReminder = s
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else vm.saveReminders(s)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        SectionCard(title = "Profile") {
            Text(
                listOf(
                    profile.name.ifBlank { "No name" }, profile.sex.label, "${profile.age} yrs",
                    "${profile.heightCm.toInt()} cm", profile.goal.label,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(profile.activity.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = { nav.navigate("profile") }) { Text("Edit profile, goal & rate") }
        }

        SectionCard(title = "Targets & coaching") {
            SettingRow("Daily calories", if (profile.customKcal > 0) "${t.kcal} (custom)" else "${t.kcal} (auto)") { editing = Edit.KCAL }
            SettingRow("Macros", "P ${t.protein}g · C ${t.carbs}g · F ${t.fat}g", null)
            SwitchRow("Adaptive calories", "Learn your real maintenance from logged food + weight trend and suggest updates at each weekly check-in.",
                profile.adaptiveEnabled) { vm.saveProfile(profile.copy(adaptiveEnabled = it)) }
            SettingRow("Water goal", "${profile.waterGoalMl} ml") { editing = Edit.WATER }
            SettingRow("Step goal", "${profile.stepGoal}") { editing = Edit.STEPS }
        }

        SectionCard(title = "Gym") {
            SettingRow("Rest timer", "${profile.restSeconds} s") { editing = Edit.REST }
            SettingRow("Barbell weight", Units.show(profile.barKg, profile.useLb)) { editing = Edit.BAR }
            SwitchRow("Keep screen on during workouts", null, profile.keepAwake) { vm.saveProfile(profile.copy(keepAwake = it)) }
            SwitchRow("Use pounds (lb)", "Data is stored in kg and converted for display.", profile.useLb) {
                vm.saveProfile(profile.copy(useLb = it))
            }
            if (Build.VERSION.SDK_INT >= 31) {
                TextButton(onClick = {
                    runCatching {
                        ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
                    }
                }) { Text("Allow precise rest-timer alarms") }
            }
        }

        SectionCard(title = "Reminders") {
            Text("Smart reminders skip themselves when you've already done the thing (e.g. no lunch reminder if lunch is logged).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SwitchRow("Meal logging", null, rem.meals) { updateReminders(rem.copy(meals = it)) }
            if (rem.meals) {
                TimeRow("Breakfast", rem.breakfast) { m -> pickTime(ctx, m) { updateReminders(rem.copy(breakfast = it)) } }
                TimeRow("Lunch", rem.lunch) { m -> pickTime(ctx, m) { updateReminders(rem.copy(lunch = it)) } }
                TimeRow("Dinner", rem.dinner) { m -> pickTime(ctx, m) { updateReminders(rem.copy(dinner = it)) } }
            }
            HorizontalDivider()
            SwitchRow("Drink water", "Includes a “+250 ml” button right in the notification.", rem.water) { updateReminders(rem.copy(water = it)) }
            if (rem.water) {
                TimeRow("From", rem.waterStart) { m -> pickTime(ctx, m) { updateReminders(rem.copy(waterStart = it)) } }
                TimeRow("Until", rem.waterEnd) { m -> pickTime(ctx, m) { updateReminders(rem.copy(waterEnd = it)) } }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(60 to "Every 1 h", 90 to "1.5 h", 120 to "2 h", 180 to "3 h").forEach { (v, l) ->
                        FilterChip(selected = rem.waterEvery == v, onClick = { updateReminders(rem.copy(waterEvery = v)) }, label = { Text(l) })
                    }
                }
            }
            HorizontalDivider()
            SwitchRow("Morning weigh-in", null, rem.weighIn) { updateReminders(rem.copy(weighIn = it)) }
            if (rem.weighIn) TimeRow("Time", rem.weighInTime) { m -> pickTime(ctx, m) { updateReminders(rem.copy(weighInTime = it)) } }
            HorizontalDivider()
            SwitchRow("Workout days", null, rem.workout) { updateReminders(rem.copy(workout = it)) }
            if (rem.workout) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.values().forEach { d ->
                        val on = d.value in rem.workoutDays
                        FilterChip(selected = on, onClick = {
                            updateReminders(rem.copy(workoutDays = if (on) rem.workoutDays - d.value else rem.workoutDays + d.value))
                        }, label = { Text(d.getDisplayName(TextStyle.SHORT, Locale.getDefault())) })
                    }
                }
                TimeRow("Time", rem.workoutTime) { m -> pickTime(ctx, m) { updateReminders(rem.copy(workoutTime = it)) } }
            }
            HorizontalDivider()
            SwitchRow("Weekly check-in", "Sunday morning summary of your week.", rem.checkIn) { updateReminders(rem.copy(checkIn = it)) }
            if (rem.checkIn) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.values().forEach { d ->
                        FilterChip(selected = rem.checkInDay == d.value, onClick = { updateReminders(rem.copy(checkInDay = d.value)) },
                            label = { Text(d.getDisplayName(TextStyle.SHORT, Locale.getDefault())) })
                    }
                }
                TimeRow("Time", rem.checkInTime) { m -> pickTime(ctx, m) { updateReminders(rem.copy(checkInTime = it)) } }
            }
        }

        SectionCard(title = "Backup") {
            Text("Everything is stored only on this phone. Export a backup regularly (e.g. to Google Drive) so you never lose your history.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { exportLauncher.launch("fitlog-backup-${LocalDate.now()}.json") }) { Text("Export") }
                OutlinedButton(onClick = { confirmImport = true }) { Text("Import") }
            }
        }

        SectionCard(title = "Danger zone") {
            TextButton(onClick = { confirmReset = true }) { Text("Erase all data", color = MaterialTheme.colorScheme.error) }
        }

        Text(
            "FitLog 1.1 · Calorie values for built-in foods are typical averages; homemade food varies with oil and portion size. " +
                "Coaching is general fitness guidance, not medical advice.",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
    }

    when (editing) {
        Edit.KCAL -> NumberDialog("Daily calorie target", "${profile.customKcal}",
            "kcal (0 = automatic / adaptive)", onDismiss = { editing = null }, decimal = false) {
            vm.saveProfile(profile.copy(customKcal = it.toInt().coerceIn(0, 10000)))
        }
        Edit.WATER -> NumberDialog("Water goal", "${profile.waterGoalMl}", "ml per day", onDismiss = { editing = null }, decimal = false) {
            vm.saveProfile(profile.copy(waterGoalMl = it.toInt().coerceIn(500, 10000)))
        }
        Edit.STEPS -> NumberDialog("Step goal", "${profile.stepGoal}", "steps per day", onDismiss = { editing = null }, decimal = false) {
            vm.saveProfile(profile.copy(stepGoal = it.toInt().coerceIn(0, 100000)))
        }
        Edit.REST -> NumberDialog("Rest timer", "${profile.restSeconds}", "seconds between sets", onDismiss = { editing = null }, decimal = false) {
            vm.saveProfile(profile.copy(restSeconds = it.toInt().coerceIn(10, 600)))
        }
        Edit.BAR -> NumberDialog("Barbell weight", Units.fmt(Units.toDisplay(profile.barKg, profile.useLb)),
            Units.label(profile.useLb), onDismiss = { editing = null }) {
            vm.saveProfile(profile.copy(barKg = Units.fromDisplay(it, profile.useLb).coerceIn(0.0, 50.0)))
        }
        null -> {}
    }
    if (confirmImport) {
        ConfirmDialog("Import backup?", "This REPLACES all current data on this phone with the backup file.", confirm = "Choose file",
            onDismiss = { confirmImport = false }) { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }
    }
    if (confirmReset) {
        ConfirmDialog("Erase everything?", "All food logs, workouts, weights and settings will be deleted. Export a backup first if unsure.",
            confirm = "Erase", onDismiss = { confirmReset = false }) { vm.resetAll { toast("All data erased") } }
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: (() -> Unit)?) {
    val mod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(mod.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f))
            Text(value, color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
    }
}

@Composable
private fun SwitchRow(label: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun TimeRow(label: String, minutes: Int, onPick: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onPick(minutes) }.padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(minutes.hhmm(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    }
}
