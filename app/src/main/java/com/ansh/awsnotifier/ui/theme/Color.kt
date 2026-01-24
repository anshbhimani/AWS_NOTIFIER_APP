package com.ansh.awsnotifier.ui.theme

import androidx.compose.ui.graphics.Color

// Midnight Pro Palette
val MidnightBackground = Color(0xFF0F172A) // Slate 900
val MidnightSurface = Color(0xFF1E293B)    // Slate 800
val MidnightSurfaceHighlight = Color(0xFF334155) // Slate 700

val ElectricBlue = Color(0xFF3B82F6)       // Blue 500
val ElectricBlueHighlight = Color(0xFF60A5FA) // Blue 400

val SuccessTeal = Color(0xFF14B8A6)        // Teal 500
val ErrorRose = Color(0xFFF43F5E)          // Rose 500
val WarningAmber = Color(0xFFF59E0B)       // Amber 500

val TextPrimary = Color(0xFFF8FAFC)        // Slate 50
val TextSecondary = Color(0xFF94A3B8)      // Slate 400
val TextTertiary = Color(0xFF64748B)       // Slate 500

val DividerColor = Color(0xFF1E293B)       // Slate 800

// Material Mapping
val md_theme_dark_primary = ElectricBlue
val md_theme_dark_onPrimary = Color.White
val md_theme_dark_primaryContainer = Color(0xFF172554) // Blue 950
val md_theme_dark_onPrimaryContainer = ElectricBlueHighlight

val md_theme_dark_secondary = TextSecondary
val md_theme_dark_onSecondary = Color.Black
val md_theme_dark_secondaryContainer = MidnightSurfaceHighlight
val md_theme_dark_onSecondaryContainer = TextPrimary

val md_theme_dark_tertiary = TextTertiary // Added
val md_theme_dark_onTertiary = TextPrimary // Added
val md_theme_dark_tertiaryContainer = MidnightSurfaceHighlight // Added
val md_theme_dark_onTertiaryContainer = TextPrimary // Added

val md_theme_dark_error = ErrorRose
val md_theme_dark_onError = Color.White // Added
val md_theme_dark_errorContainer = Color(0xFF410E0B) // Added
val md_theme_dark_onErrorContainer = Color(0xFFF2B8B5) // Added

val md_theme_dark_background = MidnightBackground
val md_theme_dark_onBackground = TextPrimary
val md_theme_dark_surface = MidnightBackground // Flat look
val md_theme_dark_onSurface = TextPrimary
val md_theme_dark_surfaceVariant = MidnightSurface
val md_theme_dark_onSurfaceVariant = TextSecondary

val md_theme_dark_outline = TextTertiary
val md_theme_dark_outlineVariant = MidnightSurfaceHighlight // Added
