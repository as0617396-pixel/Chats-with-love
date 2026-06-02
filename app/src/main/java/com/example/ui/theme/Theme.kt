package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LoveDarkColorScheme = darkColorScheme(
    primary = LoveThemeDarkPrimary,
    secondary = LoveThemeDarkSecondary,
    tertiary = LoveThemeDarkTertiary,
    background = LoveThemeDarkBackground,
    surface = LoveThemeDarkSurface,
    onPrimary = LoveThemeDarkBackground,
    onSecondary = LoveThemeDarkBackground,
    onBackground = LoveThemeLightBackground,
    onSurface = LoveThemeLightSurface
)

private val LoveLightColorScheme = lightColorScheme(
    primary = LoveThemeLightPrimary,
    secondary = LoveThemeLightSecondary,
    tertiary = LoveThemeLightTertiary,
    background = LoveThemeLightBackground,
    surface = LoveThemeLightSurface,
    onPrimary = LoveThemeLightSurface,
    onSecondary = LoveThemeLightSurface,
    onBackground = LoveThemeDarkBackground,
    onSurface = LoveThemeDarkBackground
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep our custom aesthetic locked so it's beautifully pink rather than generic Android system theme!
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LoveDarkColorScheme else LoveLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
