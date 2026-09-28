@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rubberdingyrapids.baking.ui.overview

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubberdingyrapids.baking.core.flow.FlowEngine
import com.rubberdingyrapids.baking.core.flow.FlowLayoutEngine
import com.rubberdingyrapids.baking.core.model.Quantity
import com.rubberdingyrapids.baking.core.model.Recipe
import com.rubberdingyrapids.baking.share.ShareActions
import com.rubberdingyrapids.baking.ui.app
import com.rubberdingyrapids.baking.ui.components.BottomActionBar
import com.rubberdingyrapids.baking.ui.components.BottomActionButton
import com.rubberdingyrapids.baking.ui.components.EmptyState
import com.rubberdingyrapids.baking.ui.components.FlowChart
import com.rubberdingyrapids.baking.ui.components.FlowConnector
import com.rubberdingyrapids.baking.ui.components.LoadingBox
import com.rubberdingyrapids.baking.ui.components.SectionHeader
import com.rubberdingyrapids.baking.ui.components.StepCard
import com.rubberdingyrapids.baking.ui.components.TagChip
import kotlinx.coroutines.launch

private val scalePresets = listOf(0.5f, 1f, 1.5f, 2f, 3f)

fun formatScale(scale: Float): String = when (scale) {
    0.5f -> "½×"
    else -> "${Quantity.formatNumber(scale.toDouble())}×"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    recipeId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStart: (scale: Float) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val app = app()
    val store = app.store
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loaded by store.loaded.collectAsStateWithLifecycle()
    val recipes by store.recipes.collectAsStateWithLifecycle()
    val recipe = recipes.firstOrNull { it.id == recipeId }
    var scale by rememberSaveable { mutableStateOf(1f) }
    var shareMenu by remember { mutableStateOf(false) }
    var publish by remember { mutableStateOf<PublishState>(PublishState.Idle) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (recipe != null) {
                        IconButton(onClick = { shareMenu = true }) { Icon(Icons.Outlined.Share, contentDescription = "Share recipe") }
                        DropdownMenu(expanded = shareMenu, onDismissRequest = { shareMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Send recipe file") },
                                leadingIcon = { Icon(Icons.Outlined.AttachFile, contentDescription = null) },
                                onClick = { shareMenu = false; ShareActions.shareRecipeFile(context, recipe) },
                            )
                            DropdownMenuItem(
                                text = { Text("Send as text") },
                                leadingIcon = { Icon(Icons.Outlined.Notes, contentDescription = null) },
                                onClick = { shareMenu = false; ShareActions.shareRecipeText(context, recipe) },
                            )
                            DropdownMenuItem(
                                text = { Text("Share a link") },
                                leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                                onClick = {
                                    shareMenu = false
                                    publish = PublishState.Working
                                    scope.launch {
                                        publish = try {
                                            PublishState.Done(app.cloud.publish(recipe))
                                        } catch (e: Exception) {
                                            PublishState.Failed(e.message ?: "Couldn't publish the recipe.")
                                        }
                                    }
                                },
                            )
                        }
                        IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Edit recipe") }
                    }
                },
            )
        },
        bottomBar = {
            if (recipe != null) {
                BottomActionBar {
                    BottomActionButton(
                        label = "Start",
                        icon = Icons.Default.PlayArrow,
                        onClick = { onStart(scale) },
                        enabled = recipe.ingredients.isNotEmpty() || recipe.steps.isNotEmpty(),
                    )
                }
            }
        },
    ) { padding ->
        when {
            !loaded -> LoadingBox(Modifier.padding(padding))
            recipe == null -> Column(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(icon = Icons.Outlined.Edit, title = "Recipe not found", body = "It may have been deleted.")
            }
            else -> OverviewContent(recipe, scale, onScaleChange = { scale = it }, padding = padding)
        }
    }

    if (recipe != null) {
        PublishDialog(
            state = publish,
            onDismiss = { publish = PublishState.Idle },
            onShare = { link -> ShareActions.shareLink(context, recipe, link) },
            onCopy = { link ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Recipe link", link))
            },
            onOpenSettings = { publish = PublishState.Idle; onOpenSettings() },
        )
    }
}

private sealed class PublishState {
    object Idle : PublishState()
    object Working : PublishState()
    class Done(val link: String) : PublishState()
    class Failed(val message: String) : PublishState()
}

@Composable
private fun PublishDialog(
    state: PublishState,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit,
    onCopy: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    when (state) {
        PublishState.Idle -> Unit
        PublishState.Working -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Publishing…") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Uploading the recipe.")
                }
            },
            confirmButton = {},
        )
        is PublishState.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Couldn't share a link") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = onOpenSettings) { Text("Settings") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        )
        is PublishState.Done -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Link ready") },
            text = {
                Column {
                    Text("Anyone with Cookbook can import this recipe from:")
                    Spacer(Modifier.height(8.dp))
                    Text(state.link, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = { TextButton(onClick = { onShare(state.link) }) { Text("Share") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { onCopy(state.link) }) { Text("Copy") }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            },
        )
    }
}

@Composable
private fun OverviewContent(recipe: Recipe, scale: Float, onScaleChange: (Float) -> Unit, padding: PaddingValues) {
    val scaled = remember(recipe, scale) { recipe.scaled(scale.toDouble()) }
    val analysis = remember(scaled) { FlowEngine.analyse(scaled) }
    val layout = remember(scaled) { FlowLayoutEngine.layout(scaled) }
    val preheat = remember(scaled) { scaled.steps.firstOrNull { it.preheat }?.temperature }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        modifier = Modifier.fillMaxSize().padding(padding),
    ) {
        item {
            if (recipe.tag.isNotBlank()) {
                TagChip(recipe.tag)
                Spacer(Modifier.height(8.dp))
            }
            if (recipe.description.isNotBlank()) {
                Text(recipe.description, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
            }
        }

        item {
            SectionHeader("Scale")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                scalePresets.forEach { preset ->
                    FilterChip(selected = scale == preset, onClick = { onScaleChange(preset) }, label = { Text(formatScale(preset)) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                IconButton(onClick = { onScaleChange((scale - 0.5f).coerceAtLeast(0.5f)) }, enabled = scale > 0.5f) {
                    Icon(Icons.Default.Remove, contentDescription = "Scale down")
                }
                Text(formatScale(scale), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { onScaleChange((scale + 0.5f).coerceAtMost(10f)) }, enabled = scale < 10f) {
                    Icon(Icons.Default.Add, contentDescription = "Scale up")
                }
                if (scale != 1f) {
                    Text("amounts adjusted", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item { SectionHeader("Ingredients") }
        if (scaled.ingredients.isEmpty()) {
            item { Text("No ingredients yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(scaled.ingredients, key = { "ing-${it.id}" }) { ingredient ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(ingredient.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    ingredient.quantity.format(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        item { SectionHeader("Method") }
        if (scaled.steps.isEmpty()) {
            item { Text("No steps yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (scaled.steps.any { it.preheat }) {
            item(key = "preheat") {
                PreheatCard(preheat?.format())
                FlowConnector()
            }
        }
        if (scaled.steps.isNotEmpty()) {
            item(key = "chart") {
                FlowChart(analysis = analysis, layout = layout) { stepAnalysis, _, compact ->
                    StepCard(
                        step = stepAnalysis.step,
                        index = stepAnalysis.index,
                        inputs = stepAnalysis.inputs,
                        issues = stepAnalysis.issues,
                        compact = compact,
                    )
                }
            }
        }
    }
}

@Composable
fun PreheatCard(temperature: String?, modifier: Modifier = Modifier, muted: Boolean = false) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (muted) 0.4f else 1f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(10.dp))
            Text(
                if (temperature != null) "Preheat the oven to $temperature" else "Preheat the oven",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}
