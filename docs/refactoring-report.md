# Refactoring Report

## What I characterized before refactoring

Before changing any design, I grew the existing `--self-test` mode into a real characterization suite (commit `added characterization tests`). The original `selfTest()` had 9 checks; I replaced the throw-on-first-failure helper with a `check(name, condition)` helper that reports every failure and exits non-zero so `scripts/test.sh` actually fails on a regression.

The suite documents the behavior the current game actually has, including the quirks, not an ideal version of UNO. It covers:

- matching by color, by number, and by action type (skip on skip, etc.)
- wild and wild draw four always being legal
- a card matching the called color after a wild
- scoring values (number = face, skip/reverse/draw-two = 20, wilds = 50)
- bot card priority (draw two before skip before number before wild)
- bot color choice and its R > Y > G > B tie-breaking
- drawing from the deck, including the reshuffle when the draw pile is empty and the `"W"` fallback when both piles are empty
- the card effects: skip, reverse (both the 3-player case and the 2-player "reverse acts as skip" case), draw two, and wild draw four
- human input quirks: typing `draw` while holding a legal card, typing a card code, and an illegal code causing a re-prompt

The effect and input tests only became possible after I extracted the seams described below, so the suite grew alongside the refactor. The final count is 47 checks, all runnable with `scripts/test.sh`.

## Worst design problems I found

One class held everything. `Main` had eleven static fields acting as global mutable game state, and a `playGame()` method of about 190 lines that did deck building, dealing, the turn loop, input, validation, effects, scoring, and printing.

The legality logic was duplicated. The same five-clause "is this card legal" check appeared four times: once in `isLegal()` and three more times copied inside `chooseBotCard()`. Any rule change would have to be made in four places.

Cards were raw strings like `"R5"`, `"G+2"`, `"W4"`, and every place that needed the color, rank, number, or points re-parsed the string itself.

The console was mixed into the rules. `System.out.println` calls (guarded by a `quiet` flag) and a `Scanner` were used directly inside the game logic, so the rules could not run without the console.

The bot logic was tangled with rule knowledge. The bot re-implemented the legality check instead of asking the rules.

## Which refactorings I performed

I worked in small steps, one concern per commit, and kept the tests green after each one:

1. `extracted Card helpers`: moved `color/rank/number/points` into a `Card` class; `Main` keeps thin delegates so nothing else had to change yet.
2. `extracted Deck class`: moved the draw pile, discard pile, shuffling, reshuffle, and the `"W"` fallback into a `Deck` object built with the seeded `Random`. The 21-line deck-building loop became `deck.buildFresh()`.
3. `extracted Rules and removed duplicated legal-play checks`: moved the single legality check into `Rules.isLegal` and replaced all four copies with a call to it. There is now exactly one definition of the rule.
4. `extracted BotStrategy`: moved `chooseCard` and `chooseColor` into a `BotStrategy` object that takes the game state it needs as parameters.
5. `split playGame into setupRound and applyEffect`: pulled round setup and the card-effect switch out of the turn loop, which let me finally test the effects directly.
6. `extracted ConsoleView for game output`: every game message moved into a `ConsoleView` that owns the `quiet` flag, so the rules no longer print.
7. `extracted ConsoleInput with InputSource seam`: moved `askHuman`/`askColor` into `ConsoleInput`, behind a one-method `InputSource` interface. Production wires a `Scanner`; the tests wire a small queue, which is what makes the input quirks testable without real typing.
8. `renamed for clarity`: `next()` became `advanceTurn()`, the `humanPlayers` flag list became `isHuman`, and the loop's `guard` became `safetyCounter`.

The refactorings used were mostly Extract Class, Extract Method, and one Extract Interface (`InputSource`), plus removing the duplicated conditional.

## What I intentionally preserved

The point was to keep behavior identical, so I kept the documented quirks and wrote tests for them:

- humans may type `draw` even when they have a legal play
- typing a card code for an illegal card re-prompts, but an index pointing at an illegal card is treated as a bad move (penalty card and lost turn)
- bots automatically play a drawn card when it is legal
- 2-player reverse behaves like a skip
- `draw()` reshuffles the discard pile when the draw pile is empty, and returns a literal `"W"` when both are empty
- the first up card is never a wild
- "UNO!" prints when a hand reaches one card
- the 3000-turn safety limit
- the scoring values and the rule that the winner scores the sum of the other hands

## What risks remain

`Main` still holds the live game state in static fields and still owns the turn loop. Because the state is global and static, you could not run two games at once, and the turn body (choose action, then play the card) is still a fairly long block inside `playGame()` that I did not fully extract.

There is no `Player` or `Hand` type. The player name, the `isHuman` flag, and the hand are still three parallel lists kept in sync by index.

`applyEffect` is still a `switch`-style chain. Adding a brand new card effect means editing that method rather than adding a class. I left it this way on purpose because a small switch is easier to read here than a class per effect, but it is a real limit if the effects grow.

Cards are still strings, so a malformed code would not be rejected.

The `"W"` fallback in `draw()` is a real playable wild, so in the rare case both piles empty it slightly changes play. This is preserved, not fixed.
