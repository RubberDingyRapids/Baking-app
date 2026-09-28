@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rubberdingyrapids.baking.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rubberdingyrapids.baking.core.model.MeasureUnit
import com.rubberdingyrapids.baking.core.model.Quantity
import com.rubberdingyrapids.baking.ui.theme.tagColor

@Composable
fun TagChip(tag: String, modifier: Modifier = Modifier) {
    if (tag.isBlank()) return
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(tagColor(tag))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Button showing the current unit; tapping opens a menu of all units. */
@Composable
fun UnitButton(unit: MeasureUnit, onUnitChange: (MeasureUnit) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }) { Text(unit.symbol) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MeasureUnit.entries.forEach { candidate ->
                DropdownMenuItem(
                    text = { Text("${candidate.symbol}  ·  ${candidate.name.lowercase()}") },
                    onClick = { open = false; onUnitChange(candidate) },
                )
            }
        }
    }
}

/**
 * Numeric amount field plus a [UnitButton]. Switching to a unit of the same
 * kind converts the number; switching kinds keeps the number as typed.
 */
@Composable
fun QuantityField(
    amountText: String,
    unit: MeasureUnit,
    onAmountChange: (String) -> Unit,
    onUnitChange: (MeasureUnit) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    amountFieldModifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = amountText,
            onValueChange = { text -> if (text.isEmpty() || text.matches(Regex("^\\d*[.,]?\\d*$"))) onAmountChange(text) },
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = amountFieldModifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        UnitButton(
            unit = unit,
            onUnitChange = { target ->
                val current = amountText.replace(',', '.').toDoubleOrNull()
                if (current != null) {
                    val converted = Quantity(current, unit).convertTo(target)
                    if (converted != null) onAmountChange(Quantity.formatNumber(converted.amount))
                }
                onUnitChange(target)
            },
        )
    }
}

/** Hours and minutes entry for bake and wait steps. */
@Composable
fun DurationField(
    totalSeconds: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    var hoursText by remember(totalSeconds) { mutableStateOf(if (hours == 0) "" else hours.toString()) }
    var minutesText by remember(totalSeconds) { mutableStateOf(if (minutes == 0) "" else minutes.toString()) }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = hoursText,
            onValueChange = { t ->
                if (t.length <= 2 && t.all { it.isDigit() }) {
                    hoursText = t
                    onChange((t.toIntOrNull() ?: 0) * 3600 + (minutesText.toIntOrNull() ?: 0) * 60)
                }
            },
            label = { Text("Hours") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = minutesText,
            onValueChange = { t ->
                if (t.length <= 3 && t.all { it.isDigit() }) {
                    minutesText = t
                    onChange((hoursText.toIntOrNull() ?: 0) * 3600 + (t.toIntOrNull() ?: 0) * 60)
                }
            },
            label = { Text("Minutes") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Slim action bar pinned to the bottom of a screen. It rises above the
 * keyboard and the system navigation bar (taking whichever is taller) so the
 * action is always reachable while typing.
 */
@Composable
fun BottomActionBar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(tonalElevation = 3.dp, modifier = modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            content()
        }
    }
}

@Composable
fun BottomActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().height(44.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
