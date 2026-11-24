package com.bookstore.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val LightColors = lightColorScheme(
    primary = Color(0xFF3F3DA8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E1FF),
    onPrimaryContainer = Color(0xFF0D0A5C),
    secondary = Color(0xFFB45309),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE8CC),
    onSecondaryContainer = Color(0xFF3B1B00),
    tertiary = Color(0xFF0F766E),
    background = Color(0xFFFBF9FF),
    onBackground = Color(0xFF1B1B20),
    surface = Color(0xFFFBF9FF),
    onSurface = Color(0xFF1B1B20),
    surfaceVariant = Color(0xFFE5E1EC),
    onSurfaceVariant = Color(0xFF47464F),
    outline = Color(0xFF787680),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC2C1FF),
    onPrimary = Color(0xFF14108A),
    primaryContainer = Color(0xFF2B2990),
    onPrimaryContainer = Color(0xFFE4E1FF),
    secondary = Color(0xFFFFB95C),
    secondaryContainer = Color(0xFF5E3000),
    onSecondaryContainer = Color(0xFFFFE8CC),
    tertiary = Color(0xFF5CD6C8),
    background = Color(0xFF131316),
    onBackground = Color(0xFFE4E1E6),
    surface = Color(0xFF131316),
    onSurface = Color(0xFFE4E1E6),
    surfaceVariant = Color(0xFF47464F),
    onSurfaceVariant = Color(0xFFC8C5D0),
    outline = Color(0xFF928F9A)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

/** Semantic colours that are not part of the Material scheme. */
object StatusColors {
    val inStock = Color(0xFF15803D)
    val lowStock = Color(0xFFB45309)
    val outOfStock = Color(0xFFB91C1C)
}

@Composable
fun BookstoreTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        content = content
    )
}
