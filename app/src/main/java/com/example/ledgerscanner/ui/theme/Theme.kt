package com.example.ledgerscanner.ui.theme

import android.app.Activity
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

// Light color scheme using only your provided colors
private val LightColorScheme = lightColorScheme(
    primary = Blue500,          // main brand color
    onPrimary = White,

    secondary = Blue100,
    onSecondary = Black,

    tertiary = Grey500,
    onTertiary = White,

    background = White,
    onBackground = Black,

    surface = Grey50,           // subtle card / surface background in light theme
    onSurface = Black,

    error = Grey500,            // fallback (no red in palette)
    onError = White
)

// Dark color scheme using only your provided colors
private val DarkColorScheme = darkColorScheme(
    primary = Blue100,         // softer blue looks better as primary in dark
    onPrimary = Black,

    secondary = Blue500,
    onSecondary = White,

    tertiary = Grey200,
    onTertiary = Black,

    background = Black,
    onBackground = White,

    surface = Grey200,         // cards / surfaces in dark use light grey from your palette
    onSurface = White,

    error = Grey500,           // fallback
    onError = White
)


@Composable
fun LedgerScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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