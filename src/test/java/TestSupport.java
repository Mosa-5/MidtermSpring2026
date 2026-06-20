import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Shared helpers for the rule unit tests.
final class TestSupport {

    private TestSupport() {
    }

    static List<GamePlayer> bots(int n) {
        List<GamePlayer> players = new ArrayList<GamePlayer>();
        for (int i = 1; i <= n; i++) {
            players.add(new GamePlayer("Bot" + i, false));
        }
        return players;
    }

    // A GameState with empty-handed bots and a freshly built (108-card) deck, for
    // exercising effects/scoring without dealing a full round.
    static GameState builtDeckState(int n, long seed) {
        GameState gs = new GameState(bots(n), new Random(seed));
        gs.deck.buildFresh();
        return gs;
    }

    static ConsoleView quietView() {
        ConsoleView view = new ConsoleView();
        view.setQuiet(true);
        return view;
    }
}
