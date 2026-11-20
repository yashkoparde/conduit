package com.conduit.msme.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Core "Fintech Trust" Brand Tokens
val DeepMidnightNavy = Color(0xFF0F172A)
val MidnightNavyElevated = Color(0xFF1E293B)
val SlateInkSecondary = Color(0xFF475569)
val SlateInkMuted = Color(0xFF64748B)

// Surfaces & Borders
val CleanOffWhiteSlate = Color(0xFFF8FAFC)
val SurfaceContainerSubtle = Color(0xFFF1F5F9)
val CardWhite = Color(0xFFFFFFFF)
val CardBorderSlate = Color(0xFFE2E8F0)

// Secondary Accent — Electric Tech Cyan
val ElectricTechCyan = Color(0xFF0284C7)
val ElectricTechCyanLight = Color(0xFF38BDF8)
val ElectricTechCyanContainer = Color(0xFFE0F2FE)
val OnElectricTechCyanContainer = Color(0xFF075985)

// Regional Warmth Accent — Karnataka Gold / Amber
val KarnatakaGold = Color(0xFFD97706)
val KarnatakaGoldLight = Color(0xFFF59E0B)
val KarnatakaGoldContainer = Color(0xFFFEF3C7)
val OnKarnatakaGoldContainer = Color(0xFF92400E)

// Semantic Status Colors
val SuccessGreen = Color(0xFF166534)
val SuccessGreenBright = Color(0xFF16A34A)
val SuccessGreenContainer = Color(0xFFDCFCE7)

val WarningRed = Color(0xFFDC2626)
val WarningRedContainer = Color(0xFFFEE2E2)
val OnWarningRedContainer = Color(0xFF991B1B)

// Dark Mode Surfaces (for system dark theme compatibility)
val DarkBackgroundNavy = Color(0xFF0B1120)
val DarkCardSurface = Color(0xFF162033)
val DarkCardBorder = Color(0xFF28354D)
val DarkOnSurface = Color(0xFFF1F5F9)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)

@Immutable
data class ConduitExtendedColors(
    val cardBorder: Color,
    val cyanAccent: Color,
    val cyanContainer: Color,
    val onCyanContainer: Color,
    val karnatakaGold: Color,
    val karnatakaGoldContainer: Color,
    val onKarnatakaGoldContainer: Color,
    val successGreen: Color,
    val successGreenBright: Color,
    val successContainer: Color,
    val warningRed: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val heroDarkStart: Color,
    val heroDarkEnd: Color,
    val subtleSurface: Color
)

val LightConduitExtendedColors = ConduitExtendedColors(
    cardBorder = CardBorderSlate,
    cyanAccent = ElectricTechCyan,
    cyanContainer = ElectricTechCyanContainer,
    onCyanContainer = OnElectricTechCyanContainer,
    karnatakaGold = KarnatakaGold,
    karnatakaGoldContainer = KarnatakaGoldContainer,
    onKarnatakaGoldContainer = OnKarnatakaGoldContainer,
    successGreen = SuccessGreen,
    successGreenBright = SuccessGreenBright,
    successContainer = SuccessGreenContainer,
    warningRed = WarningRed,
    warningContainer = WarningRedContainer,
    onWarningContainer = OnWarningRedContainer,
    heroDarkStart = DeepMidnightNavy,
    heroDarkEnd = MidnightNavyElevated,
    subtleSurface = SurfaceContainerSubtle
)

val DarkConduitExtendedColors = ConduitExtendedColors(
    cardBorder = DarkCardBorder,
    cyanAccent = ElectricTechCyanLight,
    cyanContainer = Color(0xFF0C4A6E),
    onCyanContainer = ElectricTechCyanContainer,
    karnatakaGold = KarnatakaGoldLight,
    karnatakaGoldContainer = Color(0xFF78350F),
    onKarnatakaGoldContainer = KarnatakaGoldContainer,
    successGreen = Color(0xFF4ADE80),
    successGreenBright = Color(0xFF22C55E),
    successContainer = Color(0xFF14532D),
    warningRed = Color(0xFFF87171),
    warningContainer = Color(0xFF7F1D1D),
    onWarningContainer = WarningRedContainer,
    heroDarkStart = Color(0xFF090F1D),
    heroDarkEnd = MidnightNavyElevated,
    subtleSurface = Color(0xFF1E293B)
)
