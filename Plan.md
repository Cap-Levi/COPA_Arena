Build a native Android app called COPA Arena using Kotlin and Jetpack Compose targeting API 26+. This is a FIFA FC26 tournament manager for local multiplayer sessions (couch gaming). Below is the complete specification.

You are an expert Android developer. Build a complete, production-ready native Android application called COPA Arena. This is a local multiplayer FIFA FC26 tournament manager — think of it as a physical FIFA tournament organizer that lives on one phone and is passed around between friends during a couch gaming session. Every requirement below must be fully implemented. Do not skip sections, do not add placeholders, do not leave TODOs. The app must compile, run, and work end-to-end.

SECTION 01 — PROJECT IDENTITY AND VISUAL THEME

App name: COPA Arena
Package name: com.copaarena.app
Version name: 1.0.0
Version code: 1
Language: English only
Default theme: Dark mode (user can switch in Settings)

Color tokens (define in res/values/colors.xml AND as MaterialTheme tokens):
Primary: #0D1B2A (deep navy), Primary Variant: #142233, Secondary: #3B6D11 (grass green), Secondary Variant: #2E5A0D, Accent / Gold: #EF9F27 (FIFA gold), Accent Dark: #BA7517, Surface: #111D2B, Surface Variant: #1A2B3C, Background: #0A1520, On Primary: #FFFFFF, On Secondary: #FFFFFF, On Background: #E8EDF2, Error: #E24B4A, Success: #3B6D11, Warning: #EF9F27, Card Border: rgba(255,255,255,0.08)

Typography (use Google Fonts via Compose):
Display font: "Bebas Neue" — used for scores, player names on cards, tournament title
Body font: "DM Sans" — used for all body text, labels, buttons
Mono font: "JetBrains Mono" — used for stats numbers

App icon: Football in a gold arena stadium — use vector drawable. Include adaptive icon for API 26+.
Logo: "COPA" in Bebas Neue + "ARENA" in DM Sans bold, football icon between the two words.

SECTION 02 — TECH STACK AND SDK CONFIG

compileSdk: 35, minSdk: 26, targetSdk: 35
Build system: Gradle with Kotlin DSL (build.gradle.kts everywhere)
DI framework: Hilt, Async: Kotlin Coroutines + Flow + StateFlow
Architecture: MVVM + Clean Architecture (UI → ViewModel → UseCase → Repository → DataSource)
Image loading: Coil 2.x, Network: Retrofit 2 + OkHttp 4 + Gson
Local DB: Room 2.6+ with KSP, Navigation: Jetpack Navigation Compose
Animations: Lottie Compose 6.x + Jetpack Compose animation APIs
Charts: MPAndroidChart 3.1.x, Fonts: Google Fonts via Compose
Logging: Timber, Signing: Android Keystore
Testing: JUnit4, MockK, Turbine, Espresso

SECTION 03 — GRADLE DEPENDENCIES (COMPLETE)

plugins { alias(libs.plugins.android.application); alias(libs.plugins.kotlin.android); alias(libs.plugins.kotlin.compose); alias(libs.plugins.ksp); alias(libs.plugins.hilt) }

dependencies {
implementation(platform("androidx.compose:compose-bom:2024.06.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-graphics")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")
implementation("androidx.compose.animation:animation")
implementation("androidx.compose.animation:animation-graphics")
debugImplementation("androidx.compose.ui:ui-tooling")
debugImplementation("androidx.compose.ui:ui-test-manifest")
implementation("androidx.compose.ui:ui-text-google-fonts:1.6.8")
implementation("androidx.core:core-ktx:1.13.1")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
implementation("androidx.activity:activity-compose:1.9.0")
implementation("androidx.navigation:navigation-compose:2.7.7")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
implementation("com.google.dagger:hilt-android:2.51.1")
ksp("com.google.dagger:hilt-compiler:2.51.1")
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-gson:2.11.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
implementation("io.coil-kt:coil-compose:2.6.0")
implementation("com.airbnb.android:lottie-compose:6.4.1")
implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
implementation("androidx.datastore:datastore-preferences:1.1.1")
implementation("androidx.core:core-splashscreen:1.0.1")
implementation("com.jakewharton.timber:timber:5.0.1")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
testImplementation("junit:junit:4.13.2")
testImplementation("io.mockk:mockk:1.13.11")
testImplementation("app.cash.turbine:turbine:1.1.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
androidTestImplementation("androidx.test.ext:junit:1.2.1")
androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

In settings.gradle.kts add JitPack: maven { url = uri("https://jitpack.io") }

In local.properties (never commit): FUTDB_API_KEY=your_key_here, KEYSTORE_PATH=../copa_arena_release.jks, KEYSTORE_PASSWORD=your_keystore_password, KEY_ALIAS=copa_arena_key, KEY_PASSWORD=your_key_password

In build.gradle.kts read local.properties and configure signingConfigs for release with isMinifyEnabled=true, isShrinkResources=true, and buildConfigField for FUTDB_API_KEY.

SECTION 04 — PROJECT FILE STRUCTURE

Full MVVM + Clean Architecture structure under app/src/main/java/com/copaarena/app/:
- CopaarenaApp.kt (Application class — Hilt + Timber init)
- MainActivity.kt (single activity host)
- data/db/ with AppDatabase.kt, entity/ (6 entities), dao/ (6 DAOs)
- data/api/ with FutdbApiService.kt, models, NetworkModule.kt
- data/repository/ (4 repositories)
- data/datastore/SettingsDataStore.kt
- domain/model/ (9 domain models + enums)
- domain/usecase/ (9 use cases)
- ui/theme/ (Color.kt, Theme.kt, Type.kt, Shape.kt)
- ui/navigation/ (NavGraph.kt, Screen.kt sealed class)
- ui/components/ (8 shared composables)
- ui/screen/ with all 13 screen folders each containing Screen + ViewModel

Assets: assets/lottie/ (5 JSONs), assets/sounds/ (2 MP3s), assets/badges/ (30 PNG club badges)

1. LOTTIE FALLBACK: Wrap all LottieComposition loads in try-catch. If a JSON file is missing or fails to parse, skip the animation silently — never crash. Use a simple alpha fade (0→1, 300ms) as fallback for any Lottie slot. 2. SOUNDS IN RES/RAW: Place goal_cheer.mp3 and whistle.mp3 in res/raw/ not assets/. Reference via R.raw.goal_cheer and R.raw.whistle in SoundManager. MediaPlayer.create(context, R.raw.X) handles this natively. 3. NO BUNDLED BADGE PNGS: Remove the assets/badges/ folder entirely. Instead: (a) primary = Coil loading from futdb imageUrl with disk cache; (b) loading placeholder = ClubInitialsAvatar composable (circle + initials + color from name hash); (c) error fallback = ic_generic_club vector drawable (football shield shape in navy/gold). 4. CLUB INITIALS AVATAR: Implement ClubInitialsAvatar(teamName: String, size: Dp) composable. Color palette of 8 options derived from teamName.hashCode() % 8. Abbreviation = first letter of each word in teamName, max 3 chars, uppercase. Font: Bebas Neue. Use as Coil placeholder parameter. 5. APP ICON: Implement ic_launcher_foreground.xml as a hand-crafted Android vector drawable (football shape with path data, gold color #EF9F27). Implement ic_launcher_background.xml as a solid #0D1B2A shape. Both must pass Android Studio's adaptive icon preview.


SECTION 05 — ROOM DB ENTITIES AND DAOS

TournamentEntity: id (PK autoGenerate), name, date (epoch), format, matchesPerFixture, status (SETUP/ACTIVE/COMPLETED), winnerId, mvpPlayerId, createdAt

PlayerEntity: id (PK), tournamentId (FK CASCADE), name, teamName, teamBadgeUrl, teamOverall, seed

MatchEntity: id (PK), tournamentId (FK CASCADE), stage (GROUP/QUARTER/SEMI/FINAL), matchNumber, playerAId, playerBId, goalsA, goalsB, status (PENDING/LIVE/COMPLETED/SKIPPED), winnerId, isDraw, leg (1/2/3)

GoalEntity: id (PK), matchId (FK CASCADE), scorerId, creditedToId, isOwnGoal, minute (optional), timestamp

TournamentStatsEntity: playerId + tournamentId (composite PK), goals, ownGoals, wins, draws, losses, goalsFor, goalsAgainst, points, finalPosition

CachedTeamEntity: teamId (PK), name, badgeUrl, overall, league, nation, cachedAt

All DAOs use Flow<T> for reads and suspend functions for writes. Include: getActiveTournament (ACTIVE status), getAllTournaments ordered by createdAt DESC, top scorers query grouped by creditedToId, global player summary aggregated across all tournaments, team search by LIKE query, head-to-head match lookup.

SECTION 06 — REPOSITORY LAYER AND USE CASES

9 use cases: CreateTournamentUseCase, GenerateBracketUseCase, RecordGoalUseCase, ConfirmMatchResultUseCase, CalculateStandingsUseCase, CalculateQualificationTipsUseCase, CheckEarlyFinalistUseCase, GetTournamentHistoryUseCase, GetGlobalStatsUseCase.

GenerateBracketUseCase generates all round-robin GROUP matches (N*(N-1)/2 * matchesPerFixture legs), plus KO stage stub slots for QUARTER/SEMI/FINAL depending on format. KO stubs use TBD playerIds until standings finalize.

CalculateStandingsUseCase sorts by Points DESC → GoalDiff DESC → GoalsFor DESC → H2H → coin flip (seeded random using tournamentId).

CheckEarlyFinalistUseCase runs after every GROUP match. If a player is mathematically guaranteed top-2 (or top-4 for SEMIFINALS format), mark their remaining GROUP matches as SKIPPABLE and notify via StateFlow.

CalculateQualificationTipsUseCase simulates best-case for each non-qualified player and generates human-readable tip text.

ConfirmMatchResultUseCase validates goal count, awards points (Win=3, Draw=1, Loss=0), handles BO2/BO3 aggregate and away goals / penalty shootout, updates TournamentStatsEntity, triggers CheckEarlyFinalistUseCase, and if FINAL match completes triggers tournament completion.

SECTION 07 — API INTEGRATION

Base URL: https://futdb.app/api/
Retrofit service: searchClubs (GET clubs?name=X with X-AUTH-TOKEN header), getClubById (GET clubs/{id}).
Response models: ClubSearchResponse with items list containing id, name, imageUrl, overall, league info.
Caching: query Room first (LIKE search, return if >=5 results). On API success upsert to cached_teams. Cache TTL 24h.
Fallback chain: Room cache → bundled assets/badges/ PNG matched by club name → generic_club.png.
Coil image loading with crossfade + error/fallback drawables.

SECTION 08 — NAVIGATION AND DEEP LINKS

Sealed Screen class for all routes: Splash, Home, CreateTournament, AddPlayers(tid), SelectFormat(tid), BracketPreview(tid), Bracket(tid), Match(mid), Stats(tid?), History, TournamentDetail(tid), Ceremony(tid), Settings.

Deep link in AndroidManifest: copa-arena://match/{matchId} opens MatchScreen directly.

Nav transitions: slideInHorizontally + slideOutHorizontally (300ms, FastOutSlowIn) for all screens. Ceremony uses fadeIn from black.

Bottom navigation bar (shown on Bracket, Stats, History): Home, Bracket, Stats, History tabs.

SECTION 09 — ALL 16 SCREENS (DETAILED SPEC)

Splash: SplashScreen API + COPA Arena logo scale/fade animation + ball Lottie + tagline, auto-navigate after 2.5s.

Home: COPA Arena header, active tournament card with progress bar + Continue button, or empty state with New Tournament CTA, History text button, New Tournament FAB.

CreateTournament: Name input, date picker (MaterialDatePicker), matches per fixture segmented button (1/2/3), Next button disabled until name filled.

AddPlayers: Player count display, Add Player bottom sheet (name + debounced team search showing badge/name/league/overall, confirm adds FC26 gold card to grid), LazyVerticalGrid 2-column player grid with delete buttons, drag-to-reorder, Next when >=2 players.

SelectFormat: Auto-suggest + format cards for ROUND_ROBIN / SEMIFINALS / QUARTERFINALS with player count compatibility, Preview Bracket button.

BracketPreview: Full bracket tree, staggered card slide-in animations, KO stubs as TBD, Kick Off button saves to DB.

Bracket (main tab): Tournament name + format badge, tabs per stage, MatchCards (FC26 avatars, VS divider, status, score), Points Table with Q/E badges and animated row highlights, Qualification Tips accordion.

Match: FC26 cards fly in from sides, live score in Bebas Neue, Add Goal FAB opening GoalSheet (scorer selection + own goal toggle), goal log with long-press delete + undo snackbar, Confirm Full Time with mismatch warning dialog, multi-leg aggregate display, penalty shootout number picker if needed.

Stats: This Tournament tab (top scorers list + bar chart + standings) and All Time tab (global leaderboard + line chart + champion banner).

History: Tournament card list with expand animation showing full bracket + player breakdown + top scorer + delete button, search filter, empty state.

TournamentDetail: Full detailed view, bracket tree, player performance table, MVP section.

Ceremony: Full-screen trophy Lottie, sequenced winner reveal (name at 1200ms, badge at 1600ms, MVP at 2400ms, standings at 2800ms, buttons at 3200ms), Share button generates Canvas 1080x1920 PNG saved to MediaStore + share intent, New Tournament button.

Settings: Sound toggle, Haptic toggle, Theme segmented (Dark/Light/System), Clear API Cache button, Clear All Data with double-confirm (type "DELETE"), About section with version and GitHub link. All persisted via DataStore.

SECTION 10 — TOURNAMENT CREATION FLOW LOGIC

Shared CreateTournamentViewModel scoped to nested creation NavGraph. State class: name, date, matchesPerFixture, players (draft list), selectedFormat, autoSuggestedFormat, isLoading, error.

Format suggestion: 2 players = ROUND_ROBIN only; 3-4 = ROUND_ROBIN; 5 = ask user (ROUND_ROBIN or SEMIFINALS); 6 = ROUND_ROBIN suggested; 7 = ask user (SEMIFINALS or QUARTERFINALS); 8 = QUARTERFINALS suggested.

SECTION 11 — BRACKET ENGINE ALGORITHM

GROUP stage: generate N*(N-1)/2 unique pairs × matchesPerFixture legs. KO stubs: ROUND_ROBIN = 1 FINAL stub; SEMIFINALS = 2 SEMI + 1 FINAL; QUARTERFINALS = 4 QUARTER + 2 SEMI + 1 FINAL.

KO seeding: Seed1 vs Seed4, Seed2 vs Seed3 (SF); Seed1 vs Seed8, Seed2 vs Seed7, Seed3 vs Seed6, Seed4 vs Seed5 (QF). Fill TBD slots when GROUP clinched.

Early skip: After each GROUP match, if player clinches qualification, mark their remaining GROUP matches SKIPPED.

BO2 aggregate: away goals rule on draw; penalty shootout dialog if still equal.
BO3: best of 3 legs; 3rd leg only if 1-1; points awarded on fixture completion.

SECTION 12 — POINTS AND TIEBREAKER SYSTEM

Win=3, Draw=1, Loss=0 per fixture (not per leg). Tiebreaker: Goal Difference → Goals For → H2H result → H2H goal difference → seeded coin flip using tournamentId.

Qualification badges: Q (green) for top N, E (red) for eliminated players. Update reactively after every match.

SECTION 13 — LIVE MATCH ENTRY

MatchUiState: match, playerA, playerB, goals list, scoreA, scoreB, isConfirming, showScoreMismatchWarning, isCompleted.

Goal entry: FAB → GoalSheet → scorer selection with gold highlight → own goal toggle → Confirm inserts GoalEntity + updates MatchEntity score + plays ball_bounce Lottie 1.5s + haptic CONFIRM + plays goal_cheer sound.

Goal deletion: long press → ConfirmDialog → delete GoalEntity + decrement score + undo Snackbar 5s.

Full time confirm: validate goal count vs score, mismatch warning with override option, award points, play confetti Lottie + whistle sound + haptic LONG_PRESS, navigate back to Bracket.

Penalty shootout: number picker dialog for each player's penalty count, sudden death radio option, persist as special GoalEntity with scorerId=-99.

SECTION 14 — QUALIFICATION TIPS ENGINE

CalculateQualificationTipsUseCase: for each non-qualified non-eliminated player, simulate best-case (player wins all, max goals) and worst-case for rivals, check if qualification is reachable, generate human-readable tip text ("Win AND hope X drops points" / "Eliminated" / "One win away" / "You've qualified"). Tips update reactively after every match result. Display as expandable rows with crossfade animation on text change.

SECTION 15 — STATS AND LEADERBOARD

Per-player stats: total goals, own goals, goals conceded, goal difference, matches played, wins, draws, losses, win rate %, avg goals per match, tournaments entered, tournaments won, best finish, biggest win, most goals in a single match.

BarChart (MPAndroidChart): goals per player, gold bars, navy background, animateY 800ms EaseInOutQuart, no description, no legend.

LineChart (MPAndroidChart): win rate across tournaments, one line per player (5-color palette), filled area 30% alpha, animateXY 1000ms.

Top Scorers: crown icon #1, medal icons #2/#3.

SECTION 16 — ANIMATIONS AND SOUND

Lottie files: confetti.json (2s on match win), trophy_reveal.json (ceremony), ball_bounce.json (1.5s on goal), crowd_wave.json (tournament kick-off).

Compose animations: screen nav slideInHorizontally 300ms FastOutSlowIn; player card add spring(dampingRatio=0.6); match card flip rotationY 600ms; standing row animateColorAsState green/red; bracket stagger 80ms per item; score animateIntAsState count-up 300ms; ceremony shimmer gradient on text; FAB pulse 1.0→1.08→1.0 EaseInOut 1.2s.

Haptic: goal scored and match confirmed = HapticFeedbackType.LongPress; error = TextHandleMove.

Sound: MediaPlayer SoundManager @Singleton via Hilt. playGoalCheer() for goals, playWhistle() for match end. Check soundEnabled from DataStore before playing. Release MediaPlayers in Application.onTerminate().

SECTION 17 — CEREMONY AND SHARE CARD

Ceremony triggered when FINAL match is confirmed. Navigate with 600ms fadeIn from black.

Reveal sequence: 0ms trophy Lottie; 1200ms winner name (Bebas Neue 72sp gold, scale+fade); 1600ms team badge; 2000ms "Tournament Champion" label; 2400ms MVP card slides up; 2800ms standings list (80ms stagger); 3200ms buttons appear.

Share card: Canvas 1080x1920 Bitmap, navy background, COPA Arena logo, tournament name (Bebas Neue 96sp gold), winner section (CHAMPION label + name + team badge), divider, all-player results table (rank | name | team badge | pts | goals), footer "Generated with COPA Arena". Save to MediaStore.Images, launch ACTION_SEND share intent with image/* type.

SECTION 18 — SETTINGS AND DATASTORE

SettingsDataStore @Singleton: SOUND_ENABLED (Boolean, true), HAPTIC_ENABLED (Boolean, true), THEME_PREFERENCE (String: DARK/LIGHT/SYSTEM, default DARK). Expose as Flow<T> with suspend setter functions.

Apply theme in MainActivity: collect themePreference, map to darkTheme boolean, pass to CopaArenaTheme.

SECTION 19 — APP SIGNING (FULL STEPS)

Generate: keytool -genkey -v -keystore copa_arena_release.jks -alias copa_arena_key -keyalg RSA -keysize 2048 -validity 10000

Store credentials in local.properties only. Add copa_arena_release.jks and local.properties to .gitignore.

Build signed APK: ./gradlew assembleRelease → app/build/outputs/apk/release/app-release.apk
Build signed AAB: ./gradlew bundleRelease → app/build/outputs/bundle/release/app-release.aab

SECTION 20 — PROGUARD RULES AND DELIVERABLES

Include full ProGuard rules for: Retrofit (keepattributes Signature InnerClasses EnclosingMethod, keep @retrofit2.http.* interfaces), OkHttp (dontwarn okhttp3/okio), Room (keep RoomDatabase subclasses), Coil (dontwarn coil), Lottie (keep com.airbnb.lottie.**), MPAndroidChart (keep com.github.mikephil.charting.**), Gson (keep TypeAdapter/TypeAdapterFactory, keepclassmembers @SerializedName fields), Hilt (keepnames @HiltViewModel classes), data classes (keep com.copaarena.app.data.** and domain.model.**).

Final deliverables checklist:
- Complete Android Studio project (Kotlin DSL, Gradle 8.x)
- All Room entities, DAOs, AppDatabase with schema export
- Hilt modules: NetworkModule, DatabaseModule, RepositoryModule
- All 9 use cases with unit tests
- All 13 screens in Jetpack Compose
- All 8 shared Compose components
- NavGraph with all routes, transitions, deep links
- SettingsDataStore with all 3 preferences
- Futdb API integration with Room caching and offline fallback
- 5 Lottie JSON files (royalty-free from lottiefiles.com)
- 2 sound effect MP3 files in assets/sounds/
- 30 bundled club badge PNGs in assets/badges/
- Adaptive app icon (foreground + background vector drawables)
- copa_arena_release.jks keystore (generated, not in git)
- Signed release APK and AAB
- Complete proguard-rules.pro
- .gitignore covering keystore, local.properties, build/
- README.md with setup, API key config, keystore commands, build commands, deep link testing, screenshots guide