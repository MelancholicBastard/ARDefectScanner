package com.example.ardefectscanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF245C46),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9F4A7),
    onPrimaryContainer = Color(0xFF193325),
    secondary = Color(0xFF56634D),
    secondaryContainer = Color(0xFFE5ECD9),
    onSecondaryContainer = Color(0xFF263721),
    tertiary = Color(0xFF89622C),
    tertiaryContainer = Color(0xFFFFE8BA),
    background = Color(0xFFF7F8F2),
    surface = Color(0xFFF7F8F2),
    surfaceVariant = Color(0xFFE8ECE2),
    onSurface = Color(0xFF1A211C),
    onSurfaceVariant = Color(0xFF526052),
    outline = Color(0xFF778375),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF1F4EB),
    surfaceContainer = Color(0xFFEBEFE4),
    surfaceContainerHigh = Color(0xFFE5EADD),
    surfaceContainerHighest = Color(0xFFDFE5D6)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBADD95),
    onPrimary = Color(0xFF193325),
    primaryContainer = Color(0xFF304D37),
    onPrimaryContainer = Color(0xFFD9F4A7),
    secondaryContainer = Color(0xFF354333),
    onSecondaryContainer = Color(0xFFE5ECD9),
    tertiary = Color(0xFFFFD699),
    tertiaryContainer = Color(0xFF59431F),
    background = Color(0xFF121914),
    surface = Color(0xFF121914),
    surfaceVariant = Color(0xFF263128),
    onSurface = Color(0xFFE4EBDD),
    onSurfaceVariant = Color(0xFFBAC8B5),
    surfaceContainerLowest = Color(0xFF0C130E),
    surfaceContainerLow = Color(0xFF18211A),
    surfaceContainer = Color(0xFF1D2820),
    surfaceContainerHigh = Color(0xFF27332A),
    surfaceContainerHighest = Color(0xFF313E33)
)

@Composable
fun ARDefectScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
