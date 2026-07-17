package com.copaarena.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Fixed brand colors: same literal value in Dark and Light — not theme-reactive ──
val Primary          = Color(0xFF0D1B2A)   // deep navy — topbars/nav/Ceremony backdrop stay navy in both modes
val PrimaryVariant   = Color(0xFF142233)
val Secondary        = Color(0xFF3B6D11)   // grass green
val SecondaryVariant = Color(0xFF2E5A0D)
val AccentGold       = Color(0xFFEF9F27)   // FIFA gold — icons, badges, decorative highlights (never body text on a light bg)
val AccentDark       = Color(0xFFBA7517)   // gold rendered as TEXT — contrast-safe on light backgrounds
val OnPrimary        = Color(0xFFFFFFFF)   // text/icons on any fixed-brand-color surface (Primary, AccentGold, Secondary)
val OnSecondary      = Color(0xFFFFFFFF)
val ErrorColor       = Color(0xFFE24B4A)
val SuccessColor     = Secondary
val WarningColor     = AccentGold
val OverlayScrim     = Color(0x80000000)   // fixed translucent-black overlay (Ceremony MVP card)

// ── Theme-reactive tokens: resolve through MaterialTheme.colorScheme, differ Dark vs Light ──
val Background: Color
    @Composable get() = MaterialTheme.colorScheme.background
val Surface: Color
    @Composable get() = MaterialTheme.colorScheme.surface
val SurfaceVariant: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val OnBackground: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground

// ── Dark-mode literal values (fed into DarkColorScheme in Theme.kt) ──
val BackgroundDark      = Color(0xFF0A1520)
val SurfaceDark         = Color(0xFF111D2B)
val SurfaceVariantDark  = Color(0xFF1A2B3C)
val OnBackgroundDark    = Color(0xFFE8EDF2)

// ── Light-mode literal values (fed into LightColorScheme in Theme.kt) — "Navy-Tinted Light" ──
val BackgroundLight     = Color(0xFFE4E9F0)
val SurfaceLight        = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF0F3F7)
val OnBackgroundLight   = Color(0xFF0D1B2A)
