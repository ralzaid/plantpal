package com.example.plantpal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PlantPalMint,
    onPrimary = PlantPalInk,
    primaryContainer = MossMist,
    onPrimaryContainer = SoftMintText,
    secondary = PlantPalPaleGreen,
    onSecondary = PlantPalInk,
    secondaryContainer = PineNeedle,
    onSecondaryContainer = SoftMintText,
    tertiary = PlantPalLightGreen,
    onTertiary = PlantPalInk,
    background = ForestNight,
    onBackground = SoftMintText,
    surface = PineNeedle,
    onSurface = SoftMintText,
    surfaceVariant = MossMist,
    onSurfaceVariant = PlantPalMint,
    outline = PlantPalMuted,
    outlineVariant = MossMist
)

private val LightColorScheme = lightColorScheme(
    primary = PlantPalGreen,
    onPrimary = PlantPalWhite,
    primaryContainer = PlantPalPaleGreen,
    onPrimaryContainer = PlantPalInk,
    secondary = PlantPalMint,
    onSecondary = PlantPalInk,
    secondaryContainer = PlantPalLightGreen,
    onSecondaryContainer = PlantPalInk,
    tertiary = PlantPalPalePink,
    onTertiary = PlantPalInk,
    background = PlantPalPaleGreen,
    onBackground = PlantPalInk,
    surface = PlantPalWhite,
    onSurface = PlantPalInk,
    surfaceVariant = Color(0xFFF6FBF7),
    onSurfaceVariant = PlantPalMuted,
    outline = PlantPalOutline,
    outlineVariant = PlantPalMint
)

@Composable
fun PlantPalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = PlantPalShapes,
        content = content
    )
}
