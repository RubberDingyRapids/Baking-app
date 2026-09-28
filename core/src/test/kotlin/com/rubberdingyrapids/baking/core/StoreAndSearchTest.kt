package com.rubberdingyrapids.baking.core

import com.rubberdingyrapids.baking.core.data.IngredientSuggestions
import com.rubberdingyrapids.baking.core.data.RecipeJson
import com.rubberdingyrapids.baking.core.data.RecipeStore
import com.rubberdingyrapids.baking.core.format.TimeFormat
import com.rubberdingyrapids.baking.core.model.ActionType
import com.rubberdingyrapids.baking.core.model.Ingredient
import com.rubberdingyrapids.baking.core.model.MeasureUnit
import com.rubberdingyrapids.baking.core.model.Quantity
import com.rubberdingyrapids.baking.core.model.Recipe
import com.rubberdingyrapids.baking.core.model.Step
import com.rubberdingyrapids.baking.core.model.StepInput
import com.rubberdingyrapids.baking.core.model.Temperature
import com.rubberdingyrapids.baking.core.model.TemperatureScale
import com.rubberdingyrapids.baking.core.search.RecipeSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class StoreAndSearchTest {

    private val brownies = Recipe(
        name = "Brownies", description = "Fudgy and dark", tag = "Chocolate",
        ingredients = listOf(
            Ingredient(id = "b", name = "butter", quantity = Quantity(200.0, MeasureUnit.GRAM)),
            Ingredient(id = "c", name = "dark chocolate", quantity = Quantity(200.0, MeasureUnit.GRAM)),
        ),
        steps = listOf(
            Step(id = "melt", action = ActionType.MELT, inputs = listOf(StepInput("b"), StepInput("c"))),
            Step(
                id = "bake", action = ActionType.BAKE, inputs = listOf(StepInput("melt")),
                durationSeconds = 25 * 60, temperature = Temperature(180, TemperatureScale.CELSIUS), preheat = true,
            ),
        ),
    )

    @Test
    fun `recipe survives a JSON round trip`() {
        val text = RecipeJson.encodeRecipe(brownies)
        assertEquals(brownies, RecipeJson.decodeRecipe(text))
    }

    @Test
    fun `store persists and reloads recipes`() = runTest {
        val dir = Files.createTempDirectory("baking").toFile()
        try {
            val store = RecipeStore(dir, Dispatchers.Unconfined)
            store.load()
            assertTrue(store.recipes.value.isEmpty())
            store.save(brownies)
            assertEquals(1, store.recipes.value.size)

            val reopened = RecipeStore(dir, Dispatchers.Unconfined)
            reopened.load()
            assertEquals(brownies.id, reopened.recipes.value.single().id)
            assertEquals("Brownies", reopened.get(brownies.id)!!.name)

            reopened.delete(brownies.id)
            assertTrue(reopened.recipes.value.isEmpty())
            assertFalse(File(dir, RecipeStore.FILE_NAME).readText().contains("Brownies"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `corrupt library is set aside instead of crashing`() = runTest {
        val dir = Files.createTempDirectory("baking").toFile()
        try {
            File(dir, RecipeStore.FILE_NAME).writeText("{ not json")
            val store = RecipeStore(dir, Dispatchers.Unconfined)
            store.load()
            assertTrue(store.recipes.value.isEmpty())
            assertTrue(dir.listFiles()!!.any { it.name.contains("corrupt") })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `search matches name tag description and ingredients`() {
        assertTrue(RecipeSearch.matches(brownies, "brown"))
        assertTrue(RecipeSearch.matches(brownies, "CHOCOLATE"))
        assertTrue(RecipeSearch.matches(brownies, "fudgy butter"))
        assertFalse(RecipeSearch.matches(brownies, "lemon"))
        assertTrue(RecipeSearch.matches(brownies, "   "))
    }

    @Test
    fun `ingredient completion prefers prefix matches`() {
        assertEquals("butter", IngredientSuggestions.complete("bu"))
        assertEquals("sugar", IngredientSuggestions.complete("Su"))
        assertNull(IngredientSuggestions.complete("butter"))
        assertNull(IngredientSuggestions.complete(""))
        assertEquals("golden syrup", IngredientSuggestions.complete("gold", extra = listOf("golden syrup")))
        assertTrue(IngredientSuggestions.suggest("choc").first().startsWith("choc"))
    }

    @Test
    fun `time formatting`() {
        assertEquals("1h 20m", TimeFormat.short(4800))
        assertEquals("45m", TimeFormat.short(2700))
        assertEquals("30s", TimeFormat.short(30))
        assertEquals("20:05", TimeFormat.clock(1205))
        assertEquals("1:00:00", TimeFormat.clock(3600))
    }
}
