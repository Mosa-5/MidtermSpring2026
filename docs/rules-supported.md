# Supported Rules

This document lists which rules from `Final_Project_UNO_rules_reference.md` are
implemented and where this project intentionally uses a simplified or variant rule.

## Card encoding

Cards are short strings: a colour prefix `R`/`Y`/`G`/`B` plus a value —
`0`–`9`, `S` (Skip), `R` (Reverse), `+2` (Draw Two) — e.g. `R5`, `BS`, `G+2`.
Wilds are `W` (Wild) and `W4` (Wild Draw Four).

## Implemented rules

| Rule | Status | Notes |
|------|--------|-------|
| Deck composition | Full | Standard 108-card deck (`Deck.buildFresh`): 4 colours, one `0` + two each `1–9` per colour, two each Skip/Reverse/Draw-Two per colour, 4 Wild, 4 Wild Draw Four. Verified by `DeckTest`. |
| Legal play validation | Full | Match by colour, number, action type, or wild; the called colour becomes active after a wild (`Rules.isLegal`). |
| Skip | Full | Next player loses their turn. |
| Reverse | Full (with documented 2p variant) | Flips direction for 3+ players; in a 2-player game Reverse acts like Skip (the player goes again). |
| Draw Two | Full | Next player draws two and is skipped. **No stacking** (see below). |
| Wild | Full | Player (human prompted, bot auto-chooses by hand majority) picks the active colour. |
| Wild Draw Four | Full | Player picks the colour; next player draws four and is skipped. **No challenge rule** (see below). |
| Draw / pass | Full | Variant: draw one card, **play it immediately if legal** (bots always do; humans are asked), otherwise pass. |
| UNO call + missed-UNO penalty | Full (self-enforced variant) | See timing note below. |
| Round end + scoring | Full | A round ends when a player empties their hand; the winner scores the sum of opponents' remaining card points (number = face value, Skip/Reverse/Draw-Two = 20, Wild/Wild-Draw-Four = 50). |
| Multi-round target score | Full | `--target N` plays rounds until a player reaches the target; the highest cumulative score is the champion. |

## Variants and simplifications

These intentionally differ from, or simplify, standard UNO. All affect visible
gameplay and are documented here per the rules reference.

- **Reverse in two-player games acts as Skip** — direction still flips, but the
  player who played it takes the next turn.
- **No Draw Two / Wild Draw Four stacking** — a forced draw is applied immediately;
  the target cannot respond with another draw card.
- **No Wild Draw Four challenge** — `W4` is always legal to play and is never challenged.
- **UNO call timing is self-enforced and immediate** — the one-card state is checked
  the moment a player reduces to one card after playing. A human is prompted to type
  `UNO`; a bot always calls. A player who reaches one card without calling immediately
  draws two penalty cards. There is no separate "catch the other player" window.
- **Starting action cards are not applied** — if the flipped starting card is a Wild
  or Wild Draw Four it is returned and a new card is drawn; if it is a Skip/Reverse/Draw-Two
  it stays as the up card but its effect is **not** applied — play simply begins on it.
- **Deck-exhaustion / turn-cap resolution** — beyond standard UNO, if the draw and
  discard piles are both empty (or a 3000-turn safety cap is hit) the round is resolved
  by awarding the win to the player holding the fewest hand points. This guarantees every
  round terminates with a winner.
- **Simple bot strategy** — bots play by a fixed priority (Draw Two > Skip > Number > Wild)
  and choose wild colours by hand majority.
- **Fixed target score via flag** — match length is set with `--target N` (e.g. `--target 500`);
  there is no default match mode (a plain run plays a single round unless `--games`/`--target` is given).
