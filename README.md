# UNO CLI

A standalone CLI UNO-like game in Java. Originally built as a monolithic procedural class for the midterm refactoring assignment; converted to a standard Maven project with logging and Docker support in Assignment 4.

## Requirements

- JDK 17 or newer
- Maven 3.9+
- Docker (only if you want to run inside a container)

## Local commands

Build:

```
mvn compile
```

Run tests:

```
mvn test
```

This runs the 47 characterization checks via `Main --self-test`. The build fails if any check fails.

Package as a runnable jar:

```
mvn package
```

Produces `target/uno-cli-1.0.0.jar`.

Run:

```
java -jar target/uno-cli-1.0.0.jar --bots 3 --games 1 --quiet --seed 42
```

Or interactively:

```
java -jar target/uno-cli-1.0.0.jar --human --bots 2 --games 1
```

## Docker

Build the image:

```
docker build -t uno-cli .
```

Run a bot game:

```
docker run --rm uno-cli --bots 3 --games 1 --quiet --seed 42
```

Run an interactive game (needs `-it` so stdin stays attached):

```
docker run --rm -it uno-cli --human --bots 2 --games 1
```

## CLI options

| Flag | Meaning |
|------|---------|
| `--bots N` | Number of bot players (default 3) |
| `--games N` | Number of games to play (default 1) |
| `--human` | Add a human player |
| `--quiet` | Suppress per-turn game output |
| `--seed N` | Deterministic shuffle seed |
| `--show-recent N` | Print the N most recent games and exit |
| `--show-wins NAME` | Print how many games NAME has won and exit |
| `--show-top N` | Print the N highest recorded scores and exit |
| `--self-test` | Run the characterization test suite |
| `--help` | Print usage |

## Card input examples

When playing as a human, you can enter cards by code or by index:

```
R5    red 5
YS    yellow skip
BR    blue reverse
G+2   green draw two
W     wild
W4    wild draw four
draw  draw a card
```

## Logging

Game events (game start, player turn, card played, card drawn, invalid input, round/game end) are logged via `java.util.logging` to stderr. Player-facing game output goes to stdout. Redirect stderr if you want a clean console:

```
java -jar target/uno-cli-1.0.0.jar --bots 3 --games 1 2>/dev/null
```

## Persistence & statistics

Completed games are stored in an embedded **H2** database via **Hibernate/JPA**, so game history and player stats persist between runs. View them with:

```
java -jar target/uno-cli-1.0.0.jar --show-recent 10
java -jar target/uno-cli-1.0.0.jar --show-wins Alice
java -jar target/uno-cli-1.0.0.jar --show-top 10
```

See [`docs/database.md`](docs/database.md) for the schema, configuration, and how to run the persistence tests.

## Project documents

- `docs/database.md` — database, ORM, schema, and persistence-test docs
- `docs/rules.html` — implemented game rules
- `docs/refactoring-report.md` — midterm refactoring report
- `docs/extension-readiness.md` — extension readiness note
