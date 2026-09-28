package com.rubberdingyrapids.baking.core

import com.rubberdingyrapids.baking.core.flow.FlowEngine
import com.rubberdingyrapids.baking.core.flow.FlowLayoutEngine
import com.rubberdingyrapids.baking.core.model.ActionType
import com.rubberdingyrapids.baking.core.model.Ingredient
import com.rubberdingyrapids.baking.core.model.MeasureUnit
import com.rubberdingyrapids.baking.core.model.Quantity
import com.rubberdingyrapids.baking.core.model.Recipe
import com.rubberdingyrapids.baking.core.model.Step
import com.rubberdingyrapids.baking.core.model.StepInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowLayoutTest {

    private val potatoes = Ingredient(id = "potatoes", name = "potatoes", quantity = Quantity(500.0, MeasureUnit.GRAM))
    private val fish = Ingredient(id = "fish", name = "fish", quantity = Quantity(2.0, MeasureUnit.PIECE))
    private val oil = Ingredient(id = "oil", name = "oil", quantity = Quantity(1.0, MeasureUnit.LITRE))

    private val cutChips = Step(id = "cutChips", action = ActionType.CHOP, inputs = listOf(StepInput("potatoes")))
    private val fryChips = Step(id = "fryChips", action = ActionType.FRY, inputs = listOf(StepInput("cutChips"), StepInput("oil", fraction = 0.5)), durationSeconds = 600)
    private val cutFish = Step(id = "cutFish", action = ActionType.CHOP, inputs = listOf(StepInput("fish")))
    private val fryFish = Step(id = "fryFish", action = ActionType.FRY, inputs = listOf(StepInput("cutFish"), StepInput("oil", fraction = 0.5)), durationSeconds = 300)
    private val plate = Step(id = "plate", action = ActionType.ADD, inputs = listOf(StepInput("fryChips"), StepInput("fryFish")))

    private val fishAndChips = Recipe(
        name = "Fish and chips",
        ingredients = listOf(potatoes, fish, oil),
        steps = listOf(cutChips, fryChips, cutFish, fryFish, plate),
    )

    @Test
    fun `dependencies only include steps, not raw ingredients`() {
        val deps = FlowEngine.dependencies(fishAndChips)
        assertEquals(emptySet<String>(), deps["cutChips"])
        assertEquals(setOf("cutChips"), deps["fryChips"])
        assertEquals(setOf("fryChips", "fryFish"), deps["plate"])
    }

    @Test
    fun `independent chains get their own lanes and join at the plate`() {
        val layout = FlowLayoutEngine.layout(fishAndChips)
        assertEquals(2, layout.laneCount)
        assertEquals(0, layout.byId.getValue("cutChips").lane)
        assertEquals(0, layout.byId.getValue("fryChips").lane)
        assertEquals(1, layout.byId.getValue("cutFish").lane)
        assertEquals(1, layout.byId.getValue("fryFish").lane)
        assertEquals(0, layout.byId.getValue("plate").lane)

        assertEquals(listOf(listOf("cutChips", "cutFish"), listOf("fryChips", "fryFish"), listOf("plate")), layout.rows.map { row -> row.map { it.stepId } })
    }

    @Test
    fun `a chain that waits for a longer one is shown in flight`() {
        val boilWater = Step(id = "boil", action = ActionType.BOIL, inputs = listOf(StepInput("oil")))
        val r = fishAndChips.copy(steps = listOf(cutChips, fryChips, boilWater, Step(id = "join", action = ActionType.ADD, inputs = listOf(StepInput("fryChips"), StepInput("boil")))))
        val layout = FlowLayoutEngine.layout(r)
        val boil = layout.byId.getValue("boil")
        assertEquals(1, boil.lane)
        assertEquals(0, boil.level)
        assertEquals(2, boil.consumedAtLevel)
        assertTrue(layout.laneInFlight(1, 1))
        assertFalse(layout.laneInFlight(1, 2))
        assertFalse(layout.laneInFlight(0, 1))
        assertTrue(layout.laneContinues(0, 0))
    }

    @Test
    fun `normalise moves a consumer after its producer`() {
        val scrambled = fishAndChips.copy(steps = listOf(plate, fryFish, cutFish, fryChips, cutChips))
        val fixed = FlowEngine.normalise(scrambled)
        val order = fixed.steps.map { it.id }
        assertTrue(order.indexOf("cutChips") < order.indexOf("fryChips"))
        assertTrue(order.indexOf("cutFish") < order.indexOf("fryFish"))
        assertTrue(order.indexOf("fryChips") < order.indexOf("plate"))
        assertTrue(order.indexOf("fryFish") < order.indexOf("plate"))
        assertTrue(FlowEngine.analyse(fixed).isValid)
    }

    @Test
    fun `editable items exclude the step itself and everything downstream`() {
        val forFryChips = FlowEngine.editableItems(fishAndChips, "fryChips").map { it.id }
        assertTrue("cutChips" in forFryChips)
        assertTrue("oil" in forFryChips)
        assertFalse("fryChips" in forFryChips)
        assertFalse("plate" in forFryChips)
        // Fish chain is unrelated, so its output is offered (it could be joined here instead).
        assertTrue("fryFish" in forFryChips)

        val forNew = FlowEngine.editableItems(fishAndChips, null).map { it.id }
        assertEquals(listOf("plate"), forNew)
    }

    @Test
    fun `a served dish is final and never offered again`() {
        val serve = Step(id = "serve", action = ActionType.SERVE, inputs = listOf(StepInput("fryChips"), StepInput("fryFish")))
        val r = fishAndChips.copy(steps = listOf(cutChips, fryChips, cutFish, fryFish, serve))
        val analysis = FlowEngine.analyse(r)
        assertTrue(analysis.isValid)
        assertFalse(analysis.leftovers.any { it.id == "serve" })
        assertFalse(FlowEngine.editableItems(r, null).any { it.id == "serve" })
        assertEquals("fried chopped potatoes + oil + fried chopped fish + oil", analysis.steps.last().output.name)
        assertEquals(0, FlowLayoutEngine.layout(r).byId.getValue("serve").lane)
    }

    @Test
    fun `new actions name their results`() {
        assertEquals("chopped onion", FlowEngine.suggestOutputName(Step(action = ActionType.CHOP), listOf("onion")))
        assertEquals("fried chopped potatoes", FlowEngine.suggestOutputName(Step(action = ActionType.FRY), listOf("chopped potatoes")))
        assertTrue(ActionType.FRY.hasDuration)
        assertFalse(ActionType.FRY.requiresDuration)
        assertTrue(ActionType.ROAST.hasTemperature)
    }
}
