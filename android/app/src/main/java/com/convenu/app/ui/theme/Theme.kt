package com.convenu.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ConvenuDarkColorScheme = darkColorScheme(
    primary = ConvenuPurple,
    onPrimary = Slate50,
    primaryContainer = ConvenuPurpleDark,
    onPrimaryContainer = ConvenuPurpleLight,
    secondary = ConvenuGreen,
    onSecondary = Slate900,
    secondaryContainer = ConvenuGreen,
    onSecondaryContainer = Slate50,
    tertiary = ConvenuBlue,
    onTertiary = Slate50,
    background = Slate900,
    onBackground = Slate100,
    surface = Slate800,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate300,
    error = ConvenuRed,
    onError = Slate50,
    outline = Slate600,
    outlineVariant = Slate700,
)

@Composable
fun ConvenuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ConvenuDarkColorScheme,
        typography = ConvenuTypography,
        content = content,
    )
}
