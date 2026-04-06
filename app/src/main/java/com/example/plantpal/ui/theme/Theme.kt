package com.example.plantpal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SageGreen,
    onPrimary = PineInk,
    primaryContainer = MossMist,
    onPrimaryContainer = SoftMintText,
    secondary = MintWash,
    onSecondary = PineInk,
    secondaryContainer = PineNeedle,
    onSecondaryContainer = SoftMintText,
    tertiary = StoneLeaf,
    onTertiary = DewWhite,
    background = ForestNight,
    onBackground = SoftMintText,
    surface = PineNeedle,
    onSurface = SoftMintText,
    surfaceVariant = MossMist,
    onSurfaceVariant = SageGreen,
    outline = StoneLeaf,
    outlineVariant = MossMist
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = DewWhite,
    primaryContainer = MintWash,
    onPrimaryContainer = PineInk,
    secondary = MossGreen,
    onSecondary = DewWhite,
    secondaryContainer = Color(0xFFDCEBDA),
    onSecondaryContainer = PineInk,
    tertiary = SageGreen,
    onTertiary = PineInk,
    background = IvoryMist,
    onBackground = PineInk,
    surface = DewWhite,
    onSurface = PineInk,
    surfaceVariant = Color(0xFFEEF2EA),
    onSurfaceVariant = StoneLeaf,
    outline = Color(0xFF97A695),
    outlineVariant = MistOutline
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
