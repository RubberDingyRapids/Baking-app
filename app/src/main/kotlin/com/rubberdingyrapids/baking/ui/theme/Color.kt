package com.rubberdingyrapids.baking.ui.theme

import androidx.compose.ui.graphics.Color

// Same palette as the Things-to-do app: a VS Code style dark editor accented
// with clay orange, so the two apps feel like siblings.
val ClayOrange = Color(0xFFD97757)
val ClayOrangeBright = Color(0xFFE89A76)
val CodeBackground = Color(0xFF1E1E1E)
val CodePanel = Color(0xFF252526)
val CodeSurfaceVariant = Color(0xFF2D2D30)
val CodeBorder = Color(0xFF3C3C3C)
val CodeText = Color(0xFFD4D4D4)
val CodeTextMuted = Color(0xFF9A9A9A)
val VsBlue = Color(0xFF9CDCFE)
val VsGreen = Color(0xFF6A9955)

/** Muted colours for tag chips, chosen from the tag text so a tag always looks the same. */
val TagPalette = listOf(
    Color(0xFF4A2E1F), Color(0xFF203040), Color(0xFF2E4A2A), Color(0xFF4A2A3E),
    Color(0xFF3A3A1F), Color(0xFF1F3F3F), Color(0xFF3A2A4A), Color(0xFF4A3A2A),
)

fun tagColor(tag: String): Color {
    if (tag.isBlank()) return TagPalette[0]
    val index = (tag.lowercase().hashCode() and 0x7fffffff) % TagPalette.size
    return TagPalette[index]
}
