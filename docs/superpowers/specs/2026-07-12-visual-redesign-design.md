# COPA Arena — Phase 2: Visual Redesign Design Doc

## Context

Phase 1 (2026-07-12) fixed correctness bugs across the FIFA tournament manager app. The user separately flagged that "no color palette is good" and the UX reads as inconsistent — most screens hardcode `Color.White`/`Color.Gray` directly instead of theme tokens, the Light theme is byte-for-byte identical to Dark (dead toggle, fixed mechanically in Phase 1 but never given a real palette), there's no consistent card/elevation language (every screen builds its own inline `Card{...}`), and the Bebas Neue/DM Sans/JetBrains Mono type system is inconsistently applied (Mono defined but unused).

Decided during brainstorming: keep the existing navy/grass-green/gold "FIFA-arena" identity rather than replacing it (it's on-brand for a tournament app) — this phase is a **design-system fix**, not a layout/UX rework (that's out of scope; Phase 3 covers animation/audio polish separately). All ~13 screens get re-skinned together in one pass so the app doesn't end up half-old/half-new.

Two visual directions were validated with the user via mockups:
- **Card style: "Stadium Lights"** — soft drop shadow + a thin gold gradient glow line across the top edge of every card, score/stat text gets a subtle gold text-glow. Chosen over a flat/bordered look (too close to today) and glassmorphism (needs API 31+ blur with a fallback, adds real implementation risk for a "design system" pass).
- **Light theme: "Navy-Tinted Light"** — background carries a faint cool blue-grey tint (`#E4E9F0`) echoing the navy brand hue, rather than generic pure-white. Chosen over a neutral off-white so Light mode still reads as "the same app," not a bolted-on generic theme.

## Color Tokens

Dark theme values are **unchanged** except resolving two ambiguities noted below. Light theme is genuinely new.

| Token | Dark (unchanged) | Light (new) |
|---|---|---|
| `Primary` | `#0D1B2A` | `#0D1B2A` (topbars/nav stay navy in both modes — brand anchor) |
| `PrimaryVariant` | `#142233` | `#142233` |
| `Secondary` / `SuccessColor` | `#3B6D11` | `#3B6D11` |
| `SecondaryVariant` | `#2E5A0D` | `#2E5A0D` |
| `AccentGold` (icons, badges, non-text highlights) | `#EF9F27` | `#EF9F27` |
| `AccentDark` (text/score rendered in gold) | `#BA7517` | `#BA7517` |
| `Surface` (cards) | `#111D2B` | `#FFFFFF` |
| `SurfaceVariant` | `#1A2B3C` | `#F0F3F7` |
| `Background` | `#0A1520` | `#E4E9F0` |
| `OnPrimary` / `OnSecondary` | `#FFFFFF` | `#FFFFFF` |
| `OnBackground` (primary text) | `#E8EDF2` | `#0D1B2A` |
| `ErrorColor` | `#E24B4A` | `#E24B4A` |
| `WarningColor` | `#EF9F27` | `#EF9F27` |
| `CardBorder` → replaced by `CardShadow` | `rgba(255,255,255,0.08)` | n/a — Stadium Lights cards use `elevation`/shadow, not a border, so this token is retired |

**Resolved ambiguities (decisions, not open questions):**
1. `SuccessColor == Secondary` (both `#3B6D11`) is **kept intentional** — "grass green = qualified/won" is a football-native metaphor that reads correctly whether it's labeling a button or a qualification badge. No change.
2. **Gold-as-text vs gold-as-accent split**: `AccentGold` (`#EF9F27`) stays for icons, badges, borders, and non-text highlights in both themes. Anywhere gold renders as **text** (scores, point totals, "Q" qualification labels used as text rather than a badge chip) must use `AccentDark` (`#BA7517`) in Light mode specifically, because `#EF9F27` on a light/white background fails legibility contrast. In Dark mode, gold text keeps using `AccentGold` as today. This is a per-usage rule enforced during the file-by-file sweep (Implementation Notes below), not a single token swap, since the same semantic role (`AccentGold`) needs to resolve differently depending on whether it's decorating a surface or rendering text.

## Typography

No font changes (Bebas Neue / DM Sans stay exactly as configured in `Type.kt`). Two fixes:
1. **JetBrains Mono actually gets used.** Today it's imported but never assigned to any `TextStyle` in `AppTypography`, despite being used ad hoc via `fontFamily = JetBrainsMono` in a few places (e.g. `BracketScreen`'s standings numbers). Add it as the font for anywhere numeric/tabular data renders (scores, points/GF/GA/GD columns, stat chart labels, MVP goal counts) — implemented as a shared `numericTextStyle(size)` helper in `ui/theme/Type.kt`, not a new Material `TextStyle` slot (Material3's `Typography` has no "mono" slot to hijack).
2. **Montserrat stays retired** — already marked "do not use," no change.

## Shape & Elevation

Current `Shapes` collapses `large` and `extraLarge` to the same `24dp` (no real large tier). New scale:
- `small = 8dp` — chips, badges, small buttons
- `medium = 12dp` — buttons, text fields, standard inputs
- `large = 16dp` — cards (the Stadium Lights card default)
- `extraLarge = 28dp` — bottom sheets, full dialogs

Elevation is expressed via the Stadium Lights card treatment, not Material's numeric `Elevation` tokens:
- Card shadow: `0 8px 24px rgba(0,0,0,0.45)` in Dark, `0 8px 20px rgba(13,27,42,0.10)` in Light.
- Top-edge glow line: `2dp` horizontal gradient, transparent → `AccentDark`/`AccentGold` → transparent, `~60-70%` opacity, drawn along the top inside edge of the card.
- No blur/glassmorphism — deferred, would need an API 31+ `RenderEffect` path plus a flat-tint fallback for API 26-30, which is real implementation risk for what's meant to be a mechanical design-system pass.

## Components

New shared composable: **`CopaCard`** in `ui/components/CopaCard.kt` — wraps Material3 `Card` with the Stadium Lights shadow + top-glow treatment and the `large` shape, taking the same `content: @Composable ColumnScope.() -> Unit` slot pattern as `Card`. This becomes the *only* card constructor used across the app; every inline `Card(...)` in the ~13 screens and remaining shared components (`FcPlayerCard`, `MatchCard`, `ConfirmDialog`, `QualTipRow`, `TeamBadge`) gets migrated to it during the sweep. `CopaBottomNavigationBar` is a navigation bar, not a card — untouched by this component, but still gets its hardcoded `Color.White`/`Color.Gray` swapped to tokens.

## Implementation Notes (the sweep)

This phase is mechanical, file-by-file:
1. Update `Color.kt` with the Light values above; update `Theme.kt`'s `LightColorScheme` to actually use them (currently identical to `DarkColorScheme`).
2. Update `Shape.kt` with the new 4-tier scale.
3. Add the `numericTextStyle` helper to `Type.kt`.
4. Build `CopaCard` in a new `ui/components/CopaCard.kt`.
5. Sweep every screen/component for: `Color.White` → `MaterialTheme.colorScheme.onSurface` (or `onPrimary` where on a colored surface), `Color.Gray` → `MaterialTheme.colorScheme.onSurfaceVariant` (with alpha where the original used `.copy(alpha=...)`), raw hex literals → the matching token, inline `Card{...}` → `CopaCard{...}`, and any place rendering a score/point/stat as text → `numericTextStyle` + the gold-text-vs-gold-accent rule above.
6. Representative files (pattern repeats across all ~13 screens + remaining components): `HomeScreen.kt`, `StatsScreen.kt`, `MatchScreen.kt`, `SplashScreen.kt`, `SettingsScreen.kt`, `AddPlayersScreen.kt`, `CreateTournamentScreen.kt`, `BracketPreviewScreen.kt`, `BracketScreen.kt`, `CeremonyScreen.kt`, `TournamentDetailScreen.kt`, `SelectFormatScreen.kt`, `HistoryScreen.kt`, `CopaBottomNavigationBar.kt`, `TeamBadge.kt`, `MatchCard.kt`, `FcPlayerCard.kt`, `ConfirmDialog.kt`, `QualTipRow.kt`.

No ViewModel, navigation, or data-layer changes — this phase touches `ui/theme/`, `ui/components/`, and the `Composable` bodies of `ui/screen/**` only.

## Verification

- `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` must stay green (no logic touched, but confirms nothing broke).
- Manual: launch app, cycle Settings theme Dark → Light → System, walk through every screen in both explicit modes checking (a) no lingering hardcoded white/gray that clashes with the new palette, (b) gold text stays legible in Light mode, (c) every card shows the Stadium Lights shadow + glow consistently, (d) numeric/score text renders in JetBrains Mono everywhere it should.
