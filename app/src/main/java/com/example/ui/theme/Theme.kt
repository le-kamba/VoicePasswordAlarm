package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryIndigoDark,
    primaryContainer = PrimaryContainerDark,
    secondary = SecondaryIndigoDark,
    tertiary = TertiaryLavenderDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = BackgroundDark,
    onSecondary = BackgroundDark,
    onTertiary = BackgroundDark,
    onBackground = TextLight,
    onSurface = TextLight,
    onSurfaceVariant = OnSurfaceMediumDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryIndigo,
    primaryContainer = PrimaryContainerLight,
    secondary = SecondaryIndigo,
    tertiary = TertiaryLavender,
    background = BackgroundSoft,
    surface = SurfaceSoft,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    onBackground = TextDark,
    onSurface = TextDark,
    onSurfaceVariant = OnSurfaceMedium
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
