package com.ansh.awsnotifier.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// AWS Orange inspired colors
private val AWSPrimary = Color(0xFFFF9900)
private val AWSPrimaryDark = Color(0xFFEC7211)
private val AWSDark = Color(0xFF232F3E)

private val LightColorScheme = lightColorScheme(
    primary = AWSPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF3E2723),
    secondary = AWSDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E7EC),
    onSecondaryContainer = AWSDark,
    tertiary = Color(0xFF1976D2),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBBDEFB),
    onTertiaryContainer = Color(0xFF0D47A1),
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFFB71C1C),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF49454F)
)

private val DarkColorScheme = darkColorScheme(
    primary = AWSPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF6D4C00),
    onPrimaryContainer = Color(0xFFFFE0B2),
    secondary = Color(0xFF90A4AE),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2D3B55),
    onSecondaryContainer = Color(0xFFECEFF1),
    tertiary = Color(0xFF64B5F6),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF0D47A1),
    onTertiaryContainer = Color(0xFFBBDEFB),
    error = Color(0xFFEF5350),
    onError = Color.Black,
    errorContainer = Color(0xFFB71C1C),
    onErrorContainer = Color(0xFFFFCDD2),
    background = Color(0xFF0B0F19),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF1C2333),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2D3B55),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF3E4C6C)
)

@Composable
fun AWSNotifierTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to true to use Material You
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Typography.kt (can be in same file or separate)
val Typography = Typography()