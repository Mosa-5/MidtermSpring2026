# Extension Readiness

## Which extension my design supports best

A smarter bot strategy. This is the extension the refactor makes easiest and the lowest risk, because the bot's decisions already live in their own object.

When I extracted `BotStrategy`, the bot stopped being scattered through the turn loop and stopped re-implementing the rules. It is now one object with two methods:

```java
int chooseCard(ArrayList<String> hand, String upCard, String calledColor)
String chooseColor(ArrayList<String> hand)
```

`Main` only touches it through a single field:

```java
static BotStrategy bot = new BotStrategy();
```

So a new strategy is an additive change, not a rewrite.

## Where the change would be implemented

1. Turn `BotStrategy` into an interface (or keep it as a base class) with the two methods above, and rename the current greedy logic to something like `GreedyBot`.
2. Add a new class, e.g. `MemoryBot implements BotStrategy`, with smarter card and color choices.
3. Pick the implementation in `Main`, either by changing the one `bot = ...` line, or by adding a `--bot-strategy greedy|memory` flag to the existing argument-parsing loop in `main()`.

No other class has to change. `Rules`, `Deck`, `Card`, `ConsoleView`, and `ConsoleInput` all stay the same, and the existing bot tests keep protecting the current behavior while a new strategy is added beside it.

## What still makes the change difficult

`Main` owns the turn loop and the game state. A strategy that wants to look ahead (for example, to ask how many cards the next player has and whether to hit them with a draw two) cannot see that. It only receives the hand, the up card, and the called color. A genuinely strong bot would need the loop to pass more of the game state (or a read-only view of it) into `chooseCard`.

`chooseColor` gets even less. It only sees the bot's own hand, so it cannot base the called color on what opponents are holding.

Every bot also uses the same single `BotStrategy` object, so a strategy that wants per-bot memory (tracking what each player has drawn or played) has nowhere to store it. Supporting that would mean giving each player its own strategy instance, which the parallel-array player model does not currently make easy.

Cards are still strings, so a strategy that wants to score how "useful" a card is has to go back through `Rules` and `Card` rather than asking a richer card object.
