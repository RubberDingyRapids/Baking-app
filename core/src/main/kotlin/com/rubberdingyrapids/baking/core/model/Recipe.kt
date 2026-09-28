package com.rubberdingyrapids.baking.core.model

import com.rubberdingyrapids.baking.core.Ids
import kotlinx.serialization.Serializable

@Serializable
data class Ingredient(
    val id: String = Ids.next(),
    val name: String,
    val quantity: Quantity,
)

/** The verbs the method screen offers. CUSTOM carries its own label. */
@Serializable
enum class ActionType(val label: String) {
    MIX("Mix"),
    WHISK("Whisk"),
    MELT("Melt"),
    ADD("Add"),
    BAKE("Bake"),
    WAIT("Wait"),
    CUSTOM("Custom");

    /** Wait and custom steps may stand alone ("rest 10 minutes"); everything else needs ingredients. */
    val requiresInputs: Boolean get() = this != WAIT && this != CUSTOM
    val hasDuration: Boolean get() = this == BAKE || this == WAIT
    val hasTemperature: Boolean get() = this == BAKE
}

/**
 * One item consumed by a step. With neither [amount] nor [fraction] set the
 * whole remaining item is used. [amount] is an absolute quantity (only valid
 * when the item has a known quantity); [fraction] is a share of the item's
 * original quantity, e.g. 0.5 for half.
 */
@Serializable
data class StepInput(
    val itemId: String,
    val amount: Quantity? = null,
    val fraction: Double? = null,
) {
    val isPartial: Boolean get() = amount != null || fraction != null
}

@Serializable
data class Step(
    val id: String = Ids.next(),
    val action: ActionType,
    /** Label for CUSTOM actions, e.g. "Knead" or "Fold". Ignored otherwise. */
    val customLabel: String = "",
    val inputs: List<StepInput> = emptyList(),
    /** Name of the item this step produces. Auto generated but editable. */
    val outputName: String = "",
    /** Free text such as "until pale and fluffy". */
    val note: String = "",
    /** Bake or wait duration. */
    val durationSeconds: Int? = null,
    /** Oven temperature for bake steps. */
    val temperature: Temperature? = null,
    /** For bake steps: also remind the cook to preheat the oven earlier. */
    val preheat: Boolean = false,
) {
    /** Text shown on the step tile, e.g. "Mix" or "Knead". */
    val actionLabel: String
        get() = if (action == ActionType.CUSTOM) customLabel.ifBlank { "Step" } else action.label
}

@Serializable
data class Recipe(
    val id: String = Ids.next(),
    val name: String,
    val description: String = "",
    val tag: String = "",
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<Step> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
) {
    /** Returns a copy with every quantity multiplied by [factor]. */
    fun scaled(factor: Double): Recipe {
        if (factor == 1.0) return this
        return copy(
            ingredients = ingredients.map { it.copy(quantity = it.quantity.scaled(factor)) },
            steps = steps.map { step ->
                step.copy(inputs = step.inputs.map { input ->
                    input.copy(amount = input.amount?.scaled(factor))
                })
            },
        )
    }
}

/**
 * Root of the on-disk JSON file. Versioned so the format can evolve and so a
 * single [Recipe] (or a whole library) can later be exported for sharing.
 */
@Serializable
data class RecipeLibrary(
    val version: Int = CURRENT_VERSION,
    val recipes: List<Recipe> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
