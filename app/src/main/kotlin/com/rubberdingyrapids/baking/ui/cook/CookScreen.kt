@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rubberdingyrapids.baking.ui.cook

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubberdingyrapids.baking.core.format.TimeFormat
import com.rubberdingyrapids.baking.core.model.Ingredient
import com.rubberdingyrapids.baking.timer.ActiveTimer
import com.rubberdingyrapids.baking.timer.TimerNotifications
import com.rubberdingyrapids.baking.ui.components.BottomActionBar
import com.rubberdingyrapids.baking.ui.components.BottomActionButton
import com.rubberdingyrapids.baking.ui.components.EmptyState
import com.rubberdingyrapids.baking.ui.components.FlowConnector
import com.rubberdingyrapids.baking.ui.components.LoadingBox
import com.rubberdingyrapids.baking.ui.components.StepAppearance
import com.rubberdingyrapids.baking.ui.components.StepCard
import com.rubberdingyrapids.baking.ui.components.describe
import com.rubberdingyrapids.baking.ui.overview.PreheatCard
import com.rubberdingyrapids.baking.ui.overview.formatScale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookScreen(
    viewModel: CookViewModel,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recipe = state.recipe

    // Going back from the step flow returns to the checklist rather than leaving cook mode.
    BackHandler(enabled = state.phase == CookPhase.COOK && !state.isFinished) { viewModel.backToGather() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(recipe?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val subtitle = when {
                            state.phase == CookPhase.GATHER -> "Get everything ready"
                            state.isFinished -> "All done"
                            else -> "Step ${(state.currentIndex ?: 0) + 1} of ${state.items.size}"
                        } + if (state.scale != 1f) "  ·  ${formatScale(state.scale)}" else ""
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (state.phase == CookPhase.COOK && !state.isFinished) viewModel.backToGather() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            when {
                recipe == null -> Unit
                state.phase == CookPhase.GATHER && state.allIngredientsChecked -> BottomAction(
                    label = "Start cooking",
                    icon = Icons.Default.PlayArrow,
                    onClick = viewModel::startCooking,
                )
                state.phase == CookPhase.COOK && state.isFinished -> BottomAction(
                    label = "Finish",
                    icon = Icons.Default.Check,
                    onClick = { viewModel.finish(); onFinished() },
                )
                else -> Unit
            }
        },
    ) { padding ->
        when {
            !state.loaded -> LoadingBox(Modifier.padding(padding))
            recipe == null -> Column(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(icon = Icons.Outlined.Edit, title = "Recipe not found", body = "It may have been deleted.")
            }
            state.phase == CookPhase.GATHER -> GatherContent(state, viewModel, padding)
            else -> FlowContent(state, viewModel, padding)
        }
    }
}

@Composable
private fun BottomAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    BottomActionBar {
        BottomActionButton(label = label, icon = icon, onClick = onClick)
    }
}

// ---- phase 1: gather ingredients -------------------------------------------

@Composable
private fun GatherContent(state: CookUiState, viewModel: CookViewModel, padding: PaddingValues) {
    val ingredients = state.recipe?.ingredients ?: emptyList()
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        modifier = Modifier.fillMaxSize().padding(padding),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Tick each ingredient as you weigh it out.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (!state.allIngredientsChecked) {
                    TextButton(onClick = viewModel::checkAllIngredients) { Text("Tick all") }
                }
            }
            val done = ingredients.count { it.id in state.checkedIngredients }
            LinearProgressIndicator(
                progress = { if (ingredients.isEmpty()) 1f else done.toFloat() / ingredients.size },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
        if (ingredients.isEmpty()) {
            item {
                Text("This recipe has no ingredients listed. You can start cooking straight away.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(ingredients, key = { it.id }) { ingredient ->
            IngredientCheckRow(
                ingredient = ingredient,
                checked = ingredient.id in state.checkedIngredients,
                onToggle = { viewModel.toggleIngredient(ingredient.id) },
            )
        }
    }
}

@Composable
private fun IngredientCheckRow(ingredient: Ingredient, checked: Boolean, onToggle: () -> Unit) {
    Card(
        onClick = onToggle,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(Modifier.padding(end = 16.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Text(
                ingredient.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (checked) TextDecoration.LineThrough else null,
                color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                ingredient.quantity.format(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ---- phase 2: the step flow ----------------------------------------------

@Composable
private fun FlowContent(state: CookUiState, viewModel: CookViewModel, padding: PaddingValues) {
    // Keep the screen awake while cooking; hands are usually covered in flour.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // One shared clock so every countdown ticks together.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val hasTimers = state.timers.isNotEmpty()
    LaunchedEffect(hasTimers) {
        while (hasTimers) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun startTimerWithPermission(item: CookItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !TimerNotifications.canPost(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.startTimer(item)
    }

    val currentIndex = state.currentIndex
    val lastCompleted = state.completed.lastOrNull()

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        modifier = Modifier.fillMaxSize().padding(padding),
    ) {
        itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
            val completed = item.id in state.completed
            val isCurrent = index == currentIndex
            val appearance = when {
                completed -> StepAppearance.Completed
                isCurrent -> StepAppearance.Current
                else -> StepAppearance.Locked
            }
            Column(Modifier.animateContentSize()) {
                if (index > 0) FlowConnector(muted = !isCurrent && !completed)
                when (item) {
                    is CookItem.Preheat -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = completed,
                            enabled = isCurrent || (completed && item.id == lastCompleted),
                            onCheckedChange = { if (it) viewModel.complete(item.id) else viewModel.uncomplete(item.id) },
                        )
                        PreheatCard(item.temperature?.format(), muted = !isCurrent, modifier = Modifier.weight(1f))
                    }
                    is CookItem.StepItem -> {
                        val expanded = state.expandedStepId == item.id && isCurrent
                        StepCard(
                            step = item.analysis.step,
                            index = state.items.indexOf(item) - (if (state.items.firstOrNull() is CookItem.Preheat) 1 else 0),
                            inputs = item.analysis.inputs,
                            issues = emptyList(),
                            appearance = appearance,
                            onClick = if (isCurrent) ({ viewModel.toggleExpanded(item.id) }) else null,
                            showNote = expanded,
                            leading = {
                                Checkbox(
                                    checked = completed,
                                    enabled = isCurrent || (completed && item.id == lastCompleted),
                                    onCheckedChange = { if (it) viewModel.complete(item.id) else viewModel.uncomplete(item.id) },
                                )
                            },
                            extraContent = if (isCurrent) {
                                {
                                    if (expanded) StepDetails(item)
                                    val step = item.analysis.step
                                    if (step.action.hasDuration && (step.durationSeconds ?: 0) > 0) {
                                        TimerControls(
                                            durationSeconds = step.durationSeconds ?: 0,
                                            timer = state.timerFor(item),
                                            now = now,
                                            onStart = { startTimerWithPermission(item) },
                                            onCancel = { viewModel.cancelTimer(item) },
                                        )
                                    }
                                }
                            } else null,
                        )
                    }
                }
            }
        }
        if (state.isFinished) {
            item(key = "done") {
                Spacer(Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Every step is done. Enjoy!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepDetails(item: CookItem.StepItem) {
    val step = item.analysis.step
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        if (item.analysis.inputs.isNotEmpty()) {
            Text("You need", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            item.analysis.inputs.forEach { input ->
                Row(Modifier.padding(vertical = 2.dp)) {
                    Text("•  ", style = MaterialTheme.typography.bodyLarge)
                    Text(input.describe(), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (step.note.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(step.note, style = MaterialTheme.typography.bodyLarge)
        }
        if (item.analysis.inputs.isEmpty() && step.note.isBlank()) {
            Text("No extra details for this step.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TimerControls(
    durationSeconds: Int,
    timer: ActiveTimer?,
    now: Long,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    when {
        timer == null -> Button(onClick = onStart, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Outlined.Alarm, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start timer · ${TimeFormat.short(durationSeconds)}")
        }
        timer.isFinished(now) -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Text("Time's up!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
            OutlinedButton(onClick = onCancel) { Text("Dismiss") }
        }
        else -> {
            val remaining = timer.remainingSeconds(now)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        TimeFormat.clock(remaining),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = onCancel) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel")
                    }
                }
                LinearProgressIndicator(
                    progress = { 1f - remaining.toFloat() / timer.durationSeconds.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
        }
    }
}
