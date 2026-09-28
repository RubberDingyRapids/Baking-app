@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rubberdingyrapids.baking.ui.cook

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.rubberdingyrapids.baking.BakingApp
import com.rubberdingyrapids.baking.core.data.RecipeStore
import com.rubberdingyrapids.baking.core.flow.FlowEngine
import com.rubberdingyrapids.baking.core.flow.StepAnalysis
import com.rubberdingyrapids.baking.core.format.TimeFormat
import com.rubberdingyrapids.baking.core.model.Recipe
import com.rubberdingyrapids.baking.core.model.Temperature
import com.rubberdingyrapids.baking.timer.ActiveTimer
import com.rubberdingyrapids.baking.timer.TimerManager
import com.rubberdingyrapids.baking.ui.CookRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class CookPhase { GATHER, COOK }

/** One tile in the cook-mode flow chart: either the preheat reminder or a real step. */
sealed class CookItem(val id: String) {
    class Preheat(val temperature: Temperature?) : CookItem(PREHEAT_ID)
    class StepItem(val analysis: StepAnalysis) : CookItem(analysis.step.id)

    companion object {
        const val PREHEAT_ID = "preheat"
    }
}

data class CookUiState(
    val loaded: Boolean,
    val recipe: Recipe?,
    val scale: Float,
    val phase: CookPhase,
    val checkedIngredients: Set<String>,
    val completed: Set<String>,
    val expandedStepId: String?,
    val items: List<CookItem>,
    val timers: Map<String, ActiveTimer>,
) {
    val allIngredientsChecked: Boolean
        get() = recipe != null && recipe.ingredients.all { it.id in checkedIngredients }

    /** Index of the step the cook is on, or null when everything is done. */
    val currentIndex: Int?
        get() = items.indexOfFirst { it.id !in completed }.takeIf { it >= 0 }

    val isFinished: Boolean get() = items.isNotEmpty() && currentIndex == null

    fun timerFor(item: CookItem): ActiveTimer? = recipe?.let { timers[TimerManager.key(it.id, item.id)] }
}

/**
 * Drives cook mode: the gather-ingredients checklist, then the step flow.
 * Progress lives in the [SavedStateHandle] so rotating the phone or switching
 * apps never loses the cook's place; timers live in [TimerManager] so they
 * ring even if the process dies.
 */
class CookViewModel(
    private val store: RecipeStore,
    private val timers: TimerManager,
    private val handle: SavedStateHandle,
    val recipeId: String,
    private val scale: Float,
) : ViewModel() {

    private val phase = handle.getStateFlow(KEY_PHASE, CookPhase.GATHER.name)
    private val checked = handle.getStateFlow<ArrayList<String>>(KEY_CHECKED, arrayListOf())
    private val completed = handle.getStateFlow<ArrayList<String>>(KEY_COMPLETED, arrayListOf())
    private val expanded = handle.getStateFlow<String?>(KEY_EXPANDED, null)

    private val recipeFlow = combine(store.loaded, store.recipes) { loaded, recipes ->
        loaded to recipes.firstOrNull { it.id == recipeId }?.scaled(scale.toDouble())
    }

    val state: StateFlow<CookUiState> = combine(
        recipeFlow, phase, checked, completed, expanded, timers.timers,
    ) { values ->
        val header = values[0] as Pair<*, *>
        val loaded = header.first as Boolean
        val recipe = header.second as Recipe?
        val items = recipe?.let(::buildItems) ?: emptyList()
        CookUiState(
            loaded = loaded,
            recipe = recipe,
            scale = scale,
            phase = CookPhase.valueOf(values[1] as String),
            checkedIngredients = (values[2] as List<String>).toSet(),
            completed = (values[3] as List<String>).toSet(),
            expandedStepId = values[4] as String?,
            items = items,
            timers = values[5] as Map<String, ActiveTimer>,
        )
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000),
        CookUiState(false, null, scale, CookPhase.GATHER, emptySet(), emptySet(), null, emptyList(), emptyMap()),
    )

    private fun buildItems(recipe: Recipe): List<CookItem> {
        val analysis = FlowEngine.analyse(recipe)
        val preheatTemp = recipe.steps.firstOrNull { it.preheat }?.temperature
        val items = mutableListOf<CookItem>()
        if (recipe.steps.any { it.preheat }) items += CookItem.Preheat(preheatTemp)
        analysis.steps.forEach { items += CookItem.StepItem(it) }
        return items
    }

    // ---- gather phase ----------------------------------------------------

    fun toggleIngredient(id: String) {
        val current = checked.value
        handle[KEY_CHECKED] = ArrayList(if (id in current) current - id else current + id)
    }

    fun checkAllIngredients() {
        handle[KEY_CHECKED] = ArrayList(state.value.recipe?.ingredients?.map { it.id } ?: emptyList())
    }

    fun startCooking() {
        handle[KEY_PHASE] = CookPhase.COOK.name
    }

    fun backToGather() {
        handle[KEY_PHASE] = CookPhase.GATHER.name
    }

    // ---- cook phase ------------------------------------------------------

    fun toggleExpanded(id: String) {
        handle[KEY_EXPANDED] = if (expanded.value == id) null else id
    }

    fun complete(id: String) {
        if (id in completed.value) return
        handle[KEY_COMPLETED] = ArrayList(completed.value + id)
        handle[KEY_EXPANDED] = null
        timers.cancel(TimerManager.key(recipeId, id))
    }

    /** Only the most recently completed step can be unticked, so the order stays intact. */
    fun uncomplete(id: String) {
        if (completed.value.lastOrNull() != id) return
        handle[KEY_COMPLETED] = ArrayList(completed.value - id)
    }

    fun startTimer(item: CookItem) {
        val step = (item as? CookItem.StepItem)?.analysis?.step ?: return
        val seconds = step.durationSeconds ?: return
        val label = "${step.actionLabel}: ${step.outputName.ifBlank { step.actionLabel }} (${TimeFormat.short(seconds)})"
        timers.start(TimerManager.key(recipeId, step.id), label, seconds)
    }

    fun cancelTimer(item: CookItem) {
        timers.cancel(TimerManager.key(recipeId, item.id))
    }

    fun finish() {
        timers.cancelAll(recipeId)
    }

    companion object {
        private const val KEY_PHASE = "phase"
        private const val KEY_CHECKED = "checked"
        private const val KEY_COMPLETED = "completed"
        private const val KEY_EXPANDED = "expanded"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BakingApp
                val handle = createSavedStateHandle()
                val route = handle.toRoute<CookRoute>()
                CookViewModel(app.store, app.timers, handle, route.recipeId, route.scale)
            }
        }
    }
}
