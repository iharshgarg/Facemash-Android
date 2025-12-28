package com.facemash.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FacebookLightColorScheme = lightColorScheme(
    primary = FacebookBlue,
    secondary = FacebookBlueLight,
    tertiary = FacebookBlue,

    background = FacebookBackground,
    surface = FacebookSurface,

    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,

    onBackground = FacebookTextPrimary,
    onSurface = FacebookTextPrimary,

    outline = FacebookDivider
)

private val FacebookDarkColorScheme = darkColorScheme(
    primary = FacebookBlueLight,
    secondary = FacebookBlue,
    tertiary = FacebookBlueLight,

    background = Color(0xFF18191A),
    surface = Color(0xFF242526),

    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurface = Color.White
)

@Composable
fun FacemashTheme(
    darkTheme: Boolean = false,      // 👈 default LIGHT like classic FB
    dynamicColor: Boolean = false,   // ❌ disable Material You
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        FacebookDarkColorScheme
    } else {
        FacebookLightColorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}