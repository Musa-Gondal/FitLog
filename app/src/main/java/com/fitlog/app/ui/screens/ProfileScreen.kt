@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.fitlog.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fitlog.app.data.ActivityLevel
import com.fitlog.app.data.Goal
import com.fitlog.app.data.Profile
import com.fitlog.app.data.Sex
import com.fitlog.app.data.Units
import com.fitlog.app.ui.components.ScreenScaffold
import com.fitlog.app.ui.components.SectionCard
import com.fitlog.app.ui.components.StatTile

/** Used both for first-run onboarding and for editing the profile later. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileForm(initial: Profile, firstRun: Boolean, onBack: (() -> Unit)?, onSave: (Profile) -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var sex by remember { mutableStateOf(initial.sex) }
    var age by remember { mutableStateOf(initial.age.toString()) }
    var height by remember { mutableStateOf(Units.fmt(initial.heightCm)) }
    var useLb by remember { mutableStateOf(initial.useLb) }
    var weight by remember { mutableStateOf(Units.fmt(Units.toDisplay(initial.weightKg, initial.useLb))) }
    var activity by remember { mutableStateOf(initial.activity) }
    var goal by remember { mutableStateOf(initial.goal) }
    var rate by remember { mutableStateOf(initial.rate) }

    val ageV = age.toIntOrNull()
    val heightV = height.replace(',', '.').toDoubleOrNull()
    val weightV = weight.replace(',', '.').toDoubleOrNull()
    val valid = ageV != null && ageV in 12..100 && heightV != null && heightV in 100.0..250.0 &&
        weightV != null && Units.fromDisplay(weightV, useLb) in 30.0..300.0

    val draft = initial.copy(
        name = name.trim(), sex = sex, age = ageV ?: initial.age, heightCm = heightV ?: initial.heightCm,
        weightKg = weightV?.let { Units.fromDisplay(it, useLb) } ?: initial.weightKg,
        activity = activity, goal = goal, useLb = useLb,
        ratePct = if (goal == Goal.MAINTAIN) -1.0 else rate,
    )

    val body: @Composable (Modifier) -> Unit = { mod ->
        Column(
            mod.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (firstRun) {
                Text("Welcome to FitLog", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("A few details so I can work out your calorie and macro targets. You can change these any time.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedTextField(name, { name = it }, label = { Text("Name (optional)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())

            Label("Sex")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sex.entries.forEach { FilterChip(selected = sex == it, onClick = { sex = it }, label = { Text(it.label) }) }
            }

            Label("Units")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !useLb, onClick = {
                    if (useLb) { weightV?.let { weight = Units.fmt(Math.round(it / Units.LB_PER_KG * 10) / 10.0) }; useLb = false }
                }, label = { Text("kg") })
                FilterChip(selected = useLb, onClick = {
                    if (!useLb) { weightV?.let { weight = Units.fmt(Math.round(it * Units.LB_PER_KG * 10) / 10.0) }; useLb = true }
                }, label = { Text("lb") })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(age, { age = it.filter(Char::isDigit) }, label = { Text("Age") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(height, { height = it }, label = { Text("Height (cm)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(weight, { weight = it }, label = { Text("Weight (${Units.label(useLb)})") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            }

            Label("Goal")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Goal.entries.forEach {
                    FilterChip(selected = goal == it, onClick = { goal = it; rate = it.defaultRate }, label = { Text(it.label) })
                }
            }
            if (goal != Goal.MAINTAIN) {
                Label(if (goal == Goal.LOSE) "Weekly rate of loss" else "Weekly rate of gain")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    goal.rates.forEach { r ->
                        val kg = r / 100 * (weightV?.let { Units.fromDisplay(it, useLb) } ?: initial.weightKg)
                        val tag = when {
                            goal == Goal.LOSE && r <= 0.25 -> "gentle"
                            goal == Goal.LOSE && r >= 1.0 -> "aggressive"
                            goal == Goal.GAIN && r >= 0.5 -> "faster, more fat"
                            r == goal.defaultRate -> "recommended"
                            else -> ""
                        }
                        FilterChip(selected = rate == r, onClick = { rate = r }, label = {
                            Text("${Units.fmt2(r)} % · ${Units.fmt2(Units.toDisplay(kg, useLb))} ${Units.label(useLb)}/wk" +
                                if (tag.isNotEmpty()) " ($tag)" else "")
                        })
                    }
                }
            }

            Label("Activity level")
            Column {
                ActivityLevel.entries.forEach { a ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = activity == a, onClick = { activity = a })
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = activity == a, onClick = { activity = a })
                        Text(a.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (valid) {
                val t = draft.targets()
                SectionCard(title = "Your daily targets") {
                    Row(Modifier.fillMaxWidth()) {
                        StatTile("Calories", "${t.kcal}", Modifier.weight(1f))
                        StatTile("Protein", "${t.protein} g", Modifier.weight(1f))
                        StatTile("Carbs", "${t.carbs} g", Modifier.weight(1f))
                        StatTile("Fat", "${t.fat} g", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("BMR ${t.bmr} kcal · maintenance ≈ ${t.tdee} kcal" + (if (t.adaptive) " (learned from your data)" else "") +
                        if (initial.customKcal > 0) " · custom calorie target set in Settings" else "",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text("Enter a valid age (12–100), height (100–250 cm) and weight.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Button(onClick = { onSave(draft) }, enabled = valid, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (firstRun) "Let's go" else "Save")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (firstRun) {
        Surface(Modifier.fillMaxSize()) { body(Modifier.systemBarsPadding().imePadding()) }
    } else {
        ScreenScaffold(title = "Profile & goals", onBack = onBack) { p -> body(Modifier.padding(p)) }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}
