# Database & Persistence (Assignment 5)

This project persists every completed UNO game so that history and player
statistics can be queried later.

## Selected Database

**H2** — an embedded, file-based SQL database.

- Production/runtime: file-backed at `./data/uno` (`AUTO_SERVER=TRUE`), so the
  game history survives between runs. H2 creates `data/uno.mv.db` automatically
  on first run; no manual database install is required.
- Tests: in-memory (`jdbc:h2:mem:...`), a fresh isolated database per test.

H2 was chosen because it needs zero external setup, runs anywhere the JAR runs,
and is one of the databases explicitly allowed by the assignment.

## Selected ORM / Persistence Framework

**Hibernate** as the **Jakarta Persistence (JPA)** provider.

- Persistence unit `uno` is configured in
  [`src/main/resources/META-INF/persistence.xml`](../src/main/resources/META-INF/persistence.xml)
  (`RESOURCE_LOCAL` transactions).
- All database access goes through a single repository class,
  [`GameStatsRepository`](../src/main/java/GameStatsRepository.java). Game logic
  never contains raw SQL — it calls repository methods only.

Credentials are **not** hard-coded as secrets: the H2 dev account is the default
empty-password `sa` local account, and the JDBC URL/credentials live in
`persistence.xml`, not scattered through the game code.

## Schema

The schema is generated automatically by Hibernate from the JPA entities
(`hibernate.hbm2ddl.auto=update` at runtime, `create-drop` in tests), so there
is no manual migration step.

| Entity | Table | Key columns |
|--------|-------|-------------|
| [`Player`](../src/main/java/Player.java) | `players` | `id` (PK), `name` (unique, not null) |
| [`Game`](../src/main/java/Game.java) | `games` | `id` (PK), `started_at`, `ended_at`, `rounds_played`, `winner_id` → `players` |
| [`Score`](../src/main/java/Score.java) | `scores` | `id` (PK), `game_id` → `games`, `player_id` → `players`, `score_value` |

Relationships:

- `games.winner_id` → `players.id` (many games can be won by one player)
- `scores.game_id` → `games.id`, `scores.player_id` → `players.id`
  (one row per player per game, holding that player's final score)

This covers all required entities: **players, games, rounds** (`rounds_played`),
**scores**, **winner**, and **timestamp** (`started_at` / `ended_at`).

## What Gets Persisted

When a game finishes, `Main` calls `GameStatsRepository.saveGame(...)`, which in
one transaction stores:

- player names (created on first sight, reused afterwards)
- game start and end timestamps
- number of rounds played
- each player's final score for that game
- the winner

## Querying Game History & Statistics

The three report features are exposed as CLI flags on the packaged JAR. They read
the persisted `./data/uno` database and print to stdout (they do not play a game).

```bash
# List the N most recent games (most recent first)
java -jar target/uno-cli-1.0.0.jar --show-recent 10

# Show how many games a given player has won
java -jar target/uno-cli-1.0.0.jar --show-wins Alice

# Show the N highest individual scores ever recorded
java -jar target/uno-cli-1.0.0.jar --show-top 10
```

These map to `findRecentGames`, `findPlayerWinCount`, and `findTopScores` in
`GameStatsRepository`. To generate data first, play some games:

```bash
java -jar target/uno-cli-1.0.0.jar --bots 3 --games 5 --quiet --seed 42
```

## Running the Persistence Tests

The persistence layer is tested in
[`src/test/java/GameStatsRepositoryTest.java`](../src/test/java/GameStatsRepositoryTest.java).
Each test spins up its own in-memory H2 database (a unique
`jdbc:h2:mem:test-<uuid>` URL with `hibernate.hbm2ddl.auto=create-drop`), so the
tests are fully isolated and never touch the developer's `./data/uno` file or any
machine-specific state.

Run them through Maven (no manual classpath setup needed):

```bash
mvn test
```

The five tests cover:

- `saveGame` round-trips timestamps, rounds played, winner, and per-player scores
- `findRecentGames` orders most-recent-first and honors the limit
- `findPlayerWinCount` counts wins for the named player only
- `findTopScores` orders descending and honors the limit
