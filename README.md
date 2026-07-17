# 🏆 COPA Arena

The app that finally settles who's the best on the couch.

You know the argument. Four controllers, one console, and someone always insists they'd have
won "if the ref wasn't cheating." COPA Arena exists so nobody can weasel out of the group stage
table ever again — brackets, standings, and stats, tracked automatically while you play.

Built fully offline. No accounts, no ads, no internet required to play a single match — your data
never leaves the phone.

## What it actually does

- **Real tournaments, not vibes** — Round Robin, Semifinals, or Quarterfinals formats, with real
  group-stage standings (points, goal difference, head-to-head tiebreaks) and automatic knockout
  seeding once qualifiers are decided.
- **661 real clubs, 51 real leagues** — pick your team from an actual FIFA-style database with
  real crests, not "Team A" and "Team B."
- **Match Center** — log goals (real scorer names included), own goals, penalty shootouts, full
  time — the app does the maths so you don't have to argue about it.
- **Sequential brackets** — no skipping ahead to dodge a match you're about to lose. Matches
  unlock in order. (You can still skip a fixture the app already knows is meaningless — it's not
  a monster.)
- **Stats that mean something** — top scorers by real player name, per-tournament and all-time
  leaderboards, win-rate charts, and a full goal-by-goal breakdown per player.
- **Multiple tournaments at once** — start a new one without losing your current grudge match.
- **Dark and Light themes** — because somebody always plays at 2am and somebody always plays at
  a barbecue.

## Tech

Kotlin + Jetpack Compose, Material 3, Room, Hilt, Coil, Lottie. Single-module Android app,
`minSdk 26`, no backend — the "server" is a bundled SQLite database.

## Running it locally

```bash
git clone https://github.com/Cap-Levi/COPA_Arena.git
cd COPA_Arena
./gradlew assembleDebug
```

Open in Android Studio like any other Gradle project if you'd rather click buttons than type them.

## Installing

Grab the latest signed APK from [Releases](https://github.com/Cap-Levi/COPA_Arena/releases) —
sideload it, and the app will tell you in Settings when a newer one drops.

## Credits

Created by **Levi**. Built with ❤️ (and a genuinely unreasonable number of hours arguing about
tiebreak rules) for FIFA couch gaming.

MIT licensed — see [LICENSE](LICENSE).
