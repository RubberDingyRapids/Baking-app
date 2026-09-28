@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rubberdingyrapids.baking.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubberdingyrapids.baking.ui.components.ConfirmDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeFormScreen(
    viewModel: EditorViewModel,
    onOpenIngredients: () -> Unit,
    onOpenMethod: () -> Unit,
    onClose: () -> Unit,
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val tags by viewModel.existingTags.collectAsStateWithLifecycle()
    var showDiscard by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    val attemptClose: () -> Unit = { if (viewModel.isDirty) { showDiscard = true } else { onClose() } }
    BackHandler(onBack = attemptClose)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "New recipe" else "Edit recipe") },
                navigationIcon = {
                    IconButton(onClick = attemptClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
                },
                actions = {
                    if (!viewModel.isNew) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete recipe")
                        }
                    }
                    TextButton(onClick = { viewModel.save(onClose) }, enabled = viewModel.canSave) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = viewModel::setName,
                label = { Text("Recipe name") },
                singleLine = true,
                isError = draft.name.isBlank() && viewModel.isDirty,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draft.description,
                onValueChange = viewModel::setDescription,
                label = { Text("Description") },
                minLines = 2,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draft.tag,
                onValueChange = viewModel::setTag,
                label = { Text("Tag") },
                placeholder = { Text("e.g. Bread, Cake, Dinner") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            val suggestions = tags.filter { it != draft.tag.trim() }
            if (suggestions.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions) { tag ->
                        SuggestionChip(onClick = { viewModel.setTag(tag) }, label = { Text(tag) })
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            SectionButton(
                icon = Icons.Outlined.Egg,
                title = "Ingredients",
                subtitle = when (draft.ingredients.size) {
                    0 -> "None added yet"
                    1 -> "1 ingredient"
                    else -> "${draft.ingredients.size} ingredients"
                },
                onClick = onOpenIngredients,
            )
            SectionButton(
                icon = Icons.Outlined.FormatListNumbered,
                title = "Method",
                subtitle = when (draft.steps.size) {
                    0 -> "No steps yet"
                    1 -> "1 step"
                    else -> "${draft.steps.size} steps"
                },
                onClick = onOpenMethod,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDiscard) {
        ConfirmDialog(
            title = "Discard changes?",
            body = "Your edits to this recipe will be lost.",
            confirmLabel = "Discard",
            destructive = true,
            onConfirm = { showDiscard = false; onClose() },
            onDismiss = { showDiscard = false },
        )
    }
    if (showDelete) {
        ConfirmDialog(
            title = "Delete \"${draft.name.ifBlank { "this recipe" }}\"?",
            body = "This cannot be undone.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { showDelete = false; viewModel.delete(onClose) },
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
private fun SectionButton(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
