@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.rubberdingyrapids.baking.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Caramel,
    onPrimary = OnCaramel,
    primaryContainer = CaramelContainer,
    onPrimaryContainer = OnCaramelContainer,
    secondary = Sage,
    secondaryContainer = SageContainer,
    onSecondaryContainer = OnSageContainer,
    tertiary = Berry,
    tertiaryContainer = BerryContainer,
    onTertiaryContainer = OnBerryContainer,
    background = Cream,
    onBackground = OnCream,
    surface = Cream,
    onSurface = OnCream,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = OnCreamSurfaceVariant,
    outline = CreamOutline,
)

private val DarkColors = darkColorScheme(
    primary = CaramelLight,
    onPrimary = OnCaramelContainer,
    primaryContainer = CaramelDark,
    onPrimaryContainer = CaramelContainer,
    secondary = SageContainer,
    secondaryContainer = Sage,
    onSecondaryContainer = SageContainer,
    tertiary = BerryContainer,
    tertiaryContainer = Berry,
    onTertiaryContainer = BerryContainer,
    background = Cocoa,
    onBackground = OnCocoa,
    surface = Cocoa,
    onSurface = OnCocoa,
    surfaceVariant = CocoaSurfaceVariant,
    onSurfaceVariant = OnCocoaSurfaceVariant,
    outline = CocoaOutline,
)

@Composable
fun BakingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, typography = BakingTypography, content = content)
}
