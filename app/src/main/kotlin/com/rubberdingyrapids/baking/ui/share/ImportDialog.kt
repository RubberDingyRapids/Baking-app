@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rubberdingyrapids.baking.ui.share

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubberdingyrapids.baking.share.ImportCoordinator
import com.rubberdingyrapids.baking.ui.app

/** Shows whatever the [ImportCoordinator] is holding: progress, a failure, or recipes to confirm. */
@Composable
fun ImportDialogHost() {
    val app = app()
    val imports = app.imports
    val state by imports.state.collectAsStateWithLifecycle()
    val existing by app.store.recipes.collectAsStateWithLifecycle()

    when (val s = state) {
        ImportCoordinator.State.Idle -> Unit
        is ImportCoordinator.State.Loading -> AlertDialog(
            onDismissRequest = imports::dismiss,
            title = { Text("Fetching recipe…") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Reading the ${s.source}.")
                }
            },
            confirmButton = { TextButton(onClick = imports::dismiss) { Text("Cancel") } },
        )
        is ImportCoordinator.State.Failed -> AlertDialog(
            onDismissRequest = imports::dismiss,
            title = { Text("Couldn't import") },
            text = { Text(s.message) },
            confirmButton = { TextButton(onClick = imports::dismiss) { Text("OK") } },
        )
        is ImportCoordinator.State.Ready -> {
            val existingIds = existing.map { it.id }.toSet()
            AlertDialog(
                onDismissRequest = imports::dismiss,
                title = { Text(if (s.recipes.size == 1) "Import recipe?" else "Import ${s.recipes.size} recipes?") },
                text = {
                    Column {
                        s.recipes.forEach { recipe ->
                            val replaces = recipe.id in existingIds
                            Text(
                                text = "• ${recipe.name}" + if (replaces) "  (replaces your copy)" else "",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                },
                confirmButton = { TextButton(onClick = imports::confirm) { Text("Import") } },
                dismissButton = { TextButton(onClick = imports::dismiss) { Text("Cancel") } },
            )
        }
    }
}

/** Asks for a link (or bin id) and hands it to the importer. Pre-filled from the clipboard when it looks like a link. */
@Composable
fun ImportLinkDialog(initial: String, onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var link by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import from link") },
        text = {
            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("Link or id") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onImport(link) }, enabled = link.isNotBlank()) { Text("Fetch") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
