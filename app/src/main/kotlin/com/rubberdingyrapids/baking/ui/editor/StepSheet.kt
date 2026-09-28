@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rubberdingyrapids.baking.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rubberdingyrapids.baking.core.Ids
import com.rubberdingyrapids.baking.core.flow.FlowEngine
import com.rubberdingyrapids.baking.core.flow.FlowItem
import com.rubberdingyrapids.baking.core.model.ActionType
import com.rubberdingyrapids.baking.core.model.Quantity
import com.rubberdingyrapids.baking.core.model.Step
import com.rubberdingyrapids.baking.core.model.StepInput
import com.rubberdingyrapids.baking.core.model.Temperature
import com.rubberdingyrapids.baking.core.model.TemperatureScale
import com.rubberdingyrapids.baking.ui.components.DurationField
import com.rubberdingyrapids.baking.ui.components.SectionHeader
import com.rubberdingyrapids.baking.ui.components.actionIcon

/** How much of a selected item the step uses. */
private enum class PortionMode { ALL, FRACTION, AMOUNT }

private data class Portion(
    val mode: PortionMode = PortionMode.ALL,
    val fraction: Double = 0.5,
    val amountText: String = "",
) {
    fun toInput(item: FlowItem): StepInput = when (mode) {
        PortionMode.ALL -> StepInput(item.id)
        PortionMode.FRACTION -> StepInput(item.id, fraction = fraction)
        PortionMode.AMOUNT -> {
            val unit = item.remaining?.unit
            val amount = amountText.replace(',', '.').toDoubleOrNull()
            if (unit != null && amount != null && amount > 0) StepInput(item.id, amount = Quantity(amount, unit)) else StepInput(item.id)
        }
    }

    companion object {
        fun from(input: StepInput, item: FlowItem?): Portion {
            val amount = input.amount
            val fraction = input.fraction
            return when {
                amount != null -> Portion(
                    PortionMode.AMOUNT,
                    amountText = Quantity.formatNumber(amount.convertTo(item?.remaining?.unit ?: amount.unit)?.amount ?: amount.amount),
                )
                fraction != null -> Portion(PortionMode.FRACTION, fraction = fraction)
                else -> Portion()
            }
        }
    }
}

private val fractionChoices = listOf(0.5 to "½", 1.0 / 3 to "⅓", 0.25 to "¼", 0.75 to "¾")

/**
 * Bottom sheet for adding or editing one method step. First the cook picks an
 * action; then they choose which items it uses (whole or in part), any timing
 * or temperature, and can rename the result.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StepSheet(
    existing: Step?,
    index: Int,
    availableItems: List<FlowItem>,
    onDismiss: () -> Unit,
    onSubmit: (Step) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val itemsById = remember(availableItems) { availableItems.associateBy { it.id } }

    var action by remember { mutableStateOf(existing?.action) }
    var customLabel by remember { mutableStateOf(existing?.customLabel ?: "") }
    val portions = remember {
        mutableStateMapOf<String, Portion>().apply {
            existing?.inputs?.forEach { input -> if (input.itemId in itemsById) put(input.itemId, Portion.from(input, itemsById[input.itemId])) }
        }
    }
    var durationSeconds by remember { mutableStateOf(existing?.durationSeconds ?: 0) }
    var tempText by remember { mutableStateOf(existing?.temperature?.value?.toString() ?: "") }
    var tempScale by remember { mutableStateOf(existing?.temperature?.scale ?: TemperatureScale.CELSIUS) }
    var preheat by remember { mutableStateOf(existing?.preheat ?: false) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var outputName by remember { mutableStateOf(existing?.outputName ?: "") }
    var nameEdited by remember { mutableStateOf(existing != null && existing.outputName.isNotBlank()) }

    val selectedNames = availableItems.filter { it.id in portions }.map { it.name }
    val suggestedName = action?.let { FlowEngine.suggestOutputName(Step(action = it, customLabel = customLabel), selectedNames) } ?: ""
    val shownName = if (nameEdited) outputName else suggestedName

    val currentAction = action
    val valid = currentAction != null &&
        (!currentAction.requiresInputs || portions.isNotEmpty()) &&
        (currentAction != ActionType.CUSTOM || customLabel.isNotBlank()) &&
        (!currentAction.hasDuration || durationSeconds > 0)

    fun submit() {
        val act = action ?: return
        val inputs = portions.mapNotNull { (id, portion) -> itemsById[id]?.let { portion.toInput(it) } }
        onSubmit(
            Step(
                id = existing?.id ?: Ids.next(),
                action = act,
                customLabel = if (act == ActionType.CUSTOM) customLabel.trim() else "",
                inputs = inputs,
                outputName = shownName.trim(),
                note = note.trim(),
                durationSeconds = if (act.hasDuration) durationSeconds else null,
                temperature = if (act.hasTemperature) tempText.toIntOrNull()?.let { Temperature(it, tempScale) } else null,
                preheat = act.hasTemperature && preheat,
            ),
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            if (currentAction == null) {
                Text("Step ${index + 1}: what happens next?", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 3,
                ) {
                    ActionType.entries.forEach { candidate ->
                        Card(
                            onClick = { action = candidate },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(actionIcon(candidate), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.height(6.dp))
                                Text(candidate.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (existing == null) {
                        IconButton(onClick = { action = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Choose a different action")
                        }
                    }
                    Icon(actionIcon(currentAction), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (currentAction == ActionType.CUSTOM) "Step ${index + 1}" else "Step ${index + 1}: ${currentAction.label}",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                if (currentAction == ActionType.CUSTOM) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { customLabel = it },
                        label = { Text("Action") },
                        placeholder = { Text("e.g. Knead, Fold, Chill") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                SectionHeader(if (currentAction.requiresInputs) "Use" else "Use (optional)")
                if (availableItems.isEmpty()) {
                    Text(
                        "Nothing is left to use. Add ingredients on the previous screen, or check earlier steps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                availableItems.forEach { item ->
                    val portion = portions[item.id]
                    ItemPicker(
                        item = item,
                        portion = portion,
                        onToggle = { checked -> if (checked) portions[item.id] = Portion() else portions.remove(item.id) },
                        onPortionChange = { portions[item.id] = it },
                    )
                }

                if (currentAction.hasDuration) {
                    SectionHeader(if (currentAction == ActionType.BAKE) "Bake for" else "Wait for")
                    DurationField(totalSeconds = durationSeconds, onChange = { durationSeconds = it }, modifier = Modifier.fillMaxWidth())
                }

                if (currentAction.hasTemperature) {
                    SectionHeader("Oven temperature")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = tempText,
                            onValueChange = { t -> if (t.length <= 3 && t.all { it.isDigit() }) tempText = t },
                            label = { Text("Temperature") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(12.dp))
                        SingleChoiceSegmentedButtonRow {
                            TemperatureScale.entries.forEachIndexed { i, scale ->
                                SegmentedButton(
                                    selected = tempScale == scale,
                                    onClick = {
                                        val current = tempText.toIntOrNull()
                                        if (current != null && scale != tempScale) {
                                            tempText = Temperature(current, tempScale).convertTo(scale).value.toString()
                                        }
                                        tempScale = scale
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = i, count = TemperatureScale.entries.size),
                                ) { Text(scale.symbol) }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("Preheat reminder", style = MaterialTheme.typography.bodyLarge)
                            Text("Adds a \"preheat the oven\" step at the start", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = preheat, onCheckedChange = { preheat = it })
                    }
                }

                SectionHeader("Result")
                OutlinedTextField(
                    value = shownName,
                    onValueChange = { text ->
                        outputName = text
                        nameEdited = text.isNotBlank()
                    },
                    label = { Text("Call it") },
                    supportingText = if (!nameEdited) ({ Text("Suggested from what you selected. Tap to rename.") }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    placeholder = { Text("e.g. until pale and fluffy") },
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = ::submit,
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(if (existing == null) "Add step" else "Save step")
                }
            }
        }
    }
}

@Composable
private fun ItemPicker(
    item: FlowItem,
    portion: Portion?,
    onToggle: (Boolean) -> Unit,
    onPortionChange: (Portion) -> Unit,
) {
    val checked = portion != null
    val remaining = item.remaining
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = checked, onCheckedChange = onToggle)
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge)
                val remainingText = remaining?.format() ?: if (item.isPartiallyUsed) "part left" else null
                val detail = listOfNotNull(remainingText, if (item.isIntermediate) "from an earlier step" else null)
                if (detail.isNotEmpty()) {
                    Text(detail.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (portion != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 48.dp, bottom = 4.dp).fillMaxWidth(),
            ) {
                FilterChip(
                    selected = portion.mode == PortionMode.ALL,
                    onClick = { onPortionChange(portion.copy(mode = PortionMode.ALL)) },
                    label = { Text("All") },
                )
                fractionChoices.forEach { (value, glyph) ->
                    FilterChip(
                        selected = portion.mode == PortionMode.FRACTION && kotlin.math.abs(portion.fraction - value) < 0.001,
                        onClick = { onPortionChange(portion.copy(mode = PortionMode.FRACTION, fraction = value)) },
                        label = { Text(glyph) },
                    )
                }
                if (remaining != null) {
                    FilterChip(
                        selected = portion.mode == PortionMode.AMOUNT,
                        onClick = { onPortionChange(portion.copy(mode = PortionMode.AMOUNT)) },
                        label = { Text("Amount") },
                    )
                }
            }
            if (portion.mode == PortionMode.AMOUNT && remaining != null) {
                OutlinedTextField(
                    value = portion.amountText,
                    onValueChange = { t -> if (t.isEmpty() || t.matches(Regex("^\\d*[.,]?\\d*$"))) onPortionChange(portion.copy(amountText = t)) },
                    label = { Text("Amount to use") },
                    suffix = { Text(remaining.unit.symbol) },
                    supportingText = { Text("${remaining.format()} available") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.padding(start = 48.dp).fillMaxWidth(),
                )
            }
        }
    }
}
