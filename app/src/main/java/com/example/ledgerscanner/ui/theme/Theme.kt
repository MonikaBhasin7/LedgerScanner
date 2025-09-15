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

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,         // #277EFF
    onPrimary = White,             // text/icons on primary
    secondary = BlueLight,         // #DBE8FB (soft blue)
    onSecondary = Color(0xFF1C1B1F),
    tertiary = SuccessGreen,       // accent (success green)
    onTertiary = White,

    background = White,
    onBackground = Color(0xFF1C1B1F),
    surface = Gray50,              // subtle grey
    onSurface = Color(0xFF1C1B1F),

    error = ErrorRed,
    onError = White
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueLight,           // softer for dark mode
    onPrimary = Color(0xFF000000),
    secondary = BluePrimary,       // stronger blue in dark
    onSecondary = White,
    tertiary = SuccessGreen,
    onTertiary = Color(0xFF000000),

    background = Color(0xFF121212),
    onBackground = White,
    surface = Color(0xFF1E1E1E),
    onSurface = White,

    error = ErrorRed,
    onError = Color(0xFF000000)
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