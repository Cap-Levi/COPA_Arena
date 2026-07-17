# COPA Arena — Phase 2 Visual Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give COPA Arena a consistent, working design system — a real Light theme, one shared card component ("Stadium Lights" style) instead of ~15 inline `Card{}` blocks, JetBrains Mono actually used for numeric/stat text, and every hardcoded `Color.White`/`Color.Gray`/raw-hex literal replaced with the correct semantic token — with zero changes to logic, navigation, or ViewModels.

**Architecture:** Two token classes: **fixed brand tokens** (`Primary`, `AccentGold`, `AccentDark`, `OnPrimary`, `Secondary`, `ErrorColor`, `OverlayScrim`) that hold the identical value in Dark and Light, and **theme-reactive tokens** (`Background`, `Surface`, `SurfaceVariant`, `OnBackground`) that become `@Composable get()` properties reading `MaterialTheme.colorScheme` — this is the one real fix that makes Light mode actually work, since every screen currently reads these as plain compile-time `val`s that never flowed through the runtime theme at all. Every inline `Card{}` becomes `CopaCard{}` (shared component, Material3 elevation + a thin gold top-edge glow line). Five components turned out to be dead code during a grep pass — deleted rather than redesigned, following the Phase 1 precedent.

**Tech Stack:** Kotlin, Jetpack Compose, Material3.

## Global Constraints

- This repo has **no git** (`git status` confirms "not a git repository") — every task's "commit" step is replaced with a plain build/verify step. Do not run any `git` commands.
- Touch only `ui/theme/`, `ui/components/`, and the `@Composable` bodies inside `ui/screen/**`. No ViewModel, navigation, DAO, or use-case changes.
- Color mapping rule (apply per-instance, don't blindly substitute):
  - Text/icon/spinner sitting on a **fixed-brand-color surface** (`AccentGold` button, `Primary`-navy TabRow/topbar/Ceremony backdrop) → `OnPrimary` (and `OnPrimary.copy(alpha = 0.5f)` for the inactive/muted variant — **never** `OnBackground` here, since `OnBackground` is theme-reactive and would go dark-navy-on-navy invisible in Light mode).
  - Text/icon sitting on a **theme-reactive surface** (`Background`, `Surface`, `SurfaceVariant`) → `OnBackground`, muted via `.copy(alpha = 0.4-0.5f)` (the existing codebase convention — there's no separate "onSurfaceVariant" token, every screen already mutes `OnBackground` with alpha).
  - Gold rendered as **text** (scores, point totals) on a theme-reactive Light background → `AccentDark`, not `AccentGold` (contrast). Gold as an icon/badge/decorative line stays `AccentGold` in both themes.
- Verify every task with: `./gradlew compileDebugKotlin` (must succeed) and `grep -n "Color\.White\|Color\.Gray\|Color\.Black\|Color(0x" <file>` on the touched file (must show zero matches, except intentional fixed-brand hex literals that already live in `Color.kt` itself).

---

## Task 1: Rewrite `Color.kt` — split fixed vs. theme-reactive tokens

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/theme/Color.kt` (full rewrite)

**Interfaces:**
- Produces: `Primary`, `PrimaryVariant`, `Secondary`, `SecondaryVariant`, `AccentGold`, `AccentDark`, `OnPrimary`, `OnSecondary`, `ErrorColor`, `SuccessColor`, `WarningColor`, `OverlayScrim` (all fixed `Color` vals); `Background`, `Surface`, `SurfaceVariant`, `OnBackground` (theme-reactive `@Composable get()` properties); `BackgroundDark/Light`, `SurfaceDark/Light`, `SurfaceVariantDark/Light`, `OnBackgroundDark/Light` (literal values consumed by Task 2's `Theme.kt`). `CardBorder` is removed (retired — Task 6 and the HomeScreen task in Task 9 replace its only two usages).

- [ ] **Step 1: Replace the full file contents**

```kotlin
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
```

- [ ] **Step 2: Verify — this file alone won't compile yet** (Theme.kt in Task 2 still references the old `Background`/`Surface`/etc. as plain vals in its scheme builders). Do not run the build until Task 2 is done; these two tasks are one atomic unit.

---

## Task 2: Rewrite `Theme.kt` — wire the real Light palette through `MaterialTheme.colorScheme`

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/theme/Theme.kt` (full rewrite)

**Interfaces:**
- Consumes: all tokens produced by Task 1 (`Primary`, `OnPrimary`, `PrimaryVariant`, `Secondary`, `OnSecondary`, `SecondaryVariant`, `AccentGold`, `ErrorColor`, `BackgroundDark/Light`, `SurfaceDark/Light`, `SurfaceVariantDark/Light`, `OnBackgroundDark/Light`).
- Produces: `COPAArenaTheme(darkTheme, dynamicColor, content)` — unchanged public signature (already consumed correctly by `MainActivity.kt` from Phase 1, no caller changes needed).

- [ ] **Step 1: Replace the full file contents**

```kotlin
package com.copaarena.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryVariant,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryVariant,
    tertiary = AccentGold,
    onTertiary = OnPrimary,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnBackgroundDark,
    error = ErrorColor,
    onError = OnPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryVariant,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryVariant,
    tertiary = AccentGold,
    onTertiary = OnPrimary,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnBackgroundLight,
    error = ErrorColor,
    onError = OnPrimary
)

@Composable
fun COPAArenaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

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
        typography = AppTypography,
        shapes = Shapes,
        content = content
    )
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL (Tasks 1+2 together must compile clean before continuing — every other task depends on these tokens existing).

---

## Task 3: New shape scale in `Shape.kt`

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/theme/Shape.kt`

**Interfaces:**
- Produces: `Shapes` with a real 4-tier scale (`large` and `extraLarge` no longer collapse to the same value).

- [ ] **Step 1: Replace the full file contents**

```kotlin
package com.copaarena.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),      // chips, badges, small buttons
    medium = RoundedCornerShape(12.dp),    // buttons, text fields, standard inputs
    large = RoundedCornerShape(16.dp),     // cards (CopaCard default)
    extraLarge = RoundedCornerShape(28.dp) // bottom sheets, full dialogs
)
```

- [ ] **Step 2: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 4: Add `numericTextStyle` helper to `Type.kt`

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/theme/Type.kt:1-40` (add imports + one function; `AppTypography` block is untouched)

**Interfaces:**
- Produces: `numericTextStyle(size: TextUnit, emphasized: Boolean = false): TextStyle` — JetBrains Mono, bold, with an optional gold glow shadow for the one "hero" scoreboard number (Task 15, MatchScreen). Table/list numeric text always uses `emphasized = false` (no glow — glow on every number in a table would be visual noise, not polish).

- [ ] **Step 1: Add imports after the existing import block (after line 10, before `val provider = ...`)**

```kotlin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.TextUnit
```

- [ ] **Step 2: Add the helper function immediately after the `AppTypography = Typography(...)` block closes (end of file, after the final `)`)**

```kotlin

fun numericTextStyle(size: TextUnit, emphasized: Boolean = false): TextStyle = TextStyle(
    fontFamily = JetBrainsMono,
    fontWeight = FontWeight.Bold,
    fontSize = size,
    shadow = if (emphasized) Shadow(color = AccentGold.copy(alpha = 0.5f), offset = Offset.Zero, blurRadius = 24f) else null
)
```

- [ ] **Step 3: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 5: New shared `CopaCard` component

**Files:**
- Create: `app/src/main/java/com/copaarena/app/ui/components/CopaCard.kt`

**Interfaces:**
- Produces: `CopaCard(modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.large, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)` — every subsequent screen task replaces its inline `Card(...)` calls with this.
- Consumes: `Surface`, `AccentGold` from Task 1.

- [ ] **Step 1: Create the file**

```kotlin
package com.copaarena.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.copaarena.app.ui.theme.AccentGold
import com.copaarena.app.ui.theme.Surface

/**
 * The one shared card used everywhere: Material3 elevation (theme-adaptive shadow, no manual
 * rgba tuning needed) plus a thin gold "Stadium Lights" glow line across the top edge.
 */
@Composable
fun CopaCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardColors = CardDefaults.cardColors(containerColor = Surface)
    val cardElevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    val body: @Composable ColumnScope.() -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, AccentGold.copy(alpha = 0.65f), Color.Transparent)
                    )
                )
        )
        content()
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = cardColors, elevation = cardElevation, content = body)
    } else {
        Card(modifier = modifier, shape = shape, colors = cardColors, elevation = cardElevation, content = body)
    }
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 6: Delete dead component files

**Files:**
- Delete: `app/src/main/java/com/copaarena/app/ui/components/ConfirmDialog.kt`
- Delete: `app/src/main/java/com/copaarena/app/ui/components/FcPlayerCard.kt`
- Delete: `app/src/main/java/com/copaarena/app/ui/components/MatchCard.kt`
- Delete: `app/src/main/java/com/copaarena/app/ui/components/QualTipRow.kt`
- Delete: `app/src/main/java/com/copaarena/app/ui/components/TeamBadge.kt`

**Why:** confirmed via `grep -rn "FcPlayerCard\|QualTipRow\|ConfirmDialog\|TeamBadge" app/src/main/java/com/copaarena/app/ui/screen` (zero matches) and `grep -rn "import com.copaarena.app.ui.components.MatchCard" app/src/main/java` (zero matches, `BracketScreen.kt` defines and uses its own local `MatchCard` in a different package) — all five files are unreferenced dead code, same pattern as `StandingsTable.kt`/`GoalSheet.kt` deleted in Phase 1. `TeamBadge` is only called from the two other dead files, so it's transitively dead too.

- [ ] **Step 1: Delete the five files**

```bash
rm "app/src/main/java/com/copaarena/app/ui/components/ConfirmDialog.kt"
rm "app/src/main/java/com/copaarena/app/ui/components/FcPlayerCard.kt"
rm "app/src/main/java/com/copaarena/app/ui/components/MatchCard.kt"
rm "app/src/main/java/com/copaarena/app/ui/components/QualTipRow.kt"
rm "app/src/main/java/com/copaarena/app/ui/components/TeamBadge.kt"
```

- [ ] **Step 2: Verify nothing references them**

Run: `grep -rn "ConfirmDialog(\|FcPlayerCard(\|QualTipRow(\|TeamBadge(" app/src/main/java --include=*.kt`
Expected: no output (confirms deletion was safe)

- [ ] **Step 3: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 7: Sweep `CopaBottomNavigationBar.kt`

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/components/CopaBottomNavigationBar.kt:15,66,102,108`

**Interfaces:**
- Consumes: `OnBackground` (theme-reactive, Task 1) — this component's `NavigationBar` uses `containerColor = Surface`, which is genuinely theme-reactive (unlike the several `Primary`-backed TabRows in later tasks), so its text/icon tokens must be the theme-reactive `OnBackground`, not the fixed `OnPrimary`.

- [ ] **Step 1: Remove the now-unused `Color` import (line 15)**

Old:
```kotlin
import androidx.compose.ui.graphics.Color
```
New: delete this line entirely (no other use of raw `Color` remains in this file after the edits below).

- [ ] **Step 2: Add the `OnBackground` import** next to the existing `AccentGold`/`Surface`/`SurfaceVariant` imports:

Old:
```kotlin
import com.copaarena.app.ui.theme.AccentGold
import com.copaarena.app.ui.theme.Surface
import com.copaarena.app.ui.theme.SurfaceVariant
```
New:
```kotlin
import com.copaarena.app.ui.theme.AccentGold
import com.copaarena.app.ui.theme.OnBackground
import com.copaarena.app.ui.theme.Surface
import com.copaarena.app.ui.theme.SurfaceVariant
```

- [ ] **Step 3: Fix `contentColor` (was line 66)**

Old:
```kotlin
    NavigationBar(
        containerColor = Surface,
        contentColor = Color.White
    ) {
```
New:
```kotlin
    NavigationBar(
        containerColor = Surface,
        contentColor = OnBackground
    ) {
```

- [ ] **Step 4: Fix inactive icon tint (was line 102)**

Old:
```kotlin
                        tint = if (isSelected) AccentGold else Color.Gray
```
New:
```kotlin
                        tint = if (isSelected) AccentGold else OnBackground.copy(alpha = 0.5f)
```

- [ ] **Step 5: Fix inactive label color (was line 108)**

Old:
```kotlin
                        color = if (isSelected) AccentGold else Color.Gray
```
New:
```kotlin
                        color = if (isSelected) AccentGold else OnBackground.copy(alpha = 0.5f)
```

- [ ] **Step 6: Verify no raw color literals remain**

Run: `grep -n "Color\.White\|Color\.Gray\|Color\.Black" app/src/main/java/com/copaarena/app/ui/components/CopaBottomNavigationBar.kt`
Expected: no output

- [ ] **Step 7: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 8: Sweep `HomeScreen.kt` — `CopaCard` + tokens

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `CopaCard` (Task 5), `OnBackground`, `OnPrimary` (Task 1).

- [ ] **Step 1: Add the `CopaCard` and `OnPrimary` imports** next to the existing `com.copaarena.app.ui.components.CopaBottomNavigationBar` import:

Old:
```kotlin
import com.copaarena.app.ui.components.CopaBottomNavigationBar
```
New:
```kotlin
import com.copaarena.app.ui.components.CopaBottomNavigationBar
import com.copaarena.app.ui.components.CopaCard
```
(`OnPrimary` comes in via the existing `import com.copaarena.app.ui.theme.*` wildcard already present in this file — no separate import line needed.)

- [ ] **Step 2: Fix the Settings icon tint**

Old:
```kotlin
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
```
New:
```kotlin
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = OnBackground)
                    }
```

- [ ] **Step 3: Fix the FAB content color**

Old:
```kotlin
            FloatingActionButton(
                onClick = { navController.navigate(Screen.CreateTournament.route) },
                containerColor = AccentGold,
                contentColor = Color.White
            ) {
```
New:
```kotlin
            FloatingActionButton(
                onClick = { navController.navigate(Screen.CreateTournament.route) },
                containerColor = AccentGold,
                contentColor = OnPrimary
            ) {
```

- [ ] **Step 4: Replace the active-tournament Card with CopaCard and drop the `CardBorder` gradient border**

Old:
```kotlin
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(CardBorder, Color.Transparent))
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "ACTIVE TOURNAMENT",
```
New:
```kotlin
                CopaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "ACTIVE TOURNAMENT",
```
(the matching closing `}` for the old `Card(...) { ... }` stays as-is — `CopaCard` takes the same trailing-lambda shape, only the opening call and its `border=`/`shape=`/`colors=` params are removed.)

- [ ] **Step 5: Fix the "Continue" button text color**

Old:
```kotlin
                            Text("Continue", color = Color.White, fontWeight = FontWeight.Bold)
```
New:
```kotlin
                            Text("Continue", color = OnPrimary, fontWeight = FontWeight.Bold)
```

- [ ] **Step 6: Replace the empty-state Card with CopaCard**

Old:
```kotlin
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(CardBorder, Color.Transparent))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.SportsSoccer,
```
New:
```kotlin
                CopaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.SportsSoccer,
```

- [ ] **Step 7: Fix the two remaining button texts/icons on the empty-state CTA**

Old:
```kotlin
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start New Tournament", color = Color.White, fontWeight = FontWeight.Bold)
```
New:
```kotlin
                            Icon(Icons.Default.Add, contentDescription = null, tint = OnPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start New Tournament", color = OnPrimary, fontWeight = FontWeight.Bold)
```

- [ ] **Step 8: Verify no raw color literals or `CardBorder` remain**

Run: `grep -n "Color\.White\|Color\.Gray\|Color\.Black\|CardBorder" app/src/main/java/com/copaarena/app/ui/screen/home/HomeScreen.kt`
Expected: no output

- [ ] **Step 9: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 9: Sweep `SplashScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/splash/SplashScreen.kt:119`

- [ ] **Step 1: Fix the "Syncing database…" text color**

Old:
```kotlin
                Text(
                    text = "Syncing database…",
                    fontFamily = DmSans,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
```
New:
```kotlin
                Text(
                    text = "Syncing database…",
                    fontFamily = DmSans,
                    color = OnBackground.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
```

- [ ] **Step 2: Remove the now-unused `Color` import**

Old:
```kotlin
import androidx.compose.ui.graphics.Color
```
New: delete this line (no other raw `Color` usage remains in this file).

- [ ] **Step 3: Verify no raw color literals remain**

Run: `grep -n "Color\.White\|Color\.Gray\|Color\.Black" app/src/main/java/com/copaarena/app/ui/screen/splash/SplashScreen.kt`
Expected: no output

- [ ] **Step 4: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 10: Sweep `CreateTournamentScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/tournament/CreateTournamentScreen.kt:106`

- [ ] **Step 1: Fix the "Next" button text color** (button's `containerColor = AccentGold`)

Old:
```kotlin
                Text(
                    if (isRestartMode) "Next → Review Teams" else "Next → Add Players",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
```
New:
```kotlin
                Text(
                    if (isRestartMode) "Next → Review Teams" else "Next → Add Players",
                    color = OnPrimary,
                    fontWeight = FontWeight.Bold
                )
```
(`OnPrimary` is already available via the existing `import com.copaarena.app.ui.theme.*` wildcard — no new import line needed. The `androidx.compose.ui.graphics.Color` import stays, since `Color.Transparent` is still used elsewhere in this file for the TopAppBar.)

- [ ] **Step 2: Verify no raw `Color.White/Gray/Black` remain**

Run: `grep -n "Color\.White\|Color\.Gray\|Color\.Black" app/src/main/java/com/copaarena/app/ui/screen/tournament/CreateTournamentScreen.kt`
Expected: no output

- [ ] **Step 3: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 11: Sweep `AddPlayersScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/tournament/AddPlayersScreen.kt:185,386`
- Modify: same file's `PlayerCard` composable — swap its `Card{}` for `CopaCard{}`

**Interfaces:**
- Consumes: `CopaCard` (Task 5).

- [ ] **Step 1: Add the `CopaCard` import**

Old:
```kotlin
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*
```
New:
```kotlin
import com.copaarena.app.ui.components.CopaCard
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*
```

- [ ] **Step 2: Fix the "Next" button text color** (line 185, button's `containerColor = AccentGold`)

Old:
```kotlin
                Text(
                    if (players.size == 4) "Next → Preview Bracket" else "Next → Select Format",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
```
New:
```kotlin
                Text(
                    if (players.size == 4) "Next → Preview Bracket" else "Next → Select Format",
                    color = OnPrimary,
                    fontWeight = FontWeight.Bold
                )
```

- [ ] **Step 3: Fix the "Add Player"/"Update Team" button text color** (line 386, same `AccentGold` button pattern)

Old:
```kotlin
                            Text(
                                if (isRestartMode) "Update Team" else "Add Player",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
```
New:
```kotlin
                            Text(
                                if (isRestartMode) "Update Team" else "Add Player",
                                color = OnPrimary,
                                fontWeight = FontWeight.Bold
                            )
```

- [ ] **Step 4: Swap `PlayerCard`'s `Card{}` for `CopaCard{}`**

Old:
```kotlin
@Composable
fun PlayerCard(player: PlayerEntity, isRestartMode: Boolean, onRemove: () -> Unit, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isRestartMode, onClick = onClick)
    ) {
```
New:
```kotlin
@Composable
fun PlayerCard(player: PlayerEntity, isRestartMode: Boolean, onRemove: () -> Unit, onClick: () -> Unit) {
    CopaCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (isRestartMode) onClick else null
    ) {
```
(the trailing `Column(...)` body and closing braces stay exactly as-is; only the wrapper call changes. Note the original used `.clickable(enabled = isRestartMode, ...)` — `CopaCard`'s `onClick` param achieves the same "only clickable in restart mode" behavior by passing `null` otherwise.)

- [ ] **Step 5: Verify no raw color literals remain**

Run: `grep -n "Color\.White\|Color\.Gray\|Color\.Black" app/src/main/java/com/copaarena/app/ui/screen/tournament/AddPlayersScreen.kt`
Expected: no output

- [ ] **Step 6: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 12: Sweep `SelectFormatScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/tournament/SelectFormatScreen.kt:104,128`
- Modify: same file's `FormatCard` composable — swap `Card{}` for `CopaCard{}`

**Interfaces:**
- Consumes: `CopaCard` (Task 5).

- [ ] **Step 1: Add the `CopaCard` import**

Old:
```kotlin
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*
```
New:
```kotlin
import com.copaarena.app.ui.components.CopaCard
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*
```

- [ ] **Step 2: Fix the matches-per-fixture selector button content color** (selected state container is `AccentGold`; unselected stays `Surface`/`OnBackground` as-is, only the `Color.White` branch changes)

Old:
```kotlin
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) AccentGold else Surface,
                            contentColor = if (isSelected) Color.White else OnBackground.copy(alpha = 0.5f)
                        ),
```
New:
```kotlin
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) AccentGold else Surface,
                            contentColor = if (isSelected) OnPrimary else OnBackground.copy(alpha = 0.5f)
                        ),
```

- [ ] **Step 3: Fix the "Preview Bracket" button text color** (button's `containerColor = AccentGold`)

Old:
```kotlin
                Text("Preview Bracket", color = Color.White, fontWeight = FontWeight.Bold)
```
New:
```kotlin
                Text("Preview Bracket", color = OnPrimary, fontWeight = FontWeight.Bold)
```

- [ ] **Step 4: Swap `FormatCard`'s `Card{}` for `CopaCard{}`** — this one uses Material3's `border` param for the selected-state outline, which `CopaCard` doesn't expose; keep the selection border by applying it via `Modifier.border` on the outer modifier instead

Old:
```kotlin
@Composable
fun FormatCard(format: TournamentFormat, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = if (isSelected) BorderStroke(2.dp, AccentGold) else BorderStroke(1.dp, SurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.titleMedium,
            color = if (isSelected) AccentGold else OnBackground
        )
    }
}
```
New:
```kotlin
@Composable
fun FormatCard(format: TournamentFormat, label: String, isSelected: Boolean, onClick: () -> Unit) {
    CopaCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) AccentGold else SurfaceVariant,
                shape = MaterialTheme.shapes.large
            ),
        onClick = onClick
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.titleMedium,
            color = if (isSelected) AccentGold else OnBackground
        )
    }
}
```
(add `import androidx.compose.foundation.border` next to the existing `androidx.compose.foundation.BorderStroke` import — `BorderStroke` itself is no longer used and its import can be removed.)

- [ ] **Step 5: Verify no raw color literals remain**

Run: `grep -n "Color\.White\|Color\.Gray\|Color\.Black" app/src/main/java/com/copaarena/app/ui/screen/tournament/SelectFormatScreen.kt`
Expected: no output

- [ ] **Step 6: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 13: Sweep `BracketPreviewScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/tournament/BracketPreviewScreen.kt:162,168`
- Modify: same file's summary `Card{}` — swap for `CopaCard{}`

**Interfaces:**
- Consumes: `CopaCard` (Task 5).

- [ ] **Step 1: Add the `CopaCard` import**

Old:
```kotlin
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*
```
New:
```kotlin
import com.copaarena.app.ui.components.CopaCard
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*
```

- [ ] **Step 2: Fix the loading spinner color** (button's `containerColor = AccentGold`)

Old:
```kotlin
                if (isKickOffLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
```
New:
```kotlin
                if (isKickOffLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = OnPrimary,
                        strokeWidth = 2.dp
                    )
```

- [ ] **Step 3: Fix the "KICK OFF" button text color**

Old:
```kotlin
                    Text(
                        "⚽ KICK OFF",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BebasNeue,
                        fontSize = MaterialTheme.typography.titleLarge.fontSize
                    )
```
New:
```kotlin
                    Text(
                        "⚽ KICK OFF",
                        color = OnPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = BebasNeue,
                        fontSize = MaterialTheme.typography.titleLarge.fontSize
                    )
```

- [ ] **Step 4: Swap the summary Card for CopaCard**

Old:
```kotlin
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
```
New:
```kotlin
            CopaCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
```

- [ ] **Step 5: Verify no raw color literals remain**

Run: `grep -n "Color\.White\|Color\.Gray\|Color\.Black" app/src/main/java/com/copaarena/app/ui/screen/tournament/BracketPreviewScreen.kt`
Expected: no output

- [ ] **Step 6: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 14: Sweep `BracketScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/tournament/BracketScreen.kt:79,96`
- Modify: same file's `MatchesTab`/`StandingsTab` inline `Card{}` calls and the local `MatchCard` composable — swap for `CopaCard{}`
- Modify: numeric standings text — use `numericTextStyle` instead of raw `fontFamily = JetBrainsMono`

**Interfaces:**
- Consumes: `CopaCard` (Task 5), `numericTextStyle` (Task 4), `OnPrimary` (Task 1).

- [ ] **Step 1: Add the `CopaCard` import**

Old:
```kotlin
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*
import com.copaarena.app.ui.components.CopaBottomNavigationBar
```
New:
```kotlin
import com.copaarena.app.ui.components.CopaBottomNavigationBar
import com.copaarena.app.ui.components.CopaCard
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*
```

- [ ] **Step 2: Fix the TabRow content color** (this TabRow's `containerColor = Primary`, a fixed-brand-color surface — not theme-reactive)

Old:
```kotlin
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Primary,
                contentColor = Color.White,
```
New:
```kotlin
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Primary,
                contentColor = OnPrimary,
```

- [ ] **Step 3: Fix the inactive tab text color** (same fixed-`Primary` surface — must NOT use theme-reactive `OnBackground`, which would be invisible in Light mode on this always-navy bar)

Old:
```kotlin
                        Text(
                            title,
                            color = if (selectedTabIndex == index) AccentGold else Color.Gray,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
```
New:
```kotlin
                        Text(
                            title,
                            color = if (selectedTabIndex == index) AccentGold else OnPrimary.copy(alpha = 0.5f),
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
```

- [ ] **Step 4: Swap the local `MatchCard`'s `Card{}` for `CopaCard{}`**

Old:
```kotlin
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (isSeeded) it.clickable(onClick = onClick) else it },
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
```
New:
```kotlin
    CopaCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (isSeeded) onClick else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
```

- [ ] **Step 5: Verify no raw color literals remain**

Run: `grep -n "Color\.White\|Color\.Gray\|Color\.Black" app/src/main/java/com/copaarena/app/ui/screen/tournament/BracketScreen.kt`
Expected: no output

- [ ] **Step 6: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 15: Sweep `MatchScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/match/MatchScreen.kt:78,155,265,273,488` (approximate — see exact old/new blocks below)
- Modify: the live scoreboard `Text` — use `numericTextStyle(56.sp, emphasized = true)` (this is the one "hero" number in the whole app that gets the gold glow)

**Interfaces:**
- Consumes: `numericTextStyle` (Task 4), `OnPrimary` (Task 1).

- [ ] **Step 1: Fix the FAB content color**

Old:
```kotlin
                FloatingActionButton(
                    onClick = { showGoalSheet = true },
                    containerColor = AccentGold,
                    contentColor = Color.White
                ) {
```
New:
```kotlin
                FloatingActionButton(
                    onClick = { showGoalSheet = true },
                    containerColor = AccentGold,
                    contentColor = OnPrimary
                ) {
```

- [ ] **Step 2: Apply `numericTextStyle` to the live scoreboard number (this is the one emphasized/glow usage in the app)**

Old:
```kotlin
                        Text(
                            text = "$scoreA - $scoreB",
                            fontFamily = BebasNeue,
                            fontSize = 56.sp,
                            color = AccentGold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
```
New:
```kotlin
                        Text(
                            text = "$scoreA - $scoreB",
                            style = com.copaarena.app.ui.theme.numericTextStyle(56.sp, emphasized = true),
                            color = AccentGold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
```

- [ ] **Step 3: Fix the "Confirm Full Time" button text color**

Old:
```kotlin
                    Text("Confirm Full Time", color = Color.White, fontWeight = FontWeight.Bold)
```
New:
```kotlin
                    Text("Confirm Full Time", color = OnPrimary, fontWeight = FontWeight.Bold)
```

- [ ] **Step 4: Fix the goal-sheet scorer TabRow inactive text** (two occurrences — this TabRow's `containerColor = Primary`, same fixed-brand-surface rule as Task 14)

Old:
```kotlin
                        Tab(selected = selectedScorerTab == 0, onClick = { selectedScorerTab = 0 }) {
                            Text(
                                pA?.name ?: "Player A",
                                modifier = Modifier.padding(16.dp),
                                color = if (selectedScorerTab == 0) AccentGold else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Tab(selected = selectedScorerTab == 1, onClick = { selectedScorerTab = 1 }) {
                            Text(
                                pB?.name ?: "Player B",
                                modifier = Modifier.padding(16.dp),
                                color = if (selectedScorerTab == 1) AccentGold else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
```
New:
```kotlin
                        Tab(selected = selectedScorerTab == 0, onClick = { selectedScorerTab = 0 }) {
                            Text(
                                pA?.name ?: "Player A",
                                modifier = Modifier.padding(16.dp),
                                color = if (selectedScorerTab == 0) AccentGold else OnPrimary.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Tab(selected = selectedScorerTab == 1, onClick = { selectedScorerTab = 1 }) {
                            Text(
                                pB?.name ?: "Player B",
                                modifier = Modifier.padding(16.dp),
                                color = if (selectedScorerTab == 1) AccentGold else OnPrimary.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        }
```

- [ ] **Step 5: Fix the "Confirm Shootout Result" button text color** (button's `containerColor = AccentGold`, in `PenaltyShootoutEntry`)

Old:
```kotlin
                Text("Confirm Shootout Result", color = Color.White, fontWeight = FontWeight.Bold)
```
New:
```kotlin
                Text("Confirm Shootout Result", color = OnPrimary, fontWeight = FontWeight.Bold)
```

- [ ] **Step 6: Swap the scoreboard `Card{}`, the goal-log item `Card{}`, and the `PenaltyShootoutEntry` `Card{}` for `CopaCard{}`** — add the import first:

Old:
```kotlin
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*
```
New:
```kotlin
import com.copaarena.app.ui.components.CopaCard
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*
```
Then, for the scoreboard card:

Old:
```kotlin
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(SurfaceVariant, Surface)
                            )
                        )
                        .padding(24.dp)
                ) {
```
New:
```kotlin
            CopaCard(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(SurfaceVariant, Surface)
                            )
                        )
                        .padding(24.dp)
                ) {
```
For the goal-log item card:

Old:
```kotlin
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(onLongPress = { goalToDelete = goal })
                            },
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
```
New:
```kotlin
                    CopaCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(onLongPress = { goalToDelete = goal })
                            }
                    ) {
```
For `PenaltyShootoutEntry`'s card:

Old:
```kotlin
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "STILL LEVEL — PENALTY SHOOTOUT",
```
New:
```kotlin
    CopaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "STILL LEVEL — PENALTY SHOOTOUT",
```

- [ ] **Step 7: Verify no raw color literals remain**

Run: `grep -n "Color\.White\|Color\.Gray\|Color\.Black" app/src/main/java/com/copaarena/app/ui/screen/match/MatchScreen.kt`
Expected: no output

- [ ] **Step 8: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 16: Sweep `StatsScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/stats/StatsScreen.kt:55,72,141,143,156,227,228,230,248`
- Modify: both inline `Card{}` calls — swap for `CopaCard{}`

**Interfaces:**
- Consumes: `CopaCard` (Task 5), `OnPrimary` (Task 1).

- [ ] **Step 1: Add the `CopaCard` import**

Old:
```kotlin
import com.copaarena.app.ui.components.CopaBottomNavigationBar
import com.copaarena.app.ui.theme.*
```
New:
```kotlin
import com.copaarena.app.ui.components.CopaBottomNavigationBar
import com.copaarena.app.ui.components.CopaCard
import com.copaarena.app.ui.theme.*
```

- [ ] **Step 2: Fix the TabRow content color** (`containerColor = Primary`, fixed-brand surface)

Old:
```kotlin
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Primary,
                contentColor = Color.White,
```
New:
```kotlin
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Primary,
                contentColor = OnPrimary,
```

- [ ] **Step 3: Fix the inactive tab text color**

Old:
```kotlin
                            Text(
                                title,
                                color = if (selectedTabIndex == index) AccentGold else Color.Gray,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
```
New:
```kotlin
                            Text(
                                title,
                                color = if (selectedTabIndex == index) AccentGold else OnPrimary.copy(alpha = 0.5f),
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
```

- [ ] **Step 4: Swap the Top Scorers item Card for CopaCard**

Old:
```kotlin
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                colors = CardDefaults.cardColors(containerColor = Surface),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(modifier = Modifier.weight(1f)) {
                                        Text(icon, modifier = Modifier.width(32.dp))
```
New:
```kotlin
                            CopaCard(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(modifier = Modifier.weight(1f)) {
                                        Text(icon, modifier = Modifier.width(32.dp))
```

- [ ] **Step 5: Fix the BarChart's Java-interop text colors** (`.toArgb()` calls — `OnPrimary` is a fixed token, safe to reference from these non-`@Composable` `factory`/`update` lambdas; the chart's own background is pinned to `Primary`/navy via `setGridBackgroundColor`, so its text must stay the fixed white regardless of app theme)

Old:
```kotlin
                                    factory = { context ->
                                        BarChart(context).apply {
                                            description.isEnabled = false
                                            legend.isEnabled = false
                                            axisLeft.textColor = Color.White.toArgb()
                                            axisRight.isEnabled = false
                                            xAxis.textColor = Color.White.toArgb()
                                            setGridBackgroundColor(Primary.toArgb())
```
New:
```kotlin
                                    factory = { context ->
                                        BarChart(context).apply {
                                            description.isEnabled = false
                                            legend.isEnabled = false
                                            axisLeft.textColor = OnPrimary.toArgb()
                                            axisRight.isEnabled = false
                                            xAxis.textColor = OnPrimary.toArgb()
                                            setGridBackgroundColor(Primary.toArgb())
```
And:
```kotlin
                                        val dataSet = BarDataSet(entries, "Goals").apply {
                                            color = AccentGold.toArgb()
                                            valueTextColor = Color.White.toArgb()
                                        }
```
New:
```kotlin
                                        val dataSet = BarDataSet(entries, "Goals").apply {
                                            color = AccentGold.toArgb()
                                            valueTextColor = OnPrimary.toArgb()
                                        }
```

- [ ] **Step 6: Swap the Global Leaderboard item Card for CopaCard**

Old:
```kotlin
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                colors = CardDefaults.cardColors(containerColor = Surface),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${index + 1}.",
```
New:
```kotlin
                            CopaCard(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${index + 1}.",
```

- [ ] **Step 7: Fix the LineChart's Java-interop text colors** (same reasoning as Step 5)

Old:
```kotlin
                                factory = { context ->
                                    LineChart(context).apply {
                                        description.isEnabled = false
                                        legend.textColor = Color.White.toArgb()
                                        axisLeft.textColor = Color.White.toArgb()
                                        axisRight.isEnabled = false
                                        xAxis.textColor = Color.White.toArgb()
                                        setGridBackgroundColor(Primary.toArgb())
```
New:
```kotlin
                                factory = { context ->
                                    LineChart(context).apply {
                                        description.isEnabled = false
                                        legend.textColor = OnPrimary.toArgb()
                                        axisLeft.textColor = OnPrimary.toArgb()
                                        axisRight.isEnabled = false
                                        xAxis.textColor = OnPrimary.toArgb()
                                        setGridBackgroundColor(Primary.toArgb())
```
And:
```kotlin
                                        LineDataSet(entries, stat.playerName).apply {
                                            color = AccentGold.toArgb()
                                            setDrawFilled(true)
                                            fillAlpha = (0.3f * 255).toInt()
                                            fillColor = color
                                            valueTextColor = Color.White.toArgb()
                                        }
```
New:
```kotlin
                                        LineDataSet(entries, stat.playerName).apply {
                                            color = AccentGold.toArgb()
                                            setDrawFilled(true)
                                            fillAlpha = (0.3f * 255).toInt()
                                            fillColor = color
                                            valueTextColor = OnPrimary.toArgb()
                                        }
```

- [ ] **Step 8: Verify no raw color literals remain**

Run: `grep -n "Color\.White\|Color\.Gray\|Color\.Black" app/src/main/java/com/copaarena/app/ui/screen/stats/StatsScreen.kt`
Expected: no output

- [ ] **Step 9: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 17: Sweep `HistoryScreen.kt` — CopaCard only (no hardcoded colors found)

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/history/HistoryScreen.kt`

**Interfaces:**
- Consumes: `CopaCard` (Task 5).

- [ ] **Step 1: Add the `CopaCard` import**

Old:
```kotlin
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*
import com.copaarena.app.ui.components.CopaBottomNavigationBar
```
New:
```kotlin
import com.copaarena.app.ui.components.CopaBottomNavigationBar
import com.copaarena.app.ui.components.CopaCard
import com.copaarena.app.ui.navigation.Screen
import com.copaarena.app.ui.theme.*
```

- [ ] **Step 2: Swap the tournament-history item Card for CopaCard**

Old:
```kotlin
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate(Screen.TournamentDetail.createRoute(tournament.id)) },
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
```
New:
```kotlin
                        CopaCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { navController.navigate(Screen.TournamentDetail.createRoute(tournament.id)) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
```

- [ ] **Step 3: Verify no raw color literals remain**

Run: `grep -n "Color\.White\|Color\.Gray\|Color\.Black" app/src/main/java/com/copaarena/app/ui/screen/history/HistoryScreen.kt`
Expected: no output (this file had none to begin with — this task only touches card styling)

- [ ] **Step 4: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 18: Sweep `TournamentDetailScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/tournament/TournamentDetailScreen.kt:146`
- Modify: the standings-row `Card{}` — swap for `CopaCard{}`

**Interfaces:**
- Consumes: `CopaCard` (Task 5), `OnPrimary` (Task 1).

- [ ] **Step 1: Add the `CopaCard` import**

Old:
```kotlin
import com.copaarena.app.ui.theme.*
import java.text.SimpleDateFormat
```
New:
```kotlin
import com.copaarena.app.ui.components.CopaCard
import com.copaarena.app.ui.theme.*
import java.text.SimpleDateFormat
```

- [ ] **Step 2: Swap the standings-row Card for CopaCard**

Old:
```kotlin
                items(standings) { stat ->
                    val player = players.find { it.id == stat.playerId }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
```
New:
```kotlin
                items(standings) { stat ->
                    val player = players.find { it.id == stat.playerId }
                    CopaCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
```

- [ ] **Step 3: Fix the "Restart Tournament" button text color** (button's `containerColor = AccentGold`)

Old:
```kotlin
                Text("Restart Tournament", color = Color.White, fontWeight = FontWeight.Bold)
```
New:
```kotlin
                Text("Restart Tournament", color = OnPrimary, fontWeight = FontWeight.Bold)
```

- [ ] **Step 4: Verify no raw color literals remain**

Run: `grep -n "Color\.White\|Color\.Gray\|Color\.Black" app/src/main/java/com/copaarena/app/ui/screen/tournament/TournamentDetailScreen.kt`
Expected: no output

- [ ] **Step 5: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 19: Sweep `CeremonyScreen.kt` — fixed "always-dark" exception

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/ceremony/CeremonyScreen.kt:87,95,101,104,125,137`

**Why this file is different:** `CeremonyScreen` deliberately renders on a fixed `Primary` (navy) full-screen backdrop regardless of the app-wide Dark/Light setting — a celebratory takeover screen, same brand-anchor logic as topbars staying navy. Its internal tokens must all stay on the **fixed** side (`OnPrimary`, `OverlayScrim`), never the theme-reactive `Surface`/`OnBackground` — using the theme-reactive tokens here would put a white `CopaCard` floating on a Light-mode-still-navy backdrop, which looks broken. No `CopaCard` usage in this file for that reason.

**Interfaces:**
- Consumes: `OnPrimary`, `OverlayScrim` (Task 1).

- [ ] **Step 1: Fix the champion badge background** (a white circle behind the team badge image, sits on the fixed-navy backdrop)

Old:
```kotlin
                AsyncImage(
                    model = champion?.teamBadgeUrl,
                    contentDescription = null,
                    modifier = Modifier.size(128.dp).clip(RoundedCornerShape(8.dp)).background(Color.White),
                    contentScale = ContentScale.Crop
                )
```
New:
```kotlin
                AsyncImage(
                    model = champion?.teamBadgeUrl,
                    contentDescription = null,
                    modifier = Modifier.size(128.dp).clip(RoundedCornerShape(8.dp)).background(OnPrimary),
                    contentScale = ContentScale.Crop
                )
```

- [ ] **Step 2: Fix the "TOURNAMENT CHAMPION" label color**

Old:
```kotlin
                Text("TOURNAMENT CHAMPION", style = MaterialTheme.typography.titleLarge, color = Color.White)
```
New:
```kotlin
                Text("TOURNAMENT CHAMPION", style = MaterialTheme.typography.titleLarge, color = OnPrimary)
```

- [ ] **Step 3: Fix the MVP card overlay background**

Old:
```kotlin
                Card(colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha=0.5f))) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐ MVP: ", color = AccentGold)
                        Text(mvp?.name ?: "", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
```
New:
```kotlin
                Card(colors = CardDefaults.cardColors(containerColor = OverlayScrim)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐ MVP: ", color = AccentGold)
                        Text(mvp?.name ?: "", color = OnPrimary, fontWeight = FontWeight.Bold)
                    }
                }
```
(this stays a plain Material3 `Card`, not `CopaCard` — it's a fixed dark overlay chip on the Ceremony backdrop, not a themed content card, and doesn't need the Stadium Lights glow treatment.)

- [ ] **Step 4: Fix the "Finish" button text color**

Old:
```kotlin
                        Text("Finish", color = Color.White)
```
New:
```kotlin
                        Text("Finish", color = OnPrimary)
```

- [ ] **Step 5: Fix the "Restart Tournament" button container color** (this button is intentionally white-on-navy for contrast, with navy `Primary` text — unchanged text color, only the container)

Old:
```kotlin
                    Button(
                        onClick = { 
                            tournament?.let { t ->
                                navController.navigate(Screen.CreateTournament.createRoute(t.id)) {
                                    popUpTo(0)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text("Restart Tournament", color = Primary)
                    }
```
New:
```kotlin
                    Button(
                        onClick = { 
                            tournament?.let { t ->
                                navController.navigate(Screen.CreateTournament.createRoute(t.id)) {
                                    popUpTo(0)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OnPrimary)
                    ) {
                        Text("Restart Tournament", color = Primary)
                    }
```

- [ ] **Step 6: Verify no raw color literals remain**

Run: `grep -n "Color\.White\|Color\.Gray\|Color\.Black" app/src/main/java/com/copaarena/app/ui/screen/ceremony/CeremonyScreen.kt`
Expected: no output

- [ ] **Step 7: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 20: Sweep `SettingsScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/settings/SettingsScreen.kt:88,104`
- Modify: all four inline `Card{}` calls — swap for `CopaCard{}`

**Interfaces:**
- Consumes: `CopaCard` (Task 5), `OnPrimary` (Task 1).

- [ ] **Step 1: Add the `CopaCard` import**

Old:
```kotlin
import androidx.navigation.NavController
import com.copaarena.app.ui.theme.*
```
New:
```kotlin
import androidx.navigation.NavController
import com.copaarena.app.ui.components.CopaCard
import com.copaarena.app.ui.theme.*
```

- [ ] **Step 2: Fix the two `Switch` thumb colors** (both identical — sound toggle and haptic toggle)

Old (appears twice, at the Sound Effects switch and the Haptic Feedback switch):
```kotlin
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = { viewModel.toggleSound(it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = AccentGold, checkedThumbColor = Color.White)
                            )
```
New:
```kotlin
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = { viewModel.toggleSound(it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = AccentGold, checkedThumbColor = OnPrimary)
                            )
```
And the second occurrence:
```kotlin
                            Switch(
                                checked = hapticEnabled,
                                onCheckedChange = { viewModel.toggleHaptic(it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = AccentGold, checkedThumbColor = Color.White)
                            )
```
New:
```kotlin
                            Switch(
                                checked = hapticEnabled,
                                onCheckedChange = { viewModel.toggleHaptic(it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = AccentGold, checkedThumbColor = OnPrimary)
                            )
```

- [ ] **Step 3: Swap the Preferences section Card for CopaCard**

Old:
```kotlin
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Sound Effects", fontWeight = FontWeight.Bold, color = OnBackground)
```
New:
```kotlin
                CopaCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Sound Effects", fontWeight = FontWeight.Bold, color = OnBackground)
```

- [ ] **Step 4: Swap the Data section Card for CopaCard**

Old:
```kotlin
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        TextButton(
                            onClick = { viewModel.clearCache() },
```
New:
```kotlin
                CopaCard {
                    Column {
                        TextButton(
                            onClick = { viewModel.clearCache() },
```

- [ ] **Step 5: Swap the About section Card for CopaCard**

Old:
```kotlin
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("COPA Arena", fontWeight = FontWeight.Bold, color = OnBackground)
```
New:
```kotlin
                CopaCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("COPA Arena", fontWeight = FontWeight.Bold, color = OnBackground)
```

- [ ] **Step 6: Verify no raw color literals remain**

Run: `grep -n "Color\.White\|Color\.Gray\|Color\.Black" app/src/main/java/com/copaarena/app/ui/screen/settings/SettingsScreen.kt`
Expected: no output

- [ ] **Step 7: Verify build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Task 21: Whole-app verification

**Files:** none (verification only)

- [ ] **Step 1: Full clean build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run the existing unit test suite** (Phase 1's `FixtureOutcomeResolverTest`/`CalculateStandingsUseCaseTest` — this phase touched no logic, so these must still be green)

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, 11/11 tests passing

- [ ] **Step 3: Confirm zero remaining raw color literals app-wide** (outside `Color.kt` itself, which legitimately defines the fixed brand literals)

Run: `grep -rn "Color\.White\|Color\.Gray\|Color\.Black" app/src/main/java/com/copaarena/app/ui --include=*.kt`
Expected: no output

- [ ] **Step 4: Manual verification** (install the debug APK on an emulator/device)
  1. Launch the app in default (System) theme.
  2. Go to Settings → App Theme → tap **Light**. Confirm: background flips to the navy-tinted light color, every card turns white with the gold top-glow line still visible, all text stays legible (no invisible gray-on-gray or gold-on-white text anywhere), and the Ceremony screen (if reached) still renders navy regardless.
  3. Tap **Dark** — confirm it flips back to the original dark palette exactly as before this phase.
  4. Walk through every screen in both Light and Dark: Home (both the empty state and, if a tournament is active, the Continue card), Create Tournament, Add Players, Select Format, Bracket Preview, Bracket (all 3 tabs), a live Match screen (including opening the goal sheet and, if applicable, the penalty shootout entry), Stats (both tabs, check chart text is legible against its fixed-navy panel in both themes), History, Tournament Detail, Ceremony, Settings.
  5. Confirm the bottom navigation bar's inactive icons/labels are visible in both themes (this was the one component reading the theme-reactive `Surface`, not the fixed-brand tokens — worth double-checking specifically).

---

## Self-Review

**Spec coverage:** Color tokens (Task 1), Theme.kt real Light wiring (Task 2), Shape scale (Task 3), Typography/numeric style (Task 4), CopaCard component (Task 5), dead component cleanup (Task 6, found during planning — not in the original spec but follows the Phase 1 precedent explicitly), every screen + the one live shared component swept (Tasks 7-20), verification (Task 21). All spec sections have a corresponding task.

**Placeholder scan:** every task has literal before/after code, no "TBD"/"add appropriate styling"/vague steps.

**Type consistency:** `CopaCard(modifier, shape, onClick, content)` signature (Task 5) matches every call site across Tasks 8, 11-20. `numericTextStyle(size, emphasized)` (Task 4) matches its one call site (Task 15). Token names (`OnPrimary`, `OverlayScrim`, `Background`/`Surface`/`SurfaceVariant`/`OnBackground` as `@Composable get()`, `BackgroundDark/Light` etc.) are identical between their Task 1 definition and every later consuming task.
