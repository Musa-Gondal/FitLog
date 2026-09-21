@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.fitlog.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitlog.app.ui.AppViewModel
import com.fitlog.app.ui.components.ConfirmDialog
import com.fitlog.app.ui.components.NumberDialog
import com.fitlog.app.ui.components.SectionCard
import java.time.LocalDate

private enum class Edit { KCAL, WATER, STEPS, REST }

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavHostController) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val t = profile.targets()
    var editing by remember { mutableStateOf<Edit?>(null) }
    var confirmImport by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    val toast: (String) -> Unit = { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show() }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportTo(uri, toast)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importFrom(uri, toast)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

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
            FilledTonalButton(onClick = { nav.navigate("profile") }) { Text("Edit profile & goal") }
        }

        SectionCard(title = "Targets") {
            SettingRow("Daily calories", if (profile.customKcal > 0) "${t.kcal} (custom)" else "${t.kcal} (auto)") { editing = Edit.KCAL }
            SettingRow("Macros", "P ${t.protein}g · C ${t.carbs}g · F ${t.fat}g", null)
            SettingRow("Water goal", "${profile.waterGoalMl} ml") { editing = Edit.WATER }
            SettingRow("Step goal", "${profile.stepGoal}") { editing = Edit.STEPS }
            SettingRow("Rest timer", "${profile.restSeconds} s") { editing = Edit.REST }
        }

        SectionCard(title = "Units") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Use pounds (lb)")
                    Text("Data is stored in kg and converted for display.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = profile.useLb, onCheckedChange = { vm.saveProfile(profile.copy(useLb = it)) })
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
            "FitLog 1.0 · Calorie values for built-in foods are typical averages; homemade food varies with oil and portion size. " +
                "This app is not medical advice.",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
    }

    when (editing) {
        Edit.KCAL -> NumberDialog("Daily calorie target", "${profile.customKcal}",
            "kcal (0 = automatic)", onDismiss = { editing = null }, decimal = false) {
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
