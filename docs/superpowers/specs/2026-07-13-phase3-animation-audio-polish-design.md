# Phase 3: Animation & Audio Polish — Design

## Context

Phases 1 (correctness bugs) and 2 (visual redesign) are done. Phase 3 was scoped from the start as "tasteful polish" animation/audio; this session the user sharpened it to: **remove the static app icon from the splash screen** and **add fun, interactive animation throughout the app**. Working assumption (user didn't respond to the tone-check question, proceeding per "do what you recommend"): keep the premium FIFA-arena visual identity from Phase 2, but push animation further than the current minimal fade/scale — bouncier, more celebratory, more responsive to taps — without going full slapstick.

**Current state (confirmed via codebase inventory):**
- Animated already: `SplashScreen` (linear fade+scale), `HomeScreen` (linear fade+scale entrance), `BracketScreen` (standings row color), `CeremonyScreen` (staggered `AnimatedVisibility` reveal + `trophy_reveal.json` background, but **no sound**).
- Fully static: `MatchScreen`, `SettingsScreen`, `StatsScreen`, `AddPlayersScreen`, `BracketPreviewScreen`, `CreateTournamentScreen`, `SelectFormatScreen`, `TournamentDetailScreen`, `HistoryScreen`.
- `NavGraph.kt` already applies global slide+fade transitions between all routes (300ms, `FastOutSlowInEasing`) — screen-to-screen motion is already handled; this phase focuses on *within-screen* animation.
- Lottie assets present: `ball_bounce.json` (used, Splash), `trophy_reveal.json` (used, Ceremony), `confetti.json` and `crowd_wave.json` (**both completely unused**).
- Audio: `goal_cheer.mp3` (on goal log) and `whistle.mp3` (on match confirm), both gated behind `SettingsDataStore.soundEnabledFlow` inside `SoundManager`. Only used in `MatchViewModel`. Ceremony — the champion moment — plays no sound at all.
- Bug found during inventory: Settings has a "Haptic Feedback" toggle wired to DataStore, but `MatchScreen`'s 5 inline `LocalHapticFeedback` calls never check it — the toggle is currently dead.

## Goals

1. Splash: replace the static logo `Image` with an animated hero moment; no static app icon shown.
2. Wire the two dead Lottie assets (`confetti.json`, `crowd_wave.json`) into real celebration moments.
3. Give the Ceremony screen sound (currently silent) — source one new short fanfare/cheer clip.
4. Add tasteful interaction animation to the screens currently fully static, reusing shared components (`CopaCard`) where possible so one change benefits many screens.
5. Fix the dead haptic-toggle bug while touching haptic code, and extend gated haptics to the new tap interactions.

## A. Splash rework

Remove the `Image(painterResource(R.drawable.app_logo), ...)` block entirely. Promote the existing `ball_bounce.json` Lottie to the hero element: larger (140dp, up from 80dp), placed where the logo used to be. Replace the current linear `tween(1000)` scale with `spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)` for a playful overshoot bounce-in, applied to both the ball and the "COPA ARENA" title text (they already share one `titleScale` value). Net effect: one bouncing ball animates in, title bounces with it, no static image anywhere on the splash screen.

## B. Celebration system (confetti + sound), one component reused three places

New shared composable `ui/components/CelebrationOverlay.kt`: plays `confetti.json` once (`iterations = 1`) over a duration, non-blocking (no `pointerInput`/`clickable` modifier, so taps pass through to whatever's underneath), auto-hides via an `onFinished` callback. Falls back silently (no crash, just skips the visual) if the Lottie composition fails to load, matching the existing `LottieView` fallback pattern.

Reused at:
1. **Goal logged** (`MatchScreen`) — short, small burst (≈600ms, contained near the scoreboard, not fullscreen) alongside the existing `goal_cheer.mp3`.
2. **Match confirmed** (`MatchScreen`, full-time) — bigger burst alongside the existing `whistle.mp3`.
3. **Ceremony reveal** (`CeremonyScreen`) — full-screen burst timed to fire on the same step that reveals the champion name, replacing the currently-decorative-only `trophy_reveal.json` background with an actual celebration moment. Also loop `crowd_wave.json` at low alpha as ambient background for the whole Ceremony screen — reuses the second dead asset for a natural "crowd celebrating" fit, no new asset needed there.

New sound: source one short (2–4s), free/CC0-licensed crowd-cheer-or-fanfare mp3, add as `res/raw/champion_fanfare.mp3`, wire a `SoundManager.playChampionFanfare()` method (same `soundEnabledFlow`-gated pattern as the existing two methods), call it from `CeremonyViewModel`/`CeremonyScreen` timed with the confetti burst.

## C. Screen-by-screen liveliness

- **MatchScreen**: score digits get a quick punch-scale (1.0 → 1.3 → 1.0, spring) on every goal change, driven by an `Animatable` keyed off `goalsA`/`goalsB`.
- **CopaCard** (shared component, already used by Bracket/TournamentDetail/History/AddPlayers cards): when `onClick` is non-null, add a press-scale (`interactionSource.collectIsPressedAsState()` → `animateFloatAsState` to ~0.97 while pressed). One change, benefits every card-based screen at once.
- **Stats / History / TournamentDetail / BracketPreview** lists: staggered entrance — each list item fades + slides up on first composition, with a small per-index delay (~40ms stagger) so the list "cascades" in rather than popping in all at once.

## D. Haptics fix

No new manager class (would be overkill — `LocalHapticFeedback` is a Composable-scoped API, not something to wrap in a Hilt singleton like `SoundManager`). Instead: in each screen that fires haptics, collect the existing `hapticEnabledFlow` and gate the calls with a single local lambda (`val triggerHaptic = { if (hapticEnabled) haptic.performHapticFeedback(...) }`), replacing the 5 unconditional call sites in `MatchScreen` and reusing the same lambda for the new card-tap feedback.

## Error handling

- Lottie/confetti failures degrade silently (matches existing `LottieView` pattern) — a missing/corrupt animation never blocks gameplay actions.
- New sound file failure follows the existing `SoundManager` try/catch-free `MediaPlayer` pattern already in place for the other two clips (no new error-handling design needed, just replicate).
- `CelebrationOverlay` never intercepts touch input — verified by construction (no pointer-handling modifiers), not by a runtime check.

## Testing

- No new unit-testable logic beyond the haptic-gating lambda (trivial, not worth a dedicated test — it's a one-line conditional already covered by existing `SettingsDataStore` tests if any exist).
- Manual, on-device verification (same emulator workflow as Phases 1–2): confirm splash shows no static icon, confetti fires at all 3 trigger points without blocking input, Ceremony has audible fanfare, list screens cascade in, card taps bounce, haptic toggle actually suppresses feedback when turned off.
- Build (`assembleDebug`) and existing test suite must stay green throughout.

## Scope check

Single cohesive phase — all 5 sections are small, independent changes to the same "make it feel alive" goal, not separate subsystems. No decomposition needed.
