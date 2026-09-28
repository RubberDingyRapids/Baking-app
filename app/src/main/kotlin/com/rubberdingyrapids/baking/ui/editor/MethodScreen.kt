@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rubberdingyrapids.baking.ui.editor

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubberdingyrapids.baking.core.flow.FlowEngine
import com.rubberdingyrapids.baking.core.model.Step
import com.rubberdingyrapids.baking.ui.components.EmptyState
import com.rubberdingyrapids.baking.ui.components.FlowConnector
import com.rubberdingyrapids.baking.ui.components.StepCard
import com.rubberdingyrapids.baking.ui.components.dragHandle
import com.rubberdingyrapids.baking.ui.components.draggedItem
import com.rubberdingyrapids.baking.ui.components.rememberDragDropState

/** A step being created (index == steps.size) or edited in the bottom sheet. */
private data class SheetTarget(val step: Step?, val index: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MethodScreen(viewModel: EditorViewModel, onBack: () -> Unit) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val analysis = remember(draft) { FlowEngine.analyse(draft) }
    var sheet by remember { mutableStateOf<SheetTarget?>(null) }

    val listState = rememberLazyListState()
    val dragState = rememberDragDropState(
        listState = listState,
        toItemIndex = { listIndex -> listIndex.takeIf { it in draft.steps.indices } },
        onMove = viewModel::moveStep,
    )

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
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { sheet = SheetTarget(null, draft.steps.size) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add step")
                }
            }
        },
    ) { padding ->
        if (draft.steps.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    icon = Icons.Outlined.AccountTree,
                    title = "Build the method",
                    body = if (draft.ingredients.isEmpty()) {
                        "Add some ingredients first, then combine them step by step here."
                    } else {
                        "Each step takes ingredients (or the result of an earlier step) and makes something new. Start with the first thing you'd do."
                    },
                    modifier = Modifier.padding(top = 48.dp),
                )
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                itemsIndexed(analysis.steps, key = { _, a -> a.step.id }) { index, stepAnalysis ->
                    Column(Modifier.animateContentSize()) {
                        if (index > 0) FlowConnector()
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteStep(stepAnalysis.step.id)
                                    true
                                } else {
                                    false
                                }
                            },
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(end = 24.dp),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete step", tint = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.draggedItem(dragState, index),
                        ) {
                            StepCard(
                                step = stepAnalysis.step,
                                index = index,
                                inputs = stepAnalysis.inputs,
                                issues = stepAnalysis.issues,
                                onClick = { sheet = SheetTarget(stepAnalysis.step, index) },
                                trailing = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.DragHandle,
                                            contentDescription = "Hold and drag to reorder",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(8.dp).dragHandle(dragState, index),
                                        )
                                        IconButton(onClick = { viewModel.deleteStep(stepAnalysis.step.id) }) {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                contentDescription = "Delete step",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
                if (analysis.leftovers.isNotEmpty()) {
                    item(key = "leftovers") {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Not used yet: " + analysis.leftovers.joinToString(", ") { item ->
                                item.remaining?.let { "${item.name} (${it.format()})" } ?: item.name
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    sheet?.let { target ->
        StepSheet(
            existing = target.step,
            index = target.index,
            availableItems = remember(draft, target) { viewModel.availableItems(target.index) },
            onDismiss = { sheet = null },
            onSubmit = { step ->
                if (target.step == null) viewModel.addStep(step) else viewModel.updateStep(step)
                sheet = null
            },
            onDelete = target.step?.let { existing -> { viewModel.deleteStep(existing.id); sheet = null } },
        )
    }
}
