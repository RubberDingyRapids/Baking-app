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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import com.rubberdingyrapids.baking.core.flow.StepAnalysis
import com.rubberdingyrapids.baking.core.format.TimeFormat
import com.rubberdingyrapids.baking.core.model.Ingredient
import com.rubberdingyrapids.baking.timer.ActiveTimer
import com.rubberdingyrapids.baking.timer.TimerNotifications
import com.rubberdingyrapids.baking.ui.components.BottomActionBar
import com.rubberdingyrapids.baking.ui.components.BottomActionButton
import com.rubberdingyrapids.baking.ui.components.EmptyState
import com.rubberdingyrapids.baking.ui.components.FlowChart
import com.rubberdingyrapids.baking.ui.components.FlowConnector
import com.rubberdingyrapids.baking.ui.components.LoadingBox
import com.rubberdingyrapids.baking.ui.components.StepAppearance
import com.rubberdingyrapids.baking.ui.components.StepCard
import com.rubberdingyrapids.baking.ui.overview.PreheatCard
import com.rubberdingyrapids.baking.ui.overview.formatScale
import kotlinx.coroutines.delay

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
                            else -> "${state.doneCount} of ${state.totalCount} done"
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
                state.phase == CookPhase.GATHER && state.allIngredientsChecked -> BottomActionBar {
                    BottomActionButton(label = "Start cooking", icon = Icons.Default.PlayArrow, onClick = viewModel::startCooking)
                }
                state.phase == CookPhase.COOK && state.isFinished -> BottomActionBar {
                    BottomActionButton(label = "Finish", icon = Icons.Default.Check, onClick = { viewModel.finish(); onFinished() })
                }
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
                    "Tick each ingredient as you get it out.",
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
    val plan = state.plan ?: return

    // Keep the screen awake while cooking; hands are usually busy.
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
    fun startTimerWithPermission(stepId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !TimerNotifications.canPost(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.startTimer(stepId)
    }

    val active = state.activeIds

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (active.size > 1) {
            Text(
                "${active.size} things can happen now. Tick each one when it's done.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (plan.hasPreheat) {
            val id = CookPlan.PREHEAT_ID
            val done = id in state.completed
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = done,
                    enabled = id in active || state.canUncomplete(id),
                    onCheckedChange = { if (it) viewModel.complete(id) else viewModel.uncomplete(id) },
                )
                PreheatCard(plan.preheat?.format(), muted = done, modifier = Modifier.weight(1f))
            }
            FlowConnector(muted = done)
        }

        FlowChart(
            analysis = plan.analysis,
            layout = plan.layout,
            mutedLane = { node -> node.stepId in state.completed },
        ) { stepAnalysis, _, compact ->
            CookStepCard(
                stepAnalysis = stepAnalysis,
                state = state,
                isActive = stepAnalysis.step.id in active,
                compact = compact,
                now = now,
                onToggleExpanded = { viewModel.toggleExpanded(stepAnalysis.step.id) },
                onComplete = { viewModel.complete(stepAnalysis.step.id) },
                onUncomplete = { viewModel.uncomplete(stepAnalysis.step.id) },
                onStartTimer = { startTimerWithPermission(stepAnalysis.step.id) },
                onCancelTimer = { viewModel.cancelTimer(stepAnalysis.step.id) },
            )
        }

        if (state.isFinished) {
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
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CookStepCard(
    stepAnalysis: StepAnalysis,
    state: CookUiState,
    isActive: Boolean,
    compact: Boolean,
    now: Long,
    onToggleExpanded: () -> Unit,
    onComplete: () -> Unit,
    onUncomplete: () -> Unit,
    onStartTimer: () -> Unit,
    onCancelTimer: () -> Unit,
) {
    val step = stepAnalysis.step
    val completed = step.id in state.completed
    val appearance = when {
        completed -> StepAppearance.Completed
        isActive -> StepAppearance.Current
        else -> StepAppearance.Locked
    }
    val expanded = state.expandedStepId == step.id && isActive
    StepCard(
        step = step,
        index = stepAnalysis.index,
        inputs = stepAnalysis.inputs,
        issues = emptyList(),
        appearance = appearance,
        compact = compact,
        onClick = if (isActive) onToggleExpanded else null,
        showNote = expanded,
        leading = {
            Checkbox(
                checked = completed,
                enabled = isActive || state.canUncomplete(step.id),
                onCheckedChange = { if (it) onComplete() else onUncomplete() },
                modifier = if (compact) Modifier.padding(0.dp) else Modifier,
            )
        },
        extraContent = if (isActive) {
            {
                if (expanded) StepDetails(stepAnalysis)
                val seconds = step.durationSeconds ?: 0
                if (step.action.hasDuration && seconds > 0) {
                    TimerControls(
                        durationSeconds = seconds,
                        timer = state.timerFor(step.id),
                        now = now,
                        compact = compact,
                        onStart = onStartTimer,
                        onCancel = onCancelTimer,
                    )
                }
            }
        } else null,
    )
}

@Composable
private fun StepDetails(item: StepAnalysis) {
    val step = item.step
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        if (item.inputs.isNotEmpty()) {
            Text("You need", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            item.inputs.forEach { input ->
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
        if (item.inputs.isEmpty() && step.note.isBlank()) {
            Text("No extra details for this step.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TimerControls(
    durationSeconds: Int,
    timer: ActiveTimer?,
    now: Long,
    compact: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    when {
        timer == null -> Button(onClick = onStart, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Outlined.Alarm, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (compact) TimeFormat.short(durationSeconds) else "Start timer · ${TimeFormat.short(durationSeconds)}")
        }
        timer.isFinished(now) -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(6.dp))
                Text("Time's up!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
            }
            OutlinedButton(onClick = onCancel) { Text("Dismiss") }
        }
        else -> {
            val remaining = timer.remainingSeconds(now)
            Column {
                Text(
                    TimeFormat.clock(remaining),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 24.sp else 34.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                LinearProgressIndicator(
                    progress = { 1f - remaining.toFloat() / timer.durationSeconds.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
                OutlinedButton(onClick = onCancel, modifier = Modifier.padding(top = 6.dp)) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Cancel")
                }
            }
        }
    }
}
