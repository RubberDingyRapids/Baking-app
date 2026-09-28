package com.rubberdingyrapids.baking.core.model

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt

/** What a unit measures. Conversion is only possible within the same kind. */
enum class UnitKind { MASS, VOLUME, COUNT }

/**
 * Units a baker actually uses. [toBase] converts one of this unit into the
 * kind's base unit (grams for mass, millilitres for volume, pieces for count).
 */
@Serializable
enum class MeasureUnit(val symbol: String, val kind: UnitKind, val toBase: Double) {
    GRAM("g", UnitKind.MASS, 1.0),
    KILOGRAM("kg", UnitKind.MASS, 1000.0),
    OUNCE("oz", UnitKind.MASS, 28.349523125),
    POUND("lb", UnitKind.MASS, 453.59237),
    MILLILITRE("ml", UnitKind.VOLUME, 1.0),
    LITRE("l", UnitKind.VOLUME, 1000.0),
    CUP("cup", UnitKind.VOLUME, 236.5882365),
    TABLESPOON("tbsp", UnitKind.VOLUME, 14.78676478125),
    TEASPOON("tsp", UnitKind.VOLUME, 4.92892159375),
    PIECE("pcs", UnitKind.COUNT, 1.0);

    /** The unit shown after this one when the user taps the unit toggle. */
    fun next(): MeasureUnit {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }

    companion object {
        val DEFAULT = GRAM
    }
}

@Serializable
data class Quantity(val amount: Double, val unit: MeasureUnit) {

    val kind: UnitKind get() = unit.kind

    /** Amount expressed in the kind's base unit (g, ml or pcs). */
    val baseAmount: Double get() = amount * unit.toBase

    /** Converts to [target], or returns null when the kinds differ (e.g. grams to cups). */
    fun convertTo(target: MeasureUnit): Quantity? {
        if (target.kind != unit.kind) return null
        return Quantity(baseAmount / target.toBase, target)
    }

    fun scaled(factor: Double): Quantity = copy(amount = amount * factor)

    operator fun plus(other: Quantity): Quantity? {
        val converted = other.convertTo(unit) ?: return null
        return copy(amount = amount + converted.amount)
    }

    operator fun minus(other: Quantity): Quantity? {
        val converted = other.convertTo(unit) ?: return null
        return copy(amount = amount - converted.amount)
    }

    fun isPositive(): Boolean = amount > EPSILON

    /** Human friendly text such as "250 g", "1½ cups" or "3 pcs". */
    fun format(): String = "${formatAmount(amount, unit)} ${unit.symbol}"

    companion object {
        const val EPSILON = 1e-6

        private val fractions = listOf(
            0.25 to "¼", 1.0 / 3 to "⅓", 0.5 to "½", 2.0 / 3 to "⅔", 0.75 to "¾",
        )

        fun formatAmount(amount: Double, unit: MeasureUnit): String {
            val useFractions = unit == MeasureUnit.CUP || unit == MeasureUnit.TABLESPOON ||
                unit == MeasureUnit.TEASPOON || unit == MeasureUnit.PIECE
            if (useFractions) {
                val whole = amount.toInt()
                val rest = amount - whole
                if (rest < 0.02) return whole.toString()
                val glyph = fractions.firstOrNull { abs(it.first - rest) < 0.03 }?.second
                if (glyph != null) return if (whole == 0) glyph else "$whole$glyph"
            }
            return formatNumber(amount)
        }

        fun formatNumber(value: Double): String {
            val rounded = (value * 100).roundToInt() / 100.0
            return if (rounded == rounded.toLong().toDouble()) {
                rounded.toLong().toString()
            } else {
                rounded.toString().trimEnd('0')
            }
        }
    }
}

@Serializable
enum class TemperatureScale(val symbol: String) { CELSIUS("°C"), FAHRENHEIT("°F") }

@Serializable
data class Temperature(val value: Int, val scale: TemperatureScale) {
    fun convertTo(target: TemperatureScale): Temperature = when {
        scale == target -> this
        target == TemperatureScale.FAHRENHEIT -> Temperature((value * 9.0 / 5 + 32).roundToInt(), target)
        else -> Temperature(((value - 32) * 5.0 / 9).roundToInt(), target)
    }

    fun format(): String = "$value${scale.symbol}"
}
