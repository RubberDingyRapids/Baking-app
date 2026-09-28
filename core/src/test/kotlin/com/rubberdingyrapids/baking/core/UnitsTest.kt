package com.rubberdingyrapids.baking.core

import com.rubberdingyrapids.baking.core.model.MeasureUnit
import com.rubberdingyrapids.baking.core.model.Quantity
import com.rubberdingyrapids.baking.core.model.Temperature
import com.rubberdingyrapids.baking.core.model.TemperatureScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnitsTest {
    @Test
    fun `mass converts between grams and ounces`() {
        val oz = Quantity(1.0, MeasureUnit.OUNCE).convertTo(MeasureUnit.GRAM)!!
        assertEquals(28.35, oz.amount, 0.01)
        assertEquals(2.2046, Quantity(1.0, MeasureUnit.KILOGRAM).convertTo(MeasureUnit.POUND)!!.amount, 0.001)
    }

    @Test
    fun `volume converts between cups and millilitres`() {
        assertEquals(236.59, Quantity(1.0, MeasureUnit.CUP).convertTo(MeasureUnit.MILLILITRE)!!.amount, 0.01)
        assertEquals(16.0, Quantity(1.0, MeasureUnit.CUP).convertTo(MeasureUnit.TABLESPOON)!!.amount, 0.001)
        assertEquals(3.0, Quantity(1.0, MeasureUnit.TABLESPOON).convertTo(MeasureUnit.TEASPOON)!!.amount, 0.001)
    }

    @Test
    fun `mass to volume is not converted`() {
        assertNull(Quantity(100.0, MeasureUnit.GRAM).convertTo(MeasureUnit.CUP))
    }

    @Test
    fun `formatting uses fractions for cups and trims decimals for grams`() {
        assertEquals("1½ cup", Quantity(1.5, MeasureUnit.CUP).format())
        assertEquals("⅓ cup", Quantity(1.0 / 3, MeasureUnit.CUP).format())
        assertEquals("250 g", Quantity(250.0, MeasureUnit.GRAM).format())
        assertEquals("12.5 g", Quantity(12.5, MeasureUnit.GRAM).format())
        assertEquals("3 pcs", Quantity(3.0, MeasureUnit.PIECE).format())
    }

    @Test
    fun `temperature converts both ways`() {
        assertEquals(356, Temperature(180, TemperatureScale.CELSIUS).convertTo(TemperatureScale.FAHRENHEIT).value)
        assertEquals(180, Temperature(356, TemperatureScale.FAHRENHEIT).convertTo(TemperatureScale.CELSIUS).value)
    }

    @Test
    fun `unit toggle cycles through every unit`() {
        var u = MeasureUnit.GRAM
        val seen = mutableSetOf<MeasureUnit>()
        repeat(MeasureUnit.entries.size) { seen += u; u = u.next() }
        assertEquals(MeasureUnit.entries.toSet(), seen)
        assertEquals(MeasureUnit.GRAM, u)
    }
}
