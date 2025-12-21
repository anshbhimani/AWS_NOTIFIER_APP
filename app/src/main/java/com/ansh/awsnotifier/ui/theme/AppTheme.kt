package com.ansh.awsnotifier.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Professional Enterprise Dark Theme Colors
val md_theme_primary = Color(0xFFFF9900) // AWS Orange
val md_theme_onPrimary = Color(0xFF000000)
val md_theme_primaryContainer = Color(0xFF232F3E) // AWS Dark Blue
val md_theme_onPrimaryContainer = Color(0xFFFFFFFF)

val md_theme_secondary = Color(0xFF8A9BA8)
val md_theme_onSecondary = Color(0xFF000000)
val md_theme_secondaryContainer = Color(0xFF2C3E50)
val md_theme_onSecondaryContainer = Color(0xFFECF0F1)

val md_theme_tertiary = Color(0xFF7B8A93)
val md_theme_onTertiary = Color(0xFFFFFFFF)
val md_theme_tertiaryContainer = Color(0xFF34495E)
val md_theme_onTertiaryContainer = Color(0xFFFFFFFF)

val md_theme_error = Color(0xFFE74C3C)
val md_theme_onError = Color(0xFFFFFFFF)
val md_theme_errorContainer = Color(0xFFC0392B)
val md_theme_onErrorContainer = Color(0xFFFFFFFF)

val md_theme_background = Color(0xFF0F1419)
val md_theme_onBackground = Color(0xFFE8EAED)
val md_theme_surface = Color(0xFF1A1F26)
val md_theme_onSurface = Color(0xFFFFFFFF)
val md_theme_surfaceVariant = Color(0xFF242A31)
val md_theme_onSurfaceVariant = Color(0xFFA8ABAD)
val md_theme_outline = Color(0xFF444444)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_primary,
    onPrimary = md_theme_onPrimary,
    primaryContainer = md_theme_primaryContainer,
    onPrimaryContainer = md_theme_onPrimaryContainer,
    secondary = md_theme_secondary,
    onSecondary = md_theme_onSecondary,
    secondaryContainer = md_theme_secondaryContainer,
    onSecondaryContainer = md_theme_onSecondaryContainer,
    tertiary = md_theme_tertiary,
    onTertiary = md_theme_onTertiary,
    tertiaryContainer = md_theme_tertiaryContainer,
    onTertiaryContainer = md_theme_onTertiaryContainer,
    error = md_theme_error,
    onError = md_theme_onError,
    errorContainer = md_theme_errorContainer,
    onErrorContainer = md_theme_onErrorContainer,
    background = md_theme_background,
    onBackground = md_theme_onBackground,
    surface = md_theme_surface,
    onSurface = md_theme_onSurface,
    surfaceVariant = md_theme_surfaceVariant,
    onSurfaceVariant = md_theme_onSurfaceVariant,
    outline = md_theme_outline
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
