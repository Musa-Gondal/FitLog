package com.fitlog.app.ui.components

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fitlog.app.data.Badge
import com.fitlog.app.data.Coach
import com.fitlog.app.data.DayFacts
import com.fitlog.app.data.Units
import com.fitlog.app.ui.theme.Bronze
import com.fitlog.app.ui.theme.Gold
import com.fitlog.app.ui.theme.GoodColor
import com.fitlog.app.ui.theme.InfoColor
import com.fitlog.app.ui.theme.Silver
import com.fitlog.app.ui.theme.WarnColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** App-wide snackbar (used for Undo). */
val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.18f)).padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

/** Small ring with a value in the middle – used for macros on the Today screen. */
@Composable
fun MiniRing(label: String, value: Double, target: Int, color: Color, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val p by animateFloatAsState(if (target > 0) (value / target).toFloat() else 0f, tween(700), label = "mini")
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(size)) {
                val s = 7.dp.toPx()
                val arc = Size(this.size.width - s, this.size.height - s)
                val tl = Offset(s / 2, s / 2)
                drawArc(track, -90f, 360f, false, tl, arc, style = Stroke(s, cap = StrokeCap.Round))
                drawArc(color, -90f, 360f * p.coerceIn(0f, 1f), false, tl, arc, style = Stroke(s, cap = StrokeCap.Round))
            }
            Text("${value.toInt()}", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        Text("of ${target}g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Card with a subtle diagonal gradient for hero content. */
@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = MaterialTheme.colorScheme
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(c.primaryContainer, c.surfaceContainer, c.surfaceContainer)))
            .padding(18.dp),
    ) { content() }
}

fun levelColor(l: Coach.Level): Color = when (l) {
    Coach.Level.WARN -> WarnColor
    Coach.Level.INFO -> InfoColor
    Coach.Level.GOOD -> GoodColor
}

@Composable
fun InsightRow(i: Coach.Insight, onAction: (() -> Unit)? = null) {
    val color = levelColor(i.level)
    val icon = when (i.level) {
        Coach.Level.WARN -> Icons.Filled.Warning
        Coach.Level.INFO -> Icons.Filled.Info
        Coach.Level.GOOD -> Icons.Filled.CheckCircle
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(i.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(i.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (i.action != null && onAction != null) {
                TextButton(onClick = onAction, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text(i.action)
                }
            }
        }
    }
}

// ---------------- Heatmap ----------------

enum class HeatMode(val label: String) { OVERALL("Overall"), TRAINING("Training"), NUTRITION("Nutrition") }

/**
 * GitHub-style calendar: one column per week (Mon–Sun), newest week on the right.
 */
@Composable
fun Heatmap(days: List<DayFacts>, mode: HeatMode, weeks: Int = 18, onDay: (DayFacts) -> Unit) {
    val today = LocalDate.now()
    val start = today.with(DayOfWeek.MONDAY).minusWeeks((weeks - 1).toLong())
    val byDate = days.associateBy { it.date }
    val maxVol = days.filter { !it.date.isBefore(start) }.maxOfOrNull { it.volumeKg }?.takeIf { it > 0 } ?: 1.0
    val base = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceContainerHighest
    val monthFmt = DateTimeFormatter.ofPattern("MMM")

    fun intensity(f: DayFacts?): Float = when {
        f == null -> 0f
        mode == HeatMode.OVERALL -> f.score / 5f
        mode == HeatMode.TRAINING -> if (!f.workout) 0f else (0.35f + 0.65f * (f.volumeKg / maxVol).toFloat())
        else -> when {
            !f.foodLogged -> 0f
            f.kcalOnTarget && f.proteinHit -> 1f
            f.kcalOnTarget || f.proteinHit -> 0.6f
            else -> 0.25f
        }
    }

    Column {
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(18.dp))
            (0 until weeks).forEach { w ->
                val monday = start.plusWeeks(w.toLong())
                Text(
                    if (monday.dayOfMonth <= 7) monday.format(monthFmt) else "",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f), maxLines = 1, softWrap = false,
                )
            }
        }
        (0 until 7).forEach { dow ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (dow % 2 == 0) listOf("M", "T", "W", "T", "F", "S", "S")[dow] else "",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(18.dp),
                )
                (0 until weeks).forEach { w ->
                    val d = start.plusWeeks(w.toLong()).plusDays(dow.toLong())
                    val f = byDate[d]
                    val future = d.isAfter(today)
                    val a = intensity(f)
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).padding(1.5.dp).clip(RoundedCornerShape(3.dp))
                            .background(
                                when {
                                    future -> Color.Transparent
                                    a <= 0f -> empty
                                    else -> base.copy(alpha = 0.25f + 0.75f * a)
                                }
                            )
                            .then(if (d == today) Modifier.border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(3.dp)) else Modifier)
                            .then(if (f != null && !future) Modifier.clickable { onDay(f) } else Modifier),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { a ->
                Box(
                    Modifier.padding(1.5.dp).size(11.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (a == 0f) empty else base.copy(alpha = 0.25f + 0.75f * a))
                )
            }
            Spacer(Modifier.width(4.dp))
            Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------- Badges ----------------

fun badgeIcon(key: String): ImageVector = when (key) {
    "dumbbell" -> Icons.Filled.FitnessCenter
    "fire" -> Icons.Filled.LocalFireDepartment
    "egg" -> Icons.Filled.Egg
    "target" -> Icons.Filled.GpsFixed
    "water" -> Icons.Filled.WaterDrop
    "walk" -> Icons.AutoMirrored.Filled.DirectionsWalk
    "trophy" -> Icons.Filled.EmojiEvents
    "medal" -> Icons.Filled.MilitaryTech
    "weight" -> Icons.Filled.MonitorWeight
    "scale" -> Icons.Filled.Scale
    "food" -> Icons.Filled.Restaurant
    "sun" -> Icons.Filled.WbSunny
    "flag" -> Icons.Filled.Flag
    else -> Icons.Filled.Star
}

fun tierColor(t: Int): Color = when (t) { 3 -> Gold; 2 -> Silver; else -> Bronze }

@Composable
fun BadgeTile(b: Badge, date: String?, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val color = tierColor(b.tier)
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (b.unlocked) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(46.dp).clip(CircleShape)
                    .background(if (b.unlocked) color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (b.unlocked) badgeIcon(b.icon) else Icons.Filled.Lock, null,
                    tint = if (b.unlocked) color else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(b.title, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 2)
            if (b.unlocked) {
                Text(date ?: "Unlocked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { b.progress }, modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = color, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
                )
            }
        }
    }
}

// ---------------- Plate calculator ----------------

data class PlateColor(val kg: Double, val color: Color, val height: Float)

private val kgPlates = listOf(
    PlateColor(25.0, Color(0xFFD32F2F), 1f), PlateColor(20.0, Color(0xFF1976D2), 1f), PlateColor(15.0, Color(0xFFFBC02D), 0.92f),
    PlateColor(10.0, Color(0xFF388E3C), 0.84f), PlateColor(5.0, Color(0xFFECEFF1), 0.62f), PlateColor(2.5, Color(0xFF424242), 0.5f),
    PlateColor(1.25, Color(0xFF9E9E9E), 0.42f),
)
private val lbPlates = listOf(
    PlateColor(45.0, Color(0xFF1976D2), 1f), PlateColor(35.0, Color(0xFFFBC02D), 0.92f), PlateColor(25.0, Color(0xFF388E3C), 0.84f),
    PlateColor(10.0, Color(0xFFECEFF1), 0.62f), PlateColor(5.0, Color(0xFF424242), 0.5f), PlateColor(2.5, Color(0xFF9E9E9E), 0.42f),
)

/** Greedy plate breakdown per side, in display units. Returns plates and leftover that can't be matched. */
fun platesPerSide(target: Double, bar: Double, lb: Boolean): Pair<List<PlateColor>, Double> {
    var remaining = (target - bar) / 2
    val out = mutableListOf<PlateColor>()
    if (remaining <= 0) return out to 0.0
    (if (lb) lbPlates else kgPlates).forEach { p ->
        while (remaining >= p.kg - 1e-6) { out += p; remaining -= p.kg }
    }
    return out to remaining
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlateCalculatorDialog(initialDisplay: String, lb: Boolean, barKg: Double, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initialDisplay) }
    val bars = if (lb) listOf(45.0, 35.0, 25.0, 0.0) else listOf(20.0, 15.0, 10.0, 0.0)
    var bar by remember { mutableStateOf(if (lb) Math.round(Units.toDisplay(barKg, true) / 5) * 5.0 else barKg) }
    val target = text.replace(',', '.').toDoubleOrNull() ?: 0.0
    val (plates, left) = platesPerSide(target, bar, lb)
    val unit = Units.label(lb)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plate calculator") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    text, { text = it }, label = { Text("Total weight ($unit)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Text("Bar", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    bars.forEach { b ->
                        FilterChip(selected = bar == b, onClick = { bar = b },
                            label = { Text(if (b == 0.0) "No bar" else "${Units.fmt(b)} $unit") })
                    }
                }
                if (target <= bar) {
                    Text(if (target <= 0) "Enter a weight." else "That's just the bar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Each side:", style = MaterialTheme.typography.labelLarge)
                    // Visual barbell sleeve
                    Row(Modifier.fillMaxWidth().height(80.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(18.dp).height(10.dp).background(Color(0xFF9E9E9E)))
                        Box(Modifier.width(6.dp).height(30.dp).background(Color(0xFF757575)))
                        plates.forEach { p ->
                            Box(
                                Modifier.padding(horizontal = 1.dp).width(12.dp).fillMaxHeightFraction(p.height)
                                    .clip(RoundedCornerShape(3.dp)).background(p.color)
                                    .border(1.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                            )
                        }
                        Box(Modifier.width(30.dp).height(10.dp).background(Color(0xFF9E9E9E)))
                    }
                    val grouped = plates.groupingBy { it.kg }.eachCount()
                    Text(grouped.entries.joinToString("  +  ") { "${it.value} × ${Units.fmt(it.key)}" },
                        style = MaterialTheme.typography.titleMedium)
                    if (left > 0.01) {
                        Text("Can't match exactly – ${Units.fmt2(left * 2)} $unit short with standard plates.",
                            style = MaterialTheme.typography.bodySmall, color = WarnColor)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

private fun Modifier.fillMaxHeightFraction(f: Float): Modifier = this.then(Modifier.height((80 * f).dp))

// ---------------- Time picker ----------------

fun pickTime(ctx: Context, minutes: Int, onPick: (Int) -> Unit) {
    TimePickerDialog(ctx, { _, h, m -> onPick(h * 60 + m) }, minutes / 60, minutes % 60, false).show()
}
