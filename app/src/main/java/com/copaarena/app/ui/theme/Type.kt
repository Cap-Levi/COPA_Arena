package com.copaarena.app.ui.theme

import androidx.compose.ui.text.font.Font as ResourceFont
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.copaarena.app.R
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// ── Typography ──
// Display font: Orbitron — scores, player names on cards, tournament title (futuristic esports scoreboard feel)
// Body font: Rajdhani — body text, labels, buttons (athletic/technical companion to Orbitron)
// Mono font: JetBrains Mono — stats numbers
// (identifiers below keep their original names — BebasNeue/DmSans — so every existing call site
// that references them directly keeps working; only the underlying font changed. Bundled as
// static res/font/ files rather than the downloadable Google Fonts provider used elsewhere in
// this file, since the downloadable path depends on network + Play Services font cache being
// warm and silently falls back to a system font when it isn't — not acceptable for the two
// fonts carrying the whole brand identity.)

val BebasNeue = FontFamily(
    ResourceFont(resId = R.font.orbitron_bold, weight = FontWeight.Bold)
)
val DmSans = FontFamily(
    ResourceFont(resId = R.font.rajdhani_regular, weight = FontWeight.Normal),
    ResourceFont(resId = R.font.rajdhani_medium, weight = FontWeight.Medium),
    ResourceFont(resId = R.font.rajdhani_semibold, weight = FontWeight.SemiBold),
    ResourceFont(resId = R.font.rajdhani_bold, weight = FontWeight.Bold)
)
val JetBrainsMono = FontFamily(
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = provider)
)

// Keep Montserrat available for backward compat but do not use in new code
val Montserrat = FontFamily(
    Font(googleFont = GoogleFont("Montserrat"), fontProvider = provider)
)

val AppTypography = Typography(
    // ── Display: Bebas Neue (scores, big numbers, hero text) ──
    displayLarge = TextStyle(
        fontFamily = BebasNeue,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = BebasNeue,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = BebasNeue,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),

    // ── Headline: Bebas Neue (section headers, screen titles) ──
    headlineLarge = TextStyle(
        fontFamily = BebasNeue,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 1.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = BebasNeue,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = BebasNeue,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // ── Title: DM Sans (card titles, app bar titles) ──
    titleLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // ── Body: DM Sans ──
    bodyLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // ── Label: DM Sans (buttons, chips, badges) ──
    labelLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

fun numericTextStyle(size: TextUnit, emphasized: Boolean = false): TextStyle = TextStyle(
    fontFamily = JetBrainsMono,
    fontWeight = FontWeight.Bold,
    fontSize = size,
    shadow = if (emphasized) Shadow(color = AccentGold.copy(alpha = 0.5f), offset = Offset.Zero, blurRadius = 24f) else null
)
