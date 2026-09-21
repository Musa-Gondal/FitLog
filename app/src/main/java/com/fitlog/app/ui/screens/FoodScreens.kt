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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.fitlog.app.data.CustomFood
import com.fitlog.app.data.FoodLibrary
import com.fitlog.app.data.Meals
import com.fitlog.app.ui.AppViewModel
import com.fitlog.app.ui.components.ConfirmDialog
import com.fitlog.app.ui.components.DateSwitcher
import com.fitlog.app.ui.components.LocalSnackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.fitlog.app.ui.components.ScreenScaffold
import com.fitlog.app.ui.components.SectionCard
import com.fitlog.app.ui.components.StatTile
import com.fitlog.app.ui.components.pretty
import com.fitlog.app.ui.theme.CarbColor
import com.fitlog.app.ui.theme.FatColor
import com.fitlog.app.ui.theme.ProteinColor
import kotlin.math.roundToInt

private fun Double.g() = "${roundToInt()}g"

@Composable
fun MacroLine(kcal: Double, p: Double, c: Double, f: Double) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${kcal.roundToInt()} kcal", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text("P ${p.g()}", style = MaterialTheme.typography.labelMedium, color = ProteinColor)
        Text("C ${c.g()}", style = MaterialTheme.typography.labelMedium, color = CarbColor)
        Text("F ${f.g()}", style = MaterialTheme.typography.labelMedium, color = FatColor)
    }
}

// ---------------- Daily food log ----------------

@Composable
fun FoodScreen(vm: AppViewModel, nav: NavHostController) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val date by vm.date.collectAsStateWithLifecycle()
    val entries by vm.foodEntries.collectAsStateWithLifecycle()
    val t = profile.targets()
    val kcal = entries.sumOf { it.kcal }
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            DateSwitcher(date) { vm.shiftDate(it) }
            SectionCard {
                Row(Modifier.fillMaxWidth()) {
                    StatTile("Target", "${t.kcal}", Modifier.weight(1f))
                    StatTile("Eaten", "${kcal.roundToInt()}", Modifier.weight(1f))
                    StatTile("Remaining", "${t.kcal - kcal.roundToInt()}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                MacroLine(kcal, entries.sumOf { it.protein }, entries.sumOf { it.carbs }, entries.sumOf { it.fat })
                Text("Targets: P ${t.protein}g · C ${t.carbs}g · F ${t.fat}g",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Meals.names.forEachIndexed { mi, meal ->
            val mealEntries = entries.filter { it.meal == mi }
            item(key = "meal$mi") {
                SectionCard(
                    title = "$meal · ${mealEntries.sumOf { it.kcal }.roundToInt()} kcal",
                    action = {
                        if (mealEntries.isEmpty()) {
                            IconButton(onClick = { vm.copyMealFromPreviousDay(mi) }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy from previous day")
                            }
                        }
                        IconButton(onClick = { nav.navigate("addFood/$mi") }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add food")
                        }
                    },
                ) {
                    if (mealEntries.isEmpty()) {
                        Text("Nothing logged. Tap + to add, or ⧉ to copy from ${date.minusDays(1).pretty().lowercase()}.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    mealEntries.forEach { e ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${fmtQty(e.quantity)} × ${e.servingLabel}", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                MacroLine(e.kcal, e.protein, e.carbs, e.fat)
                            }
                            IconButton(onClick = {
                                vm.deleteFood(e)
                                scope.launch {
                                    val r = snackbar.showSnackbar("Removed ${e.name}", actionLabel = "Undo", duration = SnackbarDuration.Short)
                                    if (r == SnackbarResult.ActionPerformed) vm.restoreFood(e)
                                }
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

fun fmtQty(q: Double): String = if (q % 1.0 == 0.0) q.toInt().toString() else String.format(java.util.Locale.US, "%.2f", q).trimEnd('0').trimEnd('.')

// ---------------- Add food ----------------

/** Unified row model for built-in, custom and recent foods. */
private data class Pickable(
    val name: String, val serving: String, val kcal: Double, val p: Double, val c: Double, val f: Double,
    val custom: CustomFood? = null,
)

@Composable
fun AddFoodScreen(vm: AppViewModel, nav: NavHostController, initialMeal: Int) {
    val recent by vm.recentFoods.collectAsStateWithLifecycle()
    val custom by vm.customFoods.collectAsStateWithLifecycle()
    var meal by remember { mutableIntStateOf(initialMeal.coerceIn(0, Meals.names.lastIndex)) }
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }
    var category by remember { mutableStateOf<String?>(null) }
    var picked by remember { mutableStateOf<Pickable?>(null) }
    var quickAdd by remember { mutableStateOf(false) }
    var deleteCustom by remember { mutableStateOf<CustomFood?>(null) }

    val q = query.trim().lowercase()
    val list: List<Pickable> = when (tab) {
        0 -> {
            val customRows = custom.map { Pickable(it.name, it.serving, it.kcal, it.protein, it.carbs, it.fat, it) }
            val builtIn = FoodLibrary.items
                .filter { category == null || it.category == category }
                .map { Pickable(it.name, it.serving, it.kcal, it.protein, it.carbs, it.fat) }
            (if (category == null) customRows else emptyList()) + builtIn
        }
        1 -> recent.map {
            // Stored entries are totals; convert back to one serving.
            val qn = if (it.quantity > 0) it.quantity else 1.0
            Pickable(it.name, it.servingLabel, it.kcal / qn, it.protein / qn, it.carbs / qn, it.fat / qn)
        }
        else -> custom.map { Pickable(it.name, it.serving, it.kcal, it.protein, it.carbs, it.fat, it) }
    }.filter { q.isEmpty() || it.name.lowercase().contains(q) }

    ScreenScaffold(title = "Add to ${Meals.names[meal]}", onBack = { nav.popBackStack() }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            LazyRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            ) {
                items(Meals.names.size) { i ->
                    FilterChip(selected = meal == i, onClick = { meal = i }, label = { Text(Meals.names[i]) })
                }
            }
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                placeholder = { Text("Search foods (e.g. roti, karahi, egg)") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, "Clear") }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
            TabRow(selectedTabIndex = tab) {
                listOf("All foods", "Recent", "My foods").forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                }
            }
            if (tab == 0) {
                LazyRow(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                ) {
                    item { FilterChip(selected = category == null, onClick = { category = null }, label = { Text("All") }) }
                    items(FoodLibrary.categories) { c ->
                        FilterChip(selected = category == c, onClick = { category = if (category == c) null else c },
                            label = { Text(c) })
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { quickAdd = true }, modifier = Modifier.weight(1f)) { Text("Quick add") }
                OutlinedButton(onClick = { nav.navigate("customFood") }, modifier = Modifier.weight(1f)) { Text("New food") }
            }
            LazyColumn(Modifier.fillMaxSize()) {
                if (list.isEmpty()) {
                    item {
                        Text(
                            when (tab) {
                                1 -> "Foods you log will show up here."
                                2 -> "Create your own foods with \"New food\" – e.g. your mother's daal recipe or a packaged item."
                                else -> "No match. Try another word, or use Quick add / New food."
                            },
                            modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(list) { item ->
                    ListItem(
                        headlineContent = { Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Column {
                                Text(item.serving, style = MaterialTheme.typography.bodySmall)
                                MacroLine(item.kcal, item.p, item.c, item.f)
                            }
                        },
                        trailingContent = if (item.custom != null && tab == 2) {
                            {
                                IconButton(onClick = { deleteCustom = item.custom }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete custom food")
                                }
                            }
                        } else null,
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { picked = item },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    picked?.let { item ->
        ServingDialog(item.name, item.serving, item.kcal, item.p, item.c, item.f, onDismiss = { picked = null }) { qty ->
            vm.addFood(meal, item.name, item.serving, qty, item.kcal, item.p, item.c, item.f)
            picked = null
            nav.popBackStack()
        }
    }
    if (quickAdd) {
        QuickAddDialog(onDismiss = { quickAdd = false }) { name, k, p, c, f ->
            vm.addFood(meal, name, "quick add", 1.0, k, p, c, f)
            quickAdd = false
            nav.popBackStack()
        }
    }
    deleteCustom?.let { cf ->
        ConfirmDialog("Delete ${cf.name}?", "Past log entries stay; only the saved food is removed.",
            onDismiss = { deleteCustom = null }) { vm.deleteCustomFood(cf) }
    }
}

@Composable
private fun ServingDialog(
    name: String, serving: String, kcal: Double, p: Double, c: Double, f: Double,
    onDismiss: () -> Unit, onAdd: (Double) -> Unit,
) {
    var qtyText by remember { mutableStateOf("1") }
    val qty = qtyText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it <= 50 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1 serving = $serving", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        val v = (qty ?: 1.0) - 0.5
                        if (v > 0) qtyText = fmtQty(v)
                    }) { Text("−½") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(qtyText, { qtyText = it }, label = { Text("Servings") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { qtyText = fmtQty((qty ?: 0.0) + 0.5) }) { Text("+½") }
                }
                val m = qty ?: 0.0
                MacroLine(kcal * m, p * m, c * m, f * m)
            }
        },
        confirmButton = { TextButton(onClick = { qty?.let(onAdd) }, enabled = qty != null) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun QuickAddDialog(onDismiss: () -> Unit, onAdd: (String, Double, Double, Double, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var k by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    var c by remember { mutableStateOf("") }
    var f by remember { mutableStateOf("") }
    fun num(s: String) = s.replace(',', '.').toDoubleOrNull()
    val kv = num(k)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick add") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name (optional)") }, singleLine = true)
                NumField(k, { k = it }, "Calories (kcal)")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumField(p, { p = it }, "Protein g", Modifier.weight(1f))
                    NumField(c, { c = it }, "Carbs g", Modifier.weight(1f))
                    NumField(f, { f = it }, "Fat g", Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onAdd(name.ifBlank { "Quick add" }, kv ?: 0.0, num(p) ?: 0.0, num(c) ?: 0.0, num(f) ?: 0.0)
            }, enabled = kv != null) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun NumField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(value, onChange, label = { Text(label, maxLines = 1) }, singleLine = true, modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
}

// ---------------- Create custom food ----------------

@Composable
fun CustomFoodScreen(vm: AppViewModel, nav: NavHostController) {
    var name by remember { mutableStateOf("") }
    var serving by remember { mutableStateOf("1 serving") }
    var k by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    var c by remember { mutableStateOf("") }
    var f by remember { mutableStateOf("") }
    fun num(s: String) = s.replace(',', '.').toDoubleOrNull()
    val valid = name.isNotBlank() && num(k) != null

    ScreenScaffold(title = "New food", onBack = { nav.popBackStack() }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Enter values for ONE serving. Check the nutrition label on packaged food.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(name, { name = it }, label = { Text("Food name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(serving, { serving = it }, label = { Text("Serving (e.g. 1 bowl, 100 g)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())
            NumField(k, { k = it }, "Calories (kcal)", Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField(p, { p = it }, "Protein g", Modifier.weight(1f))
                NumField(c, { c = it }, "Carbs g", Modifier.weight(1f))
                NumField(f, { f = it }, "Fat g", Modifier.weight(1f))
            }
            val macroKcal = (num(p) ?: 0.0) * 4 + (num(c) ?: 0.0) * 4 + (num(f) ?: 0.0) * 9
            if (macroKcal > 0) {
                Text("Macros add up to ≈ ${macroKcal.roundToInt()} kcal", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = {
                    vm.saveCustomFood(CustomFood(name = name.trim(), serving = serving.trim().ifBlank { "1 serving" },
                        kcal = num(k) ?: 0.0, protein = num(p) ?: 0.0, carbs = num(c) ?: 0.0, fat = num(f) ?: 0.0))
                    nav.popBackStack()
                },
                enabled = valid, modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Save food") }
        }
    }
}
