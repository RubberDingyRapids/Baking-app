package com.rubberdingyrapids.baking.core.search

import com.rubberdingyrapids.baking.core.model.Recipe

object RecipeSearch {
    /** Case-insensitive match on name, description, tag and ingredient names. Every word must match somewhere. */
    fun matches(recipe: Recipe, query: String): Boolean {
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return true
        val haystack = buildList {
            add(recipe.name)
            add(recipe.description)
            add(recipe.tag)
            recipe.ingredients.forEach { add(it.name) }
        }.joinToString(" ").lowercase()
        return words.all { haystack.contains(it) }
    }

    fun filter(recipes: List<Recipe>, query: String): List<Recipe> = recipes.filter { matches(it, query) }
}
