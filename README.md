# Zombie Survival Game

A top-down 2D zombie survival shooter built with Java 17 and JavaFX. Zombies hunt the player with A* pathfinding, and user accounts and leaderboards are stored in a local SQLite database.

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-17-007396)
![SQLite](https://img.shields.io/badge/SQLite-JDBC-003B57?logo=sqlite&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-build-C71A36?logo=apachemaven&logoColor=white)

## Overview

A complete small game with a full app around it: accounts, character selection, a game loop, persistence and a leaderboard.

Features (all present in the code):

- **Accounts**: sign up and log in. Passwords are stored as SHA-256 hashes in SQLite.
- **Character selection**: 4 animated characters with different speed, health and damage. Three have a special ability on **E** (7 s active, 14 s cooldown):
  - **Jax**: Shield (+200 HP, up to 300, for 7 s)
  - **Vi**: Triple Shot (three piercing bullets in a spread)
  - **Flash**: Speed Boost
  - **Peter**: Healing ability.
- **Zombie AI**: each zombie runs **A\*** (4-directional, Manhattan heuristic, `com.zombie.ai.AStarPathfinder`) on a 30×20 tile grid and recalculates its path every 30 frames.
- **Waves and difficulty** (`Constants.java`):

  | Difficulty | Waves | Zombies per wave | Zombie damage per hit |
  |---|---|---|---|
  | Easy | 1 | 10 | 5 |
  | Medium | 5 | 20, 40, 60, 80, 100 | 8 |
  | Hard | 5 | 40, 80, 120, 160, 200 | 12 |

  There is a 5-second countdown between waves.
- **Combat and items**: aim with the mouse and shoot with **Space** (semi-automatic). Triple Shot bullets pass through up to 3 zombies. Health packs heal 25 HP. Killed zombies have a 15% chance to drop a weapon boost or armor (10 s each). The player has 3 lives.
- **Pause** (Esc) and a game-over / win screen.
- **Leaderboard** per difficulty: the top 10 players by fastest winning time, counting each player's best time, plus your rank after a win.
- Sprite animation (idle/walk frames) and a dark CSS theme.

## Tech stack

| Area | Choice | Notes |
|---|---|---|
| Language | Java 17 | Uses switch expressions and text blocks |
| UI / rendering | JavaFX 17 (`javafx-controls`), `Canvas` + `AnimationTimer` game loop | Screens: Login, Sign Up, Character Select, Home, Game, Leaderboard |
| Persistence | SQLite via `sqlite-jdbc` (WAL mode) | No database server needed; the DB file is created on first run |
| AI | Custom A* implementation | Grid-based, re-planned periodically for performance |
| Build | Maven + `javafx-maven-plugin` | `mvn javafx:run` handles the JavaFX module path |

## Demo / Screenshots

<!-- TODO: add a screenshot at docs/screenshot.png -->
![Screenshot](docs/screenshot.png)

*TODO for the owner: capture a gameplay screenshot (a wave in progress with the HUD visible) and the character-select screen, and save them under `docs/`.*

## Dataset

This project uses no dataset. Game data lives in a local SQLite file, **`zombie_game.db`**, which is **not included in the repository** (it's gitignored). It is created automatically in the **current working directory** the first time the game runs:

- `DBManager` runs `CREATE TABLE IF NOT EXISTS` for `characters`, `users` and `scores`.
- It seeds the four characters (Jax/SHIELD, Vi/PIERCING, Flash/SMOKE, Peter/HEALTH) if they're missing.

To reset all accounts and scores, delete `zombie_game.db` (and any `-shm` / `-wal` files next to it).

Sprites are bundled under `src/main/resources/assets/` (see Credits).

## Installation

Prerequisites: JDK 17+ and Maven 3.6+.

```bash
git clone https://github.com/aliyahseri/zombie-survival-game.git
cd zombie-survival-game
mvn clean compile
```

## Usage

```bash
mvn clean javafx:run
```

Main class: `com.zombie.app.MainApp`, configured in the `javafx-maven-plugin`. Run it from the project root so `zombie_game.db` is created there.

Controls:

| Key / input | Action |
|---|---|
| W A S D / arrow keys | Move |
| Mouse | Aim |
| Space | Shoot |
| E | Use character ability |
| Esc | Pause / resume |
| Q (while paused) | Quit to home screen |
| H (on game-over screen) | Return to home screen |

## Results

This is a game, so it has no model metrics. Run results are stored per user in the `scores` table. A run counts toward the leaderboard only if it's a win: 1 wave cleared on Easy, or 5 waves on Medium/Hard.

## Project structure

```
zombie-survival-game/
├── pom.xml
└── src/main/
    ├── java/com/zombie/
    │   ├── app/MainApp.java            # entry point -> ui.MainApp
    │   ├── ui/                         # MainApp (JavaFX Application, screen navigation), Login/SignUp/
    │   │                               # CharacterSelect/Home/Leaderboard screens
    │   ├── game/                       # Game (loop, waves, input, HUD), Player, Zombie, Bullet,
    │   │                               # HealthPack, ItemDrop, GameState
    │   ├── ai/AStarPathfinder.java     # grid A*
    │   ├── db/                         # DBManager (schema + seeding), UserDAO, CharacterDAO, ScoreDAO
    │   ├── model/                      # User, Character, Score
    │   ├── exception/                  # AuthException, DatabaseException, GameException
    │   └── util/                       # Constants, AssetLoader
    └── resources/
        ├── css/dark-theme.css
        └── assets/
            ├── characters/char1..char4/  # idle_0-5.png, walk_0-7.png per character
            ├── enemies/                  # zombie idle/walk frames
            ├── items/bullet.png
            └── ASSETS_LICENSE.txt        # CC0 license of the sprite pack
```

## Challenges & what I learned

- **Pathfinding at scale**: up to 200 zombies chasing the player means A* can't run every frame. Each zombie re-plans every 30 frames on a coarse tile grid.
- **Separating game state from UI**: a `GameState` singleton holds the selected character so the game, home and selection screens agree on it, with fallbacks to the user's saved selection in the database.
- **Layered data access**: DAO classes with custom checked exceptions (`AuthException`, `DatabaseException`) keep SQL out of the UI code.
- **Schema evolution**: `DBManager` adds new columns (`ability`, `last_selected_character`) to older database files with `ALTER TABLE` at startup.
- **Asset robustness**: images load from the classpath, and missing sprites fall back to colored shapes instead of crashing.
- [TODO: confirm: team size, your role, and what you personally built]

## Known issues & limitations

- **Shared connection closed on every DAO call**: every DAO method uses `try (Connection conn = dbManager.getConnection())`, which closes the singleton connection held by `DBManager`. `getConnection()` silently reopens it (re-running the PRAGMAs) on the next call, so it works, but a connection is opened and closed per query, and overlapping calls could close a connection another call is still using.
- **Weak password hashing**: `UserDAO.hashPassword` uses unsalted, single-round SHA-256 (with platform-default string encoding). Use bcrypt or Argon2 for anything real.
- **Peter's HEALTH ability does nothing**: `Player.applyCharacterStats` only handles character IDs 1-3, so Peter gets default stats and ability `NONE`.
- **Ability names differ between the DB and the game**: the DB stores `PIERCING` / `SMOKE`, while `Player` uses `TRIPLE_SHOT` / `SPEED_BOOST` (selected by character ID). The character-select screen therefore shows raw codes for some abilities.
- **Double file extensions**: `assets/characters/character_1.png.png` and `character_2.png.png`. They are not referenced by the code (sprites load from `char<N>/`), and `character_3/4` don't exist.
- **Missing optional item sprites**: `health_pack.png`, `weapon_boost.png`, `armor.png`, `wall.png` and `menu_bg.png` are referenced in `Constants` but not bundled, so those objects render as colored shapes. The seeded character `image_path` values (`character_N.png`) also don't resolve on the classpath.
- **Unused dependencies**: `javafx-fxml` and `slf4j-simple` are declared in `pom.xml` but never used.
- **Database location**: `zombie_game.db` is created in the working directory, not in a per-user data folder.
- **No automated tests.**

## Future improvements

- Use one long-lived connection (don't close it in DAOs) or a small connection pool.
- Switch to bcrypt/Argon2 password hashing with per-user salts.
- Implement Peter's HEALTH ability and unify ability codes between the database and `Player`.
- Clean up unused assets and dependencies; add the missing item sprites.
- Store the database in the user's app-data directory, and add unit tests for `AStarPathfinder` and the DAOs.
- Package a runnable fat JAR or jlink image for easy distribution.

## Credits / Acknowledgements

- Character, zombie and bullet sprites: **Free 2D Animated Vector Game Character Sprites** by Raphael Gonçalves ([Rgsdev](https://www.patreon.com/rgsdev)), **CC0 / public domain** (see `src/main/resources/assets/ASSETS_LICENSE.txt`).
- [OpenJFX](https://openjfx.io/), [sqlite-jdbc](https://github.com/xerial/sqlite-jdbc).
- Collaborators: [TODO: confirm]

## License

MIT, see [LICENSE](LICENSE). The bundled sprites are CC0 (see Credits).

## Author

**Aliyah Seri**
- GitHub: [aliyahseri](https://github.com/aliyahseri)
