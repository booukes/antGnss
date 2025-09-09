package com.example.antLabs.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,           // used for buttons, highlights
    onPrimary = TextPrimary,        // text/icons on primary color
    secondary = AccentBlue,         // secondary accents
    onSecondary = TextPrimary,
    background = DarkBackground,    // app background
    onBackground = TextPrimary,     // main text     // cards, sheets
    onSurface = TextPrimary,
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun AntLabsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
