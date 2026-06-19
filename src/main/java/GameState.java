import java.util.ArrayList;
import java.util.List;

// The mutable state of a single UNO round plus the pure rule operations that drive it:
// dealing, turn advance, action-card effects, and round scoring. Holds no console I/O,
// so the whole turn model can be exercised in unit tests without a console.
public class GameState {

    public final List<GamePlayer> players;
    public final Deck deck;
    public final java.util.Random random;

    public int currentPlayer = 0;
    public int direction = 1;
    public String upCard = "";
    public String calledColor = "";

    public GameState(List<GamePlayer> players, java.util.Random random) {
        this.players = players;
        this.random = random;
        this.deck = new Deck(random);
    }

    public int playerCount() {
        return players.size();
    }

    public GamePlayer current() {
        return players.get(currentPlayer);
    }

    public String currentName() {
        return current().name();
    }

    public ArrayList<String> currentHand() {
        return current().hand();
    }

    public String draw() {
        return deck.draw();
    }

    public boolean deckHasCards() {
        return deck.hasCards();
    }

    // Builds a fresh deck, deals 7 cards each, flips a non-wild starting up card, and
    // picks a random starting player. Resets per-round flags.
    public void setupRound() {
        deck.buildFresh();
        for (GamePlayer p : players) {
            p.hand().clear();
            p.setCalledUno(false);
        }
        for (GamePlayer p : players) {
            for (int j = 0; j < 7; j++) {
                p.hand().add(deck.draw());
            }
        }
        upCard = deck.draw();
        while (upCard.startsWith("W")) {
            deck.discard(upCard);
            upCard = deck.draw();
        }
        calledColor = "";
        direction = 1;
        currentPlayer = random.nextInt(players.size());
    }

    public void advanceTurn() {
        currentPlayer += direction;
        if (currentPlayer >= players.size()) {
            currentPlayer = 0;
        }
        if (currentPlayer < 0) {
            currentPlayer = players.size() - 1;
        }
    }

    // Applies an action card's effect on the turn order / hands. Returns a ForcedDraw
    // describing any cards a player was forced to draw (so the caller can announce it),
    // or null for cards with no forced draw.
    public ForcedDraw applyEffect(String card) {
        String rank = Card.rank(card);
        if (rank.equals("SKIP")) {
            advanceTurn();
            advanceTurn();
            return null;
        } else if (rank.equals("REVERSE")) {
            direction = direction * -1;
            if (players.size() == 2) {
                advanceTurn();
                advanceTurn();
            } else {
                advanceTurn();
            }
            return null;
        } else if (rank.equals("DRAW_TWO")) {
            advanceTurn();
            currentHand().add(deck.draw());
            currentHand().add(deck.draw());
            ForcedDraw drew = new ForcedDraw(currentName(), 2);
            advanceTurn();
            return drew;
        } else if (rank.equals("WILD_DRAW_FOUR")) {
            advanceTurn();
            for (int i = 0; i < 4; i++) {
                currentHand().add(deck.draw());
            }
            ForcedDraw drew = new ForcedDraw(currentName(), 4);
            advanceTurn();
            return drew;
        } else {
            advanceTurn();
            return null;
        }
    }

    // Total point value of a player's remaining hand.
    public int handPoints(int playerIndex) {
        int total = 0;
        for (String c : players.get(playerIndex).hand()) {
            total += Card.points(c);
        }
        return total;
    }

    // Points a round winner scores: the sum of every other player's remaining hand.
    public int opponentsHandPoints(int winnerIndex) {
        int total = 0;
        for (int i = 0; i < players.size(); i++) {
            if (i != winnerIndex) {
                total += handPoints(i);
            }
        }
        return total;
    }

    // Index of the player holding the fewest hand points (lowest index breaks ties).
    // Used to resolve a round that ends without anyone emptying their hand.
    public int fewestCardsWinner() {
        int winner = 0;
        int fewest = Integer.MAX_VALUE;
        for (int i = 0; i < players.size(); i++) {
            int pts = handPoints(i);
            if (pts < fewest) {
                fewest = pts;
                winner = i;
            }
        }
        return winner;
    }

    // Cards a player was forced to draw by an action card, for the engine to announce.
    public static class ForcedDraw {
        public final String playerName;
        public final int count;

        public ForcedDraw(String playerName, int count) {
            this.playerName = playerName;
            this.count = count;
        }
    }
}
