package com.yangcy.gofish.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EditorialDarkPrimary,
    secondary = EditorialDarkSecondary,
    tertiary = EditorialDarkTertiary,
    background = EditorialDarkBackground,
    surface = EditorialDarkSurface,
    surfaceVariant = EditorialDarkSurfaceVariant,
    outline = EditorialDarkOutline,
    onPrimary = Color(0xFF003258),
    onSecondary = Color(0xFF003258),
    onTertiary = Color(0xFF003258),
    onBackground = EditorialDarkOnBackground,
    onSurface = EditorialDarkOnSurface,
    onSurfaceVariant = EditorialDarkOnSurfaceVariant,
    primaryContainer = EditorialDarkPrimaryContainer,
    onPrimaryContainer = EditorialDarkOnPrimaryContainer
)

private val LightColorScheme = lightColorScheme(
    primary = EditorialPrimary,
    secondary = EditorialSecondary,
    tertiary = EditorialTertiary,
    background = EditorialBackground,
    surface = EditorialSurface,
    surfaceVariant = EditorialSurfaceVariant,
    outline = EditorialOutline,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = EditorialOnBackground,
    onSurface = EditorialOnSurface,
    onSurfaceVariant = EditorialOnSurfaceVariant,
    primaryContainer = EditorialPrimaryContainer,
    onPrimaryContainer = EditorialOnPrimaryContainer
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
