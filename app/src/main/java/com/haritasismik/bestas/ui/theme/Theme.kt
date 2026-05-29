package com.haritasismik.bestas.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Renk paleti - Doğal/gerçekçi tema
val WoodBrown = Color(0xFF8B6914)
val DarkWood = Color(0xFF5C4033)
val LightWood = Color(0xFFD4A76A)
val StoneGray = Color(0xFF7A7A7A)
val DarkStone = Color(0xFF4A4A4A)
val PistachioGreen = Color(0xFF93C572)
val DarkPistachio = Color(0xFF6B8E23)
val GoldAccent = Color(0xFFDAA520)
val CreamWhite = Color(0xFFFFF8DC)
val DeepRed = Color(0xFF8B0000)

private val DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    secondary = LightWood,
    tertiary = PistachioGreen,
    background = Color(0xFF1A1A2E),
    surface = Color(0xFF16213E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = CreamWhite,
    onSurface = CreamWhite
)

private val LightColorScheme = lightColorScheme(
    primary = DarkWood,
    secondary = WoodBrown,
    tertiary = DarkPistachio,
    background = CreamWhite,
    surface = Color(0xFFFFF5E1),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = DarkWood,
    onSurface = DarkWood
)

@Composable
fun BesTasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
