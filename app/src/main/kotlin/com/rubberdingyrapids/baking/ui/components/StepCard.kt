@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rubberdingyrapids.baking.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Blender
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.OutdoorGrill
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Cyclone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.rubberdingyrapids.baking.core.flow.FlowIssue
import com.rubberdingyrapids.baking.core.flow.ResolvedInput
import com.rubberdingyrapids.baking.core.format.TimeFormat
import com.rubberdingyrapids.baking.core.model.ActionType
import com.rubberdingyrapids.baking.core.model.Step

fun actionIcon(action: ActionType): ImageVector = when (action) {
    ActionType.CHOP -> Icons.Outlined.ContentCut
    ActionType.MIX -> Icons.Outlined.Blender
    ActionType.WHISK -> Icons.Outlined.Cyclone
    ActionType.ADD -> Icons.Outlined.Add
    ActionType.MELT -> Icons.Outlined.WaterDrop
    ActionType.FRY -> Icons.Outlined.OutdoorGrill
    ActionType.BOIL -> Icons.Outlined.Waves
    ActionType.SIMMER -> Icons.Outlined.Water
    ActionType.BAKE -> Icons.Outlined.LocalFireDepartment
    ActionType.ROAST -> Icons.Outlined.Whatshot
    ActionType.WAIT -> Icons.Outlined.HourglassEmpty
    ActionType.SERVE -> Icons.Outlined.Restaurant
    ActionType.CUSTOM -> Icons.Outlined.Build
}

/** How a step tile is drawn depending on where the cook is in the flow. */
enum class StepAppearance { Normal, Current, Locked, Completed }

/** Small vertical connector drawn between two step tiles in the flow chart. */
@Composable
fun FlowConnector(modifier: Modifier = Modifier, muted: Boolean = false) {
    val color = MaterialTheme.colorScheme.outline.copy(alpha = if (muted) 0.35f else 0.7f)
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(2.dp).height(10.dp).background(color))
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
    }
}

/**
 * One tile in the flow chart. Shared by the method editor, the overview and
 * cook mode, which differ only in [appearance] and the slots they fill.
 */
@Composable
fun StepCard(
    step: Step,
    index: Int,
    inputs: List<ResolvedInput>,
    issues: List<FlowIssue>,
    modifier: Modifier = Modifier,
    appearance: StepAppearance = StepAppearance.Normal,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showNote: Boolean = true,
    /** Tighter layout for side-by-side lanes. */
    compact: Boolean = false,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val container = when {
        appearance == StepAppearance.Completed -> scheme.surfaceVariant.copy(alpha = 0.35f)
        step.action.isTerminal && appearance == StepAppearance.Current -> scheme.tertiaryContainer
        appearance == StepAppearance.Current -> scheme.primaryContainer
        step.action.isTerminal -> scheme.tertiaryContainer.copy(alpha = 0.6f)
        else -> scheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val alpha = when (appearance) {
        StepAppearance.Locked -> 0.5f
        StepAppearance.Completed -> 0.55f
        else -> 1f
    }
    val outputName = step.outputName.ifBlank { step.actionLabel }

    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = container,
            disabledContainerColor = container,
            contentColor = scheme.onSurface,
            disabledContentColor = scheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (appearance == StepAppearance.Current) 3.dp else 0.dp),
        modifier = modifier.fillMaxWidth().alpha(alpha),
    ) {
        val pad = if (compact) 8.dp else 12.dp
        Row(Modifier.padding(start = pad, end = if (compact) 4.dp else 8.dp, top = pad, bottom = pad), verticalAlignment = Alignment.Top) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                if (compact) {
                    ActionBadge(step, index)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = outputName,
                        style = MaterialTheme.typography.titleSmall,
                        textDecoration = if (appearance == StepAppearance.Completed) TextDecoration.LineThrough else null,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ActionBadge(step, index)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = outputName,
                            style = MaterialTheme.typography.titleMedium,
                            textDecoration = if (appearance == StepAppearance.Completed) TextDecoration.LineThrough else null,
                        )
                    }
                }
                if (inputs.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = inputs.joinToString(" · ") { it.describe() },
                        style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                val meta = buildList {
                    step.durationSeconds?.takeIf { it > 0 }?.let { add(Icons.Outlined.Schedule to TimeFormat.short(it)) }
                    step.temperature?.let { add(Icons.Outlined.Thermostat to it.format()) }
                }
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        meta.forEach { (icon, text) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = scheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text(text, style = MaterialTheme.typography.labelLarge, color = scheme.primary)
                            }
                        }
                        if (step.preheat) {
                            Text("preheat", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                        }
                    }
                }
                if (showNote && step.note.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(step.note, style = MaterialTheme.typography.bodyMedium)
                }
                issues.forEach { issue ->
                    Spacer(Modifier.height(4.dp))
                    Text(issue.message, style = MaterialTheme.typography.labelLarge, color = scheme.error)
                }
                if (extraContent != null) {
                    Spacer(Modifier.height(10.dp))
                    extraContent()
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(4.dp))
                trailing()
            }
        }
    }
}

@Composable
private fun ActionBadge(step: Step, index: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            "${index + 1}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(6.dp))
        Icon(actionIcon(step.action), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(step.actionLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
    }
}
