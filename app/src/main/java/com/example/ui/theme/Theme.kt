package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GitHubDarkColorScheme = darkColorScheme(
    primary = GitHubAccent,
    onPrimary = Color(0xFF0D1117),
    secondary = GitHubAccent,
    onSecondary = Color(0xFF0D1117),
    background = GitHubMainBackground,
    onBackground = GitHubPrimaryText,
    surface = GitHubSurface,
    onSurface = GitHubPrimaryText,
    surfaceVariant = GitHubKeyBackground,
    onSurfaceVariant = GitHubSecondaryText,
    outline = GitHubBorder
)

@Composable
fun GitHubDarkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GitHubDarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
