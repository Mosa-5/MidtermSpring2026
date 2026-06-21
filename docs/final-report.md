# Final Project Report

A command-line UNO game in Java. It began as the midterm refactoring project (a
monolithic procedural `Main`), gained build/logging/Docker infrastructure in
Assignment 4 and ORM persistence in Assignment 5, and in the final project grew
fuller UNO rules, a tested rule core, and a clean separation between game logic
and the console.

## UNO rules implemented

All rule areas from the feature menu are implemented: a standard 108-card deck,
legal-play validation, Skip, Reverse, Draw Two, Wild, Wild Draw Four, draw/pass,
UNO call with a missed-UNO penalty, round scoring, and multi-round play to a
target score. A few intentional simplifications (no stacking, no Wild Draw Four
challenge, self-enforced UNO timing, Reverse-as-Skip in two-player games) are
listed in [`rules-supported.md`](rules-supported.md).

## Playing from the CLI

Build and run:

```bash
mvn package
java -jar target/uno-cli-1.0.0.jar --human --bots 2 --target 500
```

Common flags:

- `--bots N` — number of bot players (default 3)
- `--human` — add a human player named "You"
- `--games N` — play N independent rounds (default 1)
- `--target N` — match mode: play rounds until a player reaches N points, then
  declare the champion (e.g. `--target 500`)
- `--seed N` — deterministic shuffle for reproducible games
- `--quiet` — suppress per-turn output
- `--show-recent N` / `--show-wins NAME` / `--show-top N` — query saved history

During a human turn you enter a card by its code (e.g. `R5`, `G+2`, `W4`) or by
its hand index, or type `draw`. After a wild you are asked for a colour, and when
you reach one card you are prompted to type `UNO` (forgetting costs two cards).

## Architecture: game logic vs. CLI

The game logic is fully separated from console interaction, so rules can be
tested without any I/O:

- **`Card`, `Rules`** — pure, static card parsing and legal-play validation.
- **`Deck`** — draw/discard piles, shuffling, reshuffle-on-empty.
- **`GamePlayer`** — a player's name, hand, cumulative score, and UNO-call flag
  (replaced the old index-synced parallel arrays).
- **`GameState`** — the round model: players, deck, turn pointer, direction, up
  card, called colour, plus the rule operations `setupRound`, `advanceTurn`,
  `applyEffect`, and scoring helpers. No console code.
- **`BotStrategy`** — bot card and colour choices (pure functions of the hand).
- **`GameEngine`** — drives the turn loop and is the *only* layer that touches the
  console (`ConsoleView` for output, `ConsoleInput` for input) and the match flow
  (`playRound`, `targetReached`, `announceChampion`).
- **`ConsoleInput` + `InputSource`** — the input seam: production reads `System.in`;
  tests feed a queued `InputSource`, so human-input paths are testable without typing.
- **`Main`** — argument parsing, player setup, wiring, and the statistics CLI only.
- **Persistence** (`GameStatsRepository` + JPA entities `Game`/`Player`/`Score`) —
  each finished round is saved to an embedded H2 database via Hibernate; see
  [`database.md`](database.md).

Because state lives on instances (not static fields) and the only I/O sits in
`GameEngine`/`ConsoleView`/`ConsoleInput`, the rules, effects, scoring, and match
logic all run headless in unit tests.

## Tests

Tests run with `mvn test` (JUnit 5 via Surefire), with no manual classpath setup:

- `CardTest`, `RulesTest` — card helpers, scoring values, legal-play validation
- `DeckTest` — 108-card composition counts and draw-pile reshuffle/fallback
- `EffectsTest` — Skip, Reverse (2p and 3p), Draw Two, Wild Draw Four
- `BotStrategyTest` — card priority and colour tie-breaking
- `ConsoleInputTest` — draw/pass, card codes, colour and UNO prompts via the seam
- `UnoPenaltyTest` — the missed-UNO two-card penalty
- `MatchTest` — target detection, champion selection, cross-round score accumulation
- `GameRuntimeTest` — a seeded end-to-end check that every bot game (2–4 players ×
  50 seeds) finishes with a winner and a nonzero score
- `GameStatsRepositoryTest` — persistence against an isolated in-memory H2 database

These replaced the earlier 47-check `--self-test` harness; the same coverage now
lives in focused JUnit classes alongside the new feature tests.

## Limitations

- The simplifications in [`rules-supported.md`](rules-supported.md): no stacking,
  no Wild Draw Four challenge, Reverse-as-Skip with two players, self-enforced UNO
  timing, and starting action cards not applying their effect.
- The bot uses a fixed-priority heuristic, not adaptive strategy.
- Text-only CLI; no GUI.
- Match length is a fixed `--target` value rather than a configurable rule set.
