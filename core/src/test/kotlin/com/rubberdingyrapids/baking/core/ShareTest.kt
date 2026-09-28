package com.rubberdingyrapids.baking.core

import com.rubberdingyrapids.baking.core.data.RecipeJson
import com.rubberdingyrapids.baking.core.model.ActionType
import com.rubberdingyrapids.baking.core.model.Ingredient
import com.rubberdingyrapids.baking.core.model.MeasureUnit
import com.rubberdingyrapids.baking.core.model.Quantity
import com.rubberdingyrapids.baking.core.model.Recipe
import com.rubberdingyrapids.baking.core.model.RecipeLibrary
import com.rubberdingyrapids.baking.core.model.Step
import com.rubberdingyrapids.baking.core.model.StepInput
import com.rubberdingyrapids.baking.core.model.Temperature
import com.rubberdingyrapids.baking.core.model.TemperatureScale
import com.rubberdingyrapids.baking.core.share.RecipeText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ShareTest {
    private val brownies = Recipe(
        id = "b1", name = "Brownies", description = "Fudgy", tag = "Chocolate",
        ingredients = listOf(
            Ingredient(id = "b", name = "butter", quantity = Quantity(200.0, MeasureUnit.GRAM)),
            Ingredient(id = "c", name = "chocolate", quantity = Quantity(200.0, MeasureUnit.GRAM)),
        ),
        steps = listOf(
            Step(id = "melt", action = ActionType.MELT, inputs = listOf(StepInput("b", amount = Quantity(100.0, MeasureUnit.GRAM)), StepInput("c")), note = "Gently"),
            Step(id = "bake", action = ActionType.BAKE, inputs = listOf(StepInput("melt")), durationSeconds = 1500, temperature = Temperature(180, TemperatureScale.CELSIUS), preheat = true),
            Step(id = "serve", action = ActionType.SERVE, inputs = listOf(StepInput("bake")), outputName = "Brownies"),
        ),
    )

    @Test
    fun `plain text has ingredients and numbered steps`() {
        val text = RecipeText.render(brownies)
        assertTrue(text.startsWith("Brownies\nFudgy\nTag: Chocolate"))
        assertTrue(text.contains("- butter: 200 g"))
        assertTrue(text.contains("1. Melt butter (100 g), chocolate → melted butter + chocolate"))
        assertTrue(text.contains("   Gently"))
        assertTrue(text.contains("2. Bake melted butter + chocolate for 25m at 180°C (preheat the oven first)"))
        assertTrue(text.contains("3. Serve baked melted butter + chocolate"))
        assertTrue(text.endsWith("Sent from Cookbook"))
    }

    @Test
    fun `decodeAny accepts a recipe, a library and a server envelope`() {
        val single = RecipeJson.decodeAny(RecipeJson.encodeRecipe(brownies))
        assertEquals(listOf(brownies), single)

        val library = RecipeJson.decodeAny(RecipeJson.encodeLibrary(RecipeLibrary(recipes = listOf(brownies, brownies.copy(id = "b2", name = "More")))))
        assertEquals(listOf("Brownies", "More"), library.map { it.name })

        val wrapped = """{"record": ${RecipeJson.encodeRecipe(brownies)}, "metadata": {"id": "abc", "private": false}}"""
        assertEquals(listOf(brownies), RecipeJson.decodeAny(wrapped))

        try {
            RecipeJson.decodeAny("""{"hello": "world"}""")
            fail("should reject")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }
}
