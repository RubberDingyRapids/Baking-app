@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rubberdingyrapids.baking.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubberdingyrapids.baking.core.flow.FlowEngine
import com.rubberdingyrapids.baking.core.flow.FlowLayoutEngine
import com.rubberdingyrapids.baking.core.model.Step
import com.rubberdingyrapids.baking.ui.components.BottomActionBar
import com.rubberdingyrapids.baking.ui.components.BottomActionButton
import com.rubberdingyrapids.baking.ui.components.EmptyState
import com.rubberdingyrapids.baking.ui.components.FlowChart
import com.rubberdingyrapids.baking.ui.components.StepCard

/** A step being created (null) or edited in the bottom sheet. */
private data class SheetTarget(val step: Step?)

@Composable
fun MethodScreen(viewModel: EditorViewModel, onBack: () -> Unit) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val analysis = remember(draft) { FlowEngine.analyse(draft) }
    val layout = remember(draft) { FlowLayoutEngine.layout(draft) }
    var sheet by remember { mutableStateOf<SheetTarget?>(null) }
    var reveal by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Method")
                        if (draft.name.isNotBlank()) {
                            Text(draft.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to recipe") }
                },
            )
        },
        bottomBar = {
            BottomActionBar {
                BottomActionButton(label = "Add step", icon = Icons.Default.Add, onClick = { sheet = SheetTarget(null) })
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (draft.steps.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.AccountTree,
                    title = "Build the method",
                    body = if (draft.ingredients.isEmpty()) {
                        "Add some ingredients first, then combine them step by step here."
                    } else {
                        "Each step takes ingredients (or the result of an earlier step) and makes something new. Steps that don't depend on each other run side by side."
                    },
                    modifier = Modifier.padding(top = 48.dp),
                )
            } else {
                FlowChart(analysis = analysis, layout = layout, revealStepId = reveal) { stepAnalysis, _, compact ->
                    StepCard(
                        step = stepAnalysis.step,
                        index = stepAnalysis.index,
                        inputs = stepAnalysis.inputs,
                        issues = stepAnalysis.issues,
                        compact = compact,
                        onClick = { sheet = SheetTarget(stepAnalysis.step) },
                        trailing = {
                            IconButton(onClick = { viewModel.deleteStep(stepAnalysis.step.id) }) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Delete step",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }
                if (analysis.leftovers.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Not used yet: " + analysis.leftovers.joinToString(", ") { item ->
                            item.remaining?.let { "${item.name} (${it.format()})" } ?: item.name
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    sheet?.let { target ->
        val stepNumber = target.step?.let { existing -> draft.steps.indexOfFirst { it.id == existing.id } + 1 }?.takeIf { it > 0 }
        StepSheet(
            existing = target.step,
            stepNumber = stepNumber,
            availableItems = remember(draft, target) { viewModel.editableItems(target.step?.id) },
            onDismiss = { sheet = null },
            onSubmit = { step ->
                if (target.step == null) viewModel.addStep(step) else viewModel.updateStep(step)
                reveal = step.id
                sheet = null
            },
            onDelete = target.step?.let { existing -> { viewModel.deleteStep(existing.id); sheet = null } },
        )
    }
}
