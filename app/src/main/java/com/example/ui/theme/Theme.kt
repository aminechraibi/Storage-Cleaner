package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CleanDarkPrimary,
    onPrimary = CleanDarkBackground,
    primaryContainer = CleanDarkPrimaryContainer,
    onPrimaryContainer = CleanDarkOnPrimaryContainer,
    secondary = CleanDarkPrimary,
    onSecondary = CleanDarkBackground,
    secondaryContainer = CleanDarkSurfaceVariant,
    onSecondaryContainer = CleanDarkOnPrimaryContainer,
    tertiary = AmberLight,
    onTertiary = Color.Black,
    background = CleanDarkBackground,
    onBackground = Color(0xFFE1E4DD),
    surface = CleanDarkSurface,
    onSurface = Color(0xFFE1E4DD),
    surfaceVariant = CleanDarkSurfaceVariant,
    onSurfaceVariant = CleanDarkTextMuted,
    outline = CleanDarkBorder,
    outlineVariant = CleanDarkBorder.copy(alpha = 0.5f),
    error = RoseLight,
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = CleanGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = CleanGreenContainer,
    onPrimaryContainer = CleanGreenOnContainer,
    secondary = CleanGreenPrimary,
    onSecondary = Color.White,
    secondaryContainer = CleanGreenLight,
    onSecondaryContainer = CleanGreenOnContainer,
    tertiary = AmberReview,
    onTertiary = Color.White,
    background = CleanBackground,
    onBackground = CleanOnBackground,
    surface = CleanSurface,
    onSurface = CleanOnSurface,
    surfaceVariant = CleanGreenLight,
    onSurfaceVariant = CleanTextMuted,
    outline = CleanSurfaceBorder,
    outlineVariant = CleanSurfaceBorder.copy(alpha = 0.6f),
    error = RoseSensitive,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent Clean Minimalism branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
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

