package com.rubberdingyrapids.baking.core

import com.rubberdingyrapids.baking.core.flow.FlowEngine
import com.rubberdingyrapids.baking.core.flow.FlowIssue
import com.rubberdingyrapids.baking.core.model.ActionType
import com.rubberdingyrapids.baking.core.model.Ingredient
import com.rubberdingyrapids.baking.core.model.MeasureUnit
import com.rubberdingyrapids.baking.core.model.Quantity
import com.rubberdingyrapids.baking.core.model.Recipe
import com.rubberdingyrapids.baking.core.model.Step
import com.rubberdingyrapids.baking.core.model.StepInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowEngineTest {

    private val flour = Ingredient(id = "flour", name = "flour", quantity = Quantity(500.0, MeasureUnit.GRAM))
    private val sugar = Ingredient(id = "sugar", name = "sugar", quantity = Quantity(200.0, MeasureUnit.GRAM))
    private val butter = Ingredient(id = "butter", name = "butter", quantity = Quantity(250.0, MeasureUnit.GRAM))
    private val eggs = Ingredient(id = "eggs", name = "eggs", quantity = Quantity(3.0, MeasureUnit.PIECE))

    private fun recipe(vararg steps: Step) = Recipe(
        name = "Test", ingredients = listOf(flour, sugar, butter, eggs), steps = steps.toList(),
    )

    @Test
    fun `raw ingredients are available before the first step`() {
        val items = FlowEngine.availableItems(recipe(), 0)
        assertEquals(listOf("flour", "sugar", "butter", "eggs"), items.map { it.name })
    }

    @Test
    fun `mixing two ingredients produces a combined item and hides the consumed ones`() {
        val mix = Step(id = "mix", action = ActionType.MIX, inputs = listOf(StepInput("flour"), StepInput("sugar")))
        val r = recipe(mix)
        val items = FlowEngine.availableItems(r, 1)
        assertEquals(listOf("butter", "eggs", "flour + sugar mix"), items.map { it.name })
        val mixItem = items.last()
        assertTrue(mixItem.isIntermediate)
        assertEquals(Quantity(700.0, MeasureUnit.GRAM), mixItem.quantity)
    }

    @Test
    fun `partial amount leaves the remainder available`() {
        val melt = Step(
            id = "melt", action = ActionType.MELT,
            inputs = listOf(StepInput("butter", amount = Quantity(100.0, MeasureUnit.GRAM))),
        )
        val items = FlowEngine.availableItems(recipe(melt), 1)
        val butterLeft = items.first { it.id == "butter" }
        assertEquals(150.0, butterLeft.remaining!!.amount, 1e-6)
        assertTrue(butterLeft.isPartiallyUsed)
        assertEquals("melted butter", items.first { it.id == "melt" }.name)
        assertEquals(100.0, items.first { it.id == "melt" }.quantity!!.amount, 1e-6)
    }

    @Test
    fun `fraction input works for items with and without quantities`() {
        val half = Step(id = "half", action = ActionType.WHISK, inputs = listOf(StepInput("eggs", fraction = 0.5)))
        val items = FlowEngine.availableItems(recipe(half), 1)
        assertEquals(1.5, items.first { it.id == "eggs" }.remaining!!.amount, 1e-6)
        assertEquals(1.5, items.first { it.id == "half" }.quantity!!.amount, 1e-6)
    }

    @Test
    fun `mixing mass and count gives an unknown quantity but still tracks fractions`() {
        val mix = Step(id = "mix", action = ActionType.MIX, inputs = listOf(StepInput("flour"), StepInput("eggs")))
        val useHalf = Step(id = "half", action = ActionType.ADD, inputs = listOf(StepInput("mix", fraction = 0.5)))
        val analysis = FlowEngine.analyse(recipe(mix, useHalf))
        assertNull(analysis.steps[0].output.quantity)
        assertTrue(analysis.isValid)
        val leftoverMix = analysis.leftovers.first { it.id == "mix" }
        assertEquals(0.5, leftoverMix.remainingFraction, 1e-6)
    }

    @Test
    fun `using an item twice in full is flagged`() {
        val a = Step(id = "a", action = ActionType.MELT, inputs = listOf(StepInput("butter")))
        val b = Step(id = "b", action = ActionType.MIX, inputs = listOf(StepInput("butter"), StepInput("sugar")))
        val analysis = FlowEngine.analyse(recipe(a, b))
        assertFalse(analysis.isValid)
        assertTrue(analysis.steps[1].issues.any { it is FlowIssue.NothingLeft })
    }

    @Test
    fun `deleting a step drops references to its output`() {
        val mix = Step(id = "mix", action = ActionType.MIX, inputs = listOf(StepInput("flour"), StepInput("sugar")))
        val bake = Step(id = "bake", action = ActionType.BAKE, inputs = listOf(StepInput("mix")), durationSeconds = 1800)
        val r = FlowEngine.deleteStep(recipe(mix, bake), "mix")
        assertEquals(1, r.steps.size)
        assertTrue(r.steps[0].inputs.isEmpty())
        assertTrue(FlowEngine.analyse(r).steps[0].issues.any { it is FlowIssue.NoInputs })
    }

    @Test
    fun `moving a step before its input source is flagged as missing`() {
        val mix = Step(id = "mix", action = ActionType.MIX, inputs = listOf(StepInput("flour"), StepInput("sugar")))
        val bake = Step(id = "bake", action = ActionType.BAKE, inputs = listOf(StepInput("mix")))
        val moved = FlowEngine.moveStep(recipe(mix, bake), 1, 0)
        assertEquals("bake", moved.steps[0].id)
        val analysis = FlowEngine.analyse(moved)
        assertTrue(analysis.steps[0].issues.any { it is FlowIssue.MissingItem })
    }

    @Test
    fun `output names are suggested per action and can be overridden`() {
        val names = listOf("flour", "sugar", "butter", "eggs")
        assertEquals("flour, sugar + 2 more mix", FlowEngine.suggestOutputName(Step(action = ActionType.MIX), names))
        assertEquals("baked batter", FlowEngine.suggestOutputName(Step(action = ActionType.BAKE), listOf("batter")))
        assertEquals("knead dough", FlowEngine.suggestOutputName(Step(action = ActionType.CUSTOM, customLabel = "Knead"), listOf("dough")))
        val step = Step(id = "s", action = ActionType.MIX, inputs = listOf(StepInput("flour")), outputName = "dry mix")
        assertEquals("dry mix", FlowEngine.availableItems(recipe(step), 1).first { it.id == "s" }.name)
    }

    @Test
    fun `scaling multiplies ingredient and absolute input amounts`() {
        val melt = Step(id = "melt", action = ActionType.MELT, inputs = listOf(StepInput("butter", amount = Quantity(100.0, MeasureUnit.GRAM))))
        val doubled = recipe(melt).scaled(2.0)
        assertEquals(1000.0, doubled.ingredients[0].quantity.amount, 1e-6)
        assertEquals(200.0, doubled.steps[0].inputs[0].amount!!.amount, 1e-6)
    }
}
