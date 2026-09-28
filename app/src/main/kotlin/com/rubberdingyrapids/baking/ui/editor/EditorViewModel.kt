@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rubberdingyrapids.baking.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.rubberdingyrapids.baking.BakingApp
import com.rubberdingyrapids.baking.core.data.RecipeJson
import com.rubberdingyrapids.baking.core.data.RecipeStore
import com.rubberdingyrapids.baking.core.flow.FlowAnalysis
import com.rubberdingyrapids.baking.core.flow.FlowEngine
import com.rubberdingyrapids.baking.core.flow.FlowItem
import com.rubberdingyrapids.baking.core.model.Ingredient
import com.rubberdingyrapids.baking.core.model.Recipe
import com.rubberdingyrapids.baking.core.model.Step
import com.rubberdingyrapids.baking.ui.EditorRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds the recipe being created or edited. The draft is mirrored into the
 * [SavedStateHandle] as JSON so an unfinished recipe survives process death.
 */
class EditorViewModel(
    private val store: RecipeStore,
    private val handle: SavedStateHandle,
    recipeId: String?,
) : ViewModel() {

    private val original: Recipe? = recipeId?.let { store.get(it) }
    val isNew: Boolean = original == null

    private val _draft = MutableStateFlow(
        handle.get<String>(KEY_DRAFT)?.let { runCatching { RecipeJson.decodeRecipe(it) }.getOrNull() }
            ?: original
            ?: Recipe(id = recipeId ?: com.rubberdingyrapids.baking.core.Ids.next(), name = ""),
    )
    val draft: StateFlow<Recipe> = _draft.asStateFlow()

    /** Tags used by other recipes, offered as suggestions. */
    val existingTags: StateFlow<List<String>> = store.recipes
        .map { list -> list.map { it.tag.trim() }.filter { it.isNotBlank() }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isDirty: Boolean
        get() = when (original) {
            null -> _draft.value.name.isNotBlank() || _draft.value.ingredients.isNotEmpty() || _draft.value.steps.isNotEmpty()
            else -> _draft.value != original
        }

    val canSave: Boolean get() = _draft.value.name.isNotBlank()

    private fun update(transform: (Recipe) -> Recipe) {
        val next = transform(_draft.value)
        _draft.value = next
        handle[KEY_DRAFT] = RecipeJson.encodeRecipe(next)
    }

    fun setName(name: String) = update { it.copy(name = name) }
    fun setDescription(description: String) = update { it.copy(description = description) }
    fun setTag(tag: String) = update { it.copy(tag = tag) }

    // ---- ingredients ----------------------------------------------------

    fun ingredient(id: String?): Ingredient? = id?.let { wanted -> _draft.value.ingredients.firstOrNull { it.id == wanted } }

    fun upsertIngredient(ingredient: Ingredient) = update { recipe ->
        val existing = recipe.ingredients.indexOfFirst { it.id == ingredient.id }
        val list = recipe.ingredients.toMutableList()
        if (existing >= 0) list[existing] = ingredient else list += ingredient
        recipe.copy(ingredients = list)
    }

    /** Removes the ingredient and any step inputs that used it. */
    fun removeIngredient(id: String) = update { recipe ->
        recipe.copy(
            ingredients = recipe.ingredients.filterNot { it.id == id },
            steps = recipe.steps.map { step -> step.copy(inputs = step.inputs.filterNot { it.itemId == id }) },
        )
    }

    // ---- method ----------------------------------------------------------

    fun addStep(step: Step) = update { FlowEngine.normalise(it.copy(steps = it.steps + step)) }

    fun updateStep(step: Step) = update { recipe ->
        FlowEngine.normalise(recipe.copy(steps = recipe.steps.map { if (it.id == step.id) step else it }))
    }

    fun deleteStep(stepId: String) = update { FlowEngine.deleteStep(it, stepId) }

    /** Items a step may use: everything not downstream of it. Null means a brand new step. */
    fun editableItems(stepId: String?): List<FlowItem> = FlowEngine.editableItems(_draft.value, stepId)

    fun analysis(): FlowAnalysis = FlowEngine.analyse(_draft.value)

    // ---- persistence -----------------------------------------------------

    fun save(onSaved: () -> Unit) {
        if (!canSave) return
        val recipe = _draft.value.copy(
            name = _draft.value.name.trim(),
            description = _draft.value.description.trim(),
            tag = _draft.value.tag.trim(),
        )
        viewModelScope.launch {
            store.save(recipe)
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = original?.id ?: return onDeleted()
        viewModelScope.launch {
            store.delete(id)
            onDeleted()
        }
    }

    companion object {
        private const val KEY_DRAFT = "draft"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BakingApp
                val handle = createSavedStateHandle()
                val recipeId = runCatching { handle.toRoute<EditorRoute>().recipeId }.getOrNull()
                    ?: handle.get<String>("recipeId")
                EditorViewModel(app.store, handle, recipeId)
            }
        }
    }
}
