@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rubberdingyrapids.baking.ui.theme

import androidx.compose.ui.graphics.Color

// Warm bakery palette used when dynamic colour is unavailable.
val Caramel = Color(0xFFC8743B)
val CaramelDark = Color(0xFF8A4A1E)
val OnCaramel = Color(0xFFFFFFFF)
val CaramelContainer = Color(0xFFFFDCC6)
val OnCaramelContainer = Color(0xFF331200)

val Sage = Color(0xFF6B7F5A)
val SageContainer = Color(0xFFDCE8CF)
val OnSageContainer = Color(0xFF1B2415)

val Berry = Color(0xFF8C3A5A)
val BerryContainer = Color(0xFFFFD8E4)
val OnBerryContainer = Color(0xFF3A0A20)

val Cream = Color(0xFFFFFBF6)
val OnCream = Color(0xFF221A14)
val CreamSurfaceVariant = Color(0xFFF3E6DA)
val OnCreamSurfaceVariant = Color(0xFF52443A)
val CreamOutline = Color(0xFF857468)

val Cocoa = Color(0xFF1A1512)
val OnCocoa = Color(0xFFEFE2D6)
val CocoaSurfaceVariant = Color(0xFF3B3129)
val OnCocoaSurfaceVariant = Color(0xFFD8C8BA)
val CocoaOutline = Color(0xFF9E8D80)
val CaramelLight = Color(0xFFFFB783)

/** Stable pastel colours for tag chips and tile headers, chosen from the tag text. */
val TagPalette = listOf(
    Color(0xFFFFD8B5), Color(0xFFD9E8C6), Color(0xFFFFD1DC), Color(0xFFCFE3F5),
    Color(0xFFEBDCF7), Color(0xFFFFF0B3), Color(0xFFC9EDE7), Color(0xFFF5D7C8),
)

fun tagColor(tag: String): Color {
    if (tag.isBlank()) return TagPalette[0]
    val index = (tag.lowercase().hashCode() and 0x7fffffff) % TagPalette.size
    return TagPalette[index]
}
