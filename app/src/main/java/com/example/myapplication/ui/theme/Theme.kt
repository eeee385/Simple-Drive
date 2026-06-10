package com.example.myapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = SkyBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = SkyBlueContainer,
    onPrimaryContainer = SkyBlueDark,
    secondary = SkyBlueLight,
    onSecondary = SurfaceWhite,
    secondaryContainer = SkyBlueContainer,
    onSecondaryContainer = SkyBlueDark,
    tertiary = WarmAmber,
    onTertiary = TextPrimary,
    tertiaryContainer = WarmAmberContainer,
    onTertiaryContainer = WarmAmberDark,
    background = BackgroundBlue,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = MutedBlue,
    onSurfaceVariant = TextSecondary,
    outline = BorderBlue,
    outlineVariant = BorderBlue,
    error = ErrorRed,
    onError = SurfaceWhite,
    errorContainer = ErrorContainer,
    onErrorContainer = ErrorRed,
)

private val DarkColorScheme = darkColorScheme(
    primary = SkyBlueLight,
    onPrimary = TextPrimary,
    primaryContainer = SkyBlueDark,
    onPrimaryContainer = SkyBlueContainer,
    secondary = SkyBlueLight,
    onSecondary = TextPrimary,
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = SkyBlueContainer,
    tertiary = WarmAmber,
    onTertiary = TextPrimary,
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = WarmAmberContainer,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = Color(0xFFFCA5A5),
    onError = TextPrimary,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = ErrorContainer,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                androidx.compose.material3.dynamicDarkColorScheme(context)
            } else {
                androidx.compose.material3.dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
