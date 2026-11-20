package com.conduit.msme.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val ConduitLightColorScheme = lightColorScheme(
    primary = DeepMidnightNavy,
    onPrimary = Color.White,
    primaryContainer = MidnightNavyElevated,
    onPrimaryContainer = Color.White,
    secondary = ElectricTechCyan,
    onSecondary = Color.White,
    secondaryContainer = ElectricTechCyanContainer,
    onSecondaryContainer = OnElectricTechCyanContainer,
    tertiary = KarnatakaGold,
    onTertiary = Color.White,
    tertiaryContainer = KarnatakaGoldContainer,
    onTertiaryContainer = OnKarnatakaGoldContainer,
    background = CleanOffWhiteSlate,
    onBackground = DeepMidnightNavy,
    surface = CardWhite,
    onSurface = DeepMidnightNavy,
    surfaceVariant = SurfaceContainerSubtle,
    onSurfaceVariant = SlateInkSecondary,
    outline = CardBorderSlate,
    outlineVariant = Color(0xFFCBD5E1),
    error = WarningRed,
    onError = Color.White,
    errorContainer = WarningRedContainer,
    onErrorContainer = OnWarningRedContainer
)

private val ConduitDarkColorScheme = darkColorScheme(
    primary = ElectricTechCyanLight,
    onPrimary = DeepMidnightNavy,
    primaryContainer = MidnightNavyElevated,
    onPrimaryContainer = DarkOnSurface,
    secondary = ElectricTechCyanLight,
    onSecondary = DeepMidnightNavy,
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = ElectricTechCyanContainer,
    tertiary = KarnatakaGoldLight,
    onTertiary = DeepMidnightNavy,
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = KarnatakaGoldContainer,
    background = DarkBackgroundNavy,
    onBackground = DarkOnSurface,
    surface = DarkCardSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = MidnightNavyElevated,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkCardBorder,
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFF87171),
    onError = DeepMidnightNavy,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = WarningRedContainer
)

val ConduitShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val LocalConduitColors = staticCompositionLocalOf { LightConduitExtendedColors }

object ConduitThemeTokens {
    val extendedColors: ConduitExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalConduitColors.current
}

@Composable
fun ConduitTheme(
    isKannada: Boolean = false,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ConduitDarkColorScheme else ConduitLightColorScheme
    val extendedColors = if (darkTheme) DarkConduitExtendedColors else LightConduitExtendedColors
    val typography = conduitTypography(isKannada = isKannada)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalConduitColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = ConduitShapes,
            content = content
        )
    }
}
