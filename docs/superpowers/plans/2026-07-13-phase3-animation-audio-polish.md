# Phase 3: Animation & Audio Polish Implementation Plan

> **For agentic workers:** this plan is executed directly in-session (no subagent-driven-development or executing-plans handoff) — this repo has no git, and the prior two phases of this same project were executed the same way. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the static splash icon in favor of an animated hero moment, wire the two dead Lottie assets into real celebration beats (goal/full-time/champion), give Ceremony sound, and add tasteful interaction animation (press-scale, score punch, staggered list entrance) across the screens that are currently fully static.

**Architecture:** No new architectural layers. Extends existing patterns: `LottieView`/`SoundManager` (already exist), `CopaCard` (already the one shared card everywhere), `MatchViewModel`/`CeremonyViewModel` (already own the relevant state). One new shared component (`CelebrationOverlay`) and one new shared helper (`StaggeredEntrance`) go in `ui/components/`.

**Tech Stack:** Kotlin, Jetpack Compose animation APIs (`animateFloatAsState`, `Animatable`, `AnimatedVisibility`, `spring`), Lottie-Compose (already a dependency), Hilt DI (already used throughout).

## Global Constraints

- Reuse `CopaCard`, `LottieView`, `SoundManager` — do not create parallel implementations.
- All new sound playback must go through `SoundManager` and respect `SettingsDataStore.soundEnabledFlow` (same gating as the existing two sounds).
- All new/fixed haptic feedback must respect `SettingsDataStore.hapticEnabledFlow` (this is the bug fix from the spec).
- No screen transition changes — `NavGraph.kt` already has global slide+fade transitions; this plan only touches within-screen animation.
- Overlay/celebration visuals must never block touch input (no `pointerInput`/`clickable` modifier on the overlay itself).
- Follow existing package conventions: shared visuals → `ui.components`, screen-specific → the screen's own file.

---

### Task 1: Splash screen — remove static icon, bouncy Lottie hero

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/splash/SplashScreen.kt`

**Interfaces:**
- Consumes: existing `LottieView(assetName, modifier, iterations, isPlaying)` (`ui/components/LottieView.kt`), existing `ball_bounce.json` asset.
- Produces: nothing new consumed by later tasks.

- [ ] **Step 1: Remove the static logo `Image` and its now-unused import, promote the ball Lottie to Read the current file (`app/src/main/java/com/copaarena/app/ui/screen/splash/SplashScreen.kt`) to confirm line numbers haven't drifted, then replace lines 65–100 (the `Image(...)` block through the small `LottieView(ball_bounce.json, size(80.dp))` call and its `Spacer`s) with:

```kotlin
            // Animated hero — the bouncing ball IS the splash identity now, no static icon.
            LottieView(
                assetName = "ball_bounce.json",
                modifier = Modifier
                    .size(140.dp)
                    .scale(titleScale)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title (Orbitron is noticeably wider per-character than Bebas Neue was, so this
            // is sized down from the original 52.sp/4.sp to reliably fit "COPA ARENA" on
            // narrower screens without overflowing past the screen edges).
            Text(
                text = "COPA ARENA",
                fontFamily = BebasNeue,
                fontSize = 38.sp,
                fontWeight = FontWeight.Normal,
                color = AccentGold,
                letterSpacing = 2.sp,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(titleScale),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
```

Remove the now-unused `import androidx.compose.foundation.Image` and `import com.copaarena.app.R` if `R` is no longer referenced anywhere else in the file (grep the file for `R.` after the edit — `R.drawable.app_logo` was the only usage).

- [ ] **Step 2: Change the scale animation from a linear tween to a bouncy spring**

Replace the `titleScale` line:

```kotlin
    val titleScale by animateFloatAsState(if (visible) 1f else 0.7f, tween(1000))
```

with:

```kotlin
    val titleScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "splashTitleScale"
    )
```

Add the import: `import androidx.compose.animation.core.Spring` and `import androidx.compose.animation.core.spring` (alongside the existing `import androidx.compose.animation.core.animateFloatAsState` and `import androidx.compose.animation.core.tween` — `tween` stays imported/used for the `alpha` animation just above it, which is unchanged).

- [ ] **Step 3: Build and manually verify**

Run: `./gradlew.bat assembleDebug -q` from `E:/Fazeel/Android App` (or the Bash-tool equivalent). Expected: no compile errors, no unresolved `R`/`Image` references.

Install (`adb install -r`) and launch the app; screenshot the splash screen. Expected: no static square logo visible anywhere, only the bouncing ball animating in with an overshoot ("boing") feel, title text bouncing in sync, no overflow on the title text.

---

### Task 2: `CelebrationOverlay` shared component

**Files:**
- Create: `app/src/main/java/com/copaarena/app/ui/components/CelebrationOverlay.kt`

**Interfaces:**
- Produces: `@Composable fun CelebrationOverlay(visible: Boolean, modifier: Modifier = Modifier, durationMillis: Long = 700L, onFinished: () -> Unit = {})` — Task 3 and Task 5 both call this.
- Consumes: existing `LottieView` composable, existing `confetti.json` asset.

- [ ] **Step 1: Write the component**

```kotlin
package com.copaarena.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Plays confetti.json once, then calls [onFinished] after [durationMillis] so the caller can
 * reset [visible] back to false. Deliberately has no pointerInput/clickable modifier so it never
 * intercepts touches meant for the content underneath.
 */
@Composable
fun CelebrationOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    durationMillis: Long = 700L,
    onFinished: () -> Unit = {}
) {
    LaunchedEffect(visible) {
        if (visible) {
            delay(durationMillis)
            onFinished()
        }
    }
    if (visible) {
        LottieView(
            assetName = "confetti.json",
            modifier = modifier,
            iterations = 1,
            isPlaying = true
        )
    }
}
```

- [ ] **Step 2: Build to confirm it compiles**

Run: `./gradlew.bat assembleDebug -q`. Expected: no errors (this file has no other callers yet, so this only checks syntax/imports).

---

### Task 3: Goal + full-time confetti bursts in Match screen

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/match/MatchViewModel.kt`
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/match/MatchScreen.kt`

**Interfaces:**
- Consumes: `CelebrationOverlay` (Task 2).
- Produces: `MatchViewModel.celebrateGoal: StateFlow<Boolean>`, `MatchViewModel.celebrateFullTime: StateFlow<Boolean>`, `MatchViewModel.consumeGoalCelebration()`, `MatchViewModel.consumeFullTimeCelebration()` — internal to this task, no later task depends on them.

- [ ] **Step 1: Add celebration flags to `MatchViewModel`**

In `MatchViewModel.kt`, after the existing `_navigateToCeremony`/`navigateToCeremony` pair (around line 65), add:

```kotlin
    private val _celebrateGoal = MutableStateFlow(false)
    val celebrateGoal: StateFlow<Boolean> = _celebrateGoal.asStateFlow()

    private val _celebrateFullTime = MutableStateFlow(false)
    val celebrateFullTime: StateFlow<Boolean> = _celebrateFullTime.asStateFlow()

    fun consumeGoalCelebration() { _celebrateGoal.value = false }
    fun consumeFullTimeCelebration() { _celebrateFullTime.value = false }
```

In `addGoal(...)`, right after `soundManager.playGoalCheer()` (currently line 102), add:

```kotlin
            _celebrateGoal.value = true
```

In `confirmMatch(...)`, the method currently ends with:

```kotlin
        if (tournament.status == "COMPLETED") {
            _navigateToCeremony.value = refreshed.tournamentId
        }
    }
```

Change it to also fire the full-time celebration whenever the leg is actually decided (i.e. penalties are not still needed — the existing `_needsPenalties.value` was just computed one line above this block):

```kotlin
        if (!_needsPenalties.value) {
            _celebrateFullTime.value = true
        }

        if (tournament.status == "COMPLETED") {
            _navigateToCeremony.value = refreshed.tournamentId
        }
    }
```

- [ ] **Step 2: Wire both into `MatchScreen`**

In `MatchScreen.kt`, alongside the other `collectAsStateWithLifecycle()` calls near the top of the composable (after `val ceremonyTournamentId by viewModel.navigateToCeremony.collectAsStateWithLifecycle()`, currently line 51), add:

```kotlin
    val celebrateGoal by viewModel.celebrateGoal.collectAsStateWithLifecycle()
    val celebrateFullTime by viewModel.celebrateFullTime.collectAsStateWithLifecycle()
```

The screen's root is currently a `Scaffold { padding -> Column(...) { ... } }` with the goal-sheet/delete-dialog appended as siblings inside the `Scaffold`'s trailing lambda (after the `Column`, before the closing brace — see the `// ── Goal Sheet ──` and `// ── Delete Confirm ──` comments). Add the two overlays as further siblings in that same trailing lambda, right after the `Column(...)` block closes (i.e. immediately before the `// ── Goal Sheet ──` comment):

```kotlin
        // ── Goal celebration (small, near scoreboard) ──
        CelebrationOverlay(
            visible = celebrateGoal,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .size(220.dp),
            durationMillis = 700L,
            onFinished = { viewModel.consumeGoalCelebration() }
        )

        // ── Full-time celebration (bigger, fullscreen) ──
        CelebrationOverlay(
            visible = celebrateFullTime,
            modifier = Modifier.fillMaxSize(),
            durationMillis = 1200L,
            onFinished = { viewModel.consumeFullTimeCelebration() }
        )
```

This requires the `Scaffold`'s trailing content to be a `Box` (so `Modifier.align(Alignment.TopCenter)` on the first overlay resolves) rather than the bare `Column` — check whether the current `content = { padding -> Column(...) { ... } }` lambda already wraps things in a `Box` anywhere; if not, wrap the whole trailing lambda body in a `Box(modifier = Modifier.fillMaxSize())` containing the existing `Column` plus the new overlays plus the existing goal-sheet/delete-dialog siblings, so all of them share one `BoxScope` for alignment. Add the import `import com.copaarena.app.ui.components.CelebrationOverlay`.

- [ ] **Step 3: Build and manually verify**

Run: `./gradlew.bat assembleDebug -q`. Expected: no errors.

Install, play a match, log a goal — expect a small confetti burst near the scoreboard for under a second, not blocking the "Add Goal" FAB or goal log taps underneath. Confirm full time on a decided match — expect a bigger, fullscreen-ish confetti burst. Confirm a still-tied match that needs penalties does NOT fire the full-time burst until the shootout is actually submitted and decisive.

---

### Task 4: Champion fanfare sound + Ceremony confetti/ambient crowd

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/utils/SoundManager.kt`
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/ceremony/CeremonyViewModel.kt`
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/ceremony/CeremonyScreen.kt`
- Create: `app/src/main/res/raw/champion_fanfare.mp3` (sourced, not authored)

**Interfaces:**
- Consumes: `CelebrationOverlay` (Task 2), existing `LottieView`.
- Produces: `SoundManager.playChampionFanfare()`.

- [ ] **Step 1: Source the audio asset**

Search the web for a short (2–4 second) crowd-cheer-or-fanfare sound effect licensed as CC0 / public domain / "free for commercial use, no attribution required" (e.g. via pixabay.com/sound-effects or mixkit.co's free sound effects library — verify the specific clip's license page states no-attribution-required before using it, since this ships in the app). Download it and save as `app/src/main/res/raw/champion_fanfare.mp3`. Confirm the file is a valid mp3 (not an HTML error page) by checking its file size is more than a few KB and its magic bytes/extension are correct.

- [ ] **Step 2: Add `playChampionFanfare()` to `SoundManager`**

In `SoundManager.kt`, add a third player field alongside the existing two:

```kotlin
    private var fanfarePlayer: MediaPlayer? = null
```

In `init { }`, alongside the existing two `MediaPlayer.create(...)` calls:

```kotlin
            fanfarePlayer = MediaPlayer.create(context, R.raw.champion_fanfare)
```

Add the public method:

```kotlin
    fun playChampionFanfare() {
        playSound(fanfarePlayer)
    }
```

In `release()`, alongside the existing two releases:

```kotlin
            fanfarePlayer?.release()
```

and in the `finally` block:

```kotlin
            fanfarePlayer = null
```

- [ ] **Step 3: Inject `SoundManager` into `CeremonyViewModel` and expose a trigger method**

In `CeremonyViewModel.kt`, add `private val soundManager: SoundManager` to the constructor (alongside the existing `shareCardGenerator: ShareCardGenerator` param), and add the import `import com.copaarena.app.utils.SoundManager`. Add a method:

```kotlin
    fun playChampionFanfare() {
        soundManager.playChampionFanfare()
    }
```

- [ ] **Step 4: Fire confetti + fanfare + ambient crowd from `CeremonyScreen`**

In `CeremonyScreen.kt`, the existing `step` state machine advances 0→6 via a single `LaunchedEffect(Unit)` with `delay()` calls (lines 43–56); `step = 3` is when the "TOURNAMENT CHAMPION" label reveals — this is the celebratory beat. Add a second `LaunchedEffect` that reacts to `step` reaching 3:

```kotlin
    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(step) {
        if (step == 3) {
            viewModel.playChampionFanfare()
            showConfetti = true
        }
    }
```

Replace the existing `trophy_reveal.json` block (lines 58–72, the `Box(...).background(Primary)) { ... LottieAnimation(trophy_reveal...) ... }`) — keep the `trophy_reveal.json` background exactly as-is (it's the establishing visual for the whole screen), but add the ambient crowd loop and the confetti burst as further children of the same outer `Box`, right after the existing trophy `LottieAnimation`/fallback `if/else`:

```kotlin
        // Ambient crowd, looping quietly for the whole ceremony
        LottieView(
            assetName = "crowd_wave.json",
            modifier = Modifier.fillMaxSize().alpha(0.15f),
            iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever
        )

        // One-shot confetti burst timed with the champion reveal
        CelebrationOverlay(
            visible = showConfetti,
            modifier = Modifier.fillMaxSize(),
            durationMillis = 1500L,
            onFinished = { showConfetti = false }
        )
```

Add imports: `import com.copaarena.app.ui.components.LottieView` and `import com.copaarena.app.ui.components.CelebrationOverlay` (the file already imports `com.airbnb.lottie.compose.*` and `androidx.compose.ui.draw.alpha`).

- [ ] **Step 5: Build and manually verify**

Run: `./gradlew.bat assembleDebug -q`. Expected: no errors.

Play a tournament to completion; on reaching Ceremony, confirm: an audible fanfare/cheer plays once, a confetti burst fires timed with the "TOURNAMENT CHAMPION" text appearing, a faint looping crowd animation plays in the background for the rest of the screen, and none of it blocks the Share Card/Finish/Restart buttons once they appear.

---

### Task 5: Score punch animation on goal

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/match/MatchScreen.kt`

**Interfaces:**
- Consumes: nothing new.
- Produces: nothing consumed elsewhere.

- [ ] **Step 1: Animate the score text on change**

The scoreboard `Text` currently reads (inside the `CopaCard` scoreboard block, around line 129):

```kotlin
                        Text(
                            text = "$scoreA - $scoreB",
                            style = com.copaarena.app.ui.theme.numericTextStyle(56.sp, emphasized = true),
                            color = AccentGold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
```

Replace with a version that punch-scales on every score change, using `scoreA + scoreB` as the animation key so either team scoring triggers it:

```kotlin
                        val totalGoals = scoreA + scoreB
                        val scorePunch = remember { Animatable(1f) }
                        LaunchedEffect(totalGoals) {
                            if (totalGoals > 0) {
                                scorePunch.snapTo(1f)
                                scorePunch.animateTo(1.3f, animationSpec = tween(120))
                                scorePunch.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        }
                        Text(
                            text = "$scoreA - $scoreB",
                            style = com.copaarena.app.ui.theme.numericTextStyle(56.sp, emphasized = true),
                            color = AccentGold,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .scale(scorePunch.value)
                        )
```

Add imports: `import androidx.compose.animation.core.Animatable`, `import androidx.compose.animation.core.Spring`, `import androidx.compose.animation.core.spring`, `import androidx.compose.animation.core.tween` (may already be absent — check the file's existing imports first, it currently has no `androidx.compose.animation.core.*` imports at all since the screen was fully static), `import androidx.compose.ui.draw.scale`.

- [ ] **Step 2: Build and manually verify**

Run: `./gradlew.bat assembleDebug -q`. Expected: no errors.

Log a goal; the score digits should visibly "punch" larger then settle back with a slight bounce, not just instantly update.

---

### Task 6: Fix the dead haptic-toggle bug in Match screen

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/match/MatchViewModel.kt`
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/match/MatchScreen.kt`

**Interfaces:**
- Consumes: existing `SettingsDataStore.hapticEnabledFlow: Flow<Boolean>` (`data/datastore/SettingsDataStore.kt`).
- Produces: `MatchViewModel.hapticEnabled: StateFlow<Boolean>`.

- [ ] **Step 1: Inject `SettingsDataStore` into `MatchViewModel` and expose the flow**

Add `private val settingsDataStore: SettingsDataStore` to the constructor (alongside `soundManager`), import `com.copaarena.app.data.datastore.SettingsDataStore`. Add, alongside the other `StateFlow` properties:

```kotlin
    val hapticEnabled: StateFlow<Boolean> = settingsDataStore.hapticEnabledFlow
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), true)
```

(Add the import `import kotlinx.coroutines.flow.stateIn` if not already present — check the file's existing imports first.)

- [ ] **Step 2: Gate every haptic call site in `MatchScreen` behind the setting**

Collect the flow near the other state collection at the top of the composable:

```kotlin
    val hapticEnabled by viewModel.hapticEnabled.collectAsStateWithLifecycle()
```

Add one local helper right after (needs `haptic` already defined at line 53):

```kotlin
    val triggerHaptic = {
        if (hapticEnabled) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    }
```

Replace all 5 existing inline calls of the form `haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)` (currently at lines 149, 171, 315, 348, 364) with `triggerHaptic()`.

- [ ] **Step 3: Build and manually verify**

Run: `./gradlew.bat assembleDebug -q`. Expected: no errors.

In Settings, turn off "Haptic Feedback", then log a goal / confirm a match in `MatchScreen` — confirm no vibration fires. Turn it back on — confirm vibration fires again on the same actions.

---

### Task 7: Press-scale on `CopaCard` (shared — benefits Bracket/History/Stats/TournamentDetail/AddPlayers at once)

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/components/CopaCard.kt`

**Interfaces:**
- Consumes: nothing new.
- Produces: no signature change — `CopaCard`'s existing `(modifier, shape, onClick, content)` params are unchanged, so every existing call site keeps working with no edits needed anywhere else.

- [ ] **Step 1: Add a press-scale driven by the card's own interaction source**

Replace the full contents of `CopaCard.kt`'s function body with:

```kotlin
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
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val pressScale by animateFloatAsState(
            targetValue = if (isPressed) 0.97f else 1f,
            label = "cardPressScale"
        )
        Card(
            onClick = onClick,
            modifier = modifier.graphicsLayer(scaleX = pressScale, scaleY = pressScale),
            shape = shape,
            colors = cardColors,
            elevation = cardElevation,
            interactionSource = interactionSource,
            content = body
        )
    } else {
        Card(modifier = modifier, shape = shape, colors = cardColors, elevation = cardElevation, content = body)
    }
}
```

Add imports: `import androidx.compose.animation.core.animateFloatAsState`, `import androidx.compose.foundation.interaction.MutableInteractionSource`, `import androidx.compose.foundation.interaction.collectIsPressedAsState`, `import androidx.compose.runtime.getValue`, `import androidx.compose.runtime.remember`, `import androidx.compose.ui.graphics.graphicsLayer`.

- [ ] **Step 2: Build and manually verify**

Run: `./gradlew.bat assembleDebug -q`. Expected: no errors.

Tap any clickable card (a History tournament card, a Bracket match card, an AddPlayers restart-mode player card) — confirm a brief shrink-on-press feel before the tap action fires, and non-clickable `CopaCard` usages (e.g. the Match scoreboard, which passes no `onClick`) are visually unaffected.

---

### Task 8: Staggered list entrance (History, Stats, TournamentDetail)

**Files:**
- Create: `app/src/main/java/com/copaarena/app/ui/components/StaggeredEntrance.kt`
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/history/HistoryScreen.kt`
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/stats/StatsScreen.kt`
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/tournament/TournamentDetailScreen.kt`

**Interfaces:**
- Produces: `@Composable fun StaggeredEntrance(index: Int, modifier: Modifier = Modifier, staggerMillis: Long = 40L, content: @Composable () -> Unit)`.

- [ ] **Step 1: Write the shared helper**

```kotlin
package com.copaarena.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/** Fades + slides an item up on first composition, staggered by [index] so a list "cascades" in
 * instead of popping in all at once. Re-fires if [index] changes (e.g. list reordered). */
@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    staggerMillis: Long = 40L,
    content: @Composable () -> Unit
) {
    var visible by remember(index) { mutableStateOf(false) }
    LaunchedEffect(index) {
        delay(index * staggerMillis)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(250)) + slideInVertically(
            animationSpec = tween(250),
            initialOffsetY = { it / 4 }
        )
    ) {
        content()
    }
}
```

- [ ] **Step 2: Wire into `HistoryScreen`**

Change the import `import androidx.compose.foundation.lazy.items` to `import androidx.compose.foundation.lazy.itemsIndexed`. Change:

```kotlin
                    items(history) { tournament ->
```

to:

```kotlin
                    itemsIndexed(history) { index, tournament ->
                      StaggeredEntrance(index = index) {
```

and add the matching closing `}` right before the final `}` that currently closes this `items { ... }` lambda (i.e. wrap the entire existing `val formatter = ...` through the closing `CopaCard { ... }` body in the new `StaggeredEntrance { ... }` block). Add the import `import com.copaarena.app.ui.components.StaggeredEntrance`.

- [ ] **Step 3: Wire into `StatsScreen`** (both `itemsIndexed` lists already have an `index` in scope, so no `items`→`itemsIndexed` import change needed here)

In the top-scorers list, wrap the existing `CopaCard(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) { ... }` block (inside `itemsIndexed(topScorers) { index, scorer -> ... }`) in `StaggeredEntrance(index = index) { ... }`. Do the same for the global-leaderboard list's `itemsIndexed(globalStats) { index, stat -> ... }` block. Add the import `import com.copaarena.app.ui.components.StaggeredEntrance`.

- [ ] **Step 4: Wire into `TournamentDetailScreen`**

Change the import `import androidx.compose.foundation.lazy.items` to `import androidx.compose.foundation.lazy.itemsIndexed`. Change:

```kotlin
                    items(standings) { stat ->
```

to:

```kotlin
                    itemsIndexed(standings) { index, stat ->
                      StaggeredEntrance(index = index) {
```

wrapping the existing `val player = players.find { ... }` through the closing `CopaCard { ... }` body, with the matching extra closing `}`. Add the import `import com.copaarena.app.ui.components.StaggeredEntrance`.

- [ ] **Step 5: Build and manually verify**

Run: `./gradlew.bat assembleDebug -q`. Expected: no errors.

Open History (with 2+ past tournaments), Stats, and a TournamentDetail with a full standings table — confirm items cascade in top-to-bottom with a brief stagger instead of appearing all at once. Confirm re-opening the same screen still animates in (not stuck invisible from a stale `remember` state — since `StaggeredEntrance` composes fresh on each screen entry, this should just work).

---

### Task 9: Idle pulse on the BracketPreview ball icon

**Files:**
- Modify: `app/src/main/java/com/copaarena/app/ui/screen/tournament/BracketPreviewScreen.kt`

**Interfaces:**
- Consumes: nothing new.
- Produces: nothing consumed elsewhere.

- [ ] **Step 1: Animate the "Ready to go!" ball icon with a slow idle pulse**

Replace:

```kotlin
                    Icon(
                        Icons.Default.SportsSoccer,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = AccentGold.copy(alpha = 0.2f)
                    )
```

with:

```kotlin
                    val infiniteTransition = rememberInfiniteTransition(label = "ballPulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "ballPulseScale"
                    )
                    Icon(
                        Icons.Default.SportsSoccer,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .scale(pulseScale),
                        tint = AccentGold.copy(alpha = 0.2f)
                    )
```

Add imports: `import androidx.compose.animation.core.FastOutSlowInEasing`, `import androidx.compose.animation.core.RepeatMode`, `import androidx.compose.animation.core.infiniteRepeatable`, `import androidx.compose.animation.core.rememberInfiniteTransition`, `import androidx.compose.animation.core.tween` (check if already imported), `import androidx.compose.ui.draw.scale`.

- [ ] **Step 2: Build and manually verify**

Run: `./gradlew.bat assembleDebug -q`. Expected: no errors.

Reach the Bracket Preview screen ("Ready to go!") — confirm the ball icon slowly pulses in place rather than sitting perfectly still.

---

## Final verification (all tasks)

- [ ] Full clean build: `./gradlew.bat assembleDebug -q` succeeds with zero errors.
- [ ] Existing unit test suite still green: `./gradlew.bat test -q`.
- [ ] Full on-device manual pass (same emulator workflow used in Phases 1–2, via `adb`): splash has no static icon, a full match shows goal + full-time confetti without blocking taps, a completed tournament's Ceremony has audible fanfare + confetti + ambient crowd, the haptic toggle actually suppresses/allows feedback, cards visibly press-scale on tap, History/Stats/TournamentDetail lists cascade in, BracketPreview's ball icon pulses.
- [ ] `adb logcat -d | grep -ci "FATAL EXCEPTION"` returns `0` after exercising every touched screen.

## Self-review notes

- Spec section C originally said "Stats/History/TournamentDetail/**BracketPreview**: staggered list-item entrance" — corrected during planning: `BracketPreviewScreen` has a static summary card, not a list, so it got an idle icon pulse (Task 9) instead; the three actual lists got the stagger (Task 8).
- Spec section D said to "reuse the same [haptic] lambda for the new tap interactions" (i.e. card taps) — trimmed from execution: card taps already get strong tactile feedback from the new press-scale (Task 7) alone, and wiring haptics into every card-based screen would mean injecting `SettingsDataStore` into Bracket/History/Stats/TournamentDetail's view models too, which is disproportionate to the ask. Haptic-gating fix stays scoped to `MatchScreen`, where the bug actually is.
