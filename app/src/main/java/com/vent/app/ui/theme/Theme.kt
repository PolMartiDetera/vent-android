package com.vent.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = SpindriftPrimary,
    onPrimary = SpindriftOnPrimary,
    primaryContainer = SpindriftPrimaryContainer,
    onPrimaryContainer = SpindriftOnPrimaryContainer,
    secondary = SpindriftSecondary,
    onSecondary = SpindriftOnSecondary,
    secondaryContainer = SpindriftSecondaryContainer,
    onSecondaryContainer = SpindriftOnSecondaryContainer,
    tertiary = SpindriftTertiary,
    onTertiary = SpindriftOnTertiary,
    tertiaryContainer = SpindriftTertiaryContainer,
    onTertiaryContainer = SpindriftOnTertiaryContainer,
    background = SpindriftBackground,
    onBackground = SpindriftOnBackground,
    surface = SpindriftSurface,
    onSurface = SpindriftOnSurface,
    surfaceVariant = SpindriftSurfaceVariant,
    onSurfaceVariant = SpindriftOnSurfaceVariant,
    outline = SpindriftOutline,
)

private val DarkColors = darkColorScheme(
    primary = SpindriftPrimaryDark,
    onPrimary = SpindriftOnPrimaryDark,
    primaryContainer = SpindriftPrimaryContainerDark,
    onPrimaryContainer = SpindriftOnPrimaryContainerDark,
    background = SpindriftBackgroundDark,
    onBackground = SpindriftOnBackgroundDark,
    surface = SpindriftSurfaceDark,
    onSurface = SpindriftOnSurfaceDark,
    surfaceVariant = SpindriftSurfaceVariantDark,
    onSurfaceVariant = SpindriftOnSurfaceVariantDark,
    tertiaryContainer = SpindriftTertiaryContainerDark,
    onTertiaryContainer = SpindriftOnTertiaryContainerDark,
)

@Composable
fun VentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
