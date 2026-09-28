package com.rubberdingyrapids.baking.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EditorDark = darkColorScheme(
    primary = ClayOrange,
    onPrimary = Color(0xFF2E1509),
    primaryContainer = Color(0xFF4A2E1F),
    onPrimaryContainer = Color(0xFFFFDBC8),

    secondary = VsBlue,
    onSecondary = Color(0xFF00344C),
    secondaryContainer = Color(0xFF203040),
    onSecondaryContainer = Color(0xFFD3EAFB),

    tertiary = Color(0xFFCE9178),
    onTertiary = Color(0xFF3B2015),
    tertiaryContainer = Color(0xFF4A3327),
    onTertiaryContainer = Color(0xFFFFDBC8),

    background = CodeBackground,
    onBackground = CodeText,

    surface = CodePanel,
    onSurface = CodeText,
    surfaceVariant = CodeSurfaceVariant,
    onSurfaceVariant = CodeTextMuted,

    outline = ClayOrange,
    outlineVariant = CodeBorder,

    error = Color(0xFFF48771),
    onError = Color(0xFF3B0A02),

    surfaceTint = Color.Transparent,
)

/** Always dark, like the Things-to-do app it shares a look with. */
@Composable
fun BakingTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = EditorDark, typography = Typography(), content = content)
}
