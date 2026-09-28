package com.rubberdingyrapids.baking.core.flow

import com.rubberdingyrapids.baking.core.model.ActionType
import com.rubberdingyrapids.baking.core.model.Quantity
import com.rubberdingyrapids.baking.core.model.Recipe
import com.rubberdingyrapids.baking.core.model.Step
import com.rubberdingyrapids.baking.core.model.StepInput
import com.rubberdingyrapids.baking.core.model.UnitKind

/**
 * Something that can be picked as an input to a step: either a raw ingredient
 * or the output of an earlier step (an "intermediate", e.g. "flour + sugar mix").
 */
data class FlowItem(
    val id: String,
    val name: String,
    /** Original quantity, or null when it cannot be computed (mixed kinds). */
    val quantity: Quantity?,
    /** What is still unused, in the same unit as [quantity]. */
    val remaining: Quantity?,
    /** Share of the original still unused, 0..1. Always tracked. */
    val remainingFraction: Double,
    val isIntermediate: Boolean,
) {
    val isAvailable: Boolean
        get() = remaining?.isPositive() ?: (remainingFraction > Quantity.EPSILON)

    /** True when only part of the item has been used so far. */
    val isPartiallyUsed: Boolean
        get() = remainingFraction < 1.0 - Quantity.EPSILON && isAvailable
}

/** What one input actually consumed once resolved against the flow. */
data class ResolvedInput(
    val input: StepInput,
    val itemName: String,
    val consumed: Quantity?,
    /** Share of the item consumed by this step, 0..1. */
    val consumedFraction: Double,
    val wholeItem: Boolean,
)

sealed class FlowIssue(val message: String) {
    class MissingItem(val itemId: String) :
        FlowIssue("Uses an item that is not available at this point")
    class NothingLeft(val itemName: String) :
        FlowIssue("$itemName has already been used up")
    class NoInputs : FlowIssue("Select at least one ingredient")
}

data class StepAnalysis(
    val step: Step,
    val index: Int,
    val inputs: List<ResolvedInput>,
    /** The item this step produces. */
    val output: FlowItem,
    val issues: List<FlowIssue>,
) {
    val isValid: Boolean get() = issues.isEmpty()
}

data class FlowAnalysis(
    val steps: List<StepAnalysis>,
    /** Items still unused after the final step. */
    val leftovers: List<FlowItem>,
) {
    val isValid: Boolean get() = steps.all { it.isValid }
}

/**
 * Walks a recipe's steps in order, tracking which items exist and how much of
 * each is left. This is the heart of the flow-chart method editor.
 */
object FlowEngine {

    /** Items the user may pick from when editing the step at [stepIndex]. */
    fun availableItems(recipe: Recipe, stepIndex: Int): List<FlowItem> {
        val state = initialState(recipe)
        recipe.steps.take(stepIndex.coerceIn(0, recipe.steps.size)).forEach { step ->
            applyStep(step, state)
        }
        return state.values.filter { it.isAvailable }
    }

    fun analyse(recipe: Recipe): FlowAnalysis {
        val state = initialState(recipe)
        val analyses = recipe.steps.mapIndexed { index, step -> applyStep(step, state, index) }
        return FlowAnalysis(analyses, state.values.filter { it.isAvailable })
    }

    /** The name that will be suggested for a step's output given its inputs. */
    fun suggestOutputName(step: Step, inputNames: List<String>): String {
        val names = summarise(inputNames)
        if (inputNames.isEmpty()) return step.actionLabel.lowercase()
        return when (step.action) {
            ActionType.MIX -> if (inputNames.size == 1) "mixed $names" else "$names mix"
            ActionType.ADD -> names
            ActionType.CUSTOM -> "${step.customLabel.trim().lowercase().ifBlank { "prepared" }} $names"
            else -> "${step.action.pastTense} $names"
        }
    }

    /** Removes [stepId] and drops any later inputs that referenced its output. */
    fun deleteStep(recipe: Recipe, stepId: String): Recipe {
        val remaining = recipe.steps.filterNot { it.id == stepId }.map { step ->
            step.copy(inputs = step.inputs.filterNot { it.itemId == stepId })
        }
        return recipe.copy(steps = remaining)
    }

    fun moveStep(recipe: Recipe, fromIndex: Int, toIndex: Int): Recipe {
        if (fromIndex == toIndex || fromIndex !in recipe.steps.indices || toIndex !in recipe.steps.indices) return recipe
        val steps = recipe.steps.toMutableList()
        val step = steps.removeAt(fromIndex)
        steps.add(toIndex, step)
        return recipe.copy(steps = steps)
    }

    /** Names for the raw ingredients and every step output, by item id. */
    fun itemNames(recipe: Recipe): Map<String, String> {
        val names = recipe.ingredients.associate { it.id to it.name }.toMutableMap()
        recipe.steps.forEach { step ->
            names[step.id] = step.outputName.ifBlank {
                suggestOutputName(step, step.inputs.mapNotNull { names[it.itemId] })
            }
        }
        return names
    }

    // ---- branches ---------------------------------------------------------

    /** Ids of the steps whose output each step consumes. Raw ingredients are not steps and are ignored. */
    fun dependencies(recipe: Recipe): Map<String, Set<String>> {
        val stepIds = recipe.steps.map { it.id }.toSet()
        return recipe.steps.associate { step ->
            step.id to step.inputs.map { it.itemId }.filter { it in stepIds }.toSet()
        }
    }

    /** Every step that (transitively) consumes the output of [stepId]. */
    fun dependents(recipe: Recipe, stepId: String): Set<String> {
        val deps = dependencies(recipe)
        val result = mutableSetOf<String>()
        var frontier = setOf(stepId)
        while (frontier.isNotEmpty()) {
            val next = deps.filter { (id, d) -> id !in result && d.any { it in frontier } }.keys
            result += next
            frontier = next
        }
        return result
    }

    /**
     * Reorders steps so every step comes after the steps it consumes from,
     * keeping the existing order otherwise. Editing a step may make it use a
     * later step's output, and the sequential analysis relies on this order.
     */
    fun normalise(recipe: Recipe): Recipe {
        val deps = dependencies(recipe)
        val remaining = recipe.steps.toMutableList()
        val ordered = mutableListOf<Step>()
        val placed = mutableSetOf<String>()
        while (remaining.isNotEmpty()) {
            val next = remaining.firstOrNull { step -> deps.getValue(step.id).all { it in placed } }
                ?: remaining.first() // a cycle; keep going rather than hang
            remaining.remove(next)
            ordered += next
            placed += next.id
        }
        return if (ordered.map { it.id } == recipe.steps.map { it.id }) recipe else recipe.copy(steps = ordered)
    }

    /**
     * Items a step may use while being edited: everything left after all the
     * other steps, ignoring the step itself and anything downstream of it
     * (which would create a loop). Pass null for a brand new step.
     */
    fun editableItems(recipe: Recipe, stepId: String?): List<FlowItem> {
        if (stepId == null) return analyse(recipe).leftovers
        val excluded = dependents(recipe, stepId) + stepId
        val without = recipe.copy(steps = recipe.steps.filterNot { it.id in excluded })
        return analyse(without).leftovers
    }

    // ---- internals ------------------------------------------------------

    private fun initialState(recipe: Recipe): LinkedHashMap<String, FlowItem> {
        val state = LinkedHashMap<String, FlowItem>()
        recipe.ingredients.forEach { ing ->
            state[ing.id] = FlowItem(
                id = ing.id,
                name = ing.name,
                quantity = ing.quantity,
                remaining = ing.quantity,
                remainingFraction = 1.0,
                isIntermediate = false,
            )
        }
        return state
    }

    private fun applyStep(step: Step, state: LinkedHashMap<String, FlowItem>, index: Int = 0): StepAnalysis {
        val issues = mutableListOf<FlowIssue>()
        val resolved = mutableListOf<ResolvedInput>()
        if (step.inputs.isEmpty() && step.action.requiresInputs) issues += FlowIssue.NoInputs()

        step.inputs.forEach { input ->
            val item = state[input.itemId]
            if (item == null) {
                issues += FlowIssue.MissingItem(input.itemId)
                return@forEach
            }
            if (!item.isAvailable) {
                issues += FlowIssue.NothingLeft(item.name)
                return@forEach
            }
            val consumed: Quantity?
            val fraction: Double
            when {
                input.amount != null && item.quantity != null && item.remaining != null -> {
                    val wanted = input.amount.convertTo(item.remaining.unit)
                    if (wanted == null) {
                        // Incompatible unit for this item; treat as whole remainder.
                        consumed = item.remaining
                        fraction = item.remainingFraction
                    } else {
                        val clipped = if (wanted.amount > item.remaining.amount) item.remaining else wanted
                        consumed = clipped
                        fraction = if (item.quantity.amount > 0) clipped.amount / item.quantity.amount else item.remainingFraction
                    }
                }
                input.fraction != null -> {
                    fraction = input.fraction.coerceIn(0.0, item.remainingFraction)
                    consumed = item.quantity?.scaled(fraction)
                }
                else -> {
                    consumed = item.remaining
                    fraction = item.remainingFraction
                }
            }
            val wholeItem = fraction >= item.remainingFraction - Quantity.EPSILON
            resolved += ResolvedInput(input, item.name, consumed, fraction, wholeItem)
            state[item.id] = item.copy(
                remaining = if (item.remaining != null && consumed != null) item.remaining - consumed else null,
                remainingFraction = (item.remainingFraction - fraction).coerceAtLeast(0.0),
            )
        }

        val outputQuantity = combine(resolved.mapNotNull { it.consumed })
        val output = FlowItem(
            id = step.id,
            name = step.outputName.ifBlank { suggestOutputName(step, resolved.map { it.itemName }) },
            quantity = outputQuantity,
            remaining = outputQuantity,
            remainingFraction = 1.0,
            isIntermediate = true,
        )
        state[step.id] = output
        return StepAnalysis(step, index, resolved, output, issues)
    }

    /** Sums quantities when they share a kind, otherwise gives up (null). */
    private fun combine(quantities: List<Quantity>): Quantity? {
        if (quantities.isEmpty()) return null
        val kind = quantities.first().kind
        if (quantities.any { it.kind != kind }) return null
        val base = quantities.sumOf { it.baseAmount }
        val unit = when (kind) {
            UnitKind.MASS -> quantities.first().unit
            UnitKind.VOLUME -> quantities.first().unit
            UnitKind.COUNT -> quantities.first().unit
        }
        return Quantity(base / unit.toBase, unit)
    }

    private fun summarise(names: List<String>): String = when {
        names.isEmpty() -> ""
        names.size <= 3 -> names.joinToString(" + ")
        else -> "${names[0]}, ${names[1]} + ${names.size - 2} more"
    }
}
