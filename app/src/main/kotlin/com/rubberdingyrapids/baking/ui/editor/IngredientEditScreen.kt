@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rubberdingyrapids.baking.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardTab
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubberdingyrapids.baking.core.data.IngredientSuggestions
import com.rubberdingyrapids.baking.core.model.Ingredient
import com.rubberdingyrapids.baking.core.model.MeasureUnit
import com.rubberdingyrapids.baking.core.model.Quantity
import com.rubberdingyrapids.baking.ui.components.QuantityField

/**
 * Add or edit one ingredient. The name field completes common ingredients
 * inline: the untyped remainder is drawn greyed out and accepted with the
 * arrow button or the keyboard's Next key.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientEditScreen(
    viewModel: EditorViewModel,
    ingredientId: String?,
    onDone: () -> Unit,
) {
    val existing = remember(ingredientId) { viewModel.ingredient(ingredientId) }
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val id = rememberSaveable { existing?.id ?: com.rubberdingyrapids.baking.core.Ids.next() }

    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var amountText by rememberSaveable { mutableStateOf(existing?.quantity?.let { Quantity.formatNumber(it.amount) } ?: "") }
    var unit by rememberSaveable { mutableStateOf(existing?.quantity?.unit ?: MeasureUnit.DEFAULT) }

    // Names already in this recipe are offered first so spelling stays consistent.
    val knownNames = remember(draft.ingredients) { draft.ingredients.map { it.name } }
    val completion = remember(name, knownNames) { IngredientSuggestions.complete(name, knownNames) }
    val suffix = completion?.drop(name.trimStart().length).orEmpty()
    val suggestions = remember(name, knownNames) { IngredientSuggestions.suggest(name, knownNames, limit = 5) }

    val amountFocus = remember { FocusRequester() }
    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (existing == null) nameFocus.requestFocus() }

    fun accept() {
        if (suffix.isNotEmpty()) name = name.trimStart() + suffix
    }

    val amount = amountText.replace(',', '.').toDoubleOrNull()
    val valid = name.isNotBlank() && amount != null && amount > 0

    fun submit() {
        if (!valid) return
        viewModel.upsertIngredient(Ingredient(id = id, name = name.trim(), quantity = Quantity(amount!!, unit)))
        onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Add ingredient" else "Edit ingredient") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Default.Close, contentDescription = "Cancel") }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = ::submit,
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (existing == null) "Add" else "Save")
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Ingredient") },
                singleLine = true,
                visualTransformation = CompletionTransformation(suffix, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                trailingIcon = {
                    if (suffix.isNotEmpty()) {
                        IconButton(onClick = { accept() }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardTab, contentDescription = "Accept suggestion")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { accept(); amountFocus.requestFocus() }),
                modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
            )
            if (suggestions.isNotEmpty() && suggestions != listOf(name.trim().lowercase())) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions) { s ->
                        SuggestionChip(onClick = { name = s; amountFocus.requestFocus() }, label = { Text(s) })
                    }
                }
            }
            QuantityField(
                amountText = amountText,
                unit = unit,
                onAmountChange = { amountText = it },
                onUnitChange = { unit = it },
                modifier = Modifier.fillMaxWidth(),
                amountFieldModifier = Modifier.focusRequester(amountFocus),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Tip: tap the unit to switch between grams, ounces, cups and more. Amounts convert automatically where they can.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Appends [suffix] in [color] after the typed text without making it part of the value. */
private class CompletionTransformation(private val suffix: String, private val color: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (suffix.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val out = buildAnnotatedString {
            append(text)
            withStyle(SpanStyle(color = color)) { append(suffix) }
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset.coerceIn(0, text.length)
            override fun transformedToOriginal(offset: Int): Int = offset.coerceIn(0, text.length)
        }
        return TransformedText(out, mapping)
    }
}
