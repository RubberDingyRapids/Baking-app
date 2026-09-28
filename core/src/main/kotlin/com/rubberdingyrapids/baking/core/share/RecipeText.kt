package com.rubberdingyrapids.baking.core.share

import com.rubberdingyrapids.baking.core.flow.FlowEngine
import com.rubberdingyrapids.baking.core.format.TimeFormat
import com.rubberdingyrapids.baking.core.model.Recipe

/** Renders a recipe as plain text for people who don't have the app. */
object RecipeText {
    fun render(recipe: Recipe, footer: String? = "Sent from Cookbook"): String {
        val analysis = FlowEngine.analyse(recipe)
        return buildString {
            appendLine(recipe.name.trim().ifBlank { "Recipe" })
            if (recipe.description.isNotBlank()) appendLine(recipe.description.trim())
            if (recipe.tag.isNotBlank()) appendLine("Tag: ${recipe.tag.trim()}")
            appendLine()
            appendLine("Ingredients")
            if (recipe.ingredients.isEmpty()) appendLine("- none listed")
            recipe.ingredients.forEach { appendLine("- ${it.name}: ${it.quantity.format()}") }
            appendLine()
            appendLine("Method")
            if (analysis.steps.isEmpty()) appendLine("- no steps yet")
            analysis.steps.forEach { s ->
                val step = s.step
                val line = StringBuilder("${s.index + 1}. ${step.actionLabel}")
                if (s.inputs.isNotEmpty()) line.append(" ").append(s.inputs.joinToString(", ") { it.describe() })
                step.durationSeconds?.takeIf { it > 0 }?.let { line.append(" for ${TimeFormat.short(it)}") }
                step.temperature?.let { line.append(" at ${it.format()}") }
                if (step.preheat) line.append(" (preheat the oven first)")
                val result = s.output.name
                if (result.isNotBlank() && !step.action.isTerminal) line.append(" → $result")
                appendLine(line)
                if (step.note.isNotBlank()) appendLine("   ${step.note.trim()}")
            }
            if (footer != null) {
                appendLine()
                append(footer)
            }
        }.trimEnd()
    }
}
